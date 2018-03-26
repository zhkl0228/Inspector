package com.fuzhu8.inspector.sdk;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fuzhu8.inspector.advisor.Hookable;
import com.fuzhu8.inspector.advisor.Hooker;
import com.taobao.android.dexposed.XposedHelpers;

import android.app.PendingIntent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import dalvik.system.DexFile;

/**
 * @author zhkl0228
 *
 */
public class Sdk23 extends UnknownSdk {

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.sdk.Sdk#hook_defineClass(com.fuzhu8.inspector.advisor.Hooker, com.fuzhu8.inspector.advisor.Hookable)
	 */
	@Override
	public void hook_defineClass(Hooker hooker, Hookable handler) {
		try {
			Method method = DexFile.class.getDeclaredMethod("defineClassNative", String.class, ClassLoader.class, Object.class);
			hooker.hook(DexFile.class, method, handler, false);
		} catch (Throwable e) {
			try {
				Method method = DexFile.class.getDeclaredMethod("defineClass", String.class, ClassLoader.class, Object.class);
				hooker.hook(DexFile.class, method, handler, false);
			} catch (Throwable e1) {
				hooker.log(e1);
			}
		}
	}

	@Override
	public void hook_openDexFile(Hooker hooker, Hookable handler) {
	}

	/* (non-Javadoc)
         * @see com.fuzhu8.inspector.sdk.Sdk#hook_sendTextMessage(com.fuzhu8.inspector.advisor.Hooker, com.fuzhu8.inspector.advisor.Hookable, java.lang.Class)
         */
	@Override
	public void hook_sendTextMessage(Hooker hooker, Hookable handler, Class<?> ISmsClass) throws NoSuchMethodException {
		Method method = ISmsClass.getMethod("sendTextForSubscriber", int.class, String.class, String.class, String.class, String.class, PendingIntent.class, PendingIntent.class, boolean.class);
		hooker.hook(ISmsClass, method, handler, false);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.sdk.UnknownSdk#getInstalledPackages(android.content.pm.IPackageManager)
	 */
	@Override
	protected Map<String, PackageInfo> getInstalledPackages(IPackageManager pm) {
		try {
			Object list = XposedHelpers.callMethod(pm, "getInstalledPackages", PackageManager.GET_META_DATA, 0);
			List<?> pis = (List<?>) XposedHelpers.callMethod(list, "getList");
			Map<String, PackageInfo> map = new HashMap<>(pis.size());
			for(Object obj : pis) {
				PackageInfo pi = (PackageInfo) obj;
				map.put(pi.packageName, pi);
			}
			return map;
		} catch (Exception e) {
			throw new UnsupportedOperationException(e);
		}
	}

}
