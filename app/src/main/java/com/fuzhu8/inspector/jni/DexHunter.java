package com.fuzhu8.inspector.jni;

import java.nio.ByteBuffer;

import com.sun.jna.Platform;

/**
 * @author zhkl0228
 *
 */
public class DexHunter extends Native {
	
	private static final DexHunter INSTANCE = new DexHunter();
	
	private DexHunter() {
		super("DexHunter");
	}
	
	public static DexHunter getInstance() {
		return INSTANCE;
	}
	
	public void saveDexFileByCookie(long cookie, String dataDir) {
		checkSupported();
		
		_saveDexFileByCookie((int) cookie, dataDir);
	}

	private native void _saveDexFileByCookie(int cookie, String dataDir);
	
	public ByteBuffer dumpDexFileByCookie(long cookie, ClassLoader loader) {
		checkSupported();
		
		return _dumpDexFileByCookie((int) cookie, loader);
	}
    
    /**
     * DexHunter
     */
	private native ByteBuffer _dumpDexFileByCookie(int cookie, ClassLoader loader);

	@Override
	protected boolean hasSupported() {
		return !Platform.is64Bit() && Feature.isDvm();
	}

}
