package gov.usgs.earthquake.distribution;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class DeflateComparisonTest {

	@Test
	public void testLargeQuakeml() throws IllegalArgumentException, IOException {
		new DeflateComparison().testFile(new File(
				"etc/test_products/quakeml/pde20100314080803960_32.xml"));
	}

	@Test
	public void testShakemap() throws IllegalArgumentException, IOException {
		new DeflateComparison().testFile(new File(
				"etc/test_products/usa00040xz/us_shakemap_usa00040xz_1287260900624.bin"));
	}

	@Test
	public void testPager() throws IllegalArgumentException, IOException {
		new DeflateComparison().testFile(new File(
				"etc/test_products/usa00040xz/us_losspager_usa00040xz_1287260989064.bin"));
	}

}
