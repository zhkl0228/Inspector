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
public class DexClassDef extends PointerType {
	
	public static final byte SIZE_OF_DEX_CLASS_DEF = 32;

	DexClassDef(Pointer p) {
		super(p);
	}
	
	public int getClassIdx() {
		return getPointer().getInt(0x0);
	}
	
	public int getAccessFlags() {
		return getPointer().getInt(0x4);
	}
	
	public int getSuperclassIdx() {
		return getPointer().getInt(0x8);
	}
	
	public int getInterfacesOff() {
		return getPointer().getInt(0xC);
	}
	
	public int getSourceFileIdx() {
		return getPointer().getInt(0x10);
	}
	
	public int getAnnotationsOff() {
		return getPointer().getInt(0x14);
	}
	
	public int getClassDataOff() {
		return getPointer().getInt(0x18);
	}
	
	public int getStaticValuesOff() {
		return getPointer().getInt(0x1C);
	}

}
