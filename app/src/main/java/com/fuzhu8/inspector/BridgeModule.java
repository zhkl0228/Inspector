package com.fuzhu8.inspector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;

import com.fuzhu8.inspector.bridge.BridgeHooker;
import com.fuzhu8.inspector.bridge.BridgeModuleStarter;
import com.fuzhu8.inspector.bridge.BridgeRootUtilServer;
import com.fuzhu8.inspector.bridge.permissions.BridgePackageFaker;
import com.fuzhu8.inspector.bridge.permissions.FakeParsePackageResult;
import com.fuzhu8.inspector.module.AbstractModuleStarter;
import com.fuzhu8.inspector.module.ModuleStarter;

import java.io.File;

import cn.android.bridge.AndroidBridge;
import cn.android.bridge.IXposedHookLoadPackage;
import cn.android.bridge.IXposedHookZygoteInit;
import cn.android.bridge.XC_MethodHook;
import cn.android.bridge.XSharedPreferences;
import cn.android.bridge.XposedHelpers;
import cn.android.bridge.callbacks.XC_LoadPackage;

/**
 * @author zhkl0228
 *
 */
public class BridgeModule extends Module implements IXposedHookZygoteInit, IXposedHookLoadPackage {
	
	/**
	 * 是否启用
	 */
	private boolean isEnabled(XSharedPreferences pref) {
		return pref.getBoolean("pref_enable_status", true);
	}
	
	private String modulePath;
	private FakeParsePackageResult packageFakerResult;

	@SuppressLint("PrivateApi")
	@Override
	public void initZygote(StartupParam startupParam) {
		XSharedPreferences pref = new XSharedPreferences(BuildConfig.APPLICATION_ID);
		modulePath = startupParam.modulePath;
		
		try {
			if(!isEnabled(pref)) {
				return;
			}

			BridgeRootUtilServer rootUtilServer = null;
			if(pref.getBoolean("pref_local_root", false) &&
					(rootUtilServer = BridgeRootUtilServer.startRootUtilServer()) != null) {
				AndroidBridge.log("startRootUtilServer with port " + rootUtilServer.getPort() + " successfully!");
			}
			
			packageFakerResult = new BridgePackageFaker(new InspectorModuleContext(BridgeModule.class.getClassLoader(), null,
					new File(Environment.getDataDirectory(), "data/" + BuildConfig.APPLICATION_ID),
					Environment.getDataDirectory().getAbsolutePath(),
					null, modulePath, rootUtilServer, new BridgeHooker(), AbstractModuleStarter.createSdk()),
					pref,
					Manifest.permission.INTERNET,
					Manifest.permission.ACCESS_NETWORK_STATE,
					Manifest.permission.READ_PHONE_STATE,
					Manifest.permission.ACCESS_WIFI_STATE,
					Manifest.permission.ACCESS_COARSE_LOCATION,
					Manifest.permission.BLUETOOTH,
					Manifest.permission.READ_EXTERNAL_STORAGE,
					Manifest.permission.WRITE_EXTERNAL_STORAGE).fakeParsePackage();
			if(packageFakerResult != null) {
				AndroidBridge.log("initZygote XposedPackagePermissions successfully!");
			} else {
				AndroidBridge.log("initZygote XposedPackagePermissions failed: " + Build.VERSION.SDK_INT);
			}
		} catch(Throwable t) {
			AndroidBridge.log(t);
		}
	}

	/* (non-Javadoc)
	 * @see de.robv.android.xposed.IXposedHookLoadPackage#handleLoadPackage(de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam)
	 */
	@SuppressLint("PrivateApi")
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
		if(lpparam.isFirstApplication && lpparam.classLoader != null && packageFakerResult != null) {
			Class<?> packageManagerServiceClass = null;
			try {
				packageManagerServiceClass = Class.forName("com.android.server.pm.PackageManagerService");
			} catch(Throwable cnfe) {
				try {
					packageManagerServiceClass = lpparam.classLoader.loadClass("com.android.server.pm.PackageManagerService");
				} catch(Throwable ignored) {}
			}
			if(packageManagerServiceClass != null) {
				try {
					packageFakerResult.fakePackageManagerService(packageManagerServiceClass);
					packageFakerResult = null;
				} catch(Throwable t) {
					AndroidBridge.log(t);
				}
			}
		}

		if ("com.android.vpndialogs".equals(lpparam.packageName)) {
			XposedHelpers.findAndHookMethod("com.android.vpndialogs.ConfirmDialog", lpparam.classLoader, "onResume", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) {
					try {
						Object mService = XposedHelpers.getObjectField(param.thisObject, "mService");

						String mPackage = ((Activity) param.thisObject).getCallingPackage();

						Class<?>[] prepareVPNsignature = {String.class, String.class};
						if((Boolean) XposedHelpers.callMethod(mService, "prepareVpn", prepareVPNsignature,  mPackage, null)) {
							return;
						}

						XposedHelpers.callMethod(mService, "prepareVpn", prepareVPNsignature, null, mPackage);
						((Activity) param.thisObject).setResult(Activity.RESULT_OK);
						// Toast.makeText((Context) param.thisObject, "Allowed VpnService app: " + mPackage, Toast.LENGTH_LONG).show();
						AndroidBridge.log("Allowed VpnService app: " + mPackage);
						((Activity) param.thisObject).finish();
					} catch (Exception e) {
						AndroidBridge.log(e);
					}
				}
			});
		}

		try {
			XSharedPreferences pref = new XSharedPreferences(BuildConfig.APPLICATION_ID);
			pref.reload();

			if (isEnabled(pref) && canInspect(lpparam, pref)) {
				File moduleDataDir = getModuleDataDir(lpparam.appInfo.dataDir, BuildConfig.APPLICATION_ID);
				ModuleStarter moduleStarter = new BridgeModuleStarter(modulePath, pref.getBoolean("pref_debug", false),
						pref.getBoolean("pref_trace_anti", false),
						pref.getBoolean("pref_anti_thread_create", false),
						pref.getBoolean("pref_trace_file", false),
						pref.getBoolean("pref_trace_sys_call", false),
						pref.getBoolean("pref_trace_trace", false),
						pref.getBoolean("pref_just_trust_me", true), pref.getBoolean("pref_broadcast", false));
				moduleStarter.startModule(lpparam.appInfo, lpparam.processName, moduleDataDir, pref.getString("pref_collect_bytecode_text", null), lpparam.classLoader);
			}
		} catch (Throwable throwable) {
			AndroidBridge.log(throwable);
		}
	}

	private boolean canInspect(XC_LoadPackage.LoadPackageParam lpparam, XSharedPreferences pref) {
		if(lpparam.appInfo == null) {
			return false;
		}

		if((lpparam.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0 || !lpparam.isFirstApplication) {//系统应用
			return pref.getBoolean("pref_system_app", false);
		}

		boolean canHookAppService = pref.getBoolean("pref_app_service", true);
		if (!canHookAppService && lpparam.processName.contains(":")) {
			return false;
		}

		return !BuildConfig.APPLICATION_ID.equals(lpparam.packageName); // 禁止hook自身
	}

}
