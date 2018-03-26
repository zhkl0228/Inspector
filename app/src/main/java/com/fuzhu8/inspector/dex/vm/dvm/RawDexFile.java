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
public class RawDexFile extends PointerType {

	RawDexFile(Pointer p) {
		super(p);
	}
	
	public String getCacheFileName() {
		return getPointer().getPointer(0x0).getString(0x0);
	}
	
	public DvmDex getDvmDex() {
		return new DvmDex(getPointer().getPointer(0x4));
	}

}
