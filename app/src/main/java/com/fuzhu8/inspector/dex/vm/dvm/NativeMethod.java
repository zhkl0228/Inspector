package com.fuzhu8.inspector.dex.vm.dvm;

import java.util.Locale;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.ClassMethod;

/**
 * @author zhkl0228
 *
 */
public class NativeMethod implements ClassMethod {
	
	private final int addr;

	public NativeMethod(int addr) {
		super();
		
		this.addr = addr;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.dex.DexMethod#dump(java.lang.String, com.fuzhu8.inspector.Inspector)
	 */
	@Override
	public void dump(String label, Inspector inspector) {
		if(addr != 0) {//只dump调用过的本地函数
			inspector.println("addr=0x" + Integer.toHexString(addr) + ", " + label);
		}
	}

	@Override
	public short[] getInstructions() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isBytecodeMethod() {
		return false;
	}

}
