package com.fuzhu8.inspector.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * @author zhkl0228
 *
 */
public class ByteBufferCache extends SaveFileCache implements InspectCache {
	
	private final ByteBuffer data;

	public ByteBufferCache(String filename, ByteBuffer data) {
		super(filename);
		this.data = data;
	}

	@Override
	protected void writeFileData(DataOutputStream out) throws IOException {
		out.writeInt(this.data.remaining());
		byte[] buf = new byte[1024];
		while(this.data.hasRemaining()) {
			int remaining = this.data.remaining();
			int read = remaining > buf.length ? buf.length : remaining;
			this.data.get(buf, 0, read);
			out.write(buf, 0, read);
			out.flush();
		}
	}

}
