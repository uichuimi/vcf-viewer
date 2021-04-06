package org.uichuimi.variant.viewer.action;

import javafx.stage.FileChooser;
import org.uichuimi.variant.viewer.Main;

import java.io.File;

public class FileActions {

	public static final FileChooser.ExtensionFilter VCF = new FileChooser.ExtensionFilter("Variant Call Format", "*.vcf", "*.vcf.gz", "*.bcf");

	public static File openVcf() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Select VCF file");
		chooser.setInitialDirectory(new File(System.getProperty("user.home")));
		chooser.getExtensionFilters().addAll(VCF);
		chooser.setSelectedExtensionFilter(VCF);
		return chooser.showOpenDialog(Main.getWindow());
	}

	public static File saveVcf() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save VCF as");
		chooser.setInitialDirectory(new File(System.getProperty("user.home")));
		chooser.getExtensionFilters().addAll(VCF);
		chooser.setSelectedExtensionFilter(VCF);
		return chooser.showSaveDialog(Main.getWindow());
	}
}
