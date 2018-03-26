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
public class ClassPathEntry extends PointerType {
	
	static final byte SIZE_OF_CLASS_PATH_ENTRY = 12;
	
	ClassPathEntry(Pointer p) {
		super(p);
	}
	
	public ClassPathEntryKind getKind() {
		int kind = getPointer().getInt(0x0);
		switch (kind) {
		case 0:
			return ClassPathEntryKind.kCpeUnknown;
		case 1:
			return ClassPathEntryKind.kCpeJar;
		case 2:
			return ClassPathEntryKind.kCpeDex;
		case 3:
			return ClassPathEntryKind.kCpeLastEntry;
		default:
			throw new RuntimeException("Unknown kind: " + kind);
		}
	}
	
	public String getFileName() {
		Pointer pointer = getPointer().getPointer(0x4);
		return pointer.getString(0x0);
	}
	
	private Pointer getPtr() {
		return getPointer().getPointer(0x8);
	}
	
	public DvmDex getDvmDex() {
		switch (getKind()) {
		case kCpeDex:
			return new RawDexFile(getPtr()).getDvmDex();
		case kCpeJar:
			return new JarFile(getPtr()).getDvmDex();
		default:
			return null;
		}
	}
	
	public String getCacheFileName() {
		switch (getKind()) {
		case kCpeDex:
			return new RawDexFile(getPtr()).getCacheFileName();
		case kCpeJar:
			return new JarFile(getPtr()).getCacheFileName();
		default:
			return null;
		}
	}

}
