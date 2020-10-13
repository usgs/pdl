package gov.usgs.earthquake.aws;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Date;

import javax.json.JsonObject;

import gov.usgs.earthquake.distribution.URLNotification;
import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.io.JsonProduct;
import gov.usgs.util.protocolhandlers.data.Handler;

public class JsonNotification extends URLNotification {
	public static final URL EMPTY_URL;
	static {
		try {
			// make sure data protocol handler is registered
			Handler.register();
			EMPTY_URL = new URL("data:,");
		} catch (MalformedURLException mue) {
			throw new RuntimeException("failed to parse empty url");
		}
	}

	public final Timestamp created;
	public final Product product;

	JsonNotification(final JsonObject json) throws Exception {
		this(
				Timestamp.valueOf(json.getString("created")),
				new JsonProduct().getProduct(
						json.getJsonObject("product")));
	}

	JsonNotification(final Timestamp created, final Product product) throws Exception {
		super(
				product.getId(),
				// expiration date
				new Date(created.getTime() + 30 * 86400),
				// no tracker
				EMPTY_URL,
				// store product as data url
				new URL("data:;base64," +
						new String(Base64.getEncoder().encode(
								new JsonProduct().getJsonObject((product))
										.toString().getBytes("utf8")))));
		this.created = created;
		this.product = product;
	}
}
