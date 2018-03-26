package com.fuzhu8.inspector.xposed.permissions;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;

import com.fuzhu8.inspector.ModuleContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Xposed框架动态添加权限功能以及增加debuggable属性
 * @author zhkl0228
 *
 */
public class XposedPackageFaker implements FakeParsePackageResult {
	
	protected final ModuleContext context;
	protected final XSharedPreferences pref;
	private final PermissionFaker permissionFaker;

	@SuppressLint("ObsoleteSdkInt")
	public XposedPackageFaker(ModuleContext context, XSharedPreferences pref, String...permissions) {
		super();
		
		this.context = context;
		this.pref = pref;
		List<String> permissionsAdd = new ArrayList<>();
		Collections.addAll(permissionsAdd, permissions);
		
		if(Build.VERSION.SDK_INT <= 17) {
			permissionFaker = new PermissionFaker17(context, pref, permissionsAdd);
		} else if(Build.VERSION.SDK_INT <= 19) {
			permissionFaker = new PermissionFaker19(context, pref, permissionsAdd);
		} else if(Build.VERSION.SDK_INT <= 22) {
			permissionFaker = new PermissionFaker21(context, pref, permissionsAdd);
		} else if(Build.VERSION.SDK_INT <= 23) {
			permissionFaker = new PermissionFaker23(context, pref, permissionsAdd);
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
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					return null;
				}
			});
		} catch (Throwable throwable) {
			XposedBridge.log(throwable);
		}
	}

}
