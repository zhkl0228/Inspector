package com.fuzhu8.inspector.bridge.permissions;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;

import com.fuzhu8.inspector.ModuleContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.android.bridge.AndroidBridge;
import cn.android.bridge.XC_MethodHook;
import cn.android.bridge.XC_MethodReplacement;
import cn.android.bridge.XSharedPreferences;
import cn.android.bridge.XposedHelpers;

/**
 * Xposed框架动态添加权限功能以及增加debuggable属性
 * @author zhkl0228
 *
 */
public class BridgePackageFaker implements FakeParsePackageResult {
	
	protected final ModuleContext context;
	protected final XSharedPreferences pref;
	private final PermissionFaker permissionFaker;

	@SuppressLint("ObsoleteSdkInt")
	public BridgePackageFaker(ModuleContext context, XSharedPreferences pref, String...permissions) {
		super();
		
		this.context = context;
		this.pref = pref;
		List<String> permissionsAdd = new ArrayList<>();
		Collections.addAll(permissionsAdd, permissions);

		if(Build.VERSION.SDK_INT <= 25) {
			permissionFaker = new PermissionFaker25(context, pref, permissionsAdd);
		} else {
			permissionFaker = null;
		}
	}
	
	public FakeParsePackageResult fakeParsePackage() {
		if(permissionFaker != null) {
			permissionFaker.fakeParsePackage();
			return this;
		}
		return null;
	}

	@SuppressLint("PrivateApi")
	@Override
	public void fakePackageManagerService(Class<?> packageManagerServiceClass) {
		if (permissionFaker != null) {
			permissionFaker.fakePackageManagerService(packageManagerServiceClass);
		}

		// 以下防止调试时，被am杀掉进程
		try {
			ClassLoader classLoader = packageManagerServiceClass.getClassLoader();
			Class<?> activityManagerServiceClass = classLoader.loadClass("com.android.server.am.ActivityManagerService");
			Class<?> processRecordClass = classLoader.loadClass("com.android.server.am.ProcessRecord");
			XposedHelpers.findAndHookMethod(activityManagerServiceClass, "killAppAtUsersRequest", processRecordClass, Dialog.class, new XC_MethodReplacement() {
				@Override
				protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) {
					return null;
				}
			});
		} catch (Throwable throwable) {
			AndroidBridge.log(throwable);
		}
	}

}
