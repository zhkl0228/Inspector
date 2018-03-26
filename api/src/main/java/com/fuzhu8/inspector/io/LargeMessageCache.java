package com.fuzhu8.inspector.io;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * @author zhkl0228
 *
 */
public class LargeMessageCache extends AbstractInspectCache implements InspectCache {
	
	private final String msg;

	public LargeMessageCache(String msg, boolean out) {
		super(out ? 0x1100 : 0x1102);
		this.msg = msg;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.InspectCache#output(java.io.DataOutputStream)
	 */
	@Override
	protected void outputBody(DataOutputStream out) throws IOException {
		byte[] data = msg.getBytes("UTF-8");
		out.writeInt(data.length);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		byte[] buf = new byte[1024];
		int read;
		while((read = bais.read(buf)) != -1) {
			out.write(buf, 0, read);
			out.flush();
		}
	}

}
