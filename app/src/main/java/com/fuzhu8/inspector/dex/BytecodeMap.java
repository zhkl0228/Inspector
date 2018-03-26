package com.fuzhu8.inspector.dex;

import com.fuzhu8.inspector.dex.vm.dvm.DexCode;
import com.fuzhu8.inspector.dex.vm.dvm.Method;
import com.sun.jna.Pointer;

/**
 * @author zhkl0228
 *
 */
public class BytecodeMap extends DexCode implements BytecodeMethod {

	public BytecodeMap(Pointer p) {
		super(p);
	}
	
	public Method getMethod() {
		return new Method(getPointer().getPointer(0x0));
	}
	
	public String getKey() {
		return getMethod().getKey();
	}
	
	public short[] getInstructions() {
		int insnsSize = getPointer().getInt(0x4);
		return getPointer().getShortArray(0x8, insnsSize);
	}
	
	public BytecodeMap getNext() {
		Pointer pointer = getPointer().getPointer(0xC);
		if(pointer == null) {
			return null;
		}
		return new BytecodeMap(pointer);
	}

	@Override
	public int getRegistersSize() {
		return getMethod().getRegistersSize();
	}

	@Override
	public int getInsSize() {
		return getMethod().getInsSize();
	}

	@Override
	public int getOutsSize() {
		return getMethod().getOutsSize();
	}

	@Override
	public int getDebugInfoOff() {
		throw new UnsupportedOperationException();
	}

}
