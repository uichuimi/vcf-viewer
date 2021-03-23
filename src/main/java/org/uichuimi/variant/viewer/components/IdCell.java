package org.uichuimi.variant.viewer.components;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFConstants;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableCell;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

class IdCell extends TableCell<VariantContext, String> {

	@Override
	protected void updateItem(final String id, final boolean empty) {
		super.updateItem(id, empty);
		setText(null);
		setGraphic(null);
		if (!empty && id != null) {
			if (id.startsWith("rs")) {
				setGraphic(getUrlHyperlink(id));
			} else {
				setText(id.equals(VCFConstants.EMPTY_ID_FIELD) ? "-" : id);
			}
		}
	}

	private Hyperlink getUrlHyperlink(final String id) {
		final String href = "https://www.ncbi.nlm.nih.gov/snp/" + id;
		final Hyperlink hyperlink = new Hyperlink(id);
		if (Desktop.isDesktopSupported()) {
			hyperlink.setOnAction(actionEvent -> {
				// Call to Desktop.browse must be done outside main thread, otherwise it will block
				new Thread(() -> {
					try {
						Desktop.getDesktop().browse(new URL(href).toURI());
					} catch (IOException | URISyntaxException e) {
						MainView.error(e);
						e.printStackTrace();
					}
				}).start();
			});
		}
		return hyperlink;
	}
}
