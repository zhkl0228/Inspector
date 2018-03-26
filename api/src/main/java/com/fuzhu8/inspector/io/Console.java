package com.fuzhu8.inspector.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author zhkl0228
 *
 */
public interface Console {
	
	void close();
	
	@Deprecated
	void write(byte[] data) throws IOException;
	
	OutputStream getOutputStream();
	
	Command readCommand() throws IOException;
	
	void open(Closeable obj, InputStream inputStream, OutputStream outputStream) throws IOException;

}
