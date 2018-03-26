package com.fuzhu8.inspector.dex.vm;

import java.nio.ByteBuffer;

import com.fuzhu8.inspector.dex.vm.dvm.DexClassDef;
import com.fuzhu8.inspector.dex.vm.dvm.DexFieldId;
import com.fuzhu8.inspector.dex.vm.dvm.DexFileHeadersPointer;
import com.fuzhu8.inspector.dex.vm.dvm.DexHeader;
import com.fuzhu8.inspector.dex.vm.dvm.DexMethodId;
import com.fuzhu8.inspector.dex.vm.dvm.DexOptHeader;
import com.fuzhu8.inspector.dex.vm.dvm.DexProtoId;
import com.fuzhu8.inspector.dex.vm.dvm.DexStringId;
import com.fuzhu8.inspector.dex.vm.dvm.DexTypeId;
import com.fuzhu8.inspector.dex.vm.dvm.DexTypeList;
import com.sun.jna.Pointer;

public interface DexFile {

	DexFileHeadersPointer readDexFileHeadersPointer();

	DexOptHeader getDexOptHeader();

	DexHeader getHeader();

	DexStringId getStringId(int index);

	String dexGetClassDescriptor(DexClassDef classDef);

	String dexStringByTypeIdx(int idx);

	String dexStringById(int idx);

	DexTypeId dexGetTypeId(int index);

	DexFieldId getFieldId(int index);

	DexMethodId getMethodId(int index);

	DexProtoId getProtoId(int index);

	DexTypeList dexGetProtoParameters(DexProtoId dexProtoId);

	DexClassDef getClassDef(int index);

	Pointer dexGetClassData(DexClassDef classDef);

	Pointer getLinkData();

	Pointer getClassLookup();

	Pointer getRegisterMapPool();

	Pointer getBaseAddr();

	int getOverhead();

	ByteBuffer getDex();

}