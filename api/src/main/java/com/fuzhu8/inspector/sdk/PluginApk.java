package com.fuzhu8.inspector.sdk;

import java.io.File;

/**
 * @author zhkl0228
 *
 */
public class PluginApk extends Apk {

	private final String pluginClassName;
	
	public PluginApk(String versionName, int versionCode, File apkFile, String packageName, String pluginClassName) {
		super(versionName, versionCode, apkFile, packageName);
		this.pluginClassName = pluginClassName;
	}

	public String getPluginClassName() {
		return pluginClassName;
	}

	@Override
	public String toString() {
		return "PluginApk [pluginClassName=" + pluginClassName + "]";
	}

}
