package com.fuzhu8.inspector.io;

import java.io.DataOutputStream;
import java.io.IOException;


/**
 * @author zhkl0228
 *
 */
@Deprecated
public class MessageCache extends AbstractInspectCache implements InspectCache {
	
	private final String msg;

	public MessageCache(String msg, boolean out) {
		super(out ? 0x1000 : 0x1002);
		this.msg = msg;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.InspectCache#output(java.io.DataOutputStream)
	 */
	@Override
	protected void outputBody(DataOutputStream out) throws IOException {
		out.writeUTF(msg);
	}

}
