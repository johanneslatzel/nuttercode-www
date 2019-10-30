package de.nuttercode.www.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import de.nuttercode.util.buffer.DataQueue;

class HttpStreamReader {

	private final BufferedInputStream bin;
	private final DataQueue dataQueue;
	private final static int MAX_BUFFER_SIZE = 100 * 1024 * 1024; // 24MiB
	private final static int BUFFER_SIZE = 1024;
	private final static byte CR = '\r';
	private final static byte LF = '\n';

	HttpStreamReader(InputStream inputStream) throws IOException {
		bin = new BufferedInputStream(inputStream);
		dataQueue = new DataQueue();
		dataQueue.setMaxSize(MAX_BUFFER_SIZE);
	}

	public String readLine() throws IOException {
		dataQueue.clear();
		boolean crFlag = false;
		boolean exitFlag = false;
		while (!exitFlag) {
			byte current = (byte) bin.read();
			switch (current) {
			case CR:
				if (crFlag)
					dataQueue.putByte(CR);
				crFlag = true;
				break;
			case LF:
				if (crFlag)
					exitFlag = true;
				else
					dataQueue.putByte(current);
				break;
			default:
				dataQueue.putByte(current);
				crFlag = false;
				break;
			}
		}
		return new String(dataQueue.getBytes(), WebServer.UTF_8);
	}

	public byte[] readBytes(int length) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead;
		while (dataQueue.available() < length) {
			bytesRead = bin.read(buffer);
			if (bytesRead == -1)
				throw new IllegalStateException("not enough data in stream");
			dataQueue.putBytes(buffer, 0, bytesRead);
		}
		return dataQueue.getBytes();
	}

}
