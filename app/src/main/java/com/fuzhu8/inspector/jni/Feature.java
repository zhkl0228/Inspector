package com.fuzhu8.inspector.jni;

import com.sun.jna.Platform;

import android.os.SystemProperties;

import cn.banny.utils.StringUtils;

/**
 * @author zhkl0228
 *
 */
public class Feature {
	
	private static final String DVM_LIB_KEY = "persist.sys.dalvik.vm.lib";
	private static final String ART_LIB_KEY = "persist.sys.dalvik.vm.lib.2";
	
	public static boolean isArt() {
		return "libart.so".equals(SystemProperties.get(ART_LIB_KEY));
	}
	public static boolean isDvm() {
		if("libdvm.so".equals(SystemProperties.get(DVM_LIB_KEY))) {
			return true;
		}
		
		String dvmLib = SystemProperties.get(DVM_LIB_KEY);
		String artLib = SystemProperties.get(ART_LIB_KEY);
		return StringUtils.isEmpty(dvmLib) && StringUtils.isEmpty(artLib);
	}
	
	public static boolean supportDvm() {
		return !(Platform.is64Bit() || !Platform.isARM()) && !isArt() && isDvm();

	}

}
