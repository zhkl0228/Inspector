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
public class DvmDex extends PointerType {

	DvmDex(Pointer p) {
		super(p);
	}
	
	public DvmDexFile getDexFile() {
		return new DvmDexFile(getPointer().getPointer(0x0));
	}
	
	public DexHeader getHeader() {
		return new DexHeader(getPointer().getPointer(0x4));
	}
	
	public Pointer getResStrings() {
		return getPointer().getPointer(0x8);
	}
	
	public Pointer getResClasses() {
		return getPointer().getPointer(0xC);
	}
	
	public Pointer getResMethods() {
		return getPointer().getPointer(0x10);
	}
	
	public Pointer getResFields() {
		return getPointer().getPointer(0x14);
	}
	
	public Pointer getInterfaceCache() {
		return getPointer().getPointer(0x18);
	}
	
	public boolean isMappedReadOnly() {
		return getPointer().getByte(0x1C) != 0;
	}
	
	public MemMapping getMemMap() {
		return new MemMapping(getPointer().share(0x20));
	}

}
