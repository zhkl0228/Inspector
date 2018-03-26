/**
 * 
 */
package com.fuzhu8.inspector.dex.vm.dvm;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * @author zhkl0228
 *
 */
public class JarFile extends PointerType {

	JarFile(Pointer p) {
		super(p);
	}
	
	public String getCacheFileName() {
		return getPointer().getPointer(0x24).getString(0x0);
	}
	
	public DvmDex getDvmDex() {
		return new DvmDex(getPointer().getPointer(0x28));
	}

}
