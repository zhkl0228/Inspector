package com.fuzhu8.inspector.advisor;

import com.alibaba.fastjson.JSON;
import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;

/**
 * @author zhkl0228
 *
 */
public abstract class AbstractAdvisor extends AbstractHookHandler implements Hookable, HookOperation {

	public AbstractAdvisor(ModuleContext context) {
		super(context);
		
		executeHook();
	}

	protected final void hook(Inspector inspector, String className, String methodName, String...args) {
		StringBuilder script = new StringBuilder();
		script.append("hook(").append('"').append(className).append('"');
		script.append(',').append('"').append(methodName).append('"');
		for(String arg : args) {
			if(arg == null) {
				script.append(",nil");
				continue;
			}
			
			script.append(',').append('"').append(arg).append('"');
		}
		script.append(')');
		inspector.evalLuaScript(script.toString());
	}

	/**
	 * 执行hook
	 */
	protected abstract void executeHook();

	@Override
	protected Object getHandler() {
		return this;
	}
	
	/**
	 * 格式化json
	 */
	public static String formatJson(String json) {
		Object obj = JSON.parse(json);
		return JSON.toJSONString(obj, true);
	}

}
