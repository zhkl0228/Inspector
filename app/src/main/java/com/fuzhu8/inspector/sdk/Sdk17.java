package com.fuzhu8.inspector.sdk;

import java.lang.reflect.Method;
import java.util.ArrayList;
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
public class Sdk17 extends UnknownSdk implements Sdk {

	@Override
	public final void hook_defineClass(Hooker hooker, Hookable handler) {
		try {
			Method method = DexFile.class.getDeclaredMethod("defineClassNative", String.class, ClassLoader.class, int.class);
			hooker.hook(DexFile.class, method, handler, false);
		} catch (Throwable e) {
			try {
				Method method = DexFile.class.getDeclaredMethod("defineClass", String.class, ClassLoader.class, int.class);
				hooker.hook(DexFile.class, method, handler, false);
			} catch (Throwable e1) {
				hooker.log(e1);
			}
		}
	}

	@Override
	public void hook_openDexFile(Hooker hooker, Hookable handler) {
		try {
			Method method = DexFile.class.getDeclaredMethod("openDexFileNative", String.class, String.class, int.class);
			hooker.hook(DexFile.class, method, handler, false);
		} catch (Throwable throwable) {
			try {
				Method method = DexFile.class.getDeclaredMethod("openDexFile", String.class, String.class, int.class);
				hooker.hook(DexFile.class, method, handler, false);
			} catch (Throwable e1) {
				hooker.log(e1);
			}
		}
	}

	@Override
	protected Map<String, PackageInfo> getInstalledPackages(IPackageManager pm) {
		try {
			Object list = XposedHelpers.callMethod(pm, "getInstalledPackages", PackageManager.GET_META_DATA, (String) null, 0);
			final List<PackageInfo> pis = new ArrayList<PackageInfo>();
			XposedHelpers.callMethod(list, "populateList", pis, PackageInfo.CREATOR);
			Map<String, PackageInfo> map = new HashMap<String, PackageInfo>(pis.size());
			for(PackageInfo pi : pis) {
				map.put(pi.packageName, pi);
			}
			return map;
		} catch (Exception e) {
			throw new UnsupportedOperationException(e);
		}
	}

	@Override
	public void hook_sendTextMessage(Hooker hooker, Hookable handler, Class<?> ISmsClass) throws NoSuchMethodException {
		Method method = ISmsClass.getMethod("sendText", String.class, String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
		hooker.hook(ISmsClass, method, handler, false);
	}

}
