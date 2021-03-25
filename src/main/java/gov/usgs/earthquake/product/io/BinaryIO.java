package gov.usgs.earthquake.product.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class BinaryIO {

	/**
	 * Writes an int to the OutputStream buffer
	 * @param in an int to write
	 * @param out the OutputStream
	 * @throws IOException if IO error occurs
	 */
	public void writeInt(final int in, final OutputStream out)
			throws IOException {
		out.write(ByteBuffer.allocate(4).putInt(in).array());
	}

	/**
	 * Writes an long to the OutputStream buffer
	 * @param in an long to write
	 * @param out the OutputStream
	 * @throws IOException if IO error occurs
	 */
	public void writeLong(final long in, final OutputStream out)
			throws IOException {
		out.write(ByteBuffer.allocate(8).putLong(in).array());
	}

	/**
	 * Writes an array of bytes to the OutputStream buffer
	 * @param toWrite an array of bytes to write
	 * @param out the OutputStream
	 * @throws IOException if IO error occurs
	 */
	public void writeBytes(final byte[] toWrite, final OutputStream out)
			throws IOException {
		// length of string
		writeInt(toWrite.length, out);
		// string
		out.write(toWrite);
	}

	/**
	 * Writes a string to the OutputStream buffer
	 * @param toWrite a string to write
	 * @param out the OutputStream
	 * @throws IOException if IO error occurs
	 */
	public void writeString(final String toWrite, final OutputStream out)
			throws IOException {
		writeBytes(toWrite.getBytes(StandardCharsets.UTF_8), out);
	}

	/**
	 * Writes a date to the OutputStream buffer
	 * @param toWrite a date to write
	 * @param out the OutputStream
	 * @throws IOException if IO error occurs
	 */
	public void writeDate(final Date toWrite, final OutputStream out)
			throws IOException {
		writeLong(toWrite.getTime(), out);
	}

	/**
	 * Writes a long to the OutputStream buffer
	 * @param length a long to write
	 * @param in an input stream
	 * @param out the OutputStream
	 * @throws IOException if IO error occurs
	 */
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

	/**
	 * Reads 4 bytes from the InputStream
	 * @param in InputStream
	 * @return an int
	 * @throws IOException if IO Error occurs
	 */
	public int readInt(final InputStream in) throws IOException {
		byte[] buffer = new byte[4];
		readFully(buffer, in);
		return ByteBuffer.wrap(buffer).getInt();
	}

	/**
	 * Reads 8 bytes from the InputStream
	 * @param in InputStream
	 * @return a long
	 * @throws IOException if IO Error occurs
	 */
	public long readLong(final InputStream in) throws IOException {
		byte[] buffer = new byte[8];
		readFully(buffer, in);
		return ByteBuffer.wrap(buffer).getLong();
	}

	/**
	 * Reads a byte array from the InputStream
	 * @param in InputStream
	 * @return Byte[]
	 * @throws IOException if IO Error occurs
	 */
	public byte[] readBytes(final InputStream in) throws IOException {
		int length = readInt(in);
		byte[] buffer = new byte[length];
		readFully(buffer, in);
		return buffer;
	}


	/**
	 * Reads string from the InputStream
	 * @param in InputStream
	 * @return a string
	 * @throws IOException if IO Error occurs
	 */
	public String readString(final InputStream in) throws IOException {
		return this.readString(in, -1);
	}

	/**
	 * Reads string with a max length from the InputStream
	 * @param in InputStream
	 * @param maxLength of string
	 * @return an string
	 * @throws IOException if IO Error occurs
	 */
	public String readString(final InputStream in, final int maxLength) throws IOException {
		int length = readInt(in);
		if (maxLength > 0 && length > maxLength) {
			throw new IOException("request string length " + length + " greater than maxLength " + maxLength);
		}
		byte[] buffer = new byte[length];
		readFully(buffer, in);
		return new String(buffer, StandardCharsets.UTF_8);
	}

	/**
	 * Reads date from the InputStream
	 * @param in InputStream
	 * @return a date
	 * @throws IOException if IO Error occurs
	 */
	public Date readDate(final InputStream in) throws IOException {
		return new Date(readLong(in));
	}

	/**
	 * Reads stream
	 * @param in InputStream
	 * @param out OutputStream
	 * @throws IOException if IO Error occurs
	 */
	public void readStream(final InputStream in, final OutputStream out)
			throws IOException {
		long length = readLong(in);
		readStream(length, in, out);
	}

	/**
	 * Function called by previous readStream
	 * Used to read whole stream
	 * @param length length of inputstream
	 * @param in input stream
	 * @param out output stream
	 * @throws IOException if io error occurs
	 */
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

	/**
	 * Function used by other read funcs
	 * Reads from input stream until buffer is full
	 * @param buffer byte[]
	 * @param in inputstream
	 * @throws IOException if IO error occurs
	 */
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
