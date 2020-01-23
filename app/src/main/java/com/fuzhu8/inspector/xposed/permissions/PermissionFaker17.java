package com.fuzhu8.inspector.xposed.permissions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fuzhu8.inspector.BuildConfig;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.InspectorModuleContext;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.util.DisplayMetrics;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author zhkl0228
 *
 */
public class PermissionFaker17 extends PermissionFaker {

	PermissionFaker17(ModuleContext context, XSharedPreferences pref,
					  List<String> permissionsAdd) {
		super(context, pref, permissionsAdd);
	}

	@Override
	public void fakeParsePackage() {
		try {
			hook(PackageParser.class, "parsePackage", File.class, String.class, DisplayMetrics.class, int.class);
		} catch(Exception e) {
			log(e);
		}
		
		XposedHelpers.findAndHookMethod("com.android.server.pm.PackageManagerService", context.getClassLoader(), "grantPermissionsLPw", Package.class, boolean.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				super.beforeHookedMethod(param);
				
				Package pkg = (Package) param.args[0];
				if(pkg == null ||
						pkg.applicationInfo == null ||
						(pkg.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0 ||
						BuildConfig.APPLICATION_ID.equals(pkg.packageName)) {
					return;
				}
				
				final Object ps = pkg.mExtras;
				if(ps != null && "com.android.server.pm.PackageSetting".equals(ps.getClass().getCanonicalName())) {
					XposedHelpers.setBooleanField(ps, "permissionsFixed", false);
				}
				
				try {
					List<String> requestedPermissions = new ArrayList<>(pkg.requestedPermissions);
					List<String> addList = new ArrayList<>();
					
					for(String perm : permissionsAdd) {
						if(requestedPermissions.contains(perm)) {
							continue;
						}
						
						requestedPermissions.add(perm);
						addList.add(perm);
					}
					
					if(addList.isEmpty()) {
						return;
					}

					param.setObjectExtra("orig_requested_permissions", pkg.requestedPermissions);
					
					XposedHelpers.setObjectField(pkg, "requestedPermissions", requestedPermissions);

					if(pref.getBoolean("pref_debug", false)) {
						log("grantPermissionsLPw pkg=" + pkg + ", permissions=" + addList);
					}
				} catch(Throwable e) {
					XposedBridge.log(e);
				}
			}
			@Override
			protected void afterHookedMethod(MethodHookParam param)
					throws Throwable {
				super.afterHookedMethod(param);
				
				Object requestedPermissions = param.getObjectExtra("orig_requested_permissions");
				if(requestedPermissions != null) {
					XposedHelpers.setObjectField(param.args[0], "requestedPermissions", requestedPermissions);
				}
			}
		});
	}

	@SuppressWarnings("unused")
	Package parsePackage(Object thisObj, File sourceFile, String destCodePath, DisplayMetrics metrics, int flags, Package ret) {
		if(ret == null ||
				ret.applicationInfo == null) {
			return ret;
		}
		pref.reload();
		if(!pref.getBoolean("pref_debuggable_all", false) && (flags & (PackageParser.PARSE_IS_SYSTEM_DIR | PackageParser.PARSE_IS_SYSTEM)) != 0) {
			return ret;
		}
		if(!BuildConfig.APPLICATION_ID.equals(ret.packageName)) {
			ret.applicationInfo.flags |= ApplicationInfo.FLAG_DEBUGGABLE;
			if (InspectorModuleContext.isDebug()) {
				log("setDebuggable on " + ret.packageName + ", sourceFile=" + sourceFile + ", destCodePath=" + destCodePath + ", flags=" + flags);
			}
		}
		return ret;
	}

	@Override
	public void fakePackageManagerService(Class<?> packageManagerServiceClass) {
    }

}
