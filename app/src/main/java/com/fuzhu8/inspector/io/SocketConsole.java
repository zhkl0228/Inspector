package com.fuzhu8.inspector.io;

import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author zhkl0228
 *
 */
public class SocketConsole implements Console {
	
	private Closeable socket;
	
	private DataInputStream reader;
	private OutputStream outputStream;
	
	public synchronized void open(Closeable obj, InputStream inputStream, OutputStream outputStream) {
		this.socket = obj;
		this.outputStream = outputStream;
		this.reader = new DataInputStream(inputStream);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.io.Console#close()
	 */
	@Override
	public synchronized void close() {
		try { socket.close(); } catch(Exception ignored) {}
		IOUtils.closeQuietly(reader);
		IOUtils.closeQuietly(outputStream);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.io.Console#write(byte[])
	 */
	@Override
	public synchronized void write(byte[] data) throws IOException {
		outputStream.write(data);
		outputStream.flush();
	}

	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}

	@Override
	public Command readCommand() throws IOException {
		int type = reader.readUnsignedShort();
		int length;
		byte[] data;
		switch (type) {
		case 0:
			return new TextCommand(reader.readUTF());
		case 1:
			length = reader.readInt();
			data = new byte[length];
			reader.readFully(data);
			return new LuaCommand(new String(data, StandardCharsets.UTF_8));
		case 2:
			length = reader.readInt();
			data = new byte[length];
			reader.readFully(data);
			return new PcapCommand(data);
		case 5:
			return new AddressCommand(reader.readUTF());
		default:
			throw new IllegalArgumentException("Unknown command: " + type);
		}
	}

}
