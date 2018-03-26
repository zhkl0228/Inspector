package com.fuzhu8.inspector.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * @author zhkl0228
 *
 */
public class ByteBufferInspectCache extends AbstractInspectCache {
	
	private final Date date;
	private final String label;
	private final ByteBuffer buffer;
	private final int mode;

	public ByteBufferInspectCache(Date date, String label, ByteBuffer buffer, int mode) {
		super(0x3000);
		this.date = date == null ? new Date() : date;
		this.label = label == null ? "" : label;
		this.buffer = buffer == null ? ByteBuffer.wrap(new byte[0]) : buffer.duplicate();
		this.mode = mode;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.cache.AbstractInspectCache#outputBody(java.io.DataOutputStream)
	 */
	@Override
	protected void outputBody(DataOutputStream out) throws IOException {
		out.writeLong(date.getTime());
		byte[] labelData = this.label.getBytes("UTF-8");
		out.writeInt(labelData.length);
		out.write(labelData);
		out.writeBoolean(buffer != null);
		if(buffer != null) {
			out.writeInt(buffer.remaining());
			byte[] buf = new byte[1024];
			while(buffer.hasRemaining()) {
				int read = buffer.remaining() > 1024 ? 1024 : buffer.remaining();
				buffer.get(buf, 0, read);
				out.write(buf, 0, read);
			}
		}
		out.writeInt(mode);
	}

}
