module vcf.viewer {

	requires javafx.graphics;
	requires javafx.fxml;
	requires javafx.controls;
	requires htsjdk;
	requires java.desktop;
	requires org.controlsfx.controls;
	requires org.apache.commons.lang3;
	requires commons.io;

	opens org.uichuimi.variant.viewer to javafx.graphics, javafx.fxml, javafx.controls;
	opens org.uichuimi.variant.viewer.components to javafx.graphics, javafx.fxml, javafx.controls;

	exports org.uichuimi.variant.viewer;
}
