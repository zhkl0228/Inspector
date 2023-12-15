package com.fuzhu8.inspector.xposed.permissions;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.fuzhu8.inspector.BuildConfig;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.sdk.Sdk;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author zhkl0228
 *
 */
public class PermissionFaker33 extends PermissionFaker {

	PermissionFaker33(ModuleContext context, XSharedPreferences pref,
                      List<String> permissionsAdd) {
		super(context, pref, permissionsAdd);
	}

	@Override
	public void fakeParsePackage() {
	}

	@Override
	public void fakePackageManagerService(Class<?> packageManagerServiceClass) {
		doFake(packageManagerServiceClass);
    }

	@SuppressLint("PrivateApi")
	private void doFake(Class<?> packageManagerServiceClass) {
		try {
			ClassLoader classLoader = Objects.requireNonNull(packageManagerServiceClass.getClassLoader());
			Class<?> cComputerEngine = classLoader.loadClass("com.android.server.pm.ComputerEngine");

			for (Method method : cComputerEngine.getDeclaredMethods()) {
				// boolean shouldFilterApplication(PackageStateInternal ps, int callingUid, ComponentName component, int componentType, int userId)
				if ("shouldFilterApplication".equals(method.getName()) &&
						method.getParameterTypes().length == 5 &&
						method.getReturnType() == boolean.class) {
					XposedBridge.log("Hook " + method);
					XposedBridge.hookMethod(method, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							super.afterHookedMethod(param);

							boolean filter = (boolean) param.getResult();
							if (filter) {
								int callingUid = (Integer) param.args[1];
								Object ps = param.args[0];
								String packetName = (String) XposedHelpers.callMethod(ps, "getPackageName");
								if (BuildConfig.APPLICATION_ID.equals(packetName)) {
									XposedBridge.log("Enable inspector visible for uid: " + callingUid);
									param.setResult(Boolean.FALSE);
									return;
								}

								Object pkg = XposedHelpers.callMethod(ps, "getPkg");
								Bundle metaData = pkg == null ? null : (Bundle) XposedHelpers.callMethod(pkg, "getMetaData");
								if (metaData != null && metaData.getBoolean(Sdk.INSPECTOR_PLUGIN_META_KEY)) {
									XposedBridge.log("Enable inspector plugin[" + packetName + "] visible for uid: " + callingUid);
									param.setResult(Boolean.FALSE);
								}

//								XposedBridge.log("shouldFilterApplication ps=" + ps + ", metaData=" + metaData + ", callingUid=" + param.args[1]);
							}
						}
					});
				}
			}
		} catch (Throwable e) {
			XposedBridge.log(e);
		}
	}

}
