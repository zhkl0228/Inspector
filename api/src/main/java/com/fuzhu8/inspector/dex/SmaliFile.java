package com.fuzhu8.inspector.dex;

import com.fuzhu8.inspector.dex.provider.DexFileProvider;

/**
 * @author zhkl0228
 *
 */
public class SmaliFile {
	
	private final String className;
	private final String smali;
	private final DexFileProvider dex;
	public SmaliFile(String className, String smali, DexFileProvider dex) {
		super();
		this.className = className;
		this.smali = smali;
		this.dex = dex;
	}
	public String getClassName() {
		return className;
	}
	public String getSmali() {
		return smali;
	}
	public DexFileProvider getDex() {
		return dex;
	}

	@Override
	public String toString() {
		return className;
	}

}
