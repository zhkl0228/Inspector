package com.fuzhu8.inspector.dex.vm.dvm;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.BytecodeMethod;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * @author zhkl0228
 *
 */
public class DexCode extends PointerType implements BytecodeMethod {

	protected DexCode(Pointer p) {
		super(p);
	}

	@Override
	public int getRegistersSize() {
		return getPointer().getShort(0x0) & 0xFFFF;
	}

	@Override
	public int getInsSize() {
		return getPointer().getShort(0x2) & 0xFFFF;
	}

	@Override
	public int getOutsSize() {
		return getPointer().getShort(0x4) & 0xFFFF;
	}

	@Override
	public int getDebugInfoOff() {
		return getPointer().getInt(0x8);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.DexMethod#getInstructions()
	 */
	@Override
	public short[] getInstructions() {
		int insnsSize = getPointer().getInt(0xC);
		return getPointer().getShortArray(0x10, insnsSize);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.DexMethod#dump(java.lang.String, com.fuzhu8.inspector.Inspector)
	 */
	@Override
	public void dump(String label, Inspector inspector) {
		inspector.inspect(getInstructions(), label);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.DexMethod#isNative()
	 */
	@Override
	public boolean isBytecodeMethod() {
		return true;
	}

}
