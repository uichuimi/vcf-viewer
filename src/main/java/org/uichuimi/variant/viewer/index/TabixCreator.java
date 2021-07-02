package org.uichuimi.variant.viewer.index;

import htsjdk.tribble.index.Index;
import htsjdk.tribble.index.tabix.TabixFormat;
import htsjdk.tribble.index.tabix.TabixIndexCreator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import org.uichuimi.variant.viewer.utils.ResourceConsumer;

import java.io.File;
import java.io.IOException;

class TabixCreator implements ResourceConsumer<VCFHeader, VariantContext> {

	private final TabixIndexCreator tabixIndexCreator = new TabixIndexCreator(TabixFormat.VCF);
	private final File vcfFile;

	TabixCreator(File vcfFile) {this.vcfFile = vcfFile;}

	@Override
	public void start(VCFHeader vcfHeader) {
		tabixIndexCreator.setIndexSequenceDictionary(vcfHeader.getSequenceDictionary());
	}

	@Override
	public void consume(VariantContext variantContext, long position) {
		tabixIndexCreator.addFeature(variantContext, position);
	}

	@Override
	public void finnish(long position) throws IOException {
		final Index index = tabixIndexCreator.finalizeIndex(position);
		index.writeBasedOnFeatureFile(vcfFile);
	}

}
