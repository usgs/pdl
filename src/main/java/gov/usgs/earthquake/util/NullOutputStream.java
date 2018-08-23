package gov.usgs.earthquake.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Stream that ignores any written bytes.
 */
public class NullOutputStream extends OutputStream {

	public void write(byte[] b) {
		// override this method to avoid wasted call overhead.
	}

	public void write(byte[] b, int off, int len) {
		// override this method to avoid wasted call overhead.
	}

	public void write(int b) throws IOException {
		// override this method to avoid wasted call overhead.
	}

}
