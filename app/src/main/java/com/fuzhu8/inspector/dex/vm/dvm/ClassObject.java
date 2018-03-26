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
public class ClassObject extends PointerType {
	
	private static final byte CLASS_FIELD_SLOTS = 4;
	
	public ClassObject(Pointer p) {
		super(p);
	}

	public ClassObject getClazz() {
		return new ClassObject(getPointer().getPointer(0x0));
	}
	
	public int getLock() {
		return getPointer().getInt(0x4);
	}
	
	public int[] getInstanceData() {
		return getPointer().getIntArray(0x8, CLASS_FIELD_SLOTS);
	}
	
	public String getDescriptor() {
		return getPointer().getPointer(0x18).getString(0x0);
	}
	
	public String getDescriptorAlloc() {
		return getPointer().getPointer(0x1C).getString(0x0);
	}
	
	public int getAccessFlags() {
		return getPointer().getInt(0x20);
	}
	
	public int getSerialNumber() {
		return getPointer().getInt(0x24);
	}
	
	public DvmDex getDvmDex() {
		Pointer pointer = getPointer().getPointer(0x28);
		return pointer == null ? null : new DvmDex(pointer);
	}
	
	public ClassStatus getStatus() {
		return ClassStatus.valueOf(getPointer().getInt(0x2C));
	}
	
	public ClassObject getVerifyErrorClass() {
		return new ClassObject(getPointer().getPointer(0x30));
	}
	
	public int getInitThreadId() {
		return getPointer().getInt(0x34);
	}
	
	/**
	 * 32位
	 * @return size_t
	 */
	public int getObjectSize() {
		return getPointer().getInt(0x38);
	}
	
	public ClassObject getElementClass() {
		return new ClassObject(getPointer().getPointer(0x3C));
	}
	
	public int getArrayDim() {
		return getPointer().getInt(0x40);
	}
	
	public PrimitiveType getPrimitiveType() {
		return PrimitiveType.valueOf(getPointer().getInt(0x44));
	}
	
	public ClassObject getSuper() {
		return new ClassObject(getPointer().getPointer(0x48));
	}
	
	public Pointer getClassLoader() {
		return getPointer().getPointer(0x4C);
	}
	
	public Pointer getInitiatingLoaders() {
		return getPointer().getPointer(0x50);
	}
	
	public int getInitiatingLoaderCount() {
		return getPointer().getInt(0x54);
	}
	
	public int getInterfaceCount() {
		return getPointer().getInt(0x58);
	}
	
	public ClassObject[] getInterfaces() {
		Pointer[] pointers = getPointer().getPointer(0x5C).getPointerArray(0, getInterfaceCount());
		ClassObject[] objs = new ClassObject[pointers.length];
		for(int i = 0; i < objs.length; i++) {
			objs[i] = new ClassObject(pointers[i]);
		}
		return objs;
	}
	
	public int getDirectMethodCount() {
		return getPointer().getInt(0x60);
	}
	
	public Method[] getDirectMethods() {
		Pointer pointer = getPointer().getPointer(0x64);
		Method[] methods = new Method[getDirectMethodCount()];
		for(int index = 0; index < methods.length; index++) {
			methods[index] = new Method(pointer.share(index * Method.SIZE_OF_METHOD));
		}
		return methods;
	}
	
	public int getVirtualMethodCount() {
		return getPointer().getInt(0x68);
	}
	
	public Method[] getVirtualMethods() {
		Pointer pointer = getPointer().getPointer(0x6C);
		Method[] methods = new Method[getVirtualMethodCount()];
		for(int index = 0; index < methods.length; index++) {
			methods[index] = new Method(pointer.share(index * Method.SIZE_OF_METHOD));
		}
		return methods;
	}
	
	public int getVtableCount() {
		return getPointer().getInt(0x70);
	}
	
	public Method[] getVtable() {
		Pointer[] pointers = getPointer().getPointer(0x74).getPointerArray(0, getVtableCount());
		Method[] methods = new Method[pointers.length];
		for(int i = 0; i < methods.length; i++) {
			methods[i] = new Method(pointers[i]);
		}
		return methods;
	}
	
	public int getIftableCount() {
		return getPointer().getInt(0x78);
	}
	
	public Pointer getIftable() {
		return getPointer().getPointer(0x7C);
	}
	
	public int getIfviPoolCount() {
		return getPointer().getInt(0x80);
	}
	
	public int[] getIfviPool() {
		return getPointer().getPointer(0x84).getIntArray(0, getIfviPoolCount());
	}
	
	public int getIfieldCount() {
		return getPointer().getInt(0x88);
	}
	
	public int getIfieldRefCount() {
		return getPointer().getInt(0x8C);
	}
	
	public Pointer getIfields() {
		return getPointer().getPointer(0x90);
	}
	
	public int getRefOffsets() {
		return getPointer().getInt(0x94);
	}
	
	public String getSourceFile() {
		return getPointer().getPointer(0x98).getString(0x0);
	}
	
	public int getSfieldCount() {
		return getPointer().getInt(0x9C);
	}

}
