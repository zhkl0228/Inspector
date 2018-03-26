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
public class DexTypeId extends PointerType {
	
	static final byte SIZE_OF_DEX_TYPE_ID = 4;
	
	DexTypeId(Pointer p) {
		super(p);
	}

	public int getDescriptorIdx() {
		return getPointer().getInt(0x0);
	}

}
