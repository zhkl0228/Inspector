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
public class DexOrJar extends PointerType {

	public DexOrJar(Pointer p) {
		super(p);
	}
	
	public String readFileName() {
		return getPointer().getPointer(0x0).getString(0x0);
	}
	
	public boolean isDex() {
		return getPointer().getByte(0x4) != 0;
	}
	
	public RawDexFile getRawDexFile() {
		return new RawDexFile(getPointer().getPointer(0x8));
	}
	
	public JarFile getJarFile() {
		return new JarFile(getPointer().getPointer(0xC));
	}

}
