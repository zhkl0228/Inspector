package com.fuzhu8.inspector.dex.vm;

import com.fuzhu8.inspector.dex.vm.dvm.DexOrJar;
import com.fuzhu8.inspector.jni.Feature;
import com.sun.jna.Pointer;

/**
 * @author zhkl0228
 *
 */
public class DexFileFactory {
	
	public static DexFile createDexFileByCookie(long cookie) {
		if(cookie == 0) {
			throw new IllegalArgumentException("cookie is zero");
		}
		
		if(Feature.isArt()) {
			throw new UnsupportedOperationException();
		}
		
		return createDvmDexFileByCookie(cookie);
	}

	private static DexFile createDvmDexFileByCookie(long cookie) {
		DexOrJar dexOrJar = new DexOrJar(new Pointer(cookie));
		if(dexOrJar.isDex()) {
			return dexOrJar.getRawDexFile().getDvmDex().getDexFile();
		}
		return dexOrJar.getJarFile().getDvmDex().getDexFile();
	}

}
