package org.uichuimi.variant.viewer.io;

import org.junit.jupiter.api.Test;

import java.io.File;

class DbReaderTest {

	@Test
	public void testDbReader() {
		final File input = new File(DbReaderTest.class.getResource("/vcf/simple.vcf").getFile());

		new DbReader(input);
	}
}
