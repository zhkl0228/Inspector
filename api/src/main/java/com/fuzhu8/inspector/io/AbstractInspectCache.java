package com.fuzhu8.inspector.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author zhkl0228
 *
 */
public abstract class AbstractInspectCache implements InspectCache {
	
	private final int type;
	private final boolean zip;

	AbstractInspectCache(int type) {
		this(type, false);
	}

	public AbstractInspectCache(int type, boolean zip) {
		super();
		this.type = type;
		this.zip = zip;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.InspectCache#writeTo(com.fuzhu8.inspector.io.Console)
	 */
	@Override
	public final void writeTo(Console console) throws IOException {
		if(console == null) {
			throw new IllegalArgumentException();
		}
		
		synchronized (console) {
			OutputStream outputStream = console.getOutputStream();
			DataOutputStream out = new DataOutputStream(outputStream);
			if(zip) {
				out.writeShort((1 << 15) | type);
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				GZIPOutputStream gzip = new GZIPOutputStream(baos);
				DataOutputStream myOut = new DataOutputStream(gzip);
				outputBody(myOut);
				myOut.flush();
				gzip.flush();
				gzip.close();
				
				out.writeInt(baos.size());
				out.write(baos.toByteArray());
				out.flush();
			} else {
				out.writeShort(type);
				outputBody(out);
				out.flush();
			}
		}
	}
	
	protected abstract void outputBody(DataOutputStream out) throws IOException;

	@Override
	public boolean canCache() {
		return true;
	}

}
