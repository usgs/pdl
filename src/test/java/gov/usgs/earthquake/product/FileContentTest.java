package gov.usgs.earthquake.product;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class FileContentTest {

	@Test
	public void getMimeType() {
		Assert.assertEquals("works with filename", FileContent
				.defaultGetMimeType(new File("test.gz")), "application/gzip");
		Assert.assertEquals("works with path",
				FileContent.defaultGetMimeType(new File("path/test.gz")),
						"application/gzip");
		Assert.assertEquals("works with non-gz extension", FileContent
				.defaultGetMimeType(new File("test.png")), "image/png");
		Assert.assertEquals("uses last extension", FileContent
				.defaultGetMimeType(new File("path/test.tar.gz")),
						"application/gzip");
	}
}
