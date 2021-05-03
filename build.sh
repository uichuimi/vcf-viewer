#!/bin/bash
# This will create target/vcf-viewer-{version}-jar-with-dependencies.jar
mvn clean compile assembly:single

# Add all dependencies (as jars) to target/jmods
mvn package
mvn dependency:copy-dependencies -DoutputDirectory=target/jmods/

# Create vcf-viewer_1.0-1_amd64.deb (or exe in windows)
# jpackage v15 is needed
jpackage \
	--input target/jmods/ \
	--main-jar vcf-viewer-1.0-SNAPSHOT.jar \
	--main-class org.uichuimi.variant.viewer.Main \
	--name vcf-viewer \
	--add-modules javafx.fxml,javafx.base,javafx.graphics,org.controlsfx.controls \
	--module-path target/jmods
