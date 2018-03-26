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
public class DexProtoId extends PointerType {
	
	static final byte SIZE_OF_DEX_PROTO_ID = 12;

	DexProtoId(Pointer p) {
		super(p);
	}
	
	public int getShortyIdx() {
		return getPointer().getInt(0x0);
	}
	
	public int getReturnTypeIdx() {
		return getPointer().getInt(0x4);
	}
	
	public int getParametersOff() {
		return getPointer().getInt(0x8);
	}

}
