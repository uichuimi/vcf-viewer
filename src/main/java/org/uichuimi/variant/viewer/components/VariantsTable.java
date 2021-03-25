package org.uichuimi.variant.viewer.components;

import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.index.tabix.TabixIndexCreator;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import org.uichuimi.variant.VcfIndex;
import org.uichuimi.variant.viewer.utils.Chromosome;
import org.uichuimi.variant.viewer.utils.GenomeProgress;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class VariantsTable {

	@FXML
	private BorderPane filters;
	@FXML
	private Filters filtersController;
	@FXML
	private BorderPane properties;
	@FXML
	private PropertiesTable propertiesController;
	@FXML
	private BorderPane genotypes;
	@FXML
	private GenotypesTable genotypesController;

	@FXML
	private TableView<VariantContext> variantsTable;
	@FXML
	private TableColumn<VariantContext, String> snpId;
	@FXML
	private TableColumn<VariantContext, VariantContext> coordinate;
	@FXML
	private TableColumn<VariantContext, String> reference;
	@FXML
	private TableColumn<VariantContext, String> alternate;
	@FXML
	private Label placeholder;

	private VCFHeader header;
	private boolean queryable;
	private boolean gzipped;
	private File file;
	private VcfIndex index;


	public void setFile(final File file) {
		this.file = file;
		placeholder.setText("No data in " + file.getAbsolutePath());
		gzipped = isGzipped(file);
		if (VCFFileReader.isBCF(file) && gzipped) {
			MainView.error("Cannot read compressed BCF files, please decompress with bcftools view -Ou -o output.bcf input.bcf");
			return;
		}
		index();
		fill();
	}

	private boolean isGzipped(final File file) {
		if (file.getName().endsWith(".gz")) return true;
		try (InputStream is = new FileInputStream(file)) {
			for (int b : new int[]{0x1F, 0x8B}) {
				if (is.read() != b) return false;
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			MainView.error(e);
		}
		return false;
	}

	private void index() {
		try (VCFFileReader reader = new VCFFileReader(file, false)) {
			header = reader.getHeader();
			queryable = reader.isQueryable();
			header.getInfoHeaderLines().stream().map(this::createInfoColumn).forEach(variantsTable.getColumns()::add);
			filtersController.setMetadata(header, queryable);
		} catch (Exception e) {
			MainView.error(e);
		}
		final Indexer indexer = new Indexer(file);
		MainView.launch(indexer);
		indexer.setOnSucceeded(workerStateEvent -> {
			final VcfIndex index = indexer.getValue();
			filtersController.setMetadata(index);
		});
	}

	private void fill() {
		try (VCFFileReader reader = new VCFFileReader(file, false)) {
			int read = 0;
			for (final VariantContext context : reader) {
				variantsTable.getItems().add(context);
				read += 1;
				if (read >= 20) break;
			}
		}
	}

	private TableColumn<VariantContext, String> createInfoColumn(VCFInfoHeaderLine info) {
		final TableColumn<VariantContext, String> column = new TableColumn<>(info.getID());
		column.setCellValueFactory(param -> {
			final Object value = param.getValue().getAttribute(info.getID());
			return new SimpleObjectProperty<>(value == null ? VCFConstants.EMPTY_ID_FIELD : value.toString());
		});
		column.setVisible(false);
		return column;
	}

	@FXML
	private void initialize() {
		snpId.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getID()));
		snpId.setCellFactory(column -> new IdCell());
		coordinate.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue()));
		coordinate.setCellFactory(column -> new CoordinateCell());
		reference.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getReference().getBaseString()));
		reference.setCellFactory(column -> new AlleleCell());
		alternate.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getAlternateAlleles().stream().map(Allele::toString).collect(Collectors.joining(","))));
		variantsTable.getSelectionModel().selectedItemProperty().addListener((obs, prev, variant) -> select(variant));
		variantsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		variantsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
	}

	private void select(final VariantContext variant) {
		propertiesController.select(variant);
		genotypesController.select(variant);
	}

	private static class Indexer extends Task<VcfIndex> {

		private static final int LIMIT = 75;
		private final File file;

		public Indexer(final File file) {
			this.file = file;
		}

		@Override
		protected VcfIndex call() {
			final Map<String, Set<String>> options = new TreeMap<>();
			maybeTabix();
			try (VCFFileReader reader = new VCFFileReader(file, false)) {
				final Chromosome.Namespace namespace = Chromosome.Namespace.guess(reader.getHeader());
				reader.getHeader().getInfoHeaderLines().stream()
					.filter(line -> line.getType() == VCFHeaderLineType.String)
					.forEach(line -> options.put(line.getID(), new TreeSet<>()));
				int line = 0;
				for (final VariantContext variant : reader) {
					// Add new options
					for (final String id : options.keySet()) {
						final Set<String> set = options.get(id);
						final Object value = variant.getCommonInfo().getAttribute(id);
						if (value == null) continue;
						final List<Object> values;
						if (value instanceof List) {
							values = (List<Object>) value;
						} else {
							values = List.of(value);
						}
						for (final Object o : values) {
							set.add((String) o);
						}
					}
					// Check too long options
					for (final String id : new ArrayList<>(options.keySet())) {
						if (options.get(id).size() > LIMIT) {
							options.remove(id);
						}
					}
					if (line++ % 1000 == 0) {
						updateProgress(GenomeProgress.getProgress(variant, namespace), 1);
						updateMessage("Indexing " + variant.getContig() + " : " + variant.getStart());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			options.forEach((id, opts) -> System.out.printf("%s (%d) = %s%n", id, opts.size(), opts));
			return new VcfIndex(options);
		}

		private void maybeTabix()  {
			try (VCFFileReader reader = new VCFFileReader(file, false)) {
				if (!reader.isQueryable()) {
					System.out.println("File needs index");
					final TabixIndexCreator tabixIndexCreator = new TabixIndexCreator(TabixFormat.VCF);
					long pos = 0;
					tabixIndexCreator.setIndexSequenceDictionary(reader.getHeader().getSequenceDictionary());
					for (final VariantContext variantContext : reader) {
						tabixIndexCreator.addFeature(variantContext, pos++);
					}
					final Index index = tabixIndexCreator.finalizeIndex(pos);
					index.writeBasedOnFeatureFile(file);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
