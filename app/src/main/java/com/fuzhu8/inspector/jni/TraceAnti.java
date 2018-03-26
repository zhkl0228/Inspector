package com.fuzhu8.inspector.jni;

import cn.banny.utils.StringUtils;

/**
 * @author zhkl0228
 *
 */
public class TraceAnti extends Native {
	
	private static final TraceAnti INSTANCE = new TraceAnti();
	
	private TraceAnti() {
		super("TKHooklib", "DexHunter", "TraceAnti");
	}
	
	public static TraceAnti getInstance() {
		return INSTANCE;
	}
	
	public void traceAnti(String dataDir,
			boolean antiThreadCreate,
			boolean traceFile,
			boolean traceSysCall,
			boolean traceTrace,
			int patchSSL) {
		checkSupported();
		
		_traceAnti(dataDir, antiThreadCreate, traceFile, traceSysCall, traceTrace, patchSSL);
	}
	
	/**
	 * 执行anti反调试
	 * @param antiThreadCreate true表示不创建本地线程
	 * @param traceFile true表示跟踪file相关的函数
	 * @param traceSysCall true表示跟踪相关系统调用
	 * @param traceTrace true表示跟踪ptrace相关函数
	 * @param patchSSL true表示处理SSL
	 */
	private native void _traceAnti(String dataDir,
			boolean antiThreadCreate,
			boolean traceFile,
			boolean traceSysCall,
			boolean traceTrace,
			int patchSSL);
	
	public void testAntiHook(int r0, int r1, int r2, int r3) {
		checkSupported();
		
		_testAntiHook(r0, r1, r2, r3);
	}
	
	/**
	 * 测试native anti-hook
	 * @param r0 寄存器R0
	 * @param r1 寄存器R1
	 * @param r2 寄存器R2
	 * @param r3 寄存器R3
	 */
	private native void _testAntiHook(int r0, int r1, int r2, int r3);
	
	public boolean nativeHook(int addr) {
		checkSupported();
		
		return _nativeHook(addr);
	}
	
	/**
	 * native arm hook
	 */
	private native boolean _nativeHook(int addr);
	
	public void enableCollectBytecode(String filter) {
		checkSupported();
		
		_enableCollectBytecode(StringUtils.isEmpty(filter) ? null : filter);
	}
	
	/**
	 * 打开收集字节码功能
	 * @param filter 过滤
	 */
	private native void _enableCollectBytecode(String filter);

	@Override
	protected boolean hasSupported() {
		return Feature.supportDvm();
	}

}
