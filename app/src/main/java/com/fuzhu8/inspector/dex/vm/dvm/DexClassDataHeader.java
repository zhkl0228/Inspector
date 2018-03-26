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
public class DexClassDataHeader extends PointerType {

	DexClassDataHeader(Pointer p) {
		super(p);
	}
	
	public int getStaticFieldsSize() {
		return getPointer().getInt(0x0);
	}
	
	public int getInstanceFieldsSize() {
		return getPointer().getInt(0x4);
	}
	
	public int getDirectMethodsSize() {
		return getPointer().getInt(0x8);
	}
	
	public int getVirtualMethodsSize() {
		return getPointer().getInt(0xC);
	}

}
