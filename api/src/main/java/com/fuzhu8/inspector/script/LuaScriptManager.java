package com.fuzhu8.inspector.script;

import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.dex.DexFileManager;

/**
 * @author zhkl0228
 *
 */
public interface LuaScriptManager {
	
	/**
	 * 注册函数
	 */
	void registerFunction(String name, FunctionRegister function) throws RegisterException;

	/**
	 * 执行lua脚本
	 */
	void eval(String lua) throws Exception;

	/**
	 * 注册所有函数
	 */
	void registerAll(DexFileManager dexFileManager) throws Exception;

	/**
	 * 组装
	 */
	void setInspector(Inspector inspector);
	
	/**
	 * 注册全局变量
	 */
	void registerGlobalObject(String name, Object obj) throws Exception;

}
