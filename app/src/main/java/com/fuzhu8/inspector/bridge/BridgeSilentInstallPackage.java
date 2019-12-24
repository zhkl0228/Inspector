package com.fuzhu8.inspector.bridge;

import android.app.Activity;
import android.content.Intent;
import android.widget.Button;

import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.AbstractHookHandler;

import cn.android.bridge.XposedHelpers;

/**
 * @author zhkl0228
 *
 */
@SuppressWarnings("unused")
public class BridgeSilentInstallPackage extends AbstractHookHandler {

	public BridgeSilentInstallPackage(ModuleContext context) {
		super(context);
		
		this.executeHook();
	}
	
	void startInstallConfirm(Object thisObj) {
		log("startInstallConfirm autoInstall");
        Object mOk = XposedHelpers.getObjectField(thisObj, "mOk");
        XposedHelpers.setObjectField(thisObj, "mScrollView", null);
        XposedHelpers.setBooleanField(thisObj, "mOkCanInstall", true);
        Button.class.cast(mOk).performClick();
	}
	
	boolean isInstallingUnknownAppsAllowed(Object thisObj, boolean ret) {
		log("fake isInstallingUnknownAppsAllowed");
		return true;
	}
	
	boolean needConfirm(Object thisObj, Activity activity, Intent intent, boolean ret) {
		log("fake Xiaomi AdbInstall needConfirm");
		try {
			XposedHelpers.callStaticMethod(Class.class.cast(thisObj), "tryAcceptAdbInstall", new Class[] {
				Activity.class
			}, activity);
		} catch(Throwable t) {
			log(t);
		}
		return false;
	}

	private void executeHook() {
		try {
			hook("com.android.packageinstaller.AdbInstall", "needConfirm", Activity.class, Intent.class);
		} catch (Throwable e) {
			// log(e);
		}
		
		try {
			hook("com.android.packageinstaller.PackageInstallerActivity", "startInstallConfirm");
		} catch (Throwable e) {
			log(e);
		}
		
		try {
			hook("com.android.packageinstaller.PackageInstallerActivity", "isInstallingUnknownAppsAllowed");
		} catch (Throwable e) {
			log(e);
		}
	}

	@Override
	protected Object getHandler() {
		return this;
	}

}
