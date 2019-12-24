/**
 * 
 */
package com.fuzhu8.inspector.bridge;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.script.LuaScriptManager;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.android.bridge.XC_MethodHook;
import cn.android.bridge.XposedHelpers;

/**
 * @author zhkl0228
 *
 */
public class BridgeTestCallback extends XC_MethodHook {
	
	private final Inspector inspector;

	public BridgeTestCallback(final Inspector inspector) {
		super();
		this.inspector = inspector;
		
		XposedHelpers.findAndHookMethod(Method.class, "invoke", Object.class, Object[].class, this);
		/*XposedHelpers.findAndHookMethod(Integer.class, "valueOf", int.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				
				inspector.print("Integer.valueOf(" + param.args[0] + ')');
			}
		});*/
		XposedHelpers.findAndHookConstructor(StringBuffer.class, String.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				
				if("gnoL.gnal.avaj".equals(param.args[0])) {
					inspector.println("Found java.lang.Long");
				}
			}
		});
		XC_MethodHook classCallback = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				
				inspector.println("Class." + param.method.getName() + " ret=" + param.getResult());
			}
		};
		XposedHelpers.findAndHookMethod(Class.class, "getConstructor", Class[].class, classCallback);
		XposedHelpers.findAndHookMethod(Class.class, "getDeclaredConstructor", Class[].class, classCallback);
		XposedHelpers.findAndHookMethod(Class.class, "getMethod", String.class, Class[].class, classCallback);
		XposedHelpers.findAndHookMethod(Class.class, "getDeclaredMethod", String.class, Class[].class, classCallback);
		XposedHelpers.findAndHookMethod(Class.class, "getField", String.class, classCallback);
		XposedHelpers.findAndHookMethod(Class.class, "getDeclaredField", String.class, classCallback);
	}

	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		super.afterHookedMethod(param);
		
		if(param.args[0] instanceof Inspector ||
				param.args[0] instanceof DexFileManager ||
				param.args[0] instanceof LuaScriptManager) {
			return;
		}
		Method method = (Method) param.thisObject;
		if(method.getDeclaringClass() == AccessibleObject.class &&
				"setAccessible".equals(method.getName())) {
			return;
		}
		Map<Integer, byte[]> dataMap = new LinkedHashMap<Integer, byte[]>();
		StringBuffer buffer = new StringBuffer();
		buffer.append(method.getDeclaringClass().getName());
		buffer.append("->");
		buffer.append(method.getName());
		buffer.append('(');
		Object[] os = (Object[]) param.args[1];
		Object[] args = new Object[os.length + 1];
		args[0] = param.args[0];
		System.arraycopy(os, 0, args, 1, os.length);
		int index = 0;
		if(args[0] == null) {
			index = 1;
		}
		if(index < args.length) {
			for(int i = index; i < args.length; i++) {
				Object obj = args[i];
				if(obj instanceof String) {
					buffer.append('"').append(obj).append("\", ");
					continue;
				}
				
				buffer.append(obj).append(", ");
				
				if(obj instanceof byte[]) {
					dataMap.put(i, (byte[]) obj);
				}
			}
			buffer.delete(buffer.length() - 2, buffer.length());
		}
		buffer.append(')');
		
		if(param.getResult() != null) {
			buffer.append(" ret: ");
			if(param.getResult() instanceof String) {
				buffer.append('"').append(param.getResult()).append('"');
			} else {
				buffer.append(param.getResult());
			}
		}
		
		if(param.hasThrowable()) {
			buffer.append(" throws ").append(param.getThrowable().getMessage());
		}
		
		for(Map.Entry<Integer, byte[]> entry : dataMap.entrySet()) {
			inspector.inspect(entry.getValue(), "Arg_" + entry.getKey() + ": " + buffer);
		}
		
		if(param.getResult() instanceof byte[]) {
			inspector.inspect((byte[]) param.getResult(), "Ret: " + buffer);
		}
		
		inspector.println(buffer);
	}

}
