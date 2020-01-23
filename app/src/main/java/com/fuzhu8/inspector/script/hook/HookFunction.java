package com.fuzhu8.inspector.script.hook;

import android.app.Application;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.dex.ClassLoaderListener;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.dex.provider.DexFileProvider;
import com.fuzhu8.inspector.script.InspectorFunction;

import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zhkl0228
 *
 */
public abstract class HookFunction extends InspectorFunction implements ClassLoaderListener {
	
	private final DexFileManager dexFileManager;
	private final Set<Member> hookedSet = new HashSet<>();
	protected final ModuleContext context;

	public HookFunction(LuaState L, Inspector inspector, DexFileManager dexFileManager, ModuleContext context) {
		super(L, inspector);
		
		this.dexFileManager = dexFileManager;
		this.context = context;
	}

	@Override
	public final int execute() {
		int count = L.getTop();
		if(count >= 3) {
			String clazz = getParam(2).getString();
			String method = getParam(3).getString();
			LuaObject[] params = new LuaObject[count - 3];
			for(int i = 0; i < params.length; i++) {
				params[i] = getParam(4 + i);
			}
			
			LuaObject callback = null;
			String[] types;
			if(params.length > 0 && params[params.length - 1].isFunction()) {
				callback = params[params.length - 1];
				types = new String[params.length - 1];
			} else {
				types = new String[params.length];
			}
			for(int i = 0; i < types.length; i++) {
				types[i] = params[i].getString();
			}
			
			executeHook(clazz, method, callback, types);
		}
		return 0;
	}
	
	private final List<HookFunctionRequest> hookList = new ArrayList<>();

	private void executeHook(String clazz, String method, LuaObject callback, String...params) {
		HookFunctionRequest request = createHookFunctionRequest(clazz, method, callback, params);
		hookList.add(request);
		List<ClassNotFoundException> exceptions = new ArrayList<>();
		boolean successfully = false;
		for(DexFileProvider dex : dexFileManager.dumpDexFiles(true)) {
			try {
				if (request.tryHook(dex.getClassLoader(), inspector, dexFileManager, hookedSet)) {
					dex.print(inspector);
					successfully = true;
				}
			} catch(ClassNotFoundException e) {
				exceptions.add(e);
			} catch(Exception t) {
				log(t);
				// inspector.println("hook from classloader " + dex.getClassLoader() + " failed: " + t.getMessage());
			}
		}

		Application application;
		if (successfully) {
			exceptions.clear();
		} else if((application = context.getApplication()) != null) {
			try {
				if (request.tryHook(application.getClassLoader(), inspector, dexFileManager, hookedSet)) {
					inspector.println("hook from context classloader " + application.getClassLoader());
				}
			} catch(ClassNotFoundException e) {
				exceptions.add(e);
			} catch(Exception t) {
				log(t);
			}
		}
		
		if(!exceptions.isEmpty()) {
			for(ClassNotFoundException cnfe : exceptions) {
				inspector.println(cnfe);
			}
		}
	}

	protected abstract HookFunctionRequest createHookFunctionRequest(String clazz, String method, LuaObject callback,
			String[] params);

	@Override
	public final void notifyClassLoader(ClassLoader classLoader) {
		for(HookFunctionRequest request : hookList) {
			try {
				request.tryHook(classLoader, inspector, dexFileManager, hookedSet);
			} catch(Throwable t) {
				log(t);
				inspector.println("hook from classloader " + classLoader + " failed: " + t.getMessage());
			}
		}
	}
	
	protected abstract void log(Throwable t);

}
