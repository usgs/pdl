package gov.usgs.earthquake.eids;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.XmlProductHandler;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;

public class EQMessageProductCreatorTest {

	@Test
	public void testQuarryEvent() throws Exception {
		EQMessageProductCreator creator = new EQMessageProductCreator();
		List<Product> products = creator.getEQMessageProducts(new String(
				FileUtils.readFile(new File(
						"etc/test_products/eqxml/eqxml_quarry.xml"))));

		Assert.assertTrue("one product created", products.size() == 1);
		Product origin = products.get(0);
		Map<String, String> properties = origin.getProperties();
		Assert.assertEquals("product event type is quarry", "quarry",
				properties.get("event-type"));
	}

	@Test
	public void testSonicBoomEvent() throws Exception {
		EQMessageProductCreator creator = new EQMessageProductCreator();
		List<Product> products = creator.getEQMessageProducts(new String(
				FileUtils.readFile(new File(
						"etc/test_products/eqxml/eqxml_sonicboom.xml"))));

		Assert.assertTrue("one product created", products.size() == 1);
		Product origin = products.get(0);
		new ObjectProductSource(origin).streamTo(new XmlProductHandler(
				new StreamUtils.UnclosableOutputStream(System.err)));

		Map<String, String> properties = origin.getProperties();
		Assert.assertEquals("product event type is sonicboom", "sonicboom",
				properties.get("event-type"));
	}

	/**
	 * This test case reproduces an issue where two updates for the same event
	 * are sent in the same second, but at different milliseconds.
	 * 
	 * The correct behavior is for this to result in two separate products. The
	 * original behavior ended up with the same product id on each product,
	 * which is considered a duplicate.
	 * 
	 * @throws Exception
	 */
	@Test
	public void akSameSecondTest() throws Exception {
		// start with eqmessage products
		String rawMessage1 = "<EQMessage xmlns=\"http://www.usgs.gov/ansseqmsg\"><Source>AK</Source><Module>QuakeWatch Server, Version 1.0</Module><Sent>2012-09-18T16:06:06.565Z</Sent><Event><DataSource>AK</DataSource><EventID>10561248</EventID><Version>1</Version><Action>Update</Action><Type>Earthquake</Type><Usage>Actual</Usage><Scope>Public</Scope><Origin><Time>2012-09-18T01:44:47.000Z</Time><Latitude>56.9446</Latitude><Longitude>-154.1804</Longitude><Depth>9.2</Depth><StdError>0.92</StdError><AzimGap>68.4</AzimGap><MinDist>0.38537726</MinDist><Errh>0.3</Errh><Errz>0.5</Errz><NumPhaUsed>138</NumPhaUsed><NumStaUsed>141</NumStaUsed><Status>Automatic</Status><PreferredFlag>true</PreferredFlag><Magnitude><TypeKey>Mb</TypeKey><Value>6.0</Value><PreferredFlag>true</PreferredFlag><Comment><TypeKey>CUBE_Code</TypeKey><Text>CUBE_Code B</Text></Comment></Magnitude><Method><Class>Carol Johnson stack</Class><Algorithm>Binder</Algorithm><Comment><TypeKey>CUBE_Code</TypeKey><Text>CUBE_Code A</Text></Comment></Method></Origin></Event></EQMessage>";
		String rawMessage2 = "<EQMessage xmlns=\"http://www.usgs.gov/ansseqmsg\"><Source>AK</Source><Module>QuakeWatch Server, Version 1.0</Module><Sent>2012-09-18T16:06:06.603Z</Sent><Event><DataSource>AK</DataSource><EventID>10561248</EventID><Version>2</Version><Action>Update</Action><Type>Earthquake</Type><Usage>Actual</Usage><Scope>Public</Scope><Origin><Time>2012-09-18T01:44:49.000Z</Time><Latitude>56.8835</Latitude><Longitude>-154.0788</Longitude><Depth>48.1</Depth><StdError>0.85</StdError><AzimGap>129.6</AzimGap><MinDist>0.32878339</MinDist><Errh>0.7</Errh><Errz>0.1</Errz><NumPhaUsed>93</NumPhaUsed><NumStaUsed>86</NumStaUsed><Status>Reviewed</Status><PreferredFlag>true</PreferredFlag><Magnitude><TypeKey>Mb</TypeKey><Value>5.5</Value><PreferredFlag>true</PreferredFlag><Comment><TypeKey>CUBE_Code</TypeKey><Text>CUBE_Code B</Text></Comment></Magnitude><Method><Class>Carol Johnson stack</Class><Algorithm>Binder</Algorithm><Comment><TypeKey>CUBE_Code</TypeKey><Text>CUBE_Code a</Text></Comment></Method></Origin></Event></EQMessage>";

		// convert eqxml to products
		EQMessageProductCreator creator = new EQMessageProductCreator();
		List<Product> message1Products = creator
				.getEQMessageProducts(rawMessage1);
		List<Product> message2Products = creator
				.getEQMessageProducts(rawMessage2);
		// extract origin products from list
		Product message1Origin = message1Products.get(0);
		Product message2Origin = message2Products.get(0);

		// print created products to the screen for inspection
		new ObjectProductSource(message1Origin).streamTo(new XmlProductHandler(
				new StreamUtils.UnclosableOutputStream(System.err)));
		new ObjectProductSource(message2Origin).streamTo(new XmlProductHandler(
				new StreamUtils.UnclosableOutputStream(System.err)));

		// verify they generate different ids
		Assert.assertTrue("messages have different product ids",
				!message1Origin.getId().equals(message2Origin.getId()));
	}

	@Test
	public void mtTest() throws Exception {
		String rawMessage = "<EQMessage xmlns=\"http://www.usgs.gov/ansseqmsg\"><Source>US</Source><Sender>USGS, NEIS, Golden, Colorado (and predecessors)</Sender><Module>Bulletin Hydra Version 1.7.4</Module><MsgSrc>US</MsgSrc><Sent>2012-01-26T20:36:33.357Z</Sent><Event><DataSource>US</DataSource><EventID>C0007RAM</EventID><Version>A</Version><Action>Update</Action><Type>Earthquake</Type><Origin><MomentTensor><SourceKey>NEIC</SourceKey><TypeKey>Mwr</TypeKey><Action>Update</Action><MagMw>4.867</MagMw><M0>25100000000000000</M0><Tensor><Mrr>-7041000000000000</Mrr><Mtt>14838000000000000</Mtt><Mpp>-7797000000000000</Mpp><Mrt>-17950000000000000</Mrt><Mrp>-1719000000000000</Mrp><Mtp>-11910000000000000</Mtp></Tensor><NodalPlanes><Fault><Strike>335.94</Strike><Slip>-16.98</Slip><Dip>37.74</Dip></Fault><Fault><Strike>79.52</Strike><Slip>-126.51</Slip><Dip>79.7</Dip></Fault></NodalPlanes><DerivedDepth>26.00</DerivedDepth><PerDblCpl>56.93</PerDblCpl><NumObs>30</NumObs><PreferredFlag>true</PreferredFlag></MomentTensor></Origin></Event></EQMessage>";

		EQMessageProductCreator creator = new EQMessageProductCreator();
		List<Product> messageProducts = creator
				.getEQMessageProducts(rawMessage);
		Product mtProduct = messageProducts.get(0);
		Assert.assertEquals("product type", "moment-tensor", mtProduct.getId()
				.getType());
	}

	@Test
	public void testPreserveOriginalContent() throws Exception {
		EQMessageProductCreator creator = new EQMessageProductCreator();
		String rawEqxml = new String(FileUtils.readFile(new File(
				"etc/test_products/eqxml/eqxml_event.xml")));
		List<Product> products = creator.getEQMessageProducts(rawEqxml);

		Assert.assertTrue("raw eqxml contains newlines",
				rawEqxml.indexOf("\n") != -1);
		Product origin = products.get(0);
		String productEqxml = new String(StreamUtils.readStream(origin
				.getContents().get("eqxml.xml").getInputStream()));
		Assert.assertEquals("content is unchanged in product", rawEqxml,
				productEqxml);
	}

}
