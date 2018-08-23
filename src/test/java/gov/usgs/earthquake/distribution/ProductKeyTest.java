package gov.usgs.earthquake.distribution;

import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.Assert;
import org.junit.Test;

import gov.usgs.earthquake.product.Product;
import gov.usgs.earthquake.product.ProductTest;
import gov.usgs.util.Config;
import gov.usgs.util.CryptoUtils;

public class ProductKeyTest {

	public static final String DSA_PUBLIC_KEY = "ssh-dss "
			+ "AAAAB3NzaC1kc3MAAACBAMIFu8X7O3JM2dxXxq1HVyYtmHqS/bNWGUEeWBf0fr8L"
			+ "IdJBNwbRz0Xs+iPn/mhwmKxV5Q1sL1yhnSUF1m64NXGNFeUjMk9qNoJipdSx7cda"
			+ "npltifdwBRqSwrl9sSjxsY9krwWzf9FTcyyteybY1h8Brxl1qDSy9/1XjFAhpy3d"
			+ "AAAAFQCHZy+IdGUxkST8/M/pM1SvG6R3swAAAIBStJadtHCh6FaUCtcNl1Ux34Hg"
			+ "LQ2my8SI7CDW3ztrRHb0ounSOYwMjUcxTvMRBJ2iTNKZNj3f7NCCQ8u1gIzY7keT"
			+ "v+2uKhgqm4N9NUfjDhUSkQQ4YuPld4hZf8i/gpqL/q7shnLC6IXQaFaDtRIZLzjt"
			+ "2t8DGavP3/7Evn54HQAAAIEAg2e0RFRgLZZX9Y9YLBT8lGaxJesO/AjRVVNM2m9K"
			+ "J51HzOZoJZaOCk5hG+4VKXRPrj2U1u+K9OgmF5ONYrOqIDAs9uxf/jmwZ3dOHQ69"
			+ "VQ+GzWiBFonJ5JItEAeVfbPmLQnBMz79mQ7gem7lkqQgAZeIc7sL80jredo7Vs2E"
			+ "AOA= user@host";

	public static final String DSA_PRIVATE_KEY = "-----BEGIN DSA PRIVATE KEY-----\n"
			+ "MIIBuwIBAAKBgQDCBbvF+ztyTNncV8atR1cmLZh6kv2zVhlBHlgX9H6/CyHSQTcG\n"
			+ "0c9F7Poj5/5ocJisVeUNbC9coZ0lBdZuuDVxjRXlIzJPajaCYqXUse3HWp6ZbYn3\n"
			+ "cAUaksK5fbEo8bGPZK8Fs3/RU3MsrXsm2NYfAa8Zdag0svf9V4xQIact3QIVAIdn\n"
			+ "L4h0ZTGRJPz8z+kzVK8bpHezAoGAUrSWnbRwoehWlArXDZdVMd+B4C0NpsvEiOwg\n"
			+ "1t87a0R29KLp0jmMDI1HMU7zEQSdokzSmTY93+zQgkPLtYCM2O5Hk7/trioYKpuD\n"
			+ "fTVH4w4VEpEEOGLj5XeIWX/Iv4Kai/6u7IZywuiF0GhWg7USGS847drfAxmrz9/+\n"
			+ "xL5+eB0CgYEAg2e0RFRgLZZX9Y9YLBT8lGaxJesO/AjRVVNM2m9KJ51HzOZoJZaO\n"
			+ "Ck5hG+4VKXRPrj2U1u+K9OgmF5ONYrOqIDAs9uxf/jmwZ3dOHQ69VQ+GzWiBFonJ\n"
			+ "5JItEAeVfbPmLQnBMz79mQ7gem7lkqQgAZeIc7sL80jredo7Vs2EAOACFFsFWW3X\n"
			+ "gebID5ouGfDufUH0Xee4\n" + "-----END DSA PRIVATE KEY-----";

	public static final String RSA_PUBLIC_KEY = "ssh-rsa "
			+ "AAAAB3NzaC1yc2EAAAADAQABAAABhACxEasOhDgjC2MkGDXobCrSGLjVRIqyiiaW"
			+ "X5xQXp6XbqGuwOTRb+HrRohMrRXS4DiOVdHK3qyiXF5wWMJ8D3gVRBq1cyn/QX7F"
			+ "qFbo544qOR80ylNqke0xJhxsMkC5RNVG88IwTZjq/2HZ815Zy8WkhRzPeloCDpGf"
			+ "2TKNhZc22JG50vD5/hjYUYpDHvASoef5UTEvOYByXdVByj8nv7gnbUJ26EuDrTOd"
			+ "ZtgiB95zwfzj3osTYFwEQxQpku7BGqD3Bg+Fxeqsns+zglLKUHMVwBKiENKkyzd/"
			+ "soU63EVgYSQtq0zeEG/2nvO9B5zqSL0xjJjN94qqnGNxrUUe7XH7H87nu5GJa/Fj"
			+ "PBJR1c5envmGjX7FChBEo2L+A2l2G2UFSu306IpRq1ihJheDP9zvJqXhAEkL3qvf"
			+ "DFvs+YQNbApqdrEiYweajZAYBx+Kno7+lb9hLcridcud51Y1lVtBnNUY/FJW2BeE"
			+ "9H1H19/R8UPcbWP527rLzInzQFzc9xQBaEU= user@host";

	public static final String RSA_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n"
			+ "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\n"
			+ "7i3INQ==\n" + "-----END RSA PRIVATE KEY-----";

	@Test
	public void testDsaKey() throws Exception {
		Config config = new Config();
		config.put("key", DSA_PUBLIC_KEY);
		ProductKey key = new ProductKey();
		key.configure(config);

		PrivateKey privateKey = CryptoUtils.readOpenSSHPrivateKey(
				DSA_PRIVATE_KEY.getBytes(), null);
		Product product = new ProductTest().getProduct();
		product.sign(privateKey);
		Assert.assertTrue("signature verifies",
				product.verifySignature(new PublicKey[] { key.getKey() }));
	}

	@Test
	public void testRsaKey() throws Exception {
		Config config = new Config();
		config.put("key", RSA_PUBLIC_KEY);
		ProductKey key = new ProductKey();
		key.configure(config);

		PrivateKey privateKey = CryptoUtils.readOpenSSHPrivateKey(
				RSA_PRIVATE_KEY.getBytes(), null);
		Product product = new ProductTest().getProduct();
		product.sign(privateKey);
		Assert.assertTrue("signature verifies",
				product.verifySignature(new PublicKey[] { key.getKey() }));
	}

}
