package com.fuzhu8.inspector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;

import com.fuzhu8.inspector.module.AbstractModuleStarter;
import com.fuzhu8.inspector.module.ModuleStarter;
import com.fuzhu8.inspector.xposed.XposedHooker;
import com.fuzhu8.inspector.xposed.XposedModuleStarter;
import com.fuzhu8.inspector.xposed.XposedRootUtilServer;
import com.fuzhu8.inspector.xposed.permissions.FakeParsePackageResult;
import com.fuzhu8.inspector.xposed.permissions.XposedPackageFaker;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Collections;

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
	public void initZygote(StartupParam startupParam) throws Throwable {
		XSharedPreferences pref = new XSharedPreferences(BuildConfig.APPLICATION_ID);
		modulePath = startupParam.modulePath;

		switch (Build.VERSION.SDK_INT) {
			case 25: // android 7.1.2
				return;
		}

		try {
			File tmp = new File(Environment.getExternalStorageDirectory(), "inspector");
			FileUtils.deleteQuietly(tmp);
		} catch (Throwable throwable) {
			XposedBridge.log(throwable);
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
			
			packageFakerResult = new XposedPackageFaker(new MyModuleContext(XposedModule.class.getClassLoader(), null,
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
				XposedBridge.log("initZygote XposedPackagePermissions failed: " + Build.VERSION.SDK_INT);
			}

			/*if (Build.VERSION.SDK_INT == 19) {
				*//*Class<?> SELinux = Class.forName("android.os.SELinux");
				XposedHelpers.callStaticMethod(SELinux, "setSELinuxEnforce", false);*//*

				Class<?> Zygote = Class.forName("dalvik.system.Zygote");
				// int forkAndSpecialize(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits, int mountExternal, String seInfo, String niceName, int[] unknown)
				XposedHelpers.findAndHookMethod(Zygote, "forkAndSpecialize", int.class, int.class, int[].class, int.class, int[][].class, int.class, String.class, String.class, int[].class, new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						super.beforeHookedMethod(param);

						Integer uid = (Integer) param.args[0];
						Integer gid = (Integer) param.args[1];
						int[] gids = (int[]) param.args[2];
						Integer debugFlags = (Integer) param.args[3];
						Integer mountExternal = (Integer) param.args[5];
						String seInfo = (String) param.args[6];
						String niceName = (String) param.args[7];
						XposedBridge.log("forkAndSpecialize uid=" + uid + ", gid=" + gid + ", gids=" + Arrays.toString(gids) + ", debugFlags=" + debugFlags + ", mountExternal=" + mountExternal + ", seInfo=" + seInfo + ", niceName=" + niceName + ", myUid=" + Process.myUid() + ", myPid=" + Process.myPid());
					}
				});
				// int forkSystemServer(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits, long permittedCapabilities, long effectiveCapabilities)
				XposedHelpers.findAndHookMethod(Zygote, "forkSystemServer", int.class, int.class, int[].class, int.class, int[][].class, long.class, long.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						super.afterHookedMethod(param);

						Integer uid = (Integer) param.args[0];
						Integer gid = (Integer) param.args[1];
						int[] gids = (int[]) param.args[2];
						Integer debugFlags = (Integer) param.args[3];
						XposedBridge.log("forkSystemServer uid=" + uid + ", gid=" + gid + ", gids=" + Arrays.toString(gids) + ", debugFlags=" + debugFlags + ", myUid=" + Process.myUid() + ", myPid=" + Process.myPid() + ", ret=" + param.getResult());
					}
				});
			}*/
		} catch(Throwable t) {
			XposedBridge.log(t);
		}
	}

	/* (non-Javadoc)
	 * @see de.robv.android.xposed.IXposedHookLoadPackage#handleLoadPackage(de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam)
	 */
	@SuppressLint("PrivateApi")
	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
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
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
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

		/*if(isEnabled(pref) && KrakenCapture.PACKAGE_NAME.equals(lpparam.packageName) && Build.VERSION.SDK_INT == 19) {
			hookSystemServer();
		}*/

		if (isEnabled(pref) && canInspect(lpparam, pref)) {
			File moduleDataDir = getModuleDataDir(lpparam.appInfo.dataDir, BuildConfig.APPLICATION_ID);
			int patchSSL = 0;
			try {
				for (String str : pref.getStringSet("pref_patch_ssl", Collections.<String>emptySet())) {
					patchSSL |= Integer.parseInt(str);
				}
			} catch(Exception ignored) {}
			ModuleStarter moduleStarter = new XposedModuleStarter(modulePath, pref.getBoolean("pref_debug", false),
					pref.getBoolean("pref_trace_anti", true),
					pref.getBoolean("pref_anti_thread_create", false),
					pref.getBoolean("pref_trace_file", false),
					pref.getBoolean("pref_trace_sys_call", false),
					pref.getBoolean("pref_trace_trace", false),
					patchSSL, pref.getBoolean("pref_broadcast", false));
			moduleStarter.startModule(lpparam.appInfo, lpparam.processName, moduleDataDir, pref.getString("pref_collect_bytecode_text", null), lpparam.classLoader);
		}
	}

	private boolean canInspect(LoadPackageParam lpparam, XSharedPreferences pref) {
		// XposedBridge.log("canInspect packageName=" + lpparam.packageName + ", processName=" + lpparam.processName + ", appInfo=" + lpparam.appInfo + ", systemApp=" + pref.getBoolean("pref_system_app", false));

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

	/*private void hookSystemServer() {
		XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);

				setApplication(Application.class.cast(param.thisObject));
			}
		});
	}

	private KrakenCapture systemServer;

	private void setApplication(Application application) {
		if(systemServer != null || application == null) {
			return;
		}

		systemServer = new KrakenCapture(application);
		Thread thread = new Thread(systemServer, systemServer.getClass().getSimpleName());
		thread.setDaemon(true);
		thread.start();
	}*/

}
