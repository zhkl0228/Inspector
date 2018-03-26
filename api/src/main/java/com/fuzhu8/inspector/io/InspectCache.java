package com.fuzhu8.inspector.io;

import java.io.IOException;

/**
 * @author zhkl0228
 *
 */
public interface InspectCache {
	
	void writeTo(Console console) throws IOException;

	boolean canCache();

}
