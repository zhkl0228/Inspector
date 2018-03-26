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
public class DexStringId extends PointerType {
	
	static final byte SIZE_OF_DEX_STRING_ID = 4;

	DexStringId(Pointer p) {
		super(p);
	}
	
	public int getStringDataOff() {
		return getPointer().getInt(0x0);
	}

}
