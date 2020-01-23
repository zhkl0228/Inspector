package com.fuzhu8.inspector.xposed.permissions;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.os.Binder;
import android.util.DisplayMetrics;

import com.fuzhu8.inspector.BuildConfig;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.InspectorModuleContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author zhkl0228
 *
 */
public class PermissionFaker19 extends PermissionFaker {

	PermissionFaker19(ModuleContext context, XSharedPreferences pref,
					  List<String> permissionsAdd) {
		super(context, pref, permissionsAdd);
	}

	@SuppressWarnings("unused")
	ApplicationInfo getApplicationInfo(Object thisObj, String packageName, int flags, int userId, ApplicationInfo ret) {
		if (flags == 0 && ret != null) {
			XposedBridge.log("getApplicationInfo packageName=" + packageName + ", flags=" + flags + ", userId=" + userId + ", appUserId=" + ret.uid + ", callingUserId=" + Binder.getCallingUid());
		}
		return ret;
	}

	@Override
	public void fakeParsePackage() {
		try {
			hook(PackageParser.class, "parsePackage", File.class, String.class, DisplayMetrics.class, int.class);
		} catch(Exception e) {
			log(e);
		}

		try {
			hook("com.android.server.pm.PackageManagerService", "isNewPlatformPermissionForPackage", String.class, Package.class);
		} catch (Exception e) {
			log(e);
		}

		try {
			hook("com.android.server.pm.PackageManagerService", "checkUidPermission", String.class, int.class);
		} catch (Exception e) {
			log(e);
		}
		
		XposedHelpers.findAndHookMethod("com.android.server.pm.PackageManagerService", context.getClassLoader(), "grantPermissionsLPw", Package.class, boolean.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				super.beforeHookedMethod(param);
				
				Package pkg = (Package) param.args[0];
				if (pkg == null || pkg.applicationInfo == null) {
					return;
				}
				if((pkg.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0 ||
						BuildConfig.APPLICATION_ID.equals(pkg.packageName)) {
					return;
				}
				
				try {
					List requestedPermissionsRequired = (List) XposedHelpers.getObjectField(pkg, "requestedPermissionsRequired");

					List<String> requestedPermissions = new ArrayList<>(pkg.requestedPermissions);
					List<Object> permissionsRequiredList = new ArrayList<Object>(requestedPermissionsRequired);
					List<String> addList = new ArrayList<>();
					
					for(String perm : permissionsAdd) {
						if(requestedPermissions.contains(perm)) {
							continue;
						}
						
						requestedPermissions.add(perm);
						permissionsRequiredList.add(Boolean.TRUE);
						addList.add(perm);
					}
					
					if(addList.isEmpty()) {
						return;
					}

					param.setObjectExtra("orig_requested_permissions", pkg.requestedPermissions);
					param.setObjectExtra("orig_requested_permissions_required", requestedPermissionsRequired);
					
					XposedHelpers.setObjectField(pkg, "requestedPermissions", requestedPermissions);
					XposedHelpers.setObjectField(pkg, "requestedPermissionsRequired", permissionsRequiredList);

					// log("grantPermissionsLPw pkg=" + pkg.packageName + ", permissions=" + addList);
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
				Object requestedPermissionsRequired = param.getObjectExtra("orig_requested_permissions_required");
				if(requestedPermissionsRequired != null) {
					XposedHelpers.setObjectField(param.args[0], "requestedPermissionsRequired", requestedPermissionsRequired);
				}
			}
		});
	}

	@SuppressWarnings("unused")
	boolean isNewPlatformPermissionForPackage(Object thisObj, String perm, Package pkg, boolean ret) {
		return permissionsAdd.contains(perm) || ret;

	}

	@SuppressWarnings("unused")
	int checkUidPermission(Object thisObj, String permName, int uid, int ret) {
		if (ret == PackageManager.PERMISSION_DENIED && Manifest.permission.BIND_VPN_SERVICE.equals(permName)) {
			// XposedBridge.log("checkUidPermission permName=" + permName + ", uid=" + uid + ", ret=" + ret);
			return PackageManager.PERMISSION_GRANTED;
		}
		return ret;
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
