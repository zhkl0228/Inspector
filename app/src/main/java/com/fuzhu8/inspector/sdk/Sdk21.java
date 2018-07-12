package com.fuzhu8.inspector.sdk;

import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.fuzhu8.inspector.advisor.Hookable;
import com.fuzhu8.inspector.advisor.Hooker;
import com.taobao.android.dexposed.XposedHelpers;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexFile;

/**
 * @author zhkl0228
 *
 */
public class Sdk21 extends UnknownSdk implements Sdk {

	@Override
	public final void hook_defineClass(Hooker hooker, Hookable handler) {
		try {
			Method method = DexFile.class.getDeclaredMethod("defineClassNative", String.class, ClassLoader.class, long.class);
			hooker.hook(DexFile.class, method, handler, false);
		} catch (Throwable e) {
			hooker.log(e);
		}
	}

	@Override
	public void hook_openDexFile(Hooker hooker, Hookable handler) {
	}

	/*@Override
	public void hook_sendTextMessage(Hooker hooker, Hookable handler, Class<?> ISmsClass) throws NoSuchMethodException {
		Method method = ISmsClass.getMethod("sendText", String.class, String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
		hooker.hook(ISmsClass, method, handler, false);
	}*/

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
