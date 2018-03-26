package com.fuzhu8.inspector;

import com.facebook.stetho.Stetho;
import com.fuzhu8.inspector.completer.ServerCommandCompleter;
import com.fuzhu8.inspector.io.InspectCache;
import com.fuzhu8.inspector.unicorn.Emulator;
import com.fuzhu8.tcpcap.handler.Appender;

import java.io.File;
import java.nio.ByteBuffer;

public interface Inspector extends Runnable, Appender {

	void println(Object msg);
	
	void inspect(byte[] data, String label);
	
	void inspect(ByteBuffer data, String label);
	
	void writeToConsole(InspectCache cache);
	
	void inspect(byte[] data, boolean send);
	
	void inspect(int type, byte[] data, boolean send);
	
	void evalLuaScript(String script);
	
	void inspect(short[] data, String label);
	
	void setDebug(boolean debug);
	boolean isDebug();
	
	/**
	 * 应用dataDir
	 */
	File getAppDataDir();
	
	File[] getAppLibDir();
	
	/**
	 * 模块lib目录
	 * @return directory
	 */
	File getModuleLibDir();
	
	void collectDexFile(byte[] dex, String name);
	
	/**
	 * 启用本地字节码收集功能
	 * @param filter 过滤
	 */
	void enableCollectBytecode(String filter);
	
	/**
	 * 创建一个命令完成器
	 * @param prefix 命令前缀
	 * @return 返回的对象用commit提交
	 */
	ServerCommandCompleter createCommandCompleter(String prefix);

	Emulator emuArm();
	Emulator emuThumb();

    void printHelp();

	/**
	 * manual start stetho with initializer
	 */
	boolean startStetho(Stetho.Initializer initializer);
}