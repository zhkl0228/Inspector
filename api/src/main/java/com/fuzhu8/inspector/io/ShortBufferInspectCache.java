package com.fuzhu8.inspector.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.util.Date;

/**
 * @author zhkl0228
 *
 */
public class ShortBufferInspectCache extends AbstractInspectCache {
	
	private final Date date;
	private final String label;
	private final ShortBuffer buffer;
	private final int mode;

	public ShortBufferInspectCache(Date date, String label, ShortBuffer buffer, int mode) {
		super(0x2001);
		this.date = date == null ? new Date() : date;
		this.label = label == null ? "" : label;
		this.buffer = buffer;
		this.mode = mode;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.cache.AbstractInspectCache#outputBody(java.io.DataOutputStream)
	 */
	@Override
	protected void outputBody(DataOutputStream out) throws IOException {
		out.writeLong(date.getTime());
		out.writeUTF(label);
		out.writeBoolean(buffer != null);
		if(buffer != null) {
			out.writeInt(buffer.remaining());
			while(buffer.hasRemaining()) {
				out.writeShort(buffer.get());
			}
		}
		out.writeInt(mode);
	}

}
