package com.fuzhu8.inspector.sdk;

import java.util.List;

/**
 * @author zhkl0228
 *
 */
public class ApkWrapper {
	
	private final Apk inspectorApk;
	private final List<PluginApk> pluginApks;
	private final Apk targetApk;
	
	public ApkWrapper(Apk inspectorApk, List<PluginApk> pluginApks, Apk targetApk) {
		super();
		this.inspectorApk = inspectorApk;
		this.pluginApks = pluginApks;
		this.targetApk = targetApk;
	}

	public Apk getInspectorApk() {
		return inspectorApk;
	}

	public List<PluginApk> getPluginApks() {
		return pluginApks;
	}

	public Apk getTargetApk() {
		return targetApk;
	}

}
