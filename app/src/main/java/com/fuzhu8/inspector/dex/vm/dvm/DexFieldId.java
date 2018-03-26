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
public class DexFieldId extends PointerType {
	
	static final byte SIZE_OF_DEX_FIELD_ID = 8;

	DexFieldId(Pointer p) {
		super(p);
	}
	
	public int getClassIdx() {
		return getPointer().getShort(0x0) & 0xFFFF;
	}
	
	public int getTypeIdx() {
		return getPointer().getShort(0x2) & 0xFFFF;
	}
	
	public int getNameIdx() {
		return getPointer().getInt(0x4);
	}

}
