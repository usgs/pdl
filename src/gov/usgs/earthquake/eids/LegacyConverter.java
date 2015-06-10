package gov.usgs.earthquake.eids;

import java.io.InputStream;
import java.util.Map;

import gov.usgs.earthquake.cube.CubeMessage;
import gov.usgs.earthquake.event.Converter;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.Product;
import gov.usgs.util.StreamUtils;

import org.quakeml_1_2.Quakeml;
import gov.usgs.ansseqmsg.EQMessage;

public class LegacyConverter {

	public static enum Format {
		CUBE, EQXML, QUAKEML
	};

	public static final Format CUBE = Format.CUBE;
	public static final Format EQXML = Format.EQXML;
	public static final Format QUAKEML = Format.QUAKEML;

	public static final String EQXML_CONTENT_PATH = "eqxml.xml";
	public static final String QUAKEML_CONTENT_PATH = "quakeml.xml";

	private final Format outputFormat;
	private final Converter converter;

	public LegacyConverter(final Format outputFormat) {
		this.outputFormat = outputFormat;
		this.converter = new Converter();
	}

	/**
	 * @return converter that outputs cube.
	 */
	public static LegacyConverter cubeConverter() {
		return new LegacyConverter(CUBE);
	}

	/**
	 * @return converter that outputs eqxml.
	 */
	public static LegacyConverter eqxmlConverter() {
		return new LegacyConverter(EQXML);
	}

	/**
	 * @return converter that outputs quakeml.
	 */
	public static LegacyConverter quakemlConverter() {
		return new LegacyConverter(QUAKEML);
	}

	/**
	 * Handles conversion from a product containing either eqxml or quakeml
	 * contents to either eqxml, quakeml, or cube byte array.
	 * 
	 * @param product
	 *            the product object to convert.
	 * @return byte array containing the output format, or null if unable to
	 *         convert.
	 * @throws Exception
	 */
	public byte[] convert(final Product product) throws Exception {
		Map<String, Content> contents = product.getContents();
		InputStream input = null;
		try {
			if (contents.containsKey(QUAKEML_CONTENT_PATH)) {
				input = contents.get(QUAKEML_CONTENT_PATH).getInputStream();
				return convert(converter.getQuakeml(input));
			} else if (contents.containsKey(EQXML_CONTENT_PATH)) {
				input = contents.get(EQXML_CONTENT_PATH).getInputStream();
				return convert(converter.getEQMessage(input));
			} else {
				// unable to convert
				return null;
			}
		} finally {
			StreamUtils.closeStream(input);
		}
	}

	/**
	 * Handles conversion from an eqxml to either eqxml, quakeml, or cube byte
	 * array.
	 * 
	 * @param eqxml
	 *            the eqxml object to convert.
	 * @return byte array containing output format, or null if unable to
	 *         convert.
	 * @throws Exception
	 */
	public byte[] convert(EQMessage eqxml) throws Exception {
		if (eqxml == null) {
			return null;
		}
		try {
			if (outputFormat == EQXML) {
				return converter.getString(eqxml).getBytes();
			} else if (outputFormat == CUBE) {
				return converter.getString(converter.getCubeMessage(eqxml))
						.getBytes();
			} else if (outputFormat == QUAKEML) {
				return converter.getString(converter.getQuakeml(eqxml))
						.getBytes();
			} else {
				return null;
			}
		} catch (NullPointerException npe) {
			return null;
		}
	}

	/**
	 * Handles conversion from a quakeml message to either eqxml, quakeml, or
	 * cube byte array.
	 * 
	 * @param quakeml
	 *            the quakeml object to convert.
	 * @return byte array containing output format, or null if unable to
	 *         convert.
	 * @throws Exception
	 */
	public byte[] convert(Quakeml quakeml) throws Exception {
		if (quakeml == null) {
			return null;
		}
		try {
			if (outputFormat == EQXML) {
				return converter.getString(converter.getEQMessage(quakeml))
						.getBytes();
			} else if (outputFormat == CUBE) {
				return converter.getString(converter.getCubeMessage(quakeml))
						.getBytes();
			} else if (outputFormat == QUAKEML) {
				return converter.getString(quakeml).getBytes();
			} else {
				return null;
			}
		} catch (NullPointerException npe) {
			return null;
		}
	}

	/**
	 * Handles conversion from a cube message to either eqxml, quakeml, or cube
	 * byte array.
	 * 
	 * @param cube
	 *            the cube object to convert.
	 * @return byte array containing output format, or null if unable to
	 *         convert.
	 * @throws Exception
	 */
	public byte[] convert(CubeMessage cube) throws Exception {
		if (cube == null) {
			return null;
		}
		try {
			if (outputFormat == EQXML) {
				return converter.getString(converter.getEQMessage(cube))
						.getBytes();
			} else if (outputFormat == CUBE) {
				return converter.getString(cube).getBytes();
			} else if (outputFormat == QUAKEML) {
				return converter.getString(converter.getQuakeml(cube))
						.getBytes();
			} else {
				return null;
			}
		} catch (NullPointerException npe) {
			return null;
		}
	}

}
