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
public class DexField extends PointerType {
	
	static final byte SIZE_OF_DEX_FIELD = 4;

	DexField(Pointer p) {
		super(p);
	}
	
	public int getFieldIdx() {
		return getPointer().getInt(0x0);
	}
	
	public int getAccessFlags() {
		return getPointer().getInt(0x4);
	}

}
