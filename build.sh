/home/pascual/.jdks/openjdk-15.0.2/bin/jpackage \
	--input target/jmods/ \
	--main-jar vcf-viewer-1.0-SNAPSHOT.jar \
	--main-class org.uichuimi.variant.viewer.Main \
	--name vcf-viewer \
	--add-modules javafx.fxml,javafx.base,javafx.graphics,org.controlsfx.controls \
	--module-path target/jmods
