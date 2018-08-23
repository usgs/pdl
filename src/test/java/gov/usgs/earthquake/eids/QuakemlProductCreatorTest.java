package gov.usgs.earthquake.eids;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.quakeml_1_2.Quakeml;

import gov.usgs.earthquake.cube.CubeDelete;
import gov.usgs.earthquake.cube.CubeEvent;
import gov.usgs.earthquake.cube.CubeMessage;
import gov.usgs.earthquake.event.Converter;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.ObjectProductSource;
import gov.usgs.earthquake.product.io.XmlProductHandler;
import gov.usgs.util.FileUtils;
import gov.usgs.util.StreamUtils;

public class QuakemlProductCreatorTest {

	@Test
	public void test() throws Exception {
		testFromCube(CubeEvent.EXAMPLE1);
		testFromCube(CubeEvent.EXAMPLE2);
		testFromCube(CubeDelete.EXAMPLE);
	}

	public void testFromCube(final String cube) throws Exception {
		Converter converter = new Converter();

		CubeMessage message = CubeMessage.parse(cube);
		System.err.println("CUBE");
		System.err.println(converter.getString(message));

		Quakeml quakeml = converter.getQuakeml(message);
		System.err.println("Quakeml");
		System.err.println(converter.getString(quakeml));

		List<Product> products = new QuakemlProductCreator()
				.getQuakemlProducts(quakeml);
		Iterator<Product> iter = products.iterator();
		while (iter.hasNext()) {
			Product product = iter.next();
			System.err.println("Product");
			new ObjectProductSource(product).streamTo(new XmlProductHandler(
					new StreamUtils.UnclosableOutputStream(System.err)));
			System.err.println();
		}
	}

	/**
	 * Get a list of products for external tests to use.
	 * 
	 * @return
	 * @throws Exception
	 */
	List<Product> getTestQuakemlProducts() throws Exception {
		Converter converter = new Converter();
		QuakemlProductCreator productCreator = new QuakemlProductCreator();
		List<Product> products = new LinkedList<Product>();

		String[] cubes = new String[] { CubeEvent.EXAMPLE1, CubeEvent.EXAMPLE2,
				CubeDelete.EXAMPLE };
		for (String cube : cubes) {
			CubeMessage message = CubeMessage.parse(cube);
			Quakeml quakeml = converter.getQuakeml(message);
			products.addAll(productCreator.getQuakemlProducts(quakeml));
		}

		return products;
	}

	@Test
	public void testFocalMechanism() throws Exception {
		Converter converter = new Converter();
		QuakemlProductCreator productCreator = new QuakemlProductCreator();
		List<Product> products = new LinkedList<Product>();

		byte[] bytes = FileUtils.readFile(new File(
				"etc/test_products/quakeml/quakeml_mt_usb00083i6.xml"));
		Quakeml quakeml = converter.getQuakeml(new String(bytes));
		products.addAll(productCreator.getQuakemlProducts(quakeml));

		Iterator<Product> iter = products.iterator();
		while (iter.hasNext()) {
			Product product = iter.next();
			System.err.println("Product");
			new ObjectProductSource(product).streamTo(new XmlProductHandler(
					new StreamUtils.UnclosableOutputStream(System.err)));
			System.err.println();
		}
	}

	@Test
	public void testPDEMomentTensor() throws Exception {
		Converter converter = new Converter();
		QuakemlProductCreator productCreator = new QuakemlProductCreator();
		List<Product> products = new LinkedList<Product>();

		byte[] bytes = FileUtils.readFile(new File(
				"etc/test_products/quakeml/pde20100314080803960_32.xml"));
		Quakeml quakeml = converter.getQuakeml(new String(bytes));
		products.addAll(productCreator.getQuakemlProducts(quakeml));

		Iterator<Product> iter = products.iterator();
		while (iter.hasNext()) {
			Product product = iter.next();
			System.err.println("Product");
			new ObjectProductSource(product).streamTo(new XmlProductHandler(
					new StreamUtils.UnclosableOutputStream(System.err)));
			System.err.println();
		}
	}

	@Test
	public void testPDEMomentTensor2() throws Exception {
		Converter converter = new Converter();
		QuakemlProductCreator productCreator = new QuakemlProductCreator();
		productCreator.setSendOriginWhenPhasesExist(true);

		List<Product> products = new LinkedList<Product>();

		byte[] bytes = FileUtils.readFile(new File(
				"etc/test_products/quakeml/pde20000102042021390_33.xml"));
		Quakeml quakeml = converter.getQuakeml(new String(bytes));
		products.addAll(productCreator.getQuakemlProducts(quakeml));

		Iterator<Product> iter = products.iterator();
		while (iter.hasNext()) {
			Product product = iter.next();
			System.err.println("Product");
			new ObjectProductSource(product).streamTo(new XmlProductHandler(
					new StreamUtils.UnclosableOutputStream(System.err)));
			System.err.println();
		}
	}

	@Test
	public void testFocalMechanismInline() throws Exception {
		Converter converter = new Converter();
		QuakemlProductCreator productCreator = new QuakemlProductCreator();
		List<Product> products = productCreator
				.getQuakemlProducts(converter
						.getQuakeml("<q:quakeml xmlns=\"http://quakeml.org/xmlns/bed/1.2\" xmlns:catalog=\"http://anss.org/xmlns/catalog/0.1\" xmlns:q=\"http://quakeml.org/xmlns/quakeml/1.2\"><eventParameters publicID=\"quakeml:nc.anss.org/catalog/NC/1/134998653120\"><event catalog:eventid=\"71856040\" catalog:eventsource=\"NC\" catalog:dataid=\"NC71856040_fm1\" catalog:datasource=\"NC\" publicID=\"quakeml:nc.anss.org/Event/NC/71856040\"><focalMechanism catalog:eventid=\"71856040\" catalog:eventsource=\"NC\" catalog:dataid=\"71856040_fm1\" catalog:datasource=\"NC\" publicID=\"quakeml:nc.anss.org/Mec/NC/1302154\"><waveformID locationCode=\"--\" channelCode=\"EHZ\" stationCode=\"GAXB\" networkCode=\"NC\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"AL6\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"HVC\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"CLV\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"STY\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"EHZ\" stationCode=\"GAC\" networkCode=\"NC\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"MCL\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"EHZ\" stationCode=\"GBG\" networkCode=\"NC\"></waveformID><waveformID locationCode=\"--\" channelCode=\"EHZ\" stationCode=\"GMK\" networkCode=\"NC\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"NEG\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DP1\" stationCode=\"JKB\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"BRP\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"ACR\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"HER\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"FUM\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"AL4\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"AL5\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"02\" channelCode=\"EHZ\" stationCode=\"GSG\" networkCode=\"NC\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"SQK\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"02\" channelCode=\"EHZ\" stationCode=\"GCR\" networkCode=\"NC\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"DRK\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"HBW\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"BUC\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"HHZ\" stationCode=\"GDXB\" networkCode=\"NC\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"FNF\" networkCode=\"BG\"></waveformID><waveformID locationCode=\"--\" channelCode=\"DPZ\" stationCode=\"RGP\" networkCode=\"BG\"></waveformID><triggeringOriginID>quakeml:nc.anss.org/Origin/NC/7566034</triggeringOriginID><nodalPlanes><nodalPlane1><strike><value>35.0</value><uncertainty>25.0</uncertainty></strike><dip><value>50.0</value><uncertainty>15.0</uncertainty></dip><rake><value>-80.0</value><uncertainty>20.0</uncertainty></rake></nodalPlane1><nodalPlane2><strike><value>200.0</value><uncertainty>25.0</uncertainty></strike><dip><value>41.0</value><uncertainty>15.0</uncertainty></dip><rake><value>-102.0</value><uncertainty>20.0</uncertainty></rake></nodalPlane2></nodalPlanes><stationPolarityCount>26</stationPolarityCount><misfit>0.04</misfit><methodID>FPFIT</methodID><creationInfo><agencyID>NC</agencyID><creationTime>2012-10-11T20:15:28.000Z</creationTime><version>1</version></creationInfo><evaluationStatus>preliminary</evaluationStatus></focalMechanism><magnitude catalog:eventid=\"71856040\" catalog:eventsource=\"NC\" catalog:dataid=\"NC71856040_fm1\" catalog:datasource=\"NC\" publicID=\"quakeml:nc.anss.org/Netmag/NC/4519619\"><mag><value>0.62</value><uncertainty>0.057</uncertainty></mag><type>Md</type><originID>quakeml:nc.anss.org/Origin/NC/7566034</originID><methodID>HypoinvMd</methodID><stationCount>7</stationCount><azimuthalGap>193.8</azimuthalGap><evaluationMode>manual</evaluationMode><evaluationStatus>reviewed</evaluationStatus><creationInfo><agencyID>NC</agencyID><creationTime>2012-10-11T20:14:56.000Z</creationTime><version>1</version></creationInfo></magnitude><origin catalog:eventid=\"71856040\" catalog:eventsource=\"NC\" catalog:dataid=\"NC71856040_fm1\" catalog:datasource=\"NC\" publicID=\"quakeml:nc.anss.org/Origin/NC/7566034\"><originUncertainty><horizontalUncertainty>170</horizontalUncertainty><preferredDescription>horizontal uncertainty</preferredDescription></originUncertainty><time><value>2012-10-09T20:20:34.500Z</value></time><longitude><value>-122.8018333</value></longitude><latitude><value>38.8403333</value></latitude><depth><value>2080</value></depth><depthType>from location</depthType><timeFixed>false</timeFixed><epicenterFixed>false</epicenterFixed><methodID>HYP2000</methodID><type>hypocenter</type><evaluationMode>manual</evaluationMode><evaluationStatus>final</evaluationStatus><creationInfo><agencyID>NC</agencyID><creationTime>2012-10-11T20:14:55.000Z</creationTime><version>1</version></creationInfo></origin><type>earthquake</type><creationInfo><agencyID>NC</agencyID><creationTime>2012-10-11T20:14:56.000Z</creationTime><version>2</version></creationInfo></event></eventParameters></q:quakeml>"));

		Iterator<Product> iter = products.iterator();
		while (iter.hasNext()) {
			Product product = iter.next();
			System.err.println("Product");
			new ObjectProductSource(product).streamTo(new XmlProductHandler(
					new StreamUtils.UnclosableOutputStream(System.err)));
			System.err.println();
		}

	}

	@Test
	public void testMomentTensorInline() throws Exception {
		Converter converter = new Converter();
		QuakemlProductCreator productCreator = new QuakemlProductCreator();
		List<Product> products = productCreator
				.getQuakemlProducts(converter
						.getQuakeml("<q:quakeml xmlns=\"http://quakeml.org/xmlns/bed/1.2\" xmlns:catalog=\"http://anss.org/xmlns/catalog/0.1\" xmlns:q=\"http://quakeml.org/xmlns/quakeml/1.2\"><eventParameters publicID=\"quakeml:nc.anss.org/catalog/NC/1/134764754255\"><event catalog:eventid=\"71842075\" catalog:eventsource=\"NC\" catalog:dataid=\"NC71842075_mt1\" catalog:datasource=\"NC\" publicID=\"quakeml:nc.anss.org/Event/NC/71842075\"><focalMechanism catalog:eventid=\"71842075\" catalog:eventsource=\"NC\" catalog:dataid=\"NC71842075_mt1\" catalog:datasource=\"NC\" publicID=\"quakeml:nc.anss.org/Mec/NC/4427\"><momentTensor catalog:eventid=\"71842075\" catalog:eventsource=\"NC\" catalog:dataid=\"NC71842075_mt1\" catalog:datasource=\"NC\" publicID=\"quakeml:nc.anss.org/Mec/NC/4427\"><dataUsed><stationCount>4</stationCount><componentCount>12</componentCount><shortestPeriod>20.00</shortestPeriod></dataUsed><derivedOriginID>quakeml:nc.anss.org/Origin/NC/1891197</derivedOriginID><momentMagnitudeID>quakeml:nc.anss.org/Netmag/NC/733157</momentMagnitudeID><scalarMoment><value>5438000000000000</value></scalarMoment><tensor><Mrr><value>-472600000000000</value></Mrr><Mtt><value>-4968000000000000</value></Mtt><Mpp><value>5441000000000000</value></Mpp><Mrt><value>-1093000000000000</value></Mrt><Mrp><value>-116700000000000</value></Mrp><Mtp><value>1076000000000000</value></Mtp></tensor><varianceReduction>0.9</varianceReduction><doubleCouple>0.91</doubleCouple><iso>0</iso><greensFunctionID>quakeml:nc.anss.org/tdmt/NC/gil7_</greensFunctionID><methodID>TMTS</methodID><category>regional</category><inversionType>zero trace</inversionType><evaluationMode>manual</evaluationMode><creationInfo><agencyID>NC</agencyID><creationTime>2012-09-14T18:24:29.000Z</creationTime><version>1</version></creationInfo></momentTensor><triggeringOriginID>quakeml:nc.anss.org/Origin/NC/1308405</triggeringOriginID><principalAxes><tAxis><azimuth><value>96.083</value></azimuth><plunge><value>2.201</value></plunge><length><value>5560000000000000</value></length></tAxis><pAxis><azimuth><value>5.595</value></azimuth><plunge><value>12.491</value></plunge><length><value>-5318000000000000</value></length></pAxis><nAxis><azimuth><value>195.911</value></azimuth><plunge><value>77.310</value></plunge><length><value>-244000000000000</value></length></nAxis></principalAxes><creationInfo><agencyID>NC</agencyID><creationTime>2012-09-14T18:24:29.000Z</creationTime><version>1</version></creationInfo></focalMechanism><magnitude catalog:eventid=\"71842075\" catalog:eventsource=\"NC\" catalog:dataid=\"NC71842075_mt1\" catalog:datasource=\"NC\" publicID=\"quakeml:nc.anss.org/Netmag/NC/733157\"><mag><value>4.43</value></mag><type>Mw</type><originID>quakeml:nc.anss.org/Origin/NC/1308405</originID><methodID>TMTS</methodID><stationCount>4</stationCount><evaluationMode>manual</evaluationMode><creationInfo><agencyID>NC</agencyID><creationTime>2012-09-14T18:24:28.000Z</creationTime><version>1</version></creationInfo></magnitude><origin catalog:eventid=\"71842075\" catalog:eventsource=\"NC\" catalog:dataid=\"NC71842075_mt1\" catalog:datasource=\"NC\" publicID=\"quakeml:nc.anss.org/Origin/NC/1308405\"><originUncertainty><horizontalUncertainty>380</horizontalUncertainty><preferredDescription>horizontal uncertainty</preferredDescription></originUncertainty><time><value>2012-09-14T11:53:17.710Z</value></time><longitude><value>-124.1681671</value></longitude><latitude><value>40.4448318</value></latitude><depth><value>23710</value></depth><depthType>from location</depthType><timeFixed>false</timeFixed><epicenterFixed>false</epicenterFixed><methodID>BINDER</methodID><type>hypocenter</type><evaluationMode>manual</evaluationMode><evaluationStatus>reviewed</evaluationStatus><creationInfo><agencyID>NC</agencyID><creationTime>2012-09-14T11:56:51.000Z</creationTime><version>1</version></creationInfo></origin><origin catalog:eventid=\"71842075\" catalog:eventsource=\"NC\" catalog:dataid=\"NC71842075_mt1\" catalog:datasource=\"NC\" publicID=\"quakeml:nc.anss.org/Origin/NC/1891197\"><time><value>2012-09-14T11:53:17.710Z</value></time><longitude><value>-124.1681976</value></longitude><latitude><value>40.4448013</value></latitude><depth><value>24000</value></depth><depthType>from moment tensor inversion</depthType><timeFixed>false</timeFixed><epicenterFixed>false</epicenterFixed><methodID>TMTS</methodID><type>hypocenter</type><evaluationMode>manual</evaluationMode><creationInfo><agencyID>NC</agencyID><creationTime>2012-09-14T18:24:29.000Z</creationTime><version>1</version></creationInfo></origin><preferredMagnitudeID>quakeml:nc.anss.org/Netmag/NC/733157</preferredMagnitudeID><preferredFocalMechanismID>quakeml:nc.anss.org/Mec/NC/4427</preferredFocalMechanismID><type>earthquake</type><creationInfo><agencyID>NC</agencyID><creationTime>2012-09-14T18:24:29.000Z</creationTime><version>7</version></creationInfo></event></eventParameters></q:quakeml>"));

		Iterator<Product> iter = products.iterator();
		while (iter.hasNext()) {
			Product product = iter.next();
			System.err.println("Product");
			new ObjectProductSource(product).streamTo(new XmlProductHandler(
					new StreamUtils.UnclosableOutputStream(System.err)));
			System.err.println();
		}

	}

	/**
	 * Expect an exception to be thrown if horizontal uncertainty is specified
	 * as the preferred description of the origin uncertainty, but no horizontal
	 * uncertainty is listed.
	 */
	@Test
	public void testHorizontalUncertainty() throws Exception {
		Converter converter = new Converter();
		QuakemlProductCreator productCreator = new QuakemlProductCreator();

		try {
			productCreator
					.getQuakemlProducts(converter
							.getQuakeml("<q:quakeml xmlns=\"http://quakeml.org/xmlns/bed/1.2\" xmlns:catalog=\"http://anss.org/xmlns/catalog/0.1\" xmlns:anss=\"http://anss.org/xmlns/event/0.1\" xmlns:q=\"http://quakeml.org/xmlns/quakeml/1.2\"><eventParameters publicID=\"quakeml:us.anss.org/eventParameters/meav/1400519295742\"><event publicID=\"quakeml:us.anss.org/event/meav\" catalog:datasource=\"us\" catalog:eventsource=\"us\" catalog:eventid=\"meav\"><magnitude publicID=\"quakeml:us.anss.org/magnitude/meav/Mb\"><mag><value>5.4</value></mag><type>Mb</type><originID>quakeml:us.anss.org/origin/meav</originID><methodID>quakeml:anss.org/cube/magnitudeType/B</methodID><stationCount>8</stationCount></magnitude><origin publicID=\"quakeml:us.anss.org/origin/meav\"><originUncertainty><preferredDescription>horizontal uncertainty</preferredDescription></originUncertainty><time><value>1999-04-02T18:38:19.500Z</value></time><longitude><value>168.1247</value></longitude><latitude><value>-20.1884</value></latitude><depth><value>33000</value></depth><depthType>operator assigned</depthType><quality><usedPhaseCount>19</usedPhaseCount><usedStationCount>19</usedStationCount><standardError>0.62</standardError><minimumDistance>2.050854</minimumDistance></quality><evaluationMode>automatic</evaluationMode></origin><preferredOriginID>quakeml:us.anss.org/origin/meav</preferredOriginID><preferredMagnitudeID>quakeml:us.anss.org/magnitude/meav/Mb</preferredMagnitudeID><type>earthquake</type><creationInfo><agencyID>us</agencyID><creationTime>2014-05-19T17:08:15.742Z</creationTime><version>3</version></creationInfo></event><creationInfo><creationTime>2014-05-19T17:08:15.742Z</creationTime></creationInfo></eventParameters></q:quakeml>"));
			Assert.fail();
		} catch (IllegalArgumentException e) {
			if (!e.getMessage().equals("Missing horizontal uncertainty value")) {
				throw e;
			}
		}
	}

	/**
	 * Expect an impact text product to be created when a felt report event
	 * description exists.
	 */
	@Test
	public void testFeltReport() throws Exception {
		Converter converter = new Converter();
		QuakemlProductCreator productCreator = new QuakemlProductCreator();

		List<Product> products = productCreator
				.getQuakemlProducts(converter
						.getQuakeml("<q:quakeml xmlns=\"http://quakeml.org/xmlns/bed/1.2\" xmlns:catalog=\"http://anss.org/xmlns/catalog/0.1\" xmlns:anss=\"http://anss.org/xmlns/event/0.1\" xmlns:q=\"http://quakeml.org/xmlns/quakeml/1.2\"><eventParameters publicID=\"quakeml:us.anss.org/eventParameters/meav/1400519295742\"><event publicID=\"quakeml:us.anss.org/event/meav\" catalog:datasource=\"us\" catalog:eventsource=\"us\" catalog:eventid=\"meav\"><magnitude publicID=\"quakeml:us.anss.org/magnitude/meav/Mb\"><mag><value>5.4</value></mag><type>Mb</type><originID>quakeml:us.anss.org/origin/meav</originID><methodID>quakeml:anss.org/cube/magnitudeType/B</methodID><stationCount>8</stationCount></magnitude><origin publicID=\"quakeml:us.anss.org/origin/meav\"><time><value>1999-04-02T18:38:19.500Z</value></time><longitude><value>168.1247</value></longitude><latitude><value>-20.1884</value></latitude><depth><value>33000</value></depth><depthType>operator assigned</depthType><quality><usedPhaseCount>19</usedPhaseCount><usedStationCount>19</usedStationCount><standardError>0.62</standardError><minimumDistance>2.050854</minimumDistance></quality><evaluationMode>automatic</evaluationMode></origin><preferredOriginID>quakeml:us.anss.org/origin/meav</preferredOriginID><preferredMagnitudeID>quakeml:us.anss.org/magnitude/meav/Mb</preferredMagnitudeID><type>earthquake</type><creationInfo><agencyID>us</agencyID><creationTime>2014-05-19T17:08:15.742Z</creationTime><version>3</version></creationInfo><description><type>felt report</type><text>some kind of felt report</text></description></event><creationInfo><creationTime>2014-05-19T17:08:15.742Z</creationTime></creationInfo></eventParameters></q:quakeml>"));
		Product impact = products.get(1);
		Assert.assertEquals("Impact text procuct exists", "impact-text", impact
				.getId().getType());
		Assert.assertEquals(
				"content is description text",
				"some kind of felt report",
				new String(StreamUtils.readStream(impact.getContents().get("")
						.getInputStream())));
	}

}
