package com.fuzhu8.inspector.xposed;

import android.os.Process;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.advisor.AbstractInspector;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.script.LuaScriptManager;

import java.util.HashSet;
import java.util.Set;

/**
 * @author zhkl0228
 *
 */
class XposedInspector extends AbstractInspector implements Runnable, Inspector {

	XposedInspector(ModuleContext context,
					DexFileManager dexFileManager, LuaScriptManager luaScriptManager, boolean broadcastPort) {
		super(context, dexFileManager, luaScriptManager);

		this.enableBroadcast = broadcastPort;
	}
	
	private final Set<Object> hookSet = new HashSet<>();

	@SuppressWarnings("unused")
	Object getDefaultPhone(Object thisObj, Object ret) {
		log("PhoneFactory.getDefaultPhone phone=" + ret + ", pid=" + Process.myPid());
		
		if(ret == null ||
				hookSet.contains(ret)) {
			return ret;
		}

		Class<?> phoneClass = ret.getClass();
		// XposedPrintInfoHandler handler = new XposedPrintInfoHandler();
		// hookAll(phoneClass, handler);
		
		hookSet.add(ret);
		try {
			hook(phoneClass, "getImei");
			// hook(phoneClass, "getDeviceId");
		} catch (NoSuchMethodException e) {
			log(e);
		}
		
		return ret;
	}

	@SuppressWarnings("unused")
	String getImei(Object thisObj, String imei) {
		log("getImei imei=" + imei + ", pid=" + Process.myPid());
		return imei;
	}

}
