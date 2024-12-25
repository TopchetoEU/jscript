package me.topchetoeu.jscript.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Reading {
	private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

	public static synchronized String readline() throws IOException {
		return reader.readLine();
	}

	public static byte[] streamToBytes(InputStream in) {
		if (in == null) return null;

		try {
			List<byte[]> bufs = null;
			byte[] result = null;
			int total = 0;
			int n;

			do {
				var buf = new byte[8192];
				int nread = 0;

				// read to EOF which may read more or less than buffer size
				while ((n = in.read(buf, nread, buf.length - nread)) > 0) {
					nread += n;
				}

				if (nread > 0) {
					if (Integer.MAX_VALUE - 8 - total < nread) throw new OutOfMemoryError("Required array size too large");
					if (nread < buf.length) buf = Arrays.copyOfRange(buf, 0, nread);
					total += nread;

					if (result == null) result = buf;
					else {
						if (bufs == null) {
							bufs = new ArrayList<>();
							bufs.add(result);
						}

						bufs.add(buf);
					}
				}
				// if the last call to read returned -1 or the number of bytes
				// requested have been read then break
			} while (n >= 0);

			if (bufs == null) {
				if (result == null) return new byte[0];
				return result.length == total ? result : Arrays.copyOf(result, total);
			}

			result = new byte[total];

			int offset = 0;
			int remaining = total;

			for (byte[] b : bufs) {
				int count = Math.min(b.length, remaining);
				System.arraycopy(b, 0, result, offset, count);
				offset += count;
				remaining -= count;
			}

			return result;
		}
		catch (IOException e) { throw new UncheckedIOException(e); }
	}
	public static String streamToString(InputStream in) {
		var bytes = streamToBytes(in);
		if (bytes == null) return null;
		else return new String(bytes);
	}
	
	public static InputStream resourceToStream(String name) {
		return Reading.class.getResourceAsStream("/" + name);
	}
	public static String resourceToString(String name) {
		return streamToString(resourceToStream(name));
	}
	public static byte[] resourceToBytes(String name) {
		return streamToBytes(resourceToStream(name));
	}
}
