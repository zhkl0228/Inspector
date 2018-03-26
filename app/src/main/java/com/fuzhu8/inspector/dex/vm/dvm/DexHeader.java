/**
 * 
 */
package com.fuzhu8.inspector.dex.vm.dvm;

import java.nio.ByteBuffer;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * @author zhkl0228
 *
 */
public class DexHeader extends PointerType {

	DexHeader(Pointer p) {
		super(p);
	}
	
	/**
	 * dump dex
	 * @return
	 */
	public ByteBuffer dumpDex() {
		return getPointer().getByteBuffer(0x0, getFileSize());
	}
	
	public byte[] getMagic() {
		return getPointer().getByteArray(0x0, 8);
	}
	
	public int getChecksum() {
		return getPointer().getInt(0x8);
	}
	
	public byte[] getSignature() {
		return getPointer().getByteArray(0xC, 20);
	}
	
	public int getFileSize() {
		return getPointer().getInt(0x20);
	}
	
	public int getHeaderSize() {
		return getPointer().getInt(0x24);
	}
	
	public int getEndianTag() {
		return getPointer().getInt(0x28);
	}
	
	public int getLinkSize() {
		return getPointer().getInt(0x2C);
	}
	
	public int getLinkOff() {
		return getPointer().getInt(0x30);
	}
	
	public int getMapOff() {
		return getPointer().getInt(0x34);
	}
	
	public int getStringIdsSize() {
		return getPointer().getInt(0x38);
	}
	
	public int getStringIdsOff() {
		return getPointer().getInt(0x3C);
	}
	
	public int getTypeIdsSize() {
		return getPointer().getInt(0x40);
	}
	
	public int getTypeIdsOff() {
		return getPointer().getInt(0x44);
	}
	
	public int getProtoIdsSize() {
		return getPointer().getInt(0x48);
	}
	
	public int getProtoIdsOff() {
		return getPointer().getInt(0x4C);
	}
	
	public int getFieldIdsSize() {
		return getPointer().getInt(0x50);
	}
	
	public int getFieldIdsOff() {
		return getPointer().getInt(0x54);
	}
	
	public int getMethodIdsSize() {
		return getPointer().getInt(0x58);
	}
	
	public int getMethodIdsOff() {
		return getPointer().getInt(0x5C);
	}
	
	public int getClassDefsSize() {
		return getPointer().getInt(0x60);
	}
	
	public int getClassDefsOff() {
		return getPointer().getInt(0x64);
	}
	
	public int getDataSize() {
		return getPointer().getInt(0x68);
	}
	
	public int getDataOff() {
		return getPointer().getInt(0x6C);
	}

}
