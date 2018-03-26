/**
 * 
 */
package com.fuzhu8.inspector.maps;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * @author zhkl0228
 *
 */
interface CLibrary extends Library {
	
	CLibrary INSTANCE = (CLibrary) Native.loadLibrary("c", CLibrary.class);
	
	int PROT_NONE = 0x0;
	int PROT_READ = 0x1;
	int PROT_WRITE = 0x2;
	int PROT_EXEC = 0x4;
	
	int mprotect(Pointer addr, int len, int prot);

}
