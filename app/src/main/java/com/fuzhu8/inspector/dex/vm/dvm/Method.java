package com.fuzhu8.inspector.dex.vm.dvm;

import java.lang.reflect.Modifier;

import com.fuzhu8.inspector.dex.ClassMethod;
import com.fuzhu8.inspector.dex.vm.DexFile;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * @author zhkl0228
 *
 */
public class Method extends PointerType {
	
	static final int SIZE_OF_METHOD = 56;
	
	public Method(Pointer p) {
		super(p);
	}
	
	private ClassObject getClassObject() {
		return new ClassObject(getPointer().getPointer(0x0));
	}

	public int getAccessFlags() {
		return getPointer().getInt(0x4);
	}
	
	public int getMethodIndex() {
		return getPointer().getShort(0x8) & 0xFFFF;
	}
	
	public int getRegistersSize() {
		return getPointer().getShort(0xA) & 0xFFFF;
	}
	
	public int getOutsSize() {
		return getPointer().getShort(0xC) & 0xFFFF;
	}
	
	public int getInsSize() {
		return getPointer().getShort(0xE) & 0xFFFF;
	}
	
	public String getName() {
		return getPointer().getPointer(0x10).getString(0x0);
	}
	
	private DexFile getDexFile() {
		return new DvmDexFile(getPointer().getPointer(0x14));
	}
	
	public DexProto getDexProto() {
		DexFile dexFile = getDexFile();
		int protoIdx = getPointer().getInt(0x18);
		return new DexProto(dexFile, protoIdx);
	}
	
	public String getShorty() {
		return getPointer().getPointer(0x1C).getString(0x0);
	}
	
	public String getKey() {
		DexFile dexFile = getDexFile();
		int protoIdx = this.getDexProto().getProtoIdx();
		DexProtoId dexProtoId = dexFile.getProtoId(protoIdx);
		
		DexTypeList dexTypeList = dexFile.dexGetProtoParameters(dexProtoId);
		String retType = dexFile.dexStringByTypeIdx(dexProtoId.getReturnTypeIdx());
		
		return getClassObject().getDescriptor() + "->" + this.getName() + (dexTypeList == null ? "()" : dexTypeList) + retType;
	}
	
	public ClassMethod getInsns() {
		int accessFlags = getAccessFlags();
		if(Modifier.isAbstract(accessFlags)) {
			return null;
		}
		
		int addr = getPointer().getInt(0x20);
		if(addr == 0) {
			return null;
		}
		
		if(Modifier.isNative(accessFlags)) {
			return new NativeMethod(addr);
		}
		
		return new DexCode(new Pointer(addr - 0x10));
	}

}
