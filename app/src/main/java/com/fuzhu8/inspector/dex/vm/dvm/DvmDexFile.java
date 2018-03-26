/**
 * 
 */
package com.fuzhu8.inspector.dex.vm.dvm;

import java.nio.ByteBuffer;

import com.fuzhu8.inspector.dex.vm.DexFile;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * @author zhkl0228
 *
 */
public class DvmDexFile extends PointerType implements DexFile {

	DvmDexFile(Pointer p) {
		super(p);
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#readDexFileHeadersPointer()
	 */
	@Override
	public DexFileHeadersPointer readDexFileHeadersPointer() {
		Pointer pointer = getPointer();
		int  baseAddr = pointer.getInt(0x2C);
	    int  pStringIds = pointer.getInt(0x8);
	    int  pTypeIds = pointer.getInt(0xC);
	    int  pFieldIds = pointer.getInt(0x10);
	    int  pMethodIds = pointer.getInt(0x14);
	    int  pProtoIds = pointer.getInt(0x18);
	    int  pClassDefs = pointer.getInt(0x1C);
	    int  classCount = getHeader().getClassDefsSize();
	    return new DexFileHeadersPointer(baseAddr, pStringIds, pTypeIds, pFieldIds, pMethodIds, pProtoIds, pClassDefs, classCount);
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getDexOptHeader()
	 */
	@Override
	public DexOptHeader getDexOptHeader() {
		Pointer pointer = getPointer().getPointer(0x0);
		return pointer == null ? null : new DexOptHeader(pointer);
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getHeader()
	 */
	@Override
	public DexHeader getHeader() {
		Pointer pointer = getPointer().getPointer(0x4);
		return pointer == null ? null : new DexHeader(pointer);
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getStringId(int)
	 */
	@Override
	public DexStringId getStringId(int index) {
		Pointer pointer = getPointer().getPointer(0x8);
		return new DexStringId(pointer.share(index * DexStringId.SIZE_OF_DEX_STRING_ID));
	}
	
	private String dexGetStringData(DexStringId dexStringId) {
		Pointer pointer = getBaseAddr().share(dexStringId.getStringDataOff());
		int offset = 0;
		while((pointer.getByte(offset++) & 0xFF) > 0x7F) {
			// Skip the uleb128 length.
		}
		return pointer.getString(offset);
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#dexGetClassDescriptor(com.fuzhu8.inspector.dex.vm.dvm.DexClassDef)
	 */
	@Override
	public String dexGetClassDescriptor(DexClassDef classDef) {
		return dexStringByTypeIdx(classDef.getClassIdx());
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#dexStringByTypeIdx(int)
	 */
	@Override
	public String dexStringByTypeIdx(int idx) {
		DexTypeId typeId = dexGetTypeId(idx);
		return dexStringById(typeId.getDescriptorIdx());
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#dexStringById(int)
	 */
	@Override
	public String dexStringById(int idx) {
		DexStringId pStringId = getStringId(idx);
		return dexGetStringData(pStringId);
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#dexGetTypeId(int)
	 */
	@Override
	public DexTypeId dexGetTypeId(int index) {
		Pointer pointer = getPointer().getPointer(0xC);
		return new DexTypeId(pointer.share(index * DexTypeId.SIZE_OF_DEX_TYPE_ID));
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getFieldId(int)
	 */
	@Override
	public DexFieldId getFieldId(int index) {
		Pointer pointer = getPointer().getPointer(0x10);
		return new DexFieldId(pointer.share(index * DexFieldId.SIZE_OF_DEX_FIELD_ID));
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getMethodId(int)
	 */
	@Override
	public DexMethodId getMethodId(int index) {
		Pointer pointer = getPointer().getPointer(0x14);
		return new DexMethodId(pointer.share(index * DexMethodId.SIZE_OF_DEX_METHOD_ID));
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getProtoId(int)
	 */
	@Override
	public DexProtoId getProtoId(int index) {
		Pointer pointer = getPointer().getPointer(0x18);
		return new DexProtoId(pointer.share(index * DexProtoId.SIZE_OF_DEX_PROTO_ID));
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#dexGetProtoParameters(com.fuzhu8.inspector.dex.vm.dvm.DexProtoId)
	 */
	@Override
	public DexTypeList dexGetProtoParameters(DexProtoId dexProtoId) {
		int parametersOff = dexProtoId.getParametersOff();
		if(parametersOff == 0) {
			return null;
		}
		
		return new DexTypeList(getBaseAddr().share(parametersOff), this);
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getClassDef(int)
	 */
	@Override
	public DexClassDef getClassDef(int index) {
		Pointer pointer = getPointer().getPointer(0x1C);
		return new DexClassDef(pointer.share(index * DexClassDef.SIZE_OF_DEX_CLASS_DEF));
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#dexGetClassData(com.fuzhu8.inspector.dex.vm.dvm.DexClassDef)
	 */
	@Override
	public Pointer dexGetClassData(DexClassDef classDef) {
		if(classDef.getClassDataOff() == 0) {
			return null;
		}
		
		return getBaseAddr().share(classDef.getClassDataOff());
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getLinkData()
	 */
	@Override
	public Pointer getLinkData() {
		return getPointer().getPointer(0x20);
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getClassLookup()
	 */
	@Override
	public Pointer getClassLookup() {
		return getPointer().getPointer(0x24);
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getRegisterMapPool()
	 */
	@Override
	public Pointer getRegisterMapPool() {
		return getPointer().getPointer(0x28);
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getBaseAddr()
	 */
	@Override
	public Pointer getBaseAddr() {
		return getPointer().getPointer(0x2C);
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getOverhead()
	 */
	@Override
	public int getOverhead() {
		return getPointer().getInt(0x30);
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.vm.dvm.Dex#getDex()
	 */
	@Override
	public ByteBuffer getDex() {
		DexOptHeader dexOptHeader = this.getDexOptHeader();
		if(dexOptHeader != null) {
			return dexOptHeader.dumpDex();
		}
		DexHeader dexHeader = this.getHeader();
		return dexHeader == null ? null : dexHeader.dumpDex();
	}

}
