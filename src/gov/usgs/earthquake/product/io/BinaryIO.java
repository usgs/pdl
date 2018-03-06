package gov.usgs.earthquake.product.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.util.Date;

public class BinaryIO {

	public void writeInt(final int in, final OutputStream out)
			throws IOException {
		out.write(ByteBuffer.allocate(4).putInt(in).array());
	}

	public void writeLong(final long in, final OutputStream out)
			throws IOException {
		out.write(ByteBuffer.allocate(8).putLong(in).array());
	}

	public void writeBytes(final byte[] toWrite, final OutputStream out)
			throws IOException {
		// length of string
		writeInt(toWrite.length, out);
		// string
		out.write(toWrite);
	}

	public void writeString(final String toWrite, final OutputStream out)
			throws IOException {
		writeBytes(toWrite.getBytes("UTF8"), out);
	}

	public void writeDate(final Date toWrite, final OutputStream out)
			throws IOException {
		writeLong(toWrite.getTime(), out);
	}

	public void writeStream(final long length, final InputStream in,
			final OutputStream out) throws IOException {
		writeLong(length, out);

		// transfer stream bytes
		int read = -1;
		byte[] bytes = new byte[1024];
		// read no more than length bytes
		while ((read = in.read(bytes)) != -1) {
			out.write(bytes, 0, read);
		}
	}

	public int readInt(final InputStream in) throws IOException {
		byte[] buffer = new byte[4];
		readFully(buffer, in);
		return ByteBuffer.wrap(buffer).getInt();
	}

	public long readLong(final InputStream in) throws IOException {
		byte[] buffer = new byte[8];
		readFully(buffer, in);
		return ByteBuffer.wrap(buffer).getLong();
	}

	public byte[] readBytes(final InputStream in) throws IOException {
		int length = readInt(in);
		byte[] buffer = new byte[length];
		readFully(buffer, in);
		return buffer;
	}


	public String readString(final InputStream in) throws IOException {
		return this.readString(in, -1);
	}

	public String readString(final InputStream in, final int maxLength) throws IOException {
		int length = readInt(in);
		if (maxLength > 0 && length > maxLength) {
			throw new IOException("request string length " + length + " greater than maxLength " + maxLength);
		}
		byte[] buffer = new byte[length];
		readFully(buffer, in);
		return new String(buffer, "UTF8");
	}

	public Date readDate(final InputStream in) throws IOException {
		return new Date(readLong(in));
	}

	public void readStream(final InputStream in, final OutputStream out)
			throws IOException {
		long length = readLong(in);
		readStream(length, in, out);
	}

	public void readStream(final long length, final InputStream in,
			final OutputStream out) throws IOException {
		long remaining = length;

		// transfer stream bytes, not going over total
		int read = -1;
		byte[] bytes = new byte[1024];
		int readSize = bytes.length;

		while (remaining > 0) {
			if (remaining < readSize) {
				// don't read past end of stream
				readSize = (int) remaining;
			}

			// read next chunk
			read = in.read(bytes, 0, readSize);
			if (read == -1) {
				// shouldn't be at eof, since reading length specified in stream
				throw new EOFException();
			}
			// track how much has been read
			remaining -= read;
			// transfer to out stream
			out.write(bytes, 0, read);
		}
	}

	protected void readFully(final byte[] buffer, final InputStream in)
			throws IOException {
		int length = buffer.length;
		int totalRead = 0;
		int read = -1;

		while ((read = in.read(buffer, totalRead, length - totalRead)) != -1) {
			totalRead += read;
			if (totalRead == length) {
				break;
			}
		}

		if (totalRead != length) {
			throw new IOException("EOF before buffer could be readFully, read=" + totalRead + ", expected=" + length);
		}
	}

}
