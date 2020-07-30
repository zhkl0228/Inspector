package com.fuzhu8.inspector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhkl0228
 *
 */
public class LibraryAbi {
	
	private final File appLibDir;
	private final String abi;
	
	LibraryAbi(File appLibDir, String abi) {
		super();
		this.appLibDir = appLibDir;
		this.abi = abi;
	}

	public File getAppLibDir() {
		return appLibDir;
	}

	public String getAbi() {
		return abi;
	}
	
	public final Map<String, Long> lastApkModified = new HashMap<>();

}
