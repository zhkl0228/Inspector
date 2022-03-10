package com.fuzhu8.inspector.xposed.permissions;

import android.annotation.SuppressLint;

import com.fuzhu8.inspector.ModuleContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

/**
 * @author zhkl0228
 *
 */
public class PermissionFaker31 extends PermissionFaker {

	PermissionFaker31(ModuleContext context, XSharedPreferences pref,
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
			packageManagerServiceClass = Objects.requireNonNull(packageManagerServiceClass.getClassLoader()).loadClass("com.android.server.pm.PackageManagerService$ComputerEngine");
		} catch (Throwable e) {
			XposedBridge.log(e);
		}

		for (final Method method : packageManagerServiceClass.getDeclaredMethods()) {
			if ("shouldFilterApplicationLocked".equals(method.getName()) ||
					"filterSharedLibPackageLPr".equals(method.getName()) && method.getParameterTypes().length >= 2 &&
							(method.getReturnType() == Boolean.class || method.getReturnType() == boolean.class)) {
				XposedBridge.hookMethod(method, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						super.afterHookedMethod(param);

						boolean filter = (boolean) param.getResult();
						if (filter) {
//							XposedBridge.log(method.getName() + " ps=" + param.args[0] + ", callingUid=" + param.args[1]);
							param.setResult(Boolean.FALSE);
						}
					}
				});
			}
		}
	}

}
