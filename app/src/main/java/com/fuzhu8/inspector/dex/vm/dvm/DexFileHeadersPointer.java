package com.fuzhu8.inspector.dex.vm.dvm;


/**
 * @author zhkl0228
 *
 */
public class DexFileHeadersPointer extends com.fuzhu8.inspector.raw.dex.smali.DexFileHeadersPointer {
	
	public DexFileHeadersPointer(int baseAddr, int pStringIds, int pTypeIds,
			int pFieldIds, int pMethodIds, int pProtoIds, int pClassDefs,
			int classCount) {
		super();
		
		setBaseAddr(baseAddr);
		setStringIds(pStringIds);
		setTypeIds(pTypeIds);
		setFieldIds(pFieldIds);
		setMethodIds(pMethodIds);
		setProtoIds(pProtoIds);
		setClassDefs(pClassDefs);
		setClassCount(classCount);
	}

}
