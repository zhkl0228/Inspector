/**
 * 
 */
package com.fuzhu8.inspector.dex.vm.dvm;

import com.fuzhu8.inspector.dex.vm.DexFile;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * @author zhkl0228
 *
 */
public class DexTypeList extends PointerType {
	
	private final DexFile dexFile;

	DexTypeList(Pointer p, DexFile dexFile) {
		super(p);
		this.dexFile = dexFile;
	}
	
	public int getSize() {
		return getPointer().getInt(0x0);
	}
	
	public short[] getList() {
		return getPointer().getShortArray(0x4, getSize());
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append('(');
		for(short idx : getList()) {
			buffer.append(dexFile.dexStringByTypeIdx(idx & 0xFFFF));
		}
		buffer.append(')');
		return buffer.toString();
	}

}
