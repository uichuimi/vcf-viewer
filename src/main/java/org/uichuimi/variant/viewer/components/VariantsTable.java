package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFConstants;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import org.uichuimi.variant.VcfIndex;
import org.uichuimi.variant.viewer.components.filter.Filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
			filtersController.setMetadata(header);
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
		variantsTable.getItems().clear();
		try (VCFFileReader reader = new VCFFileReader(file, false)) {
			int read = 0;
			int filtered = 0;
			for (final VariantContext context : reader) {
				if (filtersController.filter(context)) {
					variantsTable.getItems().add(context);
					filtered++;
					if (filtered >= 20) break;
				}
				read += 1;
//				if (read >= 100) break;
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
		filtersController.filter().addListener((ListChangeListener<Filter>) change -> fill());

	}

	private void select(final VariantContext variant) {
		propertiesController.select(variant);
		genotypesController.select(variant);
	}

}
