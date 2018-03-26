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
public class MemMapping extends PointerType {

	MemMapping(Pointer p) {
		super(p);
	}
	
	public Pointer getAddr() {
		return getPointer().getPointer(0x0);
	}
	
	public int getLength() {
		return getPointer().getInt(0x4);
	}
	
	public Pointer getBaseAddr() {
		return getPointer().getPointer(0x8);
	}
	
	public int getBaseLength() {
		return getPointer().getInt(0xC);
	}

}
