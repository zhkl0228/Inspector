package com.fuzhu8.inspector.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author zhkl0228
 *
 */
public class InputStreamCache extends SaveFileCache implements InspectCache {
	
	private final InputStream inputStream;
	private final int length;

	public InputStreamCache(String filename, InputStream inputStream, int length) {
		super(filename);
		this.inputStream = inputStream;
		this.length = length;
	}

	@Override
	protected void writeFileData(DataOutputStream out) throws IOException {
		out.writeInt(length);
		
		byte[] buf = new byte[1024];
		int len;
		while((len = inputStream.read(buf)) != -1) {
			out.write(buf, 0, len);
			out.flush();
		}
	}

}
