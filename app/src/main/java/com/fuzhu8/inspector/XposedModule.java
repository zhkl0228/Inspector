package com.fuzhu8.inspector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Process;

import com.fuzhu8.inspector.module.AbstractModuleStarter;
import com.fuzhu8.inspector.module.ModuleStarter;
import com.fuzhu8.inspector.xposed.XposedHooker;
import com.fuzhu8.inspector.xposed.XposedModuleStarter;
import com.fuzhu8.inspector.xposed.XposedRootUtilServer;
import com.fuzhu8.inspector.xposed.permissions.FakeParsePackageResult;
import com.fuzhu8.inspector.xposed.permissions.XposedPackageFaker;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * @author zhkl0228
 *
 */
public class XposedModule extends Module implements IXposedHookZygoteInit, IXposedHookLoadPackage {
	
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

		if (Build.VERSION.SDK_INT == 25) { // android 7.1.2
			return;
		}
		
		try {
			if(!isEnabled(pref)) {
				return;
			}

			XposedRootUtilServer rootUtilServer = null;
			if(pref.getBoolean("pref_local_root", false) &&
					(rootUtilServer = XposedRootUtilServer.startRootUtilServer()) != null) {
				XposedBridge.log("startRootUtilServer with port " + rootUtilServer.getPort() + " successfully!");
			}
			
			packageFakerResult = new XposedPackageFaker(new InspectorModuleContext(XposedModule.class.getClassLoader(), null,
					new File(Environment.getDataDirectory(), "data/" + BuildConfig.APPLICATION_ID),
					Environment.getDataDirectory().getAbsolutePath(),
					null, modulePath, rootUtilServer, new XposedHooker(), AbstractModuleStarter.createSdk()),
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
				XposedBridge.log("initZygote XposedPackagePermissions successfully!");
			} else {
				XposedBridge.log("initZygote XposedPackagePermissions failed for sdk: " + Build.VERSION.SDK_INT);
			}
		} catch(Throwable t) {
			XposedBridge.log(t);
		}
	}

	@SuppressLint("PrivateApi")
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) {
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
					XposedBridge.log(t);
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
						XposedBridge.log("Allowed VpnService app: " + mPackage);
						((Activity) param.thisObject).finish();
					} catch (Exception e) {
						XposedBridge.log(e);
					}
				}
			});
		}

		XSharedPreferences pref = new XSharedPreferences(BuildConfig.APPLICATION_ID);
		pref.reload();

		if (isEnabled(pref) && canInspect(lpparam)) {
			File moduleDataDir = getModuleDataDir(lpparam.appInfo.dataDir, BuildConfig.APPLICATION_ID);
			ModuleStarter moduleStarter = new XposedModuleStarter(modulePath, pref.getBoolean("pref_debug", false),
					pref.getBoolean("pref_trace_anti", true),
					pref.getBoolean("pref_anti_thread_create", false),
					pref.getBoolean("pref_trace_file", false),
					pref.getBoolean("pref_trace_sys_call", false),
					pref.getBoolean("pref_trace_trace", false),
					pref.getBoolean("pref_broadcast", true));
			moduleStarter.startModule(lpparam.appInfo, lpparam.processName, moduleDataDir, pref.getString("pref_collect_bytecode_text", null), lpparam.classLoader);
		}
	}

	private boolean canInspect(LoadPackageParam lpparam) {
		XposedBridge.log("testCanInspect packageName=" + lpparam.packageName + ", processName=" + lpparam.processName + ", pid=" + Process.myPid() + ", uid=" + Process.myUid() + ", appInfo=" + lpparam.appInfo);

		if(lpparam.appInfo == null || lpparam.processName == null) {
			return false;
		}

		if((lpparam.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {//系统应用
			return true;
		}

		if (lpparam.processName.contains(":")) {
			return true;
		}

		return !BuildConfig.APPLICATION_ID.equals(lpparam.packageName); // 禁止hook自身
	}

}
