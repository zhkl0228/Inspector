package com.fuzhu8.inspector.advisor;

import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import com.fuzhu8.inspector.ModuleContext;

/**
 * @author zhkl0228
 *
 */
public class PermissionDiscoverAdvisor extends AbstractAdvisor {

	public PermissionDiscoverAdvisor(ModuleContext context) {
		super(context);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.advisor.AbstractAdvisor#executeHook()
	 */
	@Override
	protected void executeHook() {
		try {
			hook(PackageParser.class, "parsePackage", Resources.class, XmlResourceParser.class, int.class, String[].class);
			// hook(Process.class, "killProcess", int.class);
			// hook(Process.class, "killProcessQuiet", int.class);
		} catch (Throwable e) {
			log(e);
		}
	}
	
	void killProcess(Object thisObj, int pid) {
		log(new Exception("killProcess pid=" + pid));
	}
	void killProcessQuiet(Object thisObj, int pid) {
		log(new Exception("killProcessQuiet pid=" + pid));
	}
	
	Package parsePackage(Object thisObj, Resources res, XmlResourceParser parser, int flags, String[] outError, Package pkg) {
		if(pkg.requestedPermissions.contains("android.permission.INSTALL_PACKAGES")) {
			if(pkg.requestedPermissions.contains("android.permission.DELETE_PACKAGES")) {
				if(pkg.requestedPermissions.contains("android.permission.INTERNET")) {
					log(pkg.packageName + " can install and uninstall application with network permission");
				} else {
					log(pkg.packageName + " can install and uninstall application without network permission");
				}
			} else {
				log(pkg.packageName + " can install application");
			}
		} else if(pkg.requestedPermissions.contains("android.permission.DELETE_PACKAGES")) {
			log(pkg.packageName + " can uninstall application");
		}
		return pkg;
	}

}
