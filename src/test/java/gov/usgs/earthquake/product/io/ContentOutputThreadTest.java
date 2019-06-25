package gov.usgs.earthquake.product.io;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import gov.usgs.earthquake.product.AbstractContent;
import gov.usgs.earthquake.product.Content;
import gov.usgs.earthquake.product.ProductId;

public class ContentOutputThreadTest {

	
	@Test
	public void testHandlerException() throws Exception {
		ProductHandler testHandler = new ObjectProductHandler() {
			@Override
			public void onContent(final ProductId id, final String path,
					final Content content) throws Exception {
				throw new Exception("for test");
			}
		};
		
		TestContent testContent = new TestContent(); 

		ContentOutputThread testThread = new ContentOutputThread(
				testHandler, null, null, testContent);
		testThread.start();
		testThread.join();
		Assert.assertTrue("close called even after exception", testContent.closeCalled);
	}
	
	
	public static class TestContent extends AbstractContent {
		public boolean closeCalled = false;

		@Override
		public InputStream getInputStream() throws IOException {
			return null;
		}

		@Override
		public void close() {
			closeCalled = true;
		}
	};

}
