package com.fuzhu8.inspector.plugin;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.Hooker;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.script.LuaScriptManager;

import java.io.File;

/**
 * @author zhkl0228
 *
 */
public class PluginContext {
	
	private final Inspector inspector;
	private final DexFileManager dexFileManager;
	private final LuaScriptManager scriptManager;
	private final ModuleContext context;
	private final String versionName;
	private final int versionCode;
	private final File inspectorApk;
	private final File pluginApk;
	
	private final String targetVersionName;
	private final int targetVersionCode;
	
	private final Hooker hooker;
	
	public PluginContext(Inspector inspector, DexFileManager dexFileManager, LuaScriptManager scriptManager,
                         ModuleContext context, String versionName, int versionCode,
                         File inspectorApk, File pluginApk, Hooker hooker,
                         String targetVersionName, int targetVersionCode) {
		super();
		this.inspector = inspector;
		this.dexFileManager = dexFileManager;
		this.scriptManager = scriptManager;
		this.context = context;
		this.versionName = versionName;
		this.versionCode = versionCode;
		this.inspectorApk = inspectorApk;
		this.pluginApk = pluginApk;
		this.hooker = hooker;
		
		this.targetVersionName = targetVersionName;
		this.targetVersionCode = targetVersionCode;
	}

	public Inspector getInspector() {
		return inspector;
	}

	public DexFileManager getDexFileManager() {
		return dexFileManager;
	}

	public LuaScriptManager getScriptManager() {
		return scriptManager;
	}

	public ModuleContext getContext() {
		return context;
	}

	public String getVersionName() {
		return versionName;
	}

	public int getVersionCode() {
		return versionCode;
	}

	public File getInspectorApk() {
		return inspectorApk;
	}

	public File getPluginApk() {
		return pluginApk;
	}

	public Hooker getHooker() {
		return hooker;
	}

	public String getTargetVersionName() {
		return targetVersionName;
	}

	public int getTargetVersionCode() {
		return targetVersionCode;
	}

}
