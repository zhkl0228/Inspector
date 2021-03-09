package com.fuzhu8.inspector;

import android.app.ActivityThread;
import android.app.LoadedApk;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.fuzhu8.inspector.dexposed.DexposedModuleStarter;
import com.fuzhu8.inspector.module.ModuleStarter;
import com.taobao.android.dexposed.DexposedBridge;
import com.taobao.android.dexposed.XC_MethodHook;
import com.taobao.android.dexposed.XC_MethodHook.Unhook;
import com.taobao.android.dexposed.XposedHelpers;

import java.io.File;

/**
 * @author zhkl0228
 *
 */
class DexposedModule extends Module {

	private static ApplicationInfo currentApplicationInfo(ActivityThread am) {
		Object boundApplication = XposedHelpers.getObjectField(am, "mBoundApplication");
		if (boundApplication == null) {
			return null;
		}

		return (ApplicationInfo) XposedHelpers.getObjectField(boundApplication, "appInfo");
	}
	
	private static ActivityThread currentActivityThread() {
		return ActivityThread.currentActivityThread();
	}
	
	private static Unhook unhook;

	/**
	 * @return can restart
	 */
	public static boolean start(String modulePath, boolean debug,
			boolean trace_anti, boolean anti_thread_create, boolean trace_file,
			boolean trace_sys_call, boolean trace_trace) {
		try {
			final ModuleStarter moduleStarter = new DexposedModuleStarter(modulePath, debug, trace_anti, anti_thread_create, trace_file, trace_sys_call, trace_trace,
					true);
			ActivityThread activityThread = currentActivityThread();
			if(activityThread == null) {
				DexposedBridge.log("activityThread is null, delay start.");
				unhook = DexposedBridge.hookMethod(String.class.getDeclaredMethod("toString"), new XC_MethodHook() {
					@Override
					protected synchronized void afterHookedMethod(MethodHookParam param) throws Throwable {
						super.afterHookedMethod(param);
						
						ActivityThread laterAT = currentActivityThread();
						if(laterAT == null) {
							return;
						}
						
						if(unhook != null) {
							unhook.unhook();
							unhook = null;
						}
						start(laterAT, moduleStarter);
					}
				});
				return false;
			}

			return start(activityThread, moduleStarter);
		} catch(Throwable t) {
			Log.d("Dexposed", t.getMessage(), t);
			return false;
		}
	}

	private static boolean start(ActivityThread activityThread, ModuleStarter moduleStarter) {
		ApplicationInfo appInfo = currentApplicationInfo(activityThread);
		if(appInfo == null) {
			DexposedBridge.log("ApplicationInfo is null.");
			return false;
		}
		
		LoadedApk loadedApk = activityThread.getPackageInfoNoCheck(appInfo, null);
		if(loadedApk == null) {
			DexposedBridge.log("LoadedApk is null.");
			return false;
		}
		
		File moduleDataDir = getModuleDataDir(appInfo.dataDir, BuildConfig.APPLICATION_ID);
		moduleStarter.startModule(appInfo, appInfo.packageName, moduleDataDir, null, loadedApk.getClassLoader());
		return false;
	}

}
