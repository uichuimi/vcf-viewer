package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFConstants;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import org.uichuimi.variant.viewer.filter.BaseFilter;
import org.uichuimi.variant.viewer.index.Indexer;
import org.uichuimi.variant.viewer.index.VcfIndex;
import org.uichuimi.variant.viewer.io.VariantContextPipe;
import org.uichuimi.variant.viewer.utils.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VariantsTable {

	@FXML
	@SuppressWarnings("unused")
	private BorderPane variantFilters;
	@FXML
	@SuppressWarnings("unused")
	private VariantFilters variantFiltersController;
	@FXML
	@SuppressWarnings("unused")
	private BorderPane variantDetails;
	@FXML
	@SuppressWarnings("unused")
	private VariantDetails variantDetailsController;
	@FXML
	private Label totalVariants;
	@FXML
	private Label filteredVariants;
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
	private TableColumn<VariantContext, Set<String>> filters;
	@FXML
	private TableColumn<VariantContext, Double> quality;
	@FXML
	private Label placeholder;

	private VcfIndex index;
	private File file;
	private VariantContextPipe reader;


	public void setFile(final File file) {
		this.file = file;
		placeholder.setText("No data in " + file.getAbsolutePath());
		if (VCFFileReader.isBCF(file) && isGzipped(file)) {
			MainView.error("Cannot read compressed BCF files, please decompress with bcftools view -Ou -o output.bcf input.bcf");
			return;
		}
		initSubPanels();
		index();
		reload();
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
		final Indexer indexer = new Indexer(file);
		indexer.setOnSucceeded(workerStateEvent -> {
			index = indexer.getValue();
			variantFiltersController.setMetadata(index);
			totalVariants.setText("Total variants: %,d".formatted(index.getLineCount()));
		});
		indexer.setOnFailed(event -> MainView.error(indexer.getException()));
		MainView.launch(indexer);
	}

	private void initSubPanels() {
		try (VCFFileReader reader = new VCFFileReader(file, false)) {
			final VCFHeader header = reader.getHeader();
			header.getInfoHeaderLines().stream().map(this::createInfoColumn).forEach(variantsTable.getColumns()::add);
			variantFiltersController.setMetadata(header);
			variantDetailsController.setHeader(header);
		} catch (Exception e) {
			e.printStackTrace();
			MainView.error(e);
		}
	}

	private void reload() {
		reload(null);
	}

	private void reload(final File output) {
		variantsTable.getItems().clear();
		if (reader != null) reader.cancel();
		final Long lineCount = index == null ? null : index.getLineCount();
		final List<BaseFilter> filterList = variantFiltersController.getFilters();
		reader = new VariantContextPipe(file, output, filterList, 2000, lineCount);
		reader.filteredProperty().addListener((obs, old, filtered) -> updateFiltered(filtered.intValue()));
		variantsTable.setItems(reader.getVariants());
		MainView.launch(reader);
	}

	private TableColumn<VariantContext, String> createInfoColumn(VCFInfoHeaderLine info) {
		final TableColumn<VariantContext, String> column = new TableColumn<>(info.getID());
		column.setCellValueFactory(param -> {
			final Object value = param.getValue().getAttribute(info.getID());
			final String rtn = value == null || value.equals(VCFConstants.EMPTY_ID_FIELD)
				? Constants.EMPTY_CELL
				: String.valueOf(value);
			return new SimpleObjectProperty<>(rtn);
		});
		column.setCellFactory(col -> new NaturalCell<>());
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
		alternate.setCellFactory(column -> new AlleleCell());
		filters.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getFilters()));
		filters.setCellFactory(column -> new FiltersCell());
		quality.setCellValueFactory(features -> new SimpleObjectProperty<>(features.getValue().getPhredScaledQual()));
		quality.setCellFactory(column -> new NumberCell(2));
		variantsTable.getSelectionModel().selectedItemProperty().addListener((obs, prev, variant) -> select(variant));
		variantsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		variantsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		variantFiltersController.setOnReload(() -> Platform.runLater(this::reload));
	}


	private void select(final VariantContext variant) {
		variantDetailsController.set(variant);
	}

	public void save(File file) {
		reload(file);
	}

	private void updateFiltered(int i) {
		Platform.runLater(() -> filteredVariants.setText("Filtered variants: %,d".formatted(i)));
	}

}
