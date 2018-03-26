package com.fuzhu8.inspector.dexposed;

import android.app.ActivityThread;
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
public class DexposedInspector extends AbstractInspector implements Runnable, Inspector {

	DexposedInspector(ModuleContext context,
					  DexFileManager dexFileManager, LuaScriptManager luaScriptManager, boolean broadcastPort) {
		super(context, dexFileManager, luaScriptManager);

		this.enableBroadcast = broadcastPort;
		
		/*try {
			DexposedBridge.findAndHookMethod(context.getClassLoader().loadClass("com.android.org.conscrypt.NativeCrypto"), "setEnabledCipherSuites", long.class, String[].class, new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					super.beforeHookedMethod(param);
					
					String[] cipherSuites = (String[]) param.args[1];
					List<String> list = new ArrayList<String>(cipherSuites.length);
					for(String suite : cipherSuites) {
						if(suite.startsWith("TLS_ECDH") || suite.startsWith("TLS_DHE") || suite.startsWith("SSL_DHE")) {
							continue;
						}
						list.add(suite);
					}
					param.args[1] = list.toArray(new String[0]);
				}
			});
		} catch(Exception e) {
			log(e);
		}*/
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
		// DexposedPrintInfoHandler handler = new DexposedPrintInfoHandler();
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

	@Override
	protected android.content.Context getAppContext() {
		android.content.Context context = super.getAppContext();
		if(context != null) {
			return context;
		}
		return ActivityThread.currentApplication();
	}

}
