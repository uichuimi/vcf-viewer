package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFConstants;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFInfoHeaderLine;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.uichuimi.variant.viewer.filter.Filter;
import org.uichuimi.variant.viewer.filter.VariantContextFilter;
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
	private VBox propertyFiltersPane;
	@FXML
	private VBox genotypeFiltersPane;
	@FXML
	private SplitPane filtersDataPane;
	@FXML
	private ToggleButton showPropertyFilters;
	@FXML
	private ToggleButton showGenotypeFilters;
	@FXML
	private SplitPane center;
	@FXML
	private VBox propertiesPane;
	@FXML
	private VBox genotypesPane;
	@FXML
	private SplitPane additionalDataPane;
	@FXML
	private ToggleButton showGenotypes;
	@FXML
	private ToggleButton showProperties;
	@FXML
	private Label totalVariants;
	@FXML
	private Label filteredVariants;
	@FXML
	private BorderPane propertyFilters;
	@FXML
	private PropertyFilters propertyFiltersController;
	@FXML
	private BorderPane genotypeFilters;
	@FXML
	private GenotypeFilters genotypeFiltersController;
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
			propertyFiltersController.setMetadata(index);
			genotypeFiltersController.setMetadata(index);
			totalVariants.setText("Total variants: %,d".formatted(index.getLineCount()));
		});
		indexer.setOnFailed(event -> MainView.error(indexer.getException()));
		MainView.launch(indexer);
	}

	private void initSubPanels() {
		try (VCFFileReader reader = new VCFFileReader(file, false)) {
			final VCFHeader header = reader.getHeader();
			header.getInfoHeaderLines().stream().map(this::createInfoColumn).forEach(variantsTable.getColumns()::add);
			propertyFiltersController.setMetadata(header);
			genotypeFiltersController.setMetadata(header);
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
		final List<VariantContextFilter> filterList = List.of(genotypeFiltersController.getFilter(), propertyFiltersController.getFilter());
		reader = new VariantContextPipe(file, output, filterList, 50, lineCount);
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
		propertyFiltersController.filterList().addListener((ListChangeListener<Filter>) change -> Platform.runLater(this::reload));
		genotypeFiltersController.setOnFilter(() -> Platform.runLater(this::reload));
		showProperties.selectedProperty().addListener((ons, prev, selected) -> showProperties(selected));
		showGenotypes.selectedProperty().addListener((ons, prev, selected) -> showGenotypes(selected));
		showPropertyFilters.selectedProperty().addListener((ons, prev, selected) -> showPropertyFilters(selected));
		showGenotypeFilters.selectedProperty().addListener((ons, prev, selected) -> showGenotypeFilters(selected));
	}

	private void showProperties(Boolean selected) {
		if (selected) {
			if (!additionalDataPane.getItems().contains(propertiesPane)) {
				additionalDataPane.getItems().add(0, propertiesPane);
			}
		} else {
			additionalDataPane.getItems().remove(propertiesPane);
		}
		hideOrShowAdditionalPane();
	}

	private void showGenotypes(Boolean selected) {
		if (selected) {
			if (!additionalDataPane.getItems().contains(genotypesPane)) {
				additionalDataPane.getItems().add(genotypesPane);
			}
		} else {
			additionalDataPane.getItems().remove(genotypesPane);
		}
		hideOrShowAdditionalPane();
	}

	private void hideOrShowAdditionalPane() {
		if (additionalDataPane.getItems().isEmpty()) {
			center.getItems().remove(additionalDataPane);
		} else {
			if (!center.getItems().contains(additionalDataPane)) {
				center.getItems().add(additionalDataPane);
			}
		}
	}

	private void showPropertyFilters(Boolean selected) {
		if (selected) {
			if (!filtersDataPane.getItems().contains(propertyFiltersPane)) {
				filtersDataPane.getItems().add(0, propertyFiltersPane);
			}
		} else {
			filtersDataPane.getItems().remove(propertyFiltersPane);
		}
		hideOrShowFiltersPane();
	}

	private void showGenotypeFilters(Boolean selected) {
		if (selected) {
			if (!filtersDataPane.getItems().contains(genotypeFiltersPane)) {
				filtersDataPane.getItems().add(genotypeFiltersPane);
			}
		} else {
			filtersDataPane.getItems().remove(genotypeFiltersPane);
		}
		hideOrShowFiltersPane();
	}

	private void hideOrShowFiltersPane() {
		if (filtersDataPane.getItems().isEmpty()) {
			center.getItems().remove(filtersDataPane);
		} else {
			if (!center.getItems().contains(filtersDataPane)) {
				center.getItems().add(0, filtersDataPane);
			}
		}
	}

	private void select(final VariantContext variant) {
		propertiesController.select(variant);
		genotypesController.select(variant);
	}

	public void save(File file) {
		reload(file);
	}

	private void updateFiltered(int i) {
		Platform.runLater(() -> filteredVariants.setText("Filtered variants: %,d".formatted(i)));
	}

}
