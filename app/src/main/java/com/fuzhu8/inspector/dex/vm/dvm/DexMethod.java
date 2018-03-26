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
public class DexMethod extends PointerType {
	
	static final byte SIZE_OF_DEX_METHOD = 12;

	DexMethod(Pointer p) {
		super(p);
	}
	
	public int getMethodIdx() {
		return getPointer().getInt(0x0);
	}
	
	public int getAccessFlags() {
		return getPointer().getInt(0x4);
	}
	
	public int getCodeOff() {
		return getPointer().getInt(0x8);
	}

}
