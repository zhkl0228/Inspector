package com.fuzhu8.inspector;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author zhkl0228
 *
 */
public interface Unpacker {
	
	/**
	 * 脱壳
	 */
	ByteBuffer dumpByLinker(String so_path) throws IOException;
	
	String getName();

}
