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
public class DexClassData extends PointerType {
	
	DexClassData(Pointer p) {
		super(p);
	}

	public DexClassDataHeader getDexClassDataHeader() {
		return new DexClassDataHeader(getPointer());
	}
	
	public DexField getStaticField(int idx) {
		Pointer pointer = getPointer().getPointer(0x10);
		if(pointer == null) {
			return null;
		}
		
		return new DexField(pointer.share(DexField.SIZE_OF_DEX_FIELD * idx));
	}
	
	public DexField getInstanceField(int idx) {
		Pointer pointer = getPointer().getPointer(0x14);;
		if(pointer == null) {
			return null;
		}
		
		return new DexField(pointer.share(DexField.SIZE_OF_DEX_FIELD * idx));
	}
	
	public DexMethod getDirectMethod(int idx) {
		Pointer pointer = getPointer().getPointer(0x18);
		if(pointer == null) {
			return null;
		}
		return new DexMethod(pointer.share(DexMethod.SIZE_OF_DEX_METHOD * idx));
	}
	
	public DexMethod getVirtualMethod(int idx) {
		Pointer pointer = getPointer().getPointer(0x1C);
		if(pointer == null) {
			return null;
		}
		return new DexMethod(pointer.share(DexMethod.SIZE_OF_DEX_METHOD * idx));
	}

}
