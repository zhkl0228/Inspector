/**
 * 
 */
package com.fuzhu8.inspector.dex.vm.dvm;

import com.fuzhu8.inspector.dex.vm.DexFile;

/**
 * @author zhkl0228
 *
 */
public class DexProto {
	
	private final DexFile dexFile;
	private final int protoIdx;
	
	public DexProto(DexFile dexFile, int protoIdx) {
		super();
		this.dexFile = dexFile;
		this.protoIdx = protoIdx;
	}

	public DexFile getDexFile() {
		return dexFile;
	}

	public int getProtoIdx() {
		return protoIdx;
	}

}
