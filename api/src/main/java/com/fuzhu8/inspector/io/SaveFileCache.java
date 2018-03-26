package com.fuzhu8.inspector.io;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author zhkl0228
 *
 */
public abstract class SaveFileCache extends AbstractInspectCache implements InspectCache {
	
	private final String filename;

	SaveFileCache(String filename) {
		super(0x1001);
		this.filename = filename;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.io.AbstractInspectCache#outputBody(java.io.DataOutputStream)
	 */
	@Override
	protected void outputBody(DataOutputStream out) throws IOException {
		out.writeUTF(filename);
		writeFileData(out);
		out.flush();
	}

	protected abstract void writeFileData(DataOutputStream out) throws IOException;

	@Override
	public boolean canCache() {
		return false;
	}
}
