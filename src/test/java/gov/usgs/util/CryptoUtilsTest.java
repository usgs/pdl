package gov.usgs.util;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import org.junit.Test;
import org.junit.Assert;

public class CryptoUtilsTest {

	/** Data used to test signatures. */
	public static final String TEST_SIGNATURE_DATA = "This is sample signature data,"
			+ " both this data and a signature would be sent.";

	/** DSA private key used for testing. */
	public static final String TEST_DSA_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n"
			+ "MIIBSwIBADCCASwGByqGSM44BAEwggEfAoGBANfuMV+HfRHDMgHw6NTplw7mzo4F\n"
			+ "MILDoKW7cegVxzT5omYU59KHjSc3verkS7cyH6qH5+90ZE76gyqJR0qpdXTgWoGy\n"
			+ "Q4TgdpRgtwFSKxcaG53xBa2E6/dZbu6OVqrpfnRSLMx0uKplDlh7BFRjB4VB4fYr\n"
			+ "G8MeFCJDCjq+aU0xAhUAgBlC7m1ZMNkC4lFUy7ltTqfsVgkCgYEAvvEErY7D5YPo\n"
			+ "V1DcdgcTEXsCXCJbcrzpYTSsYnjph5thlvKhjgAeqZ7cMsejk76jtfUXK/Z/h0Ks\n"
			+ "WhuJxh4cnLAXiB9h4ErNuoIXYIZESjb3EU0eIalBXTVR4xXyKSFEzmMvj46gKeYe\n"
			+ "9lmO2ptgLHPPDBVH2nt5Ar6B0yocuBcEFgIURG8ajQTWMgJyStypi82KfrZx3G0=\n"
			+ "-----END PRIVATE KEY-----\n";

	/** DSA public key used for testing. */
	public static final String TEST_DSA_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n"
			+ "MIIBtzCCASwGByqGSM44BAEwggEfAoGBANfuMV+HfRHDMgHw6NTplw7mzo4FMILD\n"
			+ "oKW7cegVxzT5omYU59KHjSc3verkS7cyH6qH5+90ZE76gyqJR0qpdXTgWoGyQ4Tg\n"
			+ "dpRgtwFSKxcaG53xBa2E6/dZbu6OVqrpfnRSLMx0uKplDlh7BFRjB4VB4fYrG8Me\n"
			+ "FCJDCjq+aU0xAhUAgBlC7m1ZMNkC4lFUy7ltTqfsVgkCgYEAvvEErY7D5YPoV1Dc\n"
			+ "dgcTEXsCXCJbcrzpYTSsYnjph5thlvKhjgAeqZ7cMsejk76jtfUXK/Z/h0KsWhuJ\n"
			+ "xh4cnLAXiB9h4ErNuoIXYIZESjb3EU0eIalBXTVR4xXyKSFEzmMvj46gKeYe9lmO\n"
			+ "2ptgLHPPDBVH2nt5Ar6B0yocuBcDgYQAAoGAcmDQq92IGs1/uA49raelTm+3HiU9\n"
			+ "5KLhEUwFm5I8bQBjrf9SBmXBqKF0CwAF1Apm267Al1X3dsiHGwr8ZKvwey1otazU\n"
			+ "p218U27EsAbryeDud1ahf+VZBjCRxlPCSjJcM8MK56qmFn0ZvQC4E3G46dsrLVCF\n"
			+ "FnCGWOjpSPkO+xs=\n" + "-----END PUBLIC KEY-----\n";

	/** OpenSSH format DSA Private Key used for testing. */
	public static final String TEST_OPENSSH_DSA_PRIVATE_KEY = "-----BEGIN DSA PRIVATE KEY-----\n"
			+ "MIIBuwIBAAKBgQDX7jFfh30RwzIB8OjU6ZcO5s6OBTCCw6Clu3HoFcc0+aJmFOfS\n"
			+ "h40nN73q5Eu3Mh+qh+fvdGRO+oMqiUdKqXV04FqBskOE4HaUYLcBUisXGhud8QWt\n"
			+ "hOv3WW7ujlaq6X50UizMdLiqZQ5YewRUYweFQeH2KxvDHhQiQwo6vmlNMQIVAIAZ\n"
			+ "Qu5tWTDZAuJRVMu5bU6n7FYJAoGBAL7xBK2Ow+WD6FdQ3HYHExF7AlwiW3K86WE0\n"
			+ "rGJ46YebYZbyoY4AHqme3DLHo5O+o7X1Fyv2f4dCrFobicYeHJywF4gfYeBKzbqC\n"
			+ "F2CGREo29xFNHiGpQV01UeMV8ikhRM5jL4+OoCnmHvZZjtqbYCxzzwwVR9p7eQK+\n"
			+ "gdMqHLgXAoGAcmDQq92IGs1/uA49raelTm+3HiU95KLhEUwFm5I8bQBjrf9SBmXB\n"
			+ "qKF0CwAF1Apm267Al1X3dsiHGwr8ZKvwey1otazUp218U27EsAbryeDud1ahf+VZ\n"
			+ "BjCRxlPCSjJcM8MK56qmFn0ZvQC4E3G46dsrLVCFFnCGWOjpSPkO+xsCFERvGo0E\n"
			+ "1jICckrcqYvNin62cdxt\n" + "-----END DSA PRIVATE KEY-----\n";

	/** OpenSSH format DSA Public Key used for testing. */
	public static final String TEST_OPENSSH_DSA_PUBLIC_KEY = "ssh-dss AAAAB3NzaC1kc3MAAACBANfuMV+HfRHDMgHw6NTplw7mzo4FMILDoKW7cegVxzT5omYU59KHjSc3verkS7cyH6qH5+90ZE76gyqJR0qpdXTgWoGyQ4TgdpRgtwFSKxcaG53xBa2E6/dZbu6OVqrpfnRSLMx0uKplDlh7BFRjB4VB4fYrG8MeFCJDCjq+aU0xAAAAFQCAGULubVkw2QLiUVTLuW1Op+xWCQAAAIEAvvEErY7D5YPoV1DcdgcTEXsCXCJbcrzpYTSsYnjph5thlvKhjgAeqZ7cMsejk76jtfUXK/Z/h0KsWhuJxh4cnLAXiB9h4ErNuoIXYIZESjb3EU0eIalBXTVR4xXyKSFEzmMvj46gKeYe9lmO2ptgLHPPDBVH2nt5Ar6B0yocuBcAAACAcmDQq92IGs1/uA49raelTm+3HiU95KLhEUwFm5I8bQBjrf9SBmXBqKF0CwAF1Apm267Al1X3dsiHGwr8ZKvwey1otazUp218U27EsAbryeDud1ahf+VZBjCRxlPCSjJcM8MK56qmFn0ZvQC4E3G46dsrLVCFFnCGWOjpSPkO+xs= SignatureUtils-test-dsa-key\n";

	/** RSA Public Key used for testing. */
	public static final String TEST_RSA_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n"
			+ "MIIBIDANBgkqhkiG9w0BAQEFAAOCAQ0AMIIBCAKCAQEAvKPGR9YGhEpx03BUTBAa\n"
			+ "Vk3vXD3B+soh9z9ePUh0OUDuYc0g6cgFLmMv/fljysGIxiyllT7D7it69fYBUP+z\n"
			+ "eAPHf77nQMoZwu+NB8V3lpX5wREAkg/ZRuettmp7yn4VyZ2fpV3EyJkRX4DcVf+q\n"
			+ "6OErOfR2ohKb7o1gW3SVefK0j1K59jXypu8xWW2jl6qtlkSKHt6KfMh8ZgB2g4Db\n"
			+ "RkwtlFKTHU46U9N2WIWcTIe5IeZDqoAvkf+RqFHyhynIu7T/HhHciYczUTPi7Xua\n"
			+ "0VX4Uro2DQ85M7+dSXd7ZZulOky5UnJsD72azlSi9qjl/KOw0FdSqHAU+Quxyv33\n"
			+ "5wIBIw==\n" + "-----END PUBLIC KEY-----\n";

	/** RSA Private Key used for testing. */
	public static final String TEST_RSA_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n"
			+ "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQC8o8ZH1gaESnHT\n"
			+ "cFRMEBpWTe9cPcH6yiH3P149SHQ5QO5hzSDpyAUuYy/9+WPKwYjGLKWVPsPuK3r1\n"
			+ "9gFQ/7N4A8d/vudAyhnC740HxXeWlfnBEQCSD9lG5622anvKfhXJnZ+lXcTImRFf\n"
			+ "gNxV/6ro4Ss59HaiEpvujWBbdJV58rSPUrn2NfKm7zFZbaOXqq2WRIoe3op8yHxm\n"
			+ "AHaDgNtGTC2UUpMdTjpT03ZYhZxMh7kh5kOqgC+R/5GoUfKHKci7tP8eEdyJhzNR\n"
			+ "M+Lte5rRVfhSujYNDzkzv51Jd3tlm6U6TLlScmwPvZrOVKL2qOX8o7DQV1KocBT5\n"
			+ "C7HK/ffnAgEjAoIBAQCMIePAVdj0jxK6U3HSGpc4zC4nQ9HtgDZ9J8JZaQXhY29B\n"
			+ "Vo16d1ROWE+Jd26z4Dm3yWUPyDnG3niZdOsI+Haw7N1XlSDRCykUd3AUZs3dk/tc\n"
			+ "OIQUu1Dy1/1i8AQwBeRMoPpAVEkKC067+VM4kgnu0yds4Xyy6UCilOh+c9zA/CiO\n"
			+ "utwngX1wKCHpzcKy31wu0O4mvyDfO3FGwCx5RZhggRxbNcNKILYZH+ffRYKjDZ+W\n"
			+ "bE4biVVUYiHLOlkxJWGy+UFLnonW1msMwH/8Sg6NJ9DpuTiTkQIIU9aSPNKsGHPr\n"
			+ "HDPGz3hFgnkHVnJcToE/BDgC0nMy1em/QLXhH8LLAoGBAO7i3x2cz/WvV9wFCMjm\n"
			+ "0BUSDRNdDMChUIC8eKMGchtjYMYHciUZwcU/K4ze8U6v0Sv8GLXkDkpbKvoZoTZw\n"
			+ "ICU0TWxsxZTAWxxRzFVJ2R4DVT5puuHL0e7J/cuA2pTV6uHVs+3uuDzAhS7XCZtV\n"
			+ "1nrJeiLbwVf/7JnkT2car4f3AoGBAMonZGBLmiLgnGLBCvIihoi7WUncbgnqCm0F\n"
			+ "Xb8BMRBNUUs5eoSpm3/y1ZPNBYA3X0oCsemNry1sn0KuWFe3V1E93PXfMfCx0F+B\n"
			+ "7kPQA7f8SyCuXpvJXIyZn7rIrXwx3p2At8DEZ9JUHHpGB+PVyzNeVRsr3SSBnpkJ\n"
			+ "bwcU8nORAoGAFHnYnCNiSEI6uxZf1rSyv/o7opLydulBah7IgwCNcA/VGEnH9I0t\n"
			+ "3bT1GrQGDg8R7dPHmo/j976r6Y0rE0tweDeu3WhotPqLd3S5virmuW3//gkQBLmy\n"
			+ "6JT4fyhNP/UUIfxf4TG4BTUSut87VnUSYkvIo+bzUK+JTwTw3POopUECgYARU9Vn\n"
			+ "Vu/0XGUtCTtz1xLYhRZIKNY7XTQX+SVK4thR2r3TP3DmzLWOn8kp7QB4tEn/B4uX\n"
			+ "rQ8D5L0xmepQqVCgkEYjtAuYScFnRaa1WvkBJED0LDP+uXz2G8vcz19TycKfyTRS\n"
			+ "WfpFOmjXR9TKYshGO0kflgukCxw5AM8Am2U9GwKBgQDqWoxkIlxSx+iT7NPqyt5x\n"
			+ "gWrV4AkoYuJ4DOcUHPfcnTK6QtNhf6F8JZSYdvGFd++fHPpHyw7iKrX29Hxbr42N\n"
			+ "srUKGStAQfJRZvsqRyfB4GXPAJg0NLbCWp2u6CFLVbvhWZN/HKQb02y2PmnOHxVr\n"
			+ "c0rv3iKprErMzN5mZxTTBQ==\n" + "-----END PRIVATE KEY-----\n";

	/** OpenSSH format RSA Private Key used for testing. */
	public static final String TEST_OPENSSH_RSA_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n"
			+ "MIIEogIBAAKCAQEAvKPGR9YGhEpx03BUTBAaVk3vXD3B+soh9z9ePUh0OUDuYc0g\n"
			+ "6cgFLmMv/fljysGIxiyllT7D7it69fYBUP+zeAPHf77nQMoZwu+NB8V3lpX5wREA\n"
			+ "kg/ZRuettmp7yn4VyZ2fpV3EyJkRX4DcVf+q6OErOfR2ohKb7o1gW3SVefK0j1K5\n"
			+ "9jXypu8xWW2jl6qtlkSKHt6KfMh8ZgB2g4DbRkwtlFKTHU46U9N2WIWcTIe5IeZD\n"
			+ "qoAvkf+RqFHyhynIu7T/HhHciYczUTPi7Xua0VX4Uro2DQ85M7+dSXd7ZZulOky5\n"
			+ "UnJsD72azlSi9qjl/KOw0FdSqHAU+Quxyv335wIBIwKCAQEAjCHjwFXY9I8SulNx\n"
			+ "0hqXOMwuJ0PR7YA2fSfCWWkF4WNvQVaNendUTlhPiXdus+A5t8llD8g5xt54mXTr\n"
			+ "CPh2sOzdV5Ug0QspFHdwFGbN3ZP7XDiEFLtQ8tf9YvAEMAXkTKD6QFRJCgtOu/lT\n"
			+ "OJIJ7tMnbOF8sulAopTofnPcwPwojrrcJ4F9cCgh6c3Cst9cLtDuJr8g3ztxRsAs\n"
			+ "eUWYYIEcWzXDSiC2GR/n30WCow2flmxOG4lVVGIhyzpZMSVhsvlBS56J1tZrDMB/\n"
			+ "/EoOjSfQ6bk4k5ECCFPWkjzSrBhz6xwzxs94RYJ5B1ZyXE6BPwQ4AtJzMtXpv0C1\n"
			+ "4R/CywKBgQDu4t8dnM/1r1fcBQjI5tAVEg0TXQzAoVCAvHijBnIbY2DGB3IlGcHF\n"
			+ "PyuM3vFOr9Er/Bi15A5KWyr6GaE2cCAlNE1sbMWUwFscUcxVSdkeA1U+abrhy9Hu\n"
			+ "yf3LgNqU1erh1bPt7rg8wIUu1wmbVdZ6yXoi28FX/+yZ5E9nGq+H9wKBgQDKJ2Rg\n"
			+ "S5oi4JxiwQryIoaIu1lJ3G4J6gptBV2/ATEQTVFLOXqEqZt/8tWTzQWAN19KArHp\n"
			+ "ja8tbJ9CrlhXt1dRPdz13zHwsdBfge5D0AO3/Esgrl6byVyMmZ+6yK18Md6dgLfA\n"
			+ "xGfSVBx6Rgfj1cszXlUbK90kgZ6ZCW8HFPJzkQKBgBR52JwjYkhCOrsWX9a0sr/6\n"
			+ "O6KS8nbpQWoeyIMAjXAP1RhJx/SNLd209Rq0Bg4PEe3Tx5qP4/e+q+mNKxNLcHg3\n"
			+ "rt1oaLT6i3d0ub4q5rlt//4JEAS5suiU+H8oTT/1FCH8X+ExuAU1ErrfO1Z1EmJL\n"
			+ "yKPm81CviU8E8NzzqKVBAoGAEVPVZ1bv9FxlLQk7c9cS2IUWSCjWO100F/klSuLY\n"
			+ "Udq90z9w5sy1jp/JKe0AeLRJ/weLl60PA+S9MZnqUKlQoJBGI7QLmEnBZ0WmtVr5\n"
			+ "ASRA9Cwz/rl89hvL3M9fU8nCn8k0Uln6RTpo10fUymLIRjtJH5YLpAscOQDPAJtl\n"
			+ "PRsCgYEA6lqMZCJcUsfok+zT6srecYFq1eAJKGLieAznFBz33J0yukLTYX+hfCWU\n"
			+ "mHbxhXfvnxz6R8sO4iq19vR8W6+NjbK1ChkrQEHyUWb7KkcnweBlzwCYNDS2wlqd\n"
			+ "rughS1W74VmTfxykG9Nstj5pzh8Va3NK794iqaxKzMzeZmcU0wU=\n"
			+ "-----END RSA PRIVATE KEY-----\n";

	/** OpenSSH RSA Public Key used for testing. */
	public static final String TEST_OPENSSH_RSA_PUBLIC_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAvKPGR9YGhEpx03BUTBAaVk3vXD3B+soh9z9ePUh0OUDuYc0g6cgFLmMv/fljysGIxiyllT7D7it69fYBUP+zeAPHf77nQMoZwu+NB8V3lpX5wREAkg/ZRuettmp7yn4VyZ2fpV3EyJkRX4DcVf+q6OErOfR2ohKb7o1gW3SVefK0j1K59jXypu8xWW2jl6qtlkSKHt6KfMh8ZgB2g4DbRkwtlFKTHU46U9N2WIWcTIe5IeZDqoAvkf+RqFHyhynIu7T/HhHciYczUTPi7Xua0VX4Uro2DQ85M7+dSXd7ZZulOky5UnJsD72azlSi9qjl/KOw0FdSqHAU+Quxyv335w== SignatureUtils-test-rsa-key\n";

	@Test
	public void generateRSAKeyPair() throws NoSuchAlgorithmException,
			InvalidKeyException, NoSuchPaddingException,
			IllegalArgumentException, IOException, SignatureException {
		testKeyPair(CryptoUtils.generateRSAKeyPair(CryptoUtils.RSA_2048));
	}

	@Test
	public void generateDSAKeyPair() throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalArgumentException, IOException, SignatureException {
		testKeyPair(CryptoUtils.generateDSAKeyPair(CryptoUtils.DSA_1024));
	}

	@Test
	public void generateAESKey() throws NoSuchAlgorithmException,
			InvalidKeyException, NoSuchPaddingException,
			IllegalArgumentException, IOException {
		Key key = CryptoUtils.generateAESKey(CryptoUtils.AES_128);
		testKey(key);
	}

	@Test
	public void readDSAKeyPair() throws IllegalArgumentException, IOException,
			NoSuchAlgorithmException {
		PrivateKey key = CryptoUtils.readPrivateKey(TEST_RSA_PRIVATE_KEY
				.getBytes());
		Assert.assertNotNull("rsa private key not null", key);
	}

	@Test
	public void readRSAKeyPair() throws IllegalArgumentException, IOException,
			NoSuchAlgorithmException, InvalidKeyException,
			NoSuchPaddingException, SignatureException {
		PublicKey publicKey = CryptoUtils.readPublicKey(TEST_RSA_PUBLIC_KEY
				.getBytes());
		PrivateKey privateKey = CryptoUtils.readPrivateKey(TEST_RSA_PRIVATE_KEY
				.getBytes());
		testKeyPair(new KeyPair(publicKey, privateKey));
	}

	@Test
	public void readOpenSSHDSAKeyPair() throws IllegalArgumentException,
			IOException, InvalidKeySpecException, NoSuchAlgorithmException,
			InvalidKeyException, NoSuchPaddingException, SignatureException {
		PublicKey publicKey = CryptoUtils
				.readOpenSSHPublicKey(TEST_OPENSSH_DSA_PUBLIC_KEY.getBytes());
		PrivateKey privateKey = CryptoUtils.readOpenSSHPrivateKey(
				TEST_OPENSSH_DSA_PRIVATE_KEY.getBytes(), null);

		testKeyPair(new KeyPair(publicKey, privateKey));
	}

	@Test
	public void readOpenSSHRSAKeyPair() throws IllegalArgumentException,
			IOException, InvalidKeySpecException, NoSuchAlgorithmException,
			InvalidKeyException, NoSuchPaddingException, SignatureException {
		PublicKey publicKey = CryptoUtils
				.readOpenSSHPublicKey(TEST_OPENSSH_RSA_PUBLIC_KEY.getBytes());
		PrivateKey privateKey = CryptoUtils.readOpenSSHPrivateKey(
				TEST_OPENSSH_RSA_PRIVATE_KEY.getBytes(), null);

		testKeyPair(new KeyPair(publicKey, privateKey));
	}

	/**
	 * Encrypt using public key, decrypt using private key, and assert the data
	 * matches. Then encrypt using private key, decrypt using public key, and
	 * assert the data matches.
	 *
	 * @param key
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws SignatureException
	 */
	public void testKeyPair(KeyPair key) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalArgumentException, IOException, SignatureException {
		Assert.assertNotNull("Public key is null", key.getPublic());
		Assert.assertNotNull("Private key is null", key.getPrivate());

		byte[] testData = TEST_SIGNATURE_DATA.getBytes();
		byte[] modifiedData = TEST_SIGNATURE_DATA.getBytes();
		modifiedData[0]++;

		// test signature
		String keyAlgorithm = key.getPublic().getAlgorithm();
		String signature = CryptoUtils.sign(key.getPrivate(), testData);
		Assert.assertTrue(keyAlgorithm + " signature generated",
				signature != null);
		Assert.assertTrue(keyAlgorithm + " signature verified", CryptoUtils
				.verify(key.getPublic(), testData, signature));
		Assert.assertFalse(keyAlgorithm
				+ " signature not verified after data modification",
				CryptoUtils.verify(key.getPublic(), modifiedData, signature));

		// test encryption.
		Cipher encryptionDetect = null;
		try {
			// not all keys support encryption
			encryptionDetect = CryptoUtils.getEncryptCipher(key.getPublic());
		} catch (Exception e) {
			// ignore
		}
		if (encryptionDetect != null) {
			byte[] encryptedData = CryptoUtils.encrypt(key.getPublic(),
					testData);
			byte[] decryptedData = CryptoUtils.decrypt(key.getPrivate(),
					encryptedData);
			Assert.assertArrayEquals(keyAlgorithm
					+ " encrypt public to private data match", testData,
					decryptedData);

			encryptedData = CryptoUtils.encrypt(key.getPrivate(), testData);
			decryptedData = CryptoUtils.decrypt(key.getPublic(), encryptedData);
			Assert.assertArrayEquals(keyAlgorithm
					+ " encrypt private, decrypt public data match", testData,
					decryptedData);
		}
	}

	/**
	 * Encrypt using key, decrypt using key, and assert the data matches.
	 *
	 * @param key
	 *            the symmetric key to use.
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public void testKey(Key key) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalArgumentException, IOException {
		byte[] originalData = TEST_SIGNATURE_DATA.getBytes();

		byte[] encryptedData = CryptoUtils.encrypt(key, originalData);
		byte[] decryptedData = CryptoUtils.decrypt(key, encryptedData);
		Assert.assertArrayEquals("encrypt, decrypt data match", originalData,
				decryptedData);
	}

}
