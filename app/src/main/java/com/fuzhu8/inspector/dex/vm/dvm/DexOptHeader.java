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
public class DexOptHeader extends PointerType {
	
	public DexOptHeader(Pointer p) {
		super(p);
	}
	
	/**
	 * dump odex
	 * @return
	 */
	public ByteBuffer dumpDex() {
		int optOffset = this.getOptOffset();
		int optLength = this.getOptLength();
		return getPointer().getByteBuffer(0x0, optOffset + optLength);
	}

	public byte[] getMagic() {
		return getPointer().getByteArray(0x0, 8);
	}
	
	public int getDexOffset() {
		return getPointer().getInt(0x8);
	}
	
	public int getDexLength() {
		return getPointer().getInt(0xC);
	}
	
	public int getDepsOffset() {
		return getPointer().getInt(0x10);
	}
	
	public int getDepsLength() {
		return getPointer().getInt(0x14);
	}
	
	public int getOptOffset() {
		return getPointer().getInt(0x18);
	}
	
	public int getOptLength() {
		return getPointer().getInt(0x1C);
	}
	
	public int getFlags() {
		return getPointer().getInt(0x20);
	}
	
	public int getChecksum() {
		return getPointer().getInt(0x24);
	}

}
