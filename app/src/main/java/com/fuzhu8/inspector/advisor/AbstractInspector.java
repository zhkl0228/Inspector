package com.fuzhu8.inspector.advisor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.LocalServerSocket;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Process;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.alibaba.dcm.DnsCache;
import com.alibaba.dcm.DnsCacheEntry;
import com.alibaba.dcm.DnsCacheManipulator;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.dumpapp.DumperPlugin;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.freakishfox.xAnSo.xAnSoUnpacker;
import com.fuzhu8.inspector.BuildConfig;
import com.fuzhu8.inspector.ClientConnectListener;
import com.fuzhu8.inspector.Hex;
import com.fuzhu8.inspector.Inspector;
import com.fuzhu8.inspector.LibraryAbi;
import com.fuzhu8.inspector.ModuleContext;
import com.fuzhu8.inspector.MyModuleContext;
import com.fuzhu8.inspector.Unpacker;
import com.fuzhu8.inspector.completer.DefaultServerCommandCompleter;
import com.fuzhu8.inspector.completer.ServerCommandCompleter;
import com.fuzhu8.inspector.content.InspectorBroadcastListener;
import com.fuzhu8.inspector.content.InspectorBroadcastReceiver;
import com.fuzhu8.inspector.dex.ClassMethod;
import com.fuzhu8.inspector.dex.DexFile;
import com.fuzhu8.inspector.dex.DexFileManager;
import com.fuzhu8.inspector.dex.SmaliFile;
import com.fuzhu8.inspector.dex.jf.Smali;
import com.fuzhu8.inspector.dex.provider.DexFileProvider;
import com.fuzhu8.inspector.dex.provider.StaticDexFileElement;
import com.fuzhu8.inspector.http.HttpUtils;
import com.fuzhu8.inspector.io.ByteBufferCache;
import com.fuzhu8.inspector.io.ByteBufferInspectCache;
import com.fuzhu8.inspector.io.Command;
import com.fuzhu8.inspector.io.Console;
import com.fuzhu8.inspector.io.InputStreamCache;
import com.fuzhu8.inspector.io.InspectCache;
import com.fuzhu8.inspector.io.LargeMessageCache;
import com.fuzhu8.inspector.io.OpenTraceFile;
import com.fuzhu8.inspector.io.ShortBufferInspectCache;
import com.fuzhu8.inspector.io.SocketConsole;
import com.fuzhu8.inspector.io.StringCache;
import com.fuzhu8.inspector.jni.DexHunter;
import com.fuzhu8.inspector.jni.InspectorNative;
import com.fuzhu8.inspector.jni.TraceAnti;
import com.fuzhu8.inspector.kraken.KrakenCapture;
import com.fuzhu8.inspector.maps.NativeLibraryMapInfo;
import com.fuzhu8.inspector.plugin.Plugin;
import com.fuzhu8.inspector.root.LineListener;
import com.fuzhu8.inspector.root.RootUtil;
import com.fuzhu8.inspector.script.LuaScriptManager;
import com.fuzhu8.inspector.script.MethodHashUtils;
import com.fuzhu8.inspector.stetho.StethoInitializer;
import com.fuzhu8.inspector.ui.StartVpnActivity;
import com.fuzhu8.inspector.unicorn.Emulator;
import com.fuzhu8.inspector.unicorn.EmulatorFactory;
import com.fuzhu8.inspector.vpn.InspectVpnService;
import com.fuzhu8.inspector.vpn.InspectorPacketCapture;
import com.jaredrummler.android.processes.AndroidProcesses;
import com.lahm.library.EasyProtectorLib;
import com.nagapt.udog.UDogUnpacker;
import com.thomasking.sodumphelper.MainActivity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.State;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import capstone.Arm;
import capstone.Arm_const;
import capstone.Capstone;
import cn.banny.utils.StringUtils;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import unicorn.BlockHook;
import unicorn.CodeHook;
import unicorn.Unicorn;

/**
 * @author zhkl0228
 */
@SuppressLint("DefaultLocale")
public abstract class AbstractInspector extends AbstractAdvisor implements
		Inspector, LineListener, ClientConnectListener {

	protected final DexFileManager dexFileManager;
	private final LuaScriptManager luaScriptManager;
	private final ServerCommandCompleter global;
	private final ServerCommandCompleter inspector;

	private final InspectorPacketCapture packetCapture;

	public AbstractInspector(ModuleContext context, DexFileManager dexFileManager,
							 LuaScriptManager luaScriptManager) {
		super(context);

		this.dexFileManager = dexFileManager;
		this.luaScriptManager = luaScriptManager;

		this.packetCapture = new InspectorPacketCapture(this, context.getDataDir(), context.getProcessName());

		this.initializeLogServer();

		InspectorNative.getInstance().initializeNative(this);
		TraceAnti.getInstance();
		DexHunter.getInstance();

		executeMyHook();

		global = this.createCommandCompleter(null);
		global.addCommandHelp("log", "log(msg); -- log to console");
		// global.addCommandHelp("logd", "logd(msg); -- log to logcat");
		global.addCommandHelp("where", "where(msg?); -- print the stack trace to console");
		// global.addCommandHelp("inspect", "inspect(data, label?); -- inspect bytes");
		global.addCommandHelp("dc()", "dc(); -- discover runtime stack class loaders.");
		global.addCommandHelp("hook", "hook(class, method, ..., callback?); -- hook api, method = nil means constructor, method = '*' means all constructor and method.",
				"hook(\"java.lang.String\", \"equalsIgnoreCase\", \"java.lang.String\", function(old, thisObj, ...)",
				"    local ret = old:call(thisObj, ...);",
				"    log(\"equalsIgnoreCase thisObj=\" .. thisObj .. \", ret=\" .. tostring(ret));",
				"    return ret;",
				"end);");

		inspector = this.createCommandCompleter("inspector:");
		inspector.addCommandHelp("inspector:println", "inspector:println(msg); -- print msg");
		inspector.addCommandHelp("inspector:dumpDex", "inspector:dumpDex(includeBootClassPath); -- dump dex file list");
		inspector.addCommandHelp("inspector:saveDex", "inspector:saveDex(dexPath); -- receive the dex file: saveDex(dexPath, false)",
				"inspector:saveDex(dexPath, collectAll); -- receive the dex file: saveDex(dexPath, collectAll, false)",
				"inspector:saveDex(dexPath, collectAll, dexHunter); -- receive the dex file, use dexBuffer if dexHunter");
		inspector.addCommandHelp("inspector:baksmali", "inspector:baksmali(className); -- receive the smali file: baksmali(className, false)",
				"inspector:baksmali(className, collectAll); -- receive the smali file: baksmali(className, collectAll, false)",
				"inspector:baksmali(className, collectAll, dexHunter); -- receive the smali file, use dexBuffer if dexHunter");
		inspector.addCommandHelp("inspector:saveDexByClass", "inspector:saveDexByClass(clazz); -- receive the dex contains the class");
		inspector.addCommandHelp("inspector:dexAll", "inspector:dexAll(includeBootClassPath?); -- receive all dex file");
		inspector.addCommandHelp("inspector:dumpClass", "inspector:dumpClass(dexPath, filter?); -- dump loaded class");
		inspector.addCommandHelp("inspector:hookDex", "inspector:hookDex(dexPath, hookConstructor); -- hook dex file to collect dalvik bytecode",
				"inspector:hookDex(dexPath, hookConstructor, invokeFilter); -- hook dex file to collect dalvik bytecode",
				"inspector:hookDex(dexPath, hookConstructor, invokeFilter, classFilter); -- hook dex file to collect dalvik bytecode");

		// inspector.addCommandHelp("inspector:send", "inspector:send(dest, msg); -- send text message");
		inspector.addCommandHelp("inspector:dumpField", "inspector:dumpField(clazz); -- dump fields");
		inspector.addCommandHelp("inspector:dumpMethod", "inspector:dumpMethod(clazz); -- dump methods");
		inspector.addCommandHelp("inspector:dumpClassCode", "inspector:dumpClassCode(clazz); -- dump class method code");
		inspector.addCommandHelp("inspector:dump", "inspector:dump(startAddr, length?endAddr); -- dump memory");
		inspector.addCommandHelp("inspector:mem", "inspector:mem(startAddr, length?endAddr); -- inspect memory");
		inspector.addCommandHelp("inspector:disasm", "inspector:disasm(startAddr, length?endAddr); -- arm disassemble");
		inspector.addCommandHelp("inspector:info()", "inspector:info(); -- print phone information");
		inspector.addCommandHelp("inspector:enableCollectBytecode", "inspector:enableCollectBytecode(filter); -- enable collect native bytecode");
		inspector.addCommandHelp("inspector:startMethodTracing()", "inspector:startMethodTracing();");
		inspector.addCommandHelp("inspector:stopMethodTracing", "inspector:stopMethodTracing(keywords?);");
		inspector.addCommandHelp("inspector:kill()", "inspector:kill(); -- kill the process");
		inspector.addCommandHelp("inspector:testAntiHook", "inspector:testAntiHook(r0, r1, r2, r3);");
		inspector.addCommandHelp("inspector:dumpSO", "inspector:dumpSO(so_path); -- dump so, thanks ThomasKing");
		inspector.addCommandHelp("inspector:dumpSOByUDog", "inspector:dumpSOByUDog(so_path); -- dump so, thanks devilogic");
		inspector.addCommandHelp("inspector:dumpSOByXAnSo", "inspector:dumpSOByXAnSo(so_path); -- dump so, thanks freakishfox");
		inspector.addCommandHelp("inspector:nativeHook", "inspector:nativeHook(addr); -- native inline hook, thanks ThomasKing");
		inspector.addCommandHelp("inspector:setDebug", "inspector:setDebug(debug); -- set debug status");
		inspector.addCommandHelp("inspector:dumpMaps", "inspector:dumpMaps(addr?, pull?); -- dump native library map info",
				"inspector:dumpMaps(name, pull?); -- dump native library map info");
		inspector.addCommandHelp("inspector:listAllDevs()", "inspector:listAllDevs(); -- list all iface");

		inspector.addCommandHelp("inspector:clearDnsCache()", "inspector:clearDnsCache(); -- clear dns cache");
		inspector.addCommandHelp("inspector:listDnsCache()", "inspector:listDnsCache(); -- list dns cache");

		inspector.addCommandHelp("inspector:enableAutoCompleteClasses(dexPath)", "inspector:enableAutoCompleteClasses(dexPath); -- enable auto complete classes for dex.");
		inspector.addCommandHelp("inspector:lynx", "inspector:lynx(url); -- print url content.");
		inspector.addCommandHelp("inspector:interrupt()", "inspector:interrupt(); -- interrupt current worker thread.");
		inspector.addCommandHelp("inspector:threads()", "inspector:threads(); -- list all threads.");
		inspector.addCommandHelp("inspector:thread", "inspector:thread(threadId); -- list the thread stack trace.");
		inspector.addCommandHelp("inspector:inspect", "inspector:inspect(data, labe? or boolean); -- inspect bytes");

		inspector.addCommandHelp("inspector:listHook()", "inspector:listHook(system?); -- list hooks");
		inspector.addCommandHelp("inspector:unhook", "inspector:unhook(index or keywords); -- unhook");
		inspector.addCommandHelp("inspector:unhookAll()", "inspector:unhookAll(); -- unhook all");
		inspector.addCommandHelp("inspector:pcap()", "inspector:pcap(); -- flush captured packet.");

		inspector.addCommandHelp("inspector:emuArm()", "inspector:emuArm(); -- create arm emulator.");
		inspector.addCommandHelp("inspector:emuThumb()", "inspector:emuThumb(); -- create thumb emulator.");

		inspector.addCommandHelp("inspector:logcat", "inspector:logcat(priority, tag); -- output the process's log to console. priority: VERBOSE -> 2, DEBUG -> 3, INFO -> 4, WARN -> 5, ERROR -> 6, ASSERT -> 7. empty tag disable output, '*' tag enable all tag.");
		inspector.addCommandHelp("inspector:startStetho()", "inspector:startStetho(); -- start stetho devtools server with default initializer");

		// inspector.addCommandHelp("inspector:setTcpRedirectRules", "inspector:setTcpRedirectRules(rules); -- Set tcp redirect rules over vpn, example: *:443->localhost:8888,*:8443->localhost:8888,120.35.*.*:8643->localhost:8888");

		inspector.addCommandHelp("inspector:startVpn", "inspector:startVpn(socksServer or packageName); -- Request start vpn, socks server example: localhost:8889 or packageName");
		inspector.addCommandHelp("inspector:stopVpn()", "inspector:stopVpn(); -- Request stop vpn");

		inspector.addCommandHelp("inspector:startPcap()", "inspector:startPcap(); -- Request start pcap, require root");
		inspector.addCommandHelp("inspector:frida()", "inspector:frida(); -- Load frida gadget");
		// inspector.addCommandHelp("inspector:stopPcap()", "inspector:stopPcap(); -- Request stop pcap");
	}

	@SuppressWarnings("unused")
	private void frida() {
		System.loadLibrary("frida-gadget");
		println("Load frida gadget successfully.");
	}

	private void onKrakenCapture(byte[] packet, int datalink) {
		try {
			this.packetCapture.onPacket(packet, "Kraken", datalink);
		} catch (Exception e) {
			println(e);
		}
	}

	@SuppressWarnings("unused")
	private void startPcap() {
		Context context = getAppContext();
		if (context == null) {
			err_println("start pcap failed: context is null");
			return;
		}

		RootUtil rootUtil = this.context.createRootUtil(10, null);
		rootUtil.executeAsync("dalvikvm -Djava.library.path=/vendor/lib:/system/lib:" + this.context.getModuleLibDir().getAbsolutePath() + " -cp " + this.context.getModulePath() + " com.android.internal.util.WithFramework " + KrakenCapture.class.getName());

		println("Request start pcap");
	}

	@SuppressWarnings("unused")
	private void startVpn() {
		startVpn(null);
	}

	private void startVpn(String socksServer) {
		Context context = getAppContext();
		if (context == null) {
			err_println("start vpn failed: context is null");
			return;
		}

		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PackageInfo packageInfo = null;
		try {
			if (socksServer != null) {
				packageInfo = context.getPackageManager().getPackageInfo(socksServer, PackageManager.GET_META_DATA);
			}
		} catch (NameNotFoundException ignored) {
		}

		String reason = "";
		int index = socksServer == null || packageInfo != null ? -1 : socksServer.indexOf(':');
		if (index != -1) {
			String socksHost = socksServer.substring(0, index);
			int socksPort = Integer.parseInt(socksServer.substring(index + 1));
			intent.putExtra(InspectVpnService.SOCKS_HOST_KEY, socksHost);
			intent.putExtra(InspectVpnService.SOCKS_PORT_KEY, socksPort);
			reason = " with socks server: " + socksHost + ':' + socksPort;
		} else if (packageInfo != null) {
			intent.putExtra(InspectVpnService.EXTRA_UID_KEY, packageInfo.applicationInfo.uid);
			reason = " with filter app: " + packageInfo.packageName;
		}
		intent.setClassName(BuildConfig.APPLICATION_ID, StartVpnActivity.class.getName());
		context.startActivity(intent);

		println("Request start vpn" + reason);
	}

	@SuppressWarnings("unused")
	private void stopVpn() {
		Context context = getAppContext();
		if (context == null) {
			err_println("stop vpn failed: context is null");
			return;
		}

		Intent intent = new Intent(InspectorBroadcastListener.REQUEST_STOP_VPN);
		context.sendBroadcast(intent);
		println("Request stop vpn");
	}

	@SuppressWarnings("unused")
	private void setTcpRedirectRules(String rules) {
		packetCapture.setTcpRedirectRules(rules);

		println("Set tcp redirect rules: " + rules);
	}

	private int logcatPriority;
	private String logcatTag;

	@SuppressWarnings("unused")
	public void logcat(int priority, String tag) {
		this.logcatPriority = priority;
		this.logcatTag = tag;

		String level;
		switch (priority) {
			case Log.VERBOSE:
				level = "VERBOSE";
				break;
			case Log.DEBUG:
				level = "DEBUG";
				break;
			case Log.INFO:
				level = "INFO";
				break;
			case Log.WARN:
				level = "WARN";
				break;
			case Log.ERROR:
				level = "ERROR";
				break;
			case Log.ASSERT:
				level = "ASSERT";
				break;
			default:
				if (priority < Log.VERBOSE) {
					level = "VERBOSE";
					priority = Log.VERBOSE;
				} else {
					level = null;
					priority = 0;
					tag = null;
				}
				break;
		}

		if (StringUtils.isEmpty(tag)) {
			logcatPriority = 0;
			logcatTag = null;
			println("disable logcat output.");
			return;
		}

		this.logcatPriority = priority;
		this.logcatTag = tag;
		if ("*".equals(tag)) {
			println("enable " + level + " logcat for all tag");
		} else {
			println("enable " + level + " logcat for tag: " + tag);
		}
	}

	public void pcap() throws IOException {
		packetCapture.flush();
	}

	@SuppressWarnings("unused")
	public void listHook() {
		listHook(false);
	}

	private void listHook(boolean system) {
		List<Unhook> hooks = context.getHooker().getHookList();
		for (int i = 0; i < hooks.size(); i++) {
			Unhook unhook = hooks.get(i);
			if (unhook.isUserHook() || system) {
				println(i + " " + unhook);
			}
		}
	}

	public void unhook(int index) {
		Unhook unhook = context.getHooker().getHook(index);
		if (unhook == null) {
			println("unhook failed for index: " + index);
			return;
		}
		unhook.unhook();
		println("unhook " + unhook + " successfully!");
	}

	@SuppressWarnings("unused")
	public void unhookAll() {
		unhook("");
	}

	public void unhook(String keywords) {
		List<Unhook> hooks = context.getHooker().getHookList();
		List<Unhook> list = new ArrayList<>();
		for (int i = 0; i < hooks.size(); i++) {
			Unhook unhook = hooks.get(i);
			if (unhook.isUserHook() && unhook.toString().contains(keywords)) {
				list.add(unhook);
			}
		}
		for (Unhook unhook : list) {
			unhook.unhook();
			println("unhook " + unhook + " successfully!");
		}
	}

	public void thread(int threadId) {
		Map<Thread, StackTraceElement[]> threadMap = Thread.getAllStackTraces();
		for (Map.Entry<Thread, StackTraceElement[]> entry : threadMap.entrySet()) {
			Thread thread = entry.getKey();
			if (thread.getId() != threadId) {
				continue;
			}

			println(thread.getName());
			println(thread.getState());
			for (StackTraceElement element : entry.getValue()) {
				println(element);
			}
			break;
		}
	}

	public void threads() {
		ServerCommandCompleter completer = createCommandCompleter("inspector:thread");
		Map<Thread, StackTraceElement[]> threadMap = Thread.getAllStackTraces();
		StringBuffer buffer = new StringBuffer();
		for (Thread thread : threadMap.keySet()) {
			buffer.setLength(0);
			if (thread.getState() == State.BLOCKED) {
				buffer.append('*');
			}
			buffer.append(thread.getId()).append(',').append(thread.getName()).append(',').append(thread.getState());
			println(buffer);
			completer.addCommandHelp("inspector:thread(" + thread.getId() + "); -- " + thread.getName(), "inspector:thread(threadId) -- list stack trace for thread: " + thread.getName());
		}
		completer.commit();

	}

	@SuppressWarnings("unused")
	public void lynx(String url) throws IOException {
		byte[] data = HttpUtils.sendGet(url);
		inspect(data, url);
	}

	@SuppressWarnings("unused")
	public void enableAutoCompleteClasses(String dexPath) {
		Collection<DexFileProvider> dexs = dexFileManager.dumpDexFiles(true);
		long currentTimeMillis = System.currentTimeMillis();
		ServerCommandCompleter completer = createCommandCompleter("inspector:saveDexByClass");
		for (DexFileProvider dex : dexs) {
			if (!dex.getMyPath().equals(dexPath)) {
				continue;
			}
			for (String str : dex.getClasses()) {
				completer.addCommandHelp("inspector:saveDexByClass(\"" + str + "\")", "inspector:saveDexByClass(clazz); -- receive the dex contains the class");
			}
		}
		completer.commit();
		println("initialized saveDexByClass: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		completer = createCommandCompleter("inspector:baksmali");
		for (DexFileProvider dex : dexs) {
			if (!dex.getMyPath().equals(dexPath)) {
				continue;
			}
			for (String str : dex.getClasses()) {
				completer.addCommandHelp("inspector:baksmali(\"" + str + "\"", "inspector:baksmali(className); -- receive the smali file: baksmali(className, false)",
						"inspector:baksmali(className, collectAll); -- receive the smali file: baksmali(className, collectAll, false)",
						"inspector:baksmali(className, collectAll, dexHunter); -- receive the smali file, use dexBuffer if dexHunter");
			}
		}
		completer.commit();
		println("initialized baksmali: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		completer = createCommandCompleter("inspector:dumpClassCode");
		for (DexFileProvider dex : dexs) {
			if (!dex.getMyPath().equals(dexPath)) {
				continue;
			}
			for (String str : dex.getClasses()) {
				completer.addCommandHelp("inspector:dumpClassCode(\"" + str + "\")", "inspector:dumpClassCode(clazz); -- dump class method code");
			}
		}
		completer.commit();
		println("initialized dumpClassCode: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		completer = createCommandCompleter("inspector:dumpMethod");
		for (DexFileProvider dex : dexs) {
			if (!dex.getMyPath().equals(dexPath)) {
				continue;
			}
			for (String str : dex.getClasses()) {
				completer.addCommandHelp("inspector:dumpMethod(\"" + str + "\")", "inspector:dumpMethod(clazz); -- dump methods");
			}
		}
		completer.commit();
		println("initialized dumpMethod: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		completer = createCommandCompleter("inspector:dumpField");
		for (DexFileProvider dex : dexs) {
			if (!dex.getMyPath().equals(dexPath)) {
				continue;
			}
			for (String str : dex.getClasses()) {
				completer.addCommandHelp("inspector:dumpField(\"" + str + "\")", "inspector:dumpField(clazz); -- dump fields");
			}
		}
		completer.commit();
		println("initialized dumpField: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		completer = createCommandCompleter(null);
		for (DexFileProvider dex : dexs) {
			if (!dex.getMyPath().equals(dexPath)) {
				continue;
			}
			for (String str : dex.getClasses()) {
				completer.addCommandHelp("hook(\"" + str + "\", ", "hook(class, method, ..., callback?); -- hook api, method = nil means constructor, method = '*' means all constructor and method.");
			}
		}
		completer.commit();
		println("initialized hook: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
	}

	@SuppressWarnings("unused")
	public void clearDnsCache() {
		DnsCacheManipulator.clearDnsCache();
	}

	@SuppressWarnings("unused")
	public void listDnsCache() {
		DnsCache cache = DnsCacheManipulator.getWholeDnsCache();
		for (DnsCacheEntry entry : cache.getCache()) {
			println(entry.getHost() + " -> " + Arrays.asList(entry.getIps()));
		}
	}

	@Override
	public void notifyLine(String line) {
		println(line);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while (!canStop) {
			try {
				if (serverSocket == null) {
					break;
				}

				Socket socket = serverSocket.accept();
				assert socket != null;

				socket.setKeepAlive(true);
				socket.setKeepAlive(true);
				Console console = new SocketConsole();
				this.console = console;

				console.open(socket, socket.getInputStream(), socket.getOutputStream());
				onConnected(console);

				println("Connect to console[" + console.getClass().getSimpleName() + "] successfully! ");
				InspectCache cache;
				while ((cache = cacheQueue.poll()) != null) {
					try {
						cache.writeTo(console);
					} catch (IOException ignored) {
					}
				}

				Command command;
				StringBuffer lua = new StringBuffer();
				while (this.console != null && (command = this.console.readCommand()) != null) {
					// log("Received command: " + command);

					command.execute(lua, this, this.context);
				}

			} catch (SocketTimeoutException ignored) {
			} catch (SocketException e) {
				IOUtils.closeQuietly(serverSocket);
				serverSocket = null;
				closeQuietly(localServerSocket);
				localServerSocket = null;
				initializeLogServer();
			} catch (IOException e) {
				if (MyModuleContext.isDebug()) {
					super.log(e);
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {
				}
			} catch (Exception t) {
				log(t);
			} finally {
				closeConsole();

				sendBroadcast();
			}
		}
	}

	private void closeQuietly(LocalServerSocket localServerSocket) {
		try {
			if (localServerSocket != null) {
				localServerSocket.close();
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	public void evalLuaScript(String script) {
		try {
			if (script == null || script.trim().length() < 1) {
				return;
			}

			println('\n' + script);
			luaScriptManager.eval(script);
			println("eval lua script successfully!");
		} catch (Throwable e) {
			err_println("evalLuaScript lua=\n" + script);
			println(e);
		}
	}

	@Override
	public void enableCollectBytecode(String filter) {
		try {
			TraceAnti.getInstance().enableCollectBytecode(filter);
			startMethodTracing(new File(getAppDataDir(), "collectBytecode.trace"));
		} catch (Throwable t) {
			println(t);
		}
	}

	@SuppressWarnings("unused")
	private synchronized void startMethodTracing() throws IOException {
		File tmp = new File(getAppDataDir(), context.getProcessName() + ".trace");
		startMethodTracing(tmp);
	}

	private File traceFilePath;

	private synchronized void startMethodTracing(File path) {
		FileUtils.deleteQuietly(path);
		Debug.startMethodTracing(path.getPath());
		println("method tracing started successfully to file: " + path);
		traceFilePath = path;
	}

	private synchronized void stopMethodTracing() {
		this.stopMethodTracing(null);
	}

	private synchronized void stopMethodTracing(String keywords) {
		Debug.stopMethodTracing();
		if (keywords == null) {
			println("method tracing stop successfully! ");
		}

		InputStream inputStream = null;
		try {
			if (traceFilePath != null) {
				inputStream = new FileInputStream(traceFilePath);
				writeToConsole(new OpenTraceFile(keywords == null ? "" : keywords, traceFilePath.getName(), inputStream, (int) traceFilePath.length()));
			} else if (keywords != null) {
				println("method tracing stop successfully! ");
			}
		} catch (IOException e) {
			println(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
			traceFilePath = null;
		}
	}

	@Override
	public final void printHelp() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(global.describeHelp());
		buffer.append(inspector.describeHelp());
		for (Plugin plugin : context.getPlugins()) {
			buffer.append(plugin.getHelpContent());
		}
		println(buffer);
	}

	@SuppressWarnings("unused")
	public void dumpMaps() {
		dumpMaps(0);
	}

	private void dumpMaps(long addr) {
		dumpMaps(addr, addr != 0);
	}

	private void dumpMaps(long addr, boolean pull) {
		try {
			List<NativeLibraryMapInfo> maps = NativeLibraryMapInfo.readNativeLibraryMapInfo();
			List<NativeLibraryMapInfo> printed = new ArrayList<>();
			for (NativeLibraryMapInfo map : maps) {
				if (addr == 0 || map.isWithinLibrary(addr)) {
					if (addr > 0) {
						println(map + ", offset=0x" + Long.toHexString(addr - map.getStartAddress()));
					} else {
						println(map);
					}
					printed.add(map);
				}
			}
			if (pull) {
				saveMaps(printed, false, true);
			}
		} catch (IOException e) {
			println(e);
		}
	}

	@SuppressWarnings("unused")
	public void dumpMaps(String name) {
		dumpMaps(name, false);
	}

	private void dumpMaps(String name, boolean pull) {
		try {
			List<NativeLibraryMapInfo> maps = NativeLibraryMapInfo.readNativeLibraryMapInfo();
			List<NativeLibraryMapInfo> printed = new ArrayList<>();
			for (NativeLibraryMapInfo map : maps) {
				if (StringUtils.isEmpty(name) || map.getLibraryName().contains(name)) {
					println(map);
					printed.add(map);
				}
			}
			saveMaps(printed, false, printed.size() == 1 || pull);
		} catch (IOException e) {
			println(e);
		}
	}

	/**
	 * 只保存一个map
	 *
	 * @param fullPath 是否保存所有so
	 */
	private void saveMaps(List<NativeLibraryMapInfo> list, boolean fullPath, boolean pull) {
		if (fullPath) {
			for (NativeLibraryMapInfo map : list) {
				InputStream inputStream = null;
				try {
					File file = new File(map.getLibraryName());
					inputStream = new FileInputStream(file);
					writeToConsole(new InputStreamCache(map.getLibraryName(), inputStream, (int) file.length()));
				} catch (IOException e) {
					println(e);
				} finally {
					IOUtils.closeQuietly(inputStream);
				}
			}
			return;
		}

		if (!pull) {
			return;
		}

		for (NativeLibraryMapInfo map : list) {
			String name = FilenameUtils.getName(map.getLibraryName());
			if ("so".equals(FilenameUtils.getExtension(name))) {
				String baseName = FilenameUtils.getBaseName(name);
				ByteBuffer memory = this.dexFileManager.dumpMemory(map.getStartAddress(), map.getEndAddress() - map.getStartAddress());
				writeToConsole(new ByteBufferCache(baseName + "_memory/" + name, memory));

				File file = new File(map.getLibraryName());
				if (file.canRead()) {
					FileInputStream inputStream = null;
					try {
						inputStream = new FileInputStream(file);
						writeToConsole(new InputStreamCache(baseName + "_original/" + name, inputStream, (int) file.length()));
					} catch (IOException e) {
						println(e);
					} finally {
						IOUtils.closeQuietly(inputStream);
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	public void nativeHook(int addr) {
		if (TraceAnti.getInstance().nativeHook(addr)) {
			println("Request nativeHook successfully!");
		} else {
			println("Request nativeHook failed!");
		}
	}

	@SuppressWarnings("unused")
	public void dumpSO(String so_path) {
		Unpacker unpacker = MainActivity.getDumpee();

		try {
			println("dumpSO by " + unpacker.getName());
			ByteBuffer buffer = unpacker.dumpByLinker(so_path);
			writeToConsole(new ByteBufferCache(FilenameUtils.getName(so_path), buffer));
		} catch (IOException e) {
			println(e);
		}
	}

	@SuppressWarnings("unused")
	private void dumpSOByUDog(String so_path) {
		Unpacker unpacker = UDogUnpacker.getInstance();

		try {
			println("dumpSO by " + unpacker.getName());
			ByteBuffer buffer = unpacker.dumpByLinker(so_path);
			writeToConsole(new ByteBufferCache(FilenameUtils.getName(so_path), buffer));
		} catch (IOException e) {
			println(e);
		}
	}

	@SuppressWarnings("unused")
	private void dumpSOByXAnSo(String so_path) {
		Unpacker unpacker = xAnSoUnpacker.getInstance();

		try {
			println("dumpSO by " + unpacker.getName());
			ByteBuffer buffer = unpacker.dumpByLinker(so_path);
			writeToConsole(new ByteBufferCache(FilenameUtils.getName(so_path), buffer));
		} catch (IOException e) {
			println(e);
		}
	}

	public void kill() {
		Context context = getAppContext();
		if (context != null) {
			Intent intent = new Intent(InspectorBroadcastListener.CONSOLE_DISCONNECTED);
			Bundle bundle = new Bundle();
			bundle.putInt(InspectorBroadcastReceiver.UID_KEY, context.getApplicationInfo().uid);
			int pid = Process.myPid();
			bundle.putInt(InspectorBroadcastReceiver.PID_KEY, pid);
			String processName = getProcessNameByPid(context, pid);
			if (processName != null) {
				bundle.putString(InspectorBroadcastReceiver.PROCESS_NAME_KEY, processName);
			}
			bundle.putString(InspectorBroadcastReceiver.PACKAGE_NAME_KEY, context.getPackageName());
			intent.putExtra(Bundle.class.getCanonicalName(), bundle);
			context.sendBroadcast(intent);
		}

		Process.killProcess(Process.myPid());
	}

	@Override
	public void out_print(Object msg) {
		writeToConsole(new LargeMessageCache(String.valueOf(msg), true));
	}

	@Override
	public void err_print(Object msg) {
		writeToConsole(new LargeMessageCache(String.valueOf(msg), false));
	}

	@Override
	public void printStackTrace(Throwable throwable) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		throwable.printStackTrace(printWriter);
		err_println(writer.toString());
	}

	@Override
	public void out_println(Object msg) {
		writeToConsole(new LargeMessageCache(String.valueOf(msg) + '\n', true));
	}

	@Override
	public void err_println(Object msg) {
		writeToConsole(new LargeMessageCache(String.valueOf(msg) + '\n', false));
	}

	private final Queue<InspectCache> cacheQueue = new LinkedBlockingQueue<>();

	@Override
	public synchronized final void writeToConsole(InspectCache cache) {
		if (console == null) {
			if (cache.canCache()) {
				cacheQueue.offer(cache);
				while (cacheQueue.size() > 512) {
					cacheQueue.poll();
				}
			}
			return;
		}

		try {
			cache.writeTo(console);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			closeConsole();
		}
	}

	@Override
	public final void onConnected(Console console) {
		long currentTimeMillis = System.currentTimeMillis();
		global.commit();
		println("initialized global: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		inspector.commit();
		println("initialized inspector: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		for (Plugin plugin : this.context.getPlugins()) {
			plugin.onConnected(console);

			println("initialized plugin " + plugin + ": " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
			currentTimeMillis = System.currentTimeMillis();
		}

		Collection<DexFileProvider> dexs = dexFileManager.dumpDexFiles(true);
		ServerCommandCompleter completer = createCommandCompleter("inspector:dumpClass");
		for (DexFileProvider dex : dexs) {
			completer.addCommandHelp("inspector:dumpClass(\"" + dex.getMyPath() + "\"", "inspector:dumpClass(dexPath, filter?); -- dump loaded class");
		}
		completer.commit();
		println("initialized dumpClass: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		completer = createCommandCompleter("inspector:saveDex");
		for (DexFileProvider dex : dexs) {
			completer.addCommandHelp("inspector:saveDex(\"" + dex.getMyPath() + "\"", "inspector:saveDex(dexPath); -- receive the dex file: saveDex(dexPath, false)",
					"inspector:saveDex(dexPath, collectAll); -- receive the dex file: saveDex(dexPath, collectAll, false)",
					"inspector:saveDex(dexPath, collectAll, dexHunter); -- receive the dex file, use dexBuffer if dexHunter");
		}
		completer.commit();
		println("initialized saveDex: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		completer = createCommandCompleter("inspector:hookDex");
		for (DexFileProvider dex : dexs) {
			completer.addCommandHelp("inspector:hookDex(\"" + dex.getMyPath() + "\"", "inspector:hookDex(dexPath, hookConstructor); -- hook dex file to collect dalvik bytecode",
					"inspector:hookDex(dexPath, hookConstructor, invokeFilter); -- hook dex file to collect dalvik bytecode",
					"inspector:hookDex(dexPath, hookConstructor, invokeFilter, classFilter); -- hook dex file to collect dalvik bytecode");
		}
		completer.commit();
		println("initialized hookDex: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		completer = createCommandCompleter("inspector:enableAutoCompleteClasses");
		for (DexFileProvider dex : dexs) {
			if (dex instanceof StaticDexFileElement) {
				continue;
			}

			completer.addCommandHelp("inspector:enableAutoCompleteClasses(\"" + dex.getMyPath() + "\")", "inspector:enableAutoCompleteClasses(dexPath); -- enable auto complete classes for dex.");
		}
		completer.commit();
		println("initialized enableAutoCompleteClasses: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		currentTimeMillis = System.currentTimeMillis();

		try {
			completer = createCommandCompleter("inspector:dumpSO");
			List<NativeLibraryMapInfo> maps = NativeLibraryMapInfo.readNativeLibraryMapInfo();
			for (NativeLibraryMapInfo map : maps) {
				if (!map.getLibraryName().endsWith(".so")) {
					continue;
				}
				completer.addCommandHelp("inspector:dumpSO(\"" + map.getLibraryName() + "\")", "inspector:dumpSO(so_path); -- dump so, thanks ThomasKing");
				completer.addCommandHelp("inspector:dumpSOByUDog(\"" + map.getLibraryName() + "\")", "inspector:dumpSOByUDog(so_path); -- dump so, thanks devilogic");
				completer.addCommandHelp("inspector:dumpSOByXAnSo(\"" + map.getLibraryName() + "\")", "inspector:dumpSOByXAnSo(so_path); -- dump so, thanks freakishfox");
			}
			completer.commit();
			println("initialized dumpSO: " + (System.currentTimeMillis() - currentTimeMillis) + "ms.");
		} catch (IOException e) {
			println(e);
		}

		Context context = getAppContext();
		if (context != null) {
			packetCapture.checkSniffer(this.context.getPlugins());

			Intent intent = new Intent(InspectorBroadcastListener.CONSOLE_CONNECTED);
			Bundle bundle = new Bundle();
			bundle.putInt(InspectorBroadcastReceiver.UID_KEY, context.getApplicationInfo().uid);
			int pid = Process.myPid();
			bundle.putInt(InspectorBroadcastReceiver.PID_KEY, pid);
			String processName = getProcessNameByPid(context, pid);
			if (processName != null) {
				bundle.putString(InspectorBroadcastReceiver.PROCESS_NAME_KEY, processName);
			}
			bundle.putString(InspectorBroadcastReceiver.PACKAGE_NAME_KEY, context.getPackageName());
			bundle.putBinder(InspectorBroadcastReceiver.PACKET_CAPTURE_KEY, packetCapture);
			intent.putExtra(Bundle.class.getCanonicalName(), bundle);
			context.sendBroadcast(intent);
		}
	}

	@Override
	public final void onClosed(Console console) {
		for (Plugin plugin : context.getPlugins()) {
			plugin.onClosed(console);
		}

		Context context = getAppContext();
		if (context != null) {
			Intent intent = new Intent(InspectorBroadcastListener.CONSOLE_DISCONNECTED);
			Bundle bundle = new Bundle();
			bundle.putInt(InspectorBroadcastReceiver.UID_KEY, context.getApplicationInfo().uid);
			int pid = Process.myPid();
			bundle.putInt(InspectorBroadcastReceiver.PID_KEY, pid);
			String processName = getProcessNameByPid(context, pid);
			if (processName != null) {
				bundle.putString(InspectorBroadcastReceiver.PROCESS_NAME_KEY, processName);
			}
			bundle.putString(InspectorBroadcastReceiver.PACKAGE_NAME_KEY, context.getPackageName());
			intent.putExtra(Bundle.class.getCanonicalName(), bundle);
			context.sendBroadcast(intent);
		}
	}

	private void closeConsole() {
		if (console != null) {
			onClosed(console);

			console.close();
			console = null;
		}
	}

	@SuppressWarnings("unused")
	public void hookDex(String dexPath, boolean hookConstructor) {
		hookDex(dexPath, hookConstructor, null);
	}

	private void hookDex(String dexPath, boolean hookConstructor, String invokeFilter) {
		hookDex(dexPath, hookConstructor, invokeFilter, null);
	}

	private void hookDex(String dexPath, boolean hookConstructor, String invokeFilter, String classFilter) {
		try {
			String[] hooked = dexFileManager.requestHookDex(dexPath, hookConstructor, invokeFilter, classFilter);

			boolean added = false;
			for (String clazz : hooked) {
				println("hooked: " + clazz);
				added = true;
			}
			if (!added) {
				println("hook dex finished! ");
			}
		} catch (Throwable t) {
			println(t);
		}
	}

	@SuppressWarnings("unused")
	public void dumpDex() {
		dumpDex(false);
	}

	private void dumpDex(boolean includeBootClassPath) {
		try {
			Collection<DexFileProvider> dexs = dexFileManager.dumpDexFiles(includeBootClassPath);
			if (dexs.isEmpty()) {
				println("dex is empty! ");
				return;
			}

			println("#表示静态加载，*表示动态读取，&表示类库读取，@表示系统类库。");
			for (DexFileProvider dex : dexs) {
				try {
					dex.print(this);

					if (isDebug()) {
						dex.doTest();
					}
				} catch (Throwable t) {
					println(t);
				}
			}
		} catch (Throwable t) {
			println(t);
		}
	}

	@SuppressWarnings("unused")
	public void dumpDex(String className) {
		try {
			Collection<DexFileProvider> dexs = dexFileManager.dumpDexFiles(false);
			if (dexs.isEmpty()) {
				println("dex is empty! ");
				return;
			}

			println("#表示静态加载，*表示动态读取，&表示类库读取，@表示系统类库。");
			for (DexFileProvider dex : dexs) {
				try {
					try {
						dex.loadClass(className);
					} catch (ClassNotFoundException e) {
						continue;
					}

					dex.print(this);

					if (isDebug()) {
						dex.doTest();
					}
				} catch (Throwable t) {
					println(t);
				}
			}
		} catch (Throwable t) {
			println(t);
		}
	}

	@SuppressWarnings("unused")
	public void saveDexByClass(String clazz) {
		saveDexByClass(clazz, false);
	}

	private void saveDexByClass(final String clazz, final boolean dexHunter) {
		workingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (DexFileProvider dex : dexFileManager.dumpDexFiles(true)) {
					try {
						Class<?> cls = dex.loadClass(clazz);
						DexFile dexFileData = dexFileManager.getDexFileByteClass(cls, dexHunter, true);
						if (dexFileData == null) {
							continue;
						}

						dexFileData.writeToConsole(AbstractInspector.this);
						break;
					} catch (ClassNotFoundException ignored) {
					}
				}
			}
		});
		workingThread.start();
	}

	private Thread workingThread;

	public void interrupt() throws InterruptedException {
		if (workingThread != null) {
			workingThread.interrupt();
			workingThread.join();
			workingThread = null;
		}
	}

	@SuppressWarnings("unused")
	public void saveDex(String dexPath) {
		saveDex(dexPath, false);
	}

	private void saveDex(String dexPath, boolean collectAll) {
		saveDex(dexPath, collectAll, false);
	}

	private void saveDex(final String dexPath, final boolean collectAll, final boolean dexHunter) {
		stopMethodTracing();//保存dex前停止收集字节码

		workingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					DexFile data = dexFileManager.getDexFileData(dexPath, collectAll, dexHunter);
					if (data == null) {
						println("Not found dex: " + dexPath);
						return;
					}

					data.writeToConsole(AbstractInspector.this);
				} catch (Exception e) {
					println(e);
				}
			}
		});
		workingThread.start();
	}

	public void baksmali(final String className) throws Exception {
		baksmali(className, false);
	}

	public void baksmali(final String className, final boolean collectAll) throws Exception {
		baksmali(className, collectAll, false);
	}

	public void baksmali(final String className, final boolean collectAll, final boolean dexHunter) throws Exception {
		long currentTimeMillis = System.currentTimeMillis();
		long start = currentTimeMillis;
		SmaliFile[] smalies = dexFileManager.baksmali(className, collectAll, dexHunter);
		if (smalies == null || smalies.length < 1) {
			println("Not found smali: " + className);
			return;
		}

		String[] strings = new String[smalies.length];
		for (int i = 0; i < strings.length; i++) {
			strings[i] = smalies[i].getSmali();
		}
		println("baksmali " + Arrays.asList(smalies) + ", time offset " + (System.currentTimeMillis() - currentTimeMillis) + "ms");
		currentTimeMillis = System.currentTimeMillis();
		ByteBuffer buffer = Smali.assembleSmaliFile(this, strings);

		File dex = File.createTempFile("classes", ".dex");
		ByteBuffer copy = buffer.duplicate();
		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(dex);
			outputStream.write(copy.array(), copy.position(), copy.remaining());
		} finally {
			IOUtils.closeQuietly(outputStream);
		}

		try {
			String java = null;
			JadxArgs args = new JadxArgs();
			args.setDeobfuscationOn(false);
			args.setEscapeUnicode(false);
			args.setReplaceConsts(true);
			args.setShowInconsistentCode(false);
			args.setSkipResources(true);
			args.setUseSourceNameAsClassAlias(false);
			args.setThreadsCount(1);
			args.setShowInconsistentCode(true);
			JadxDecompiler jadx = new JadxDecompiler(args);
			jadx.loadFile(dex);
			println("smali " + Arrays.asList(smalies) + ", time offset " + (System.currentTimeMillis() - currentTimeMillis) + "ms");
			currentTimeMillis = System.currentTimeMillis();
			for (JavaClass javaClass : jadx.getClasses()) {
				if (javaClass.getFullName().equals(className)) {
					java = javaClass.getCode();
				}
			}
			if (java != null) {
				err_println(java);
			}

			println("decompile " + className + " successfully from " + smalies[0].getDex().getMyPath() + ", time offset " + (System.currentTimeMillis() - currentTimeMillis) + "ms, total offset " + (System.currentTimeMillis() - start) + "ms");
			for (SmaliFile smali : smalies) {
				writeToConsole(new StringCache("baksmali/" + smali.getClassName().replace('.', '/') + ".smali", smali.getSmali()));
			}
			if (java != null) {
				writeToConsole(new StringCache("baksmali/" + className.replace('.', '/') + ".java", java));
			}
			writeToConsole(new ByteBufferCache("baksmali/" + className + ".dex", buffer));
		} finally {
			if (!dex.delete()) {
				dex.deleteOnExit();
			}
		}
	}

	@SuppressWarnings("unused")
	public void dexAll() {
		dexAll(false);
	}

	private void dexAll(final boolean includeBootClassPath) {
		workingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Collection<DexFileProvider> dexs = dexFileManager.dumpDexFiles(includeBootClassPath);
					if (dexs.isEmpty()) {
						println("dex is empty! ");
						return;
					}
					for (DexFileProvider dex : dexs) {
						if (includeBootClassPath || dex instanceof StaticDexFileElement) {
							DexFile file = dex.createDexFileData(AbstractInspector.this, null, null, false);
							file.writeToConsole(AbstractInspector.this);
						}
					}
				} catch (Exception e) {
					println(e);
				}
			}
		});
		workingThread.start();
	}

	public void send(String destinationAddress, String text) {
		SmsManager.getDefault().sendTextMessage(destinationAddress, null, text, null, null);
		println("request sendTextMessage successfully! ");
	}

	final Object getSystemService(Object thisObj, String name, Object ret) {
		log("getSystemService this=" + thisObj +
				", name=" + name +
				", ret=" + ret);
		return ret;
	}

	@SuppressLint("HardwareIds")
	private void imei() {
		Context appContext = getAppContext();
		if (appContext != null && appContext.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
			println("imei: need READ_PHONE_STATE permission.");
			return;
		}
		TelephonyManager tm = appContext == null ? null : (TelephonyManager) appContext.getSystemService(Activity.TELEPHONY_SERVICE);
		if (tm == null) {
			println("obtain telephony manager failed ctx=" + appContext);
			return;
		}
		println("imei: " + tm.getDeviceId());
	}

	@SuppressLint("HardwareIds, MissingPermission")
	private void imsi() {
		Context appContext = getAppContext();
		if (appContext != null && appContext.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
			println("imsi: need READ_PHONE_STATE permission.");
			return;
		}
		TelephonyManager tm = appContext == null ? null : (TelephonyManager) appContext.getSystemService(Activity.TELEPHONY_SERVICE);
		if (tm == null) {
			println("obtain telephony manager failed ctx=" + appContext);
			return;
		}
		println("imsi: " + tm.getSubscriberId());

		Configuration configuration = appContext.getResources().getConfiguration();
		println("mcc: " + configuration.mcc);
		println("mnc: " + configuration.mnc);

		CellLocation cellLocation = tm.getCellLocation();
		if (cellLocation != null) {
			println("cellLocation: " + JSON.toJSONString(cellLocation));
		}
	}

	@SuppressLint("HardwareIds")
	private void number() {
		Context appContext = getAppContext();
		if (appContext != null && appContext.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
			println("number: need READ_PHONE_STATE permission.");
			return;
		}
		TelephonyManager tm = appContext == null ? null : (TelephonyManager) appContext.getSystemService(Activity.TELEPHONY_SERVICE);
		if (tm == null) {
			println("obtain telephony manager failed ctx=" + appContext);
			return;
		}
		println("number: " + tm.getLine1Number());

		println("networkCountryIso: " + tm.getNetworkCountryIso());
		println("networkOperator: " + tm.getNetworkOperator());
		println("networkOperatorName: " + tm.getNetworkOperatorName());

		println("simCountryIso: " + tm.getSimCountryIso());
		println("simOperator: " + tm.getSimOperator());
		println("simOperatorName: " + tm.getSimOperatorName());

		println("simSerialNumber: " + tm.getSimSerialNumber());
		println("deviceSoftwareVersion: " + tm.getDeviceSoftwareVersion());
	}

	@SuppressLint("HardwareIds")
	@SuppressWarnings("deprecated")
	public void info() {
		Context context = getAppContext();
		if (context != null) {
			try {
				ApplicationInfo applicationInfo = context.getApplicationInfo();
				PackageInfo packageInfo = context.getPackageManager().getPackageInfo(applicationInfo.packageName, 0);
				println("packageName: " + applicationInfo.packageName);
				println("versionName: " + packageInfo.versionName);
				println("versionCode: " + packageInfo.versionCode);

				String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
				println("androidId: " + androidId);
			} catch (NameNotFoundException e) {
				log(e);
			}
		}

		imei();
		imsi();
		number();

		Field[] fields = Build.class.getFields();
		for (Field field : fields) {
			try {
				if (Modifier.isStatic(field.getModifiers())) {
					Object obj = field.get(null);
					if (obj.getClass().isArray()) {
						List<Object> list = new ArrayList<>();
						Collections.addAll(list, (Object[]) obj);
						obj = list;
					}
					println(field.getName().toLowerCase() + ": " + obj);
				}
			} catch (Throwable t) {
				log(t);
			}
		}

		fields = Build.VERSION.class.getFields();
		for (Field field : fields) {
			try {
				if (Modifier.isStatic(field.getModifiers())) {
					Object obj = field.get(null);
					if (obj.getClass().isArray()) {
						List<Object> list = new ArrayList<>();
						Collections.addAll(list, (Object[]) obj);
						obj = list;
					}
					println("version_" + field.getName().toLowerCase() + ": " + obj);
				}
			} catch (Throwable t) {
				log(t);
			}
		}

		Runtime runtime = Runtime.getRuntime();
		println("totalMemory: " + toMB(runtime.totalMemory()));
		println("freeMemory: " + toMB(runtime.freeMemory()));
		println("maxMemory: " + toMB(runtime.maxMemory()));
		println("availableProcessors: " + runtime.availableProcessors());

		if (context != null) {
			println("cacheDir: " + context.getCacheDir());
			println("externalCacheDir: " + context.getExternalCacheDir());
			println("filesDir: " + context.getFilesDir());
			// println("obbDir: " + context.getObbDir());
			println("dataDir: " + context.getApplicationInfo().dataDir);
			println("publicSourceDir: " + context.getApplicationInfo().publicSourceDir);
			println("packageCodePath: " + context.getPackageCodePath());
			println("packageResourcePath: " + context.getPackageResourcePath());
		}

		println("dataDirectory: " + Environment.getDataDirectory());
		println("downloadCacheDirectory: " + Environment.getDownloadCacheDirectory());
		println("externalStorageDirectory: " + Environment.getExternalStorageDirectory());
		println("rootDirectory: " + Environment.getRootDirectory());

		if (context != null) {
			println("processName: " + getProcessNameByPid(context, Process.myPid()));
		}
		println("pid: " + Process.myPid());
		println("uid: " + Process.myUid());
		println("tid: " + Process.myTid());

		if (context != null) {
			PackageManager pm = context.getPackageManager();
			Intent intent = pm.getLaunchIntentForPackage(context.getPackageName());
			println("launchIntent: " + intent);

			WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			if (windowManager != null) {
				DisplayMetrics metrics = new DisplayMetrics();
				windowManager.getDefaultDisplay().getMetrics(metrics);
				println("metrics: " + metrics);
			}

			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetworkInfo = connectivityManager == null ? null : connectivityManager.getActiveNetworkInfo();
			NetworkInfo mobileNetworkInfo = connectivityManager == null ? null : connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			if (activeNetworkInfo != null) {
				println("activeNetworkInfo: typeName=" + activeNetworkInfo.getTypeName() + ", subtypeName=" + activeNetworkInfo.getSubtypeName());
			}
			if (mobileNetworkInfo != null) {
				println("mobileNetworkInfo: typeName=" + mobileNetworkInfo.getTypeName() + ", subtypeName=" + mobileNetworkInfo.getSubtypeName());
			}

			WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager == null ? null : wifiManager.getConnectionInfo();
			if (wifiInfo != null) {
				println("macAddress: " + wifiInfo.getMacAddress());
				println("ssid: " + wifiInfo.getSSID());
				println("bssid: " + wifiInfo.getBSSID());
			}

			SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
			if (sensorManager != null) {
				List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
				println("sensors: " + sensors);
			}
		}

		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter != null) {
			println("bluetoothName: " + bluetoothAdapter.getName());
			println("bluetoothMacAddress: " + bluetoothAdapter.getAddress());
		}

		printStorageSize("externalStorageDirectorySize", Environment.getExternalStorageDirectory());
		printStorageSize("rootDirectorySize", Environment.getRootDirectory());
		printStorageSize("dataDirectorySize", Environment.getDataDirectory());

		try {
			int cameras = Camera.getNumberOfCameras();
			List<JSONObject> cameraInfos = new ArrayList<JSONObject>(cameras);
			for (int i = 0; i < cameras; i++) {
				Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
				Camera.getCameraInfo(i, cameraInfo);
				JSONObject obj = new JSONObject();
				obj.put("facing", cameraInfo.facing);
				obj.put("orientation", cameraInfo.orientation);
				Camera camera = null;
				try {
					camera = Camera.open(i);
					camera.cancelAutoFocus();
					List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
					obj.put("supportedPictureSizes", sizes);
				} finally {
					if (camera != null) {
						camera.release();
					}
				}
				cameraInfos.add(obj);
			}
			println("cameraInfos: " + cameraInfos);
		} catch (Throwable ignored) {
		}

		if (context != null) {
			dumpSystemInfo(context);
		}

		println("isRoot: " + EasyProtectorLib.checkIsRoot());
		println("runningInEmulator: " + EasyProtectorLib.checkIsRunningInEmulator());
		println("usingMultiVirtualApp: " + EasyProtectorLib.checkIsUsingMultiVirtualApp());
		println("xposedExist: " + EasyProtectorLib.checkIsXposedExist());
	}

	@SuppressLint({"MissingPermission", "HardwareIds"})
	private void dumpSystemInfo(Context context) {
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		if (tm == null) {
			throw new IllegalStateException("Must have android.permission.READ_PHONE_STATE permission. ");
		}

		String imei = tm.getDeviceId();
		String imsi = tm.getSubscriberId();

		org.json.JSONObject systemInfo = SystemInfo.create(context, tm, imei, imsi);
		err_println("\nsystem info json: \n" + systemInfo);
	}

	private void printStorageSize(String label, File directory) {
		StatFs statFs = new StatFs(directory.getAbsolutePath());
		println(label + ": " + (statFs.getBlockCountLong() * statFs.getBlockSizeLong()));
	}

	private String toMB(long memory) {
		return (memory * 100 / (1024 * 1024)) / 100F + "MB";
	}

	@SuppressWarnings("unused")
	public void dumpField(String clazz) {
		ClassNotFoundException cnfe = null;
		for (DexFileProvider dex : dexFileManager.dumpDexFiles(true)) {
			try {
				Class<?> cls = dex.loadClass(clazz);
				Field[] fields = cls.getDeclaredFields();
				for (Field field : fields) {
					String hash = MethodHashUtils.hashField(field);
					if (Modifier.isStatic(field.getModifiers())) {
						boolean isAccessible = field.isAccessible();
						field.setAccessible(true);
						Object val;
						try {
							val = field.get(null);
						} catch (Throwable e) {
							val = e;
						}
						field.setAccessible(isAccessible);
						if (val instanceof byte[]) {
							inspect((byte[]) val, '[' + hash + ']' + field);
						} else {
							println('[' + hash + ']' + field + "=" + val + (val == null ? "" : (" [" + val.getClass() + "]")));
						}
						continue;
					}

					println('[' + hash + ']' + field);
				}
				println("\n");
				cnfe = null;
				break;
			} catch (ClassNotFoundException t) {
				cnfe = t;
			}
		}

		if (cnfe != null) {
			println(cnfe);
		}
	}

	@SuppressWarnings("unused")
	public void dumpMethod(String clazz) {
		ClassNotFoundException cnfe = null;
		for (DexFileProvider dex : dexFileManager.dumpDexFiles(true)) {
			try {
				Class<?> cls = dex.loadClass(clazz);
				Constructor[] constructors = cls.getConstructors();
				for (Constructor constructor : constructors) {
					println('[' + MethodHashUtils.hashConstructor(constructor) + ']' + constructor);
				}
				Method[] methods = cls.getDeclaredMethods();
				for (Method method : methods) {
					println('[' + MethodHashUtils.hashMethod(method) + ']' + method);
				}
				println("\n");
				println("from classloader: " + dex.getClassLoader());
				cnfe = null;
				break;
			} catch (ClassNotFoundException t) {
				cnfe = t;
			}
		}

		if (cnfe != null) {
			println(cnfe);
		}
	}

	@SuppressWarnings("unused")
	public void dumpClassCode(String clazz) {
		for (DexFileProvider dex : dexFileManager.dumpDexFiles(true)) {
			try {
				Class<?> cls = dex.loadClass(clazz);
				dex.print(this);
				Map<String, ClassMethod> map = dexFileManager.readClassMethodBytecode(cls);
				for (Map.Entry<String, ClassMethod> entry : map.entrySet()) {
					ClassMethod code = entry.getValue();
					if (code != null) {
						code.dump(entry.getKey(), this);
					}
				}
				println("\n\n");
			} catch (ClassNotFoundException ignored) {
			}
		}
	}

	public void dump(int startAddr, int lengthOrEndAddr) {
		int length = lengthOrEndAddr;
		if (lengthOrEndAddr >= startAddr) {
			length = lengthOrEndAddr - startAddr;
		}

		if (length < 1) {
			println("length must big than zero");
			return;
		}

		ByteBuffer memory = this.dexFileManager.dumpMemory(startAddr, length);
		writeToConsole(new ByteBufferCache("dump_" + Integer.toHexString(startAddr).toUpperCase() + '-' + Integer.toHexString(startAddr + length).toUpperCase() + ".dat", memory));
	}

	public void mem(int startAddr, int lengthOrEndAddr) {
		int length = lengthOrEndAddr;
		if (lengthOrEndAddr >= startAddr) {
			length = lengthOrEndAddr - startAddr;
		}

		if (length < 1) {
			println("length must big than zero");
			return;
		}

		ByteBuffer memory = this.dexFileManager.dumpMemory(startAddr, length);
		inspect(memory, "memory 0x" + Integer.toHexString(startAddr).toUpperCase() + "-0x" + Integer.toHexString(startAddr + length).toUpperCase());
	}

	public void dumpClass(String dexPath) {
		dumpClass(dexPath, null);
	}

	private void dumpClass(String dexPath, String filter) {
		try {
			Collection<String> classes = dexFileManager.getDexClasses(dexPath);
			boolean added = false;
			for (String clazz : classes) {
				if (filter == null ||
						clazz.contains(filter)) {
					println(clazz);
					added = true;
				}
			}
			if (!added) {
				println("dump class finished! ");
			}
		} catch (Throwable t) {
			println(t.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.Inspector#print(java.lang.Object)
	 */
	@Override
	public void println(Object msg) {
		if (!(msg instanceof Throwable)) {
			out_println(String.valueOf(msg));
			return;
		}

		err_println(Log.getStackTraceString((Throwable) msg));
	}

	/*void sendDataMessage(Object thisObj, String destinationAddress, String scAddress, short destinationPort, byte[] data,
						 PendingIntent sentIntent, PendingIntent deliveryIntent) {
		for (Plugin plugin : context.getPlugins()) {
			plugin.notifySendDataMessage(destinationAddress, scAddress, destinationPort, data, sentIntent, deliveryIntent);
		}
	}

	void sendMultipartTextMessage(Object thisObj, String destinationAddress, String scAddress, ArrayList<String> parts,
								  ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
		StringBuilder buffer = new StringBuilder();
		for (String part : parts) {
			buffer.append(part);
		}
		sendTextInternal(destinationAddress, scAddress, buffer.toString(), null, null, "sendMultipartTextMessage");
	}*/

	@Override
	protected void executeHook() {
	}

	private void executeMyHook() {
		// if(System.out != null) return;

		/*try {
			hook(SmsManager.class, "sendTextMessage", String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
		} catch (NoSuchMethodException e) {
			log(e);
		}

		try {
			hook(SmsManager.class, "sendDataMessage", String.class, String.class, short.class, byte[].class, PendingIntent.class, PendingIntent.class);
		} catch (NoSuchMethodException e) {
			log(e);
		}

		try {
			hook(SmsManager.class, "sendMultipartTextMessage", String.class, String.class, ArrayList.class, ArrayList.class, ArrayList.class);
		} catch (NoSuchMethodException e) {
			log(e);
		}

		try {
			IBinder binder = ServiceManager.getService("isms2");
			hookMessageService(binder);
		} catch (Throwable t) {
			log(t);
		}
		try {
			IBinder binder = ServiceManager.getService("isms");
			hookMessageService(binder);
		} catch (Throwable t) {
			log(t);
		}*/

		try {
			// hook(Activity.class, "onCreate", Bundle.class);
			context.getHooker().hookMethod(Activity.class.getDeclaredMethod("onResume"), new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					Thread thread = new Thread(new Runnable() {
						@Override
						public void run() {
							packetCapture.checkSniffer(context.getPlugins());
						}
					});
					thread.start();

					try {
						thread.join();

						Activity activity = (Activity) param.thisObject;
						Intent intent = new Intent(InspectorBroadcastListener.ACTIVITY_RESUME);
						Bundle bundle = new Bundle();
						bundle.putInt(InspectorBroadcastReceiver.UID_KEY, activity.getApplicationInfo().uid);
						int pid = Process.myPid();
						bundle.putInt(InspectorBroadcastReceiver.PID_KEY, pid);
						String processName = getProcessNameByPid(activity, pid);
						if (processName != null) {
							bundle.putString(InspectorBroadcastReceiver.PROCESS_NAME_KEY, processName);
						}
						bundle.putString(InspectorBroadcastReceiver.PACKAGE_NAME_KEY, activity.getPackageName());
						if (console != null) {
							bundle.putBinder(InspectorBroadcastReceiver.PACKET_CAPTURE_KEY, packetCapture);
						}
						intent.putExtra(Bundle.class.getCanonicalName(), bundle);
						activity.sendBroadcast(intent);
					} catch (InterruptedException ignored) {
					}
				}
			});
		} catch (NoSuchMethodException e) {
			log(e);
		}

		try {
            context.getHooker().hookMethod(Application.class.getMethod("onCreate"), new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					initializePlugins((Application) param.thisObject);
				}
			});
		} catch (NoSuchMethodException e) {
			log(e);
		}

		try {
			Method method = Application.class.getDeclaredMethod("attach", Context.class);
			context.getHooker().hookMethod(method, new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					onAttachApplication((Application) param.thisObject);
				}
			});
		} catch (NoSuchMethodException e) {
			log(e);
		}

		try {
			hook(Thread.class, "suspend");
		} catch (NoSuchMethodException e) {
			log(e);
		}

		fakeSSL();

		try {
			MethodHook logHookHandler = new MethodHookAdapter() {
				@Override
				public void afterHookedMethod(MethodHookParam param) throws Throwable {
					super.afterHookedMethod(param);

					final String logcatTag = AbstractInspector.this.logcatTag;
					if (StringUtils.isEmpty(logcatTag)) {
						return;
					}

					try {
						String name = param.method.getName();
						if ("v".equals(name)) {
							log(param.args, Log.VERBOSE, logcatTag);
						} else if ("d".equals(name)) {
							log(param.args, Log.DEBUG, logcatTag);
						} else if ("i".equals(name)) {
							log(param.args, Log.INFO, logcatTag);
						} else if ("w".equals(name)) {
							log(param.args, Log.WARN, logcatTag);
						} else if ("e".equals(name)) {
							log(param.args, Log.ERROR, logcatTag);
						}
					} catch (Exception e) {
						println(e);
					}
				}
				private void log(Object[] args, int priority, String logcatTag) {
					if (priority < logcatPriority) {
						return;
					}

					String tag = (String) args[0];
					Object msg = args[1];
					if (args.length > 2) {
						Throwable throwable = (Throwable) args[2];
						println_native(priority, tag, msg + "\n" + Log.getStackTraceString(throwable), logcatTag);
					} else {
						if (msg instanceof Throwable) {
							println_native(priority, tag, Log.getStackTraceString((Throwable) msg), logcatTag);
						} else {
							println_native(priority, tag, String.valueOf(msg), logcatTag);
						}
					}
				}
			};
			for (Method method : Log.class.getDeclaredMethods()) {
				String name = method.getName();
				if ("v".equals(name) ||
						"d".equals(name) ||
						"i".equals(name) ||
						"w".equals(name) ||
						"e".equals(name)) {
					context.getHooker().hookMethod(method, logHookHandler);
				}
			}
		} catch (Exception e) {
			log(e);
		}
	}

	private void println_native(int priority, String tag, String msg, String logcatTag) {
		if ("*".equals(logcatTag) || logcatTag.equals(tag)) {
			char level = 'U';
			switch (priority) {
				case Log.VERBOSE:
					level = 'V';
					break;
				case Log.DEBUG:
					level = 'D';
					break;
				case Log.INFO:
					level = 'I';
					break;
				case Log.WARN:
					level = 'W';
					break;
				case Log.ERROR:
					level = 'E';
					break;
				case Log.ASSERT:
					level = 'A';
					break;
			}
			String out = level + "/" + tag + ": " + msg;
			if (priority >= Log.WARN) {
				err_println(out);
			} else {
				println(out);
			}
		}
	}

	private void fakeSSL() {
		try {
			// hook(SecretKeySpec.class, null, byte[].class, String.class);
		} catch (Exception e) {
			log(e);
		}
	}

	void SecretKeySpec(Object thisObj, byte[] key, String algorithm) {
		inspect(key, algorithm + " key");
	}

	void suspend(Thread thread) {
		StringBuilder buffer = new StringBuilder();
		buffer.append('"').append(thread.getName()).append('"');
		if (thread.isDaemon()) {
			buffer.append(" daemon");
		}
		buffer.append(" prio=").append(thread.getPriority());
		buffer.append(" tid=").append(thread.getId());
		buffer.append(" SUSPENDED");
		println(buffer.toString());
	}

	/*private boolean hookMessageService(IBinder binder) throws NoSuchMethodException {
		if (binder == null) {
			return false;
		}
		ISms sms = ISms.Stub.asInterface(binder);
		context.getSdk().hook_sendTextMessage(context.getHooker(), this, sms.getClass());
		return true;
	}*/

	private Context appContext;

	protected Context getAppContext() {
		return appContext;
	}

	private synchronized void initializePlugins(Context appContext) {
		if (appContext == null || this.appContext != null) {
			return;
		}

		appContext.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (console == null) {
					return;
				}
				Bundle bundle = intent.getExtras();
				if (bundle == null) {
					return;
				}
				byte[] packet = bundle.getByteArray(KrakenCapture.PACKET_KEY);
				int datalink = bundle.getInt(KrakenCapture.DLT_KEY);
				onKrakenCapture(packet, datalink);
			}
		}, new IntentFilter(KrakenCapture.KRAKEN_CAPTURE_ON_PACKET));
		this.appContext = appContext;
		log("set context: " + appContext);

		for (Plugin plugin : this.context.getPlugins()) {
			plugin.initialize(appContext);
		}
	}

	private synchronized void onAttachApplication(Application application) {
		application.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (console == null) {
					return;
				}
				Bundle bundle = intent.getExtras();
				if (bundle == null) {
					return;
				}
				byte[] packet = bundle.getByteArray(KrakenCapture.PACKET_KEY);
				int datalink = bundle.getInt(KrakenCapture.DLT_KEY);
				onKrakenCapture(packet, datalink);
			}
		}, new IntentFilter(KrakenCapture.KRAKEN_CAPTURE_ON_PACKET));
		this.appContext = application;
		log("set context: " + application);

		for (Plugin plugin : this.context.getPlugins()) {
			plugin.onAttachApplication(application);
		}
	}

	@SuppressWarnings("unused")
	public void listAllDevs() {
		try {
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
				throw new UnsupportedOperationException();
			}

			Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
			List<NetworkInterface> list = new ArrayList<NetworkInterface>();
			while (enumeration.hasMoreElements()) {
				NetworkInterface interface1 = enumeration.nextElement();
				if (!interface1.isUp() ||
						interface1.isVirtual() ||
						interface1.getInterfaceAddresses().isEmpty()) {
					continue;
				}
				list.add(interface1);
			}
			if (list.isEmpty()) {
				println("listAllDevs devs is empty.");
				return;
			}

			for (NetworkInterface networkInterface : list) {
				println(networkInterface.getName() + ": " + networkInterface.getInterfaceAddresses());
			}
		} catch (Throwable t) {
			log(t);
		}
	}

	String ngetInfoA(Object thisObj, String publicKey, String cId, String ret) {
		println("ngetInfoA publicKey=" + publicKey +
				", cId=" + cId +
				", ret=" + ret);
		return ret;
	}

	byte[] ngetInfoB(Object thisObj, byte[] ret) {
		inspect(ret, "ngetInfoB");
		return ret;
	}

	String ngetNormalBillingRequest(Object thisObj, String appId, String content, String type, String ret) {
		println("ngetNormalBillingRequest appId=" + appId +
				", content=" + content +
				", type=" + type +
				", ret=" + ret);
		return ret;
	}

	String ngetSecureBillingRequest(Object thisObj, String appId, String content, String type, String ret) {
		println("ngetSecureBillingRequest appId=" + appId +
				", content=" + content +
				", type=" + type +
				", ret=" + ret);
		return ret;
	}

	String ngetSecureSessionRequest(Object thisObj, String appId, String content, String string, String type, String ret) {
		println("ngetSecureSessionRequest appId=" + appId +
				", content=" + content +
				", string=" + string +
				", type=" + type +
				", ret=" + ret);
		return ret;
	}

	/**
	 * void sendTextMessage(String destinationAddress, String scAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent);
	 */
	/*void sendTextMessage(Object thisObj, String destinationAddress, String scAddress, String text,
						 PendingIntent sentIntent, PendingIntent deliveryIntent) {
		sendTextInternal(destinationAddress, scAddress, text, sentIntent, deliveryIntent, "sendTextMessage");
	}

	void sendText(Object thisObj, String destinationAddress, String scAddress, String text,
				  PendingIntent sentIntent, PendingIntent deliveryIntent) {
		sendTextInternal(destinationAddress, scAddress, text, sentIntent, deliveryIntent, "sendText");
	}

	void sendText(Object thisObj, String callingPkg, String destinationAddress, String scAddress, String text,
				  PendingIntent sentIntent, PendingIntent deliveryIntent) {
		sendTextInternal(destinationAddress, scAddress, text, sentIntent, deliveryIntent, "sendText");
	}

	void sendTextForSubscriber(Object thisObj, int subId, String callingPkg, String destinationAddress, String scAddress, String text,
							   PendingIntent sentIntent, PendingIntent deliveryIntent, boolean persistMessageForNonDefaultSmsApp) {
		sendTextInternal(destinationAddress, scAddress, text, sentIntent, deliveryIntent, "sendText");
	}

	private void sendTextInternal(String destinationAddress,
								  String scAddress, String text, PendingIntent sentIntent,
								  PendingIntent deliveryIntent, String label) {
		for (Plugin plugin : context.getPlugins()) {
			plugin.notifySendTextMessage(destinationAddress, scAddress, text, sentIntent, deliveryIntent, label);
		}
	}*/

	private static final int WPE = 16;

	/**
	 * 侦察发送的数据
	 */
	public void inspect(byte[] data, String label) {
		inspect(label, data == null ? null : ByteBuffer.wrap(data), WPE);
	}

	@Override
	public void collectDexFile(byte[] dex, String name) {
		dexFileManager.addAnonymousDex(dex, name);
	}

	@Override
	public void inspect(ByteBuffer data, String label) {
		inspect(label, data, WPE);
	}

	/**
	 * 侦察发送的数据
	 */
	public void inspect(byte[] data, boolean send) {
		inspect(send ? "发送数据" : "接收数据", data == null ? null : ByteBuffer.wrap(data), WPE);
	}

	/**
	 * 侦察发送的数据
	 */
	public void inspect(int type, byte[] data, boolean send) {
		String ts = Integer.toHexString(type).toUpperCase();
		inspect(send ? "发送数据：0x" + ts : "接收数据：0x" + ts, data == null ? null : ByteBuffer.wrap(data), WPE);
	}

	private void inspect(String label, ByteBuffer data, int mode) {
		inspect(null, label, data, mode);
	}

	private void inspect(Date date, String label, ByteBuffer buffer, int mode) {
		writeToConsole(new ByteBufferInspectCache(date, label, buffer, mode));
	}

	public void inspect(short[] data, String label) {
		writeToConsole(new ShortBufferInspectCache(null, label, data == null ? null : ShortBuffer.wrap(data), 16));
	}

	private boolean canStop;

	void stop() {
		canStop = true;
	}

	private final byte[] buffer = new byte[256];

	private final static int UDP_PORT = 20000;

	private long lastSendBroadcast;

	private Console console;

	private CharSequence label;

	protected boolean enableBroadcast;

	private synchronized void sendBroadcast() {
		Context ctx = getAppContext();
		if (label == null && ctx != null) {
			PackageManager pm = ctx.getPackageManager();
			label = pm.getApplicationLabel(ctx.getApplicationInfo());
			try {
				initializeLocalServerSocketDiscover();
			} catch (IOException e) {
				super.log(e);
			}
		}

		long current = System.currentTimeMillis();
		if (console != null &&
				current - lastSendBroadcast < 60000) {//如果已经有客户端了，则等60秒发一次广播
			return;
		}

		DatagramSocket datagramSocket = null;
		ByteArrayOutputStream baos = null;
		DataOutputStream dos = null;
		try {
			if (!enableBroadcast) {
				return;
			}

			baos = new ByteArrayOutputStream();
			dos = new DataOutputStream(baos);
			datagramSocket = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			dos.writeShort(serverSocketPort);
			if (label != null) {
				dos.writeUTF(Build.MANUFACTURER + '/' + Build.MODEL + " (" + label + ')');
			} else {
				dos.writeUTF(Build.MANUFACTURER + '/' + Build.MODEL);
			}
			dos.writeByte(0);
			dos.writeInt(Process.myPid());
			dos.writeUTF(context.getProcessName());

			packet.setData(baos.toByteArray());
			packet.setLength(baos.size());
			packet.setPort(UDP_PORT);

			InetAddress broadcastAddr = InetAddress.getByName("255.255.255.255");
			packet.setAddress(broadcastAddr);
			datagramSocket.send(packet);
			lastSendBroadcast = current;
		} catch (SocketException e) {
			// ignore
		} catch (Exception e) {
			super.log(e);
		} finally {
			IOUtils.closeQuietly(dos);
			IOUtils.closeQuietly(baos);
			if (datagramSocket != null) {
				datagramSocket.close();
			}
		}
	}

	private ServerSocket serverSocket;
	private LocalServerSocket localServerSocket;
	private int serverSocketPort;

	private void initializeLogServer() {
		try {
			serverSocket = new ServerSocket();
			serverSocket.setSoTimeout(5000);
			serverSocket.setReuseAddress(true);
			Random random = new Random();
			int times = 0;
			while (times++ < 10) {
				try {
					serverSocketPort = 20000 + random.nextInt(5000);
					serverSocket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), serverSocketPort));

					/*try {
						File tmp = new File(Environment.getExternalStorageDirectory(), "inspector/" + Process.myPid());
						FileUtils.deleteQuietly(tmp);
						FileUtils.writeStringToFile(new File(tmp, String.valueOf(serverSocketPort)), context.getProcessName());
					} catch (Exception e) {
						super.log("initializeLogServer failed: " + context.getProcessName() + ", pid=" + Process.myPid());
						XposedBridge.log(e);
					}*/

					try {
						initializeLocalServerSocketDiscover();
					} catch (Exception e) {
						log("initializeLocalServer failed: " + context.getProcessName() + ", pid=" + Process.myPid());
						log(e);
					}
					break;
				} catch (BindException e) {
					Thread.sleep(1000);
				}
			}

			// println("Begin accept on port " + serverSocketPort);
		} catch (Exception e) {
			println("initializeLogServer failed.");
			log(e);
		}
	}

	private void initializeLocalServerSocketDiscover() throws IOException {
		if (localServerSocket != null) {
			closeQuietly(localServerSocket);
			localServerSocket = null;
		}
		String label = String.valueOf(this.label);
		localServerSocket = new LocalServerSocket(PREFIX + serverSocketPort + '_' + context.getProcessName() + '_' + Hex.encodeHexString(label.getBytes("UTF-8")) + "_discover");
	}

	private static final String PREFIX = "inspector_";

	@Override
	public void log(Object msg) {
		// super.log(msg);

		println(msg);
	}

	@SuppressWarnings("unused")
	public void testAntiHook(int r0, int r1, int r2, int r3) {
		TraceAnti.getInstance().testAntiHook(r0, r1, r2, r3);
		println("mock testAntiHook successfully!");
	}

	private boolean debug;

	@Override
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	@Override
	public boolean isDebug() {
		return debug || MyModuleContext.isDebug();
	}

	@Override
	public File[] getAppLibDir() {
		LibraryAbi[] abis = context.getAbiDirectory();
		File[] files = new File[abis.length];
		for (int i = 0; i < files.length; i++) {
			files[i] = abis[i].getAppLibDir();
		}
		return files;
	}

	@Override
	public ServerCommandCompleter createCommandCompleter(String prefix) {
		return new DefaultServerCommandCompleter(this, prefix);
	}

	private void print_ins_detail(Capstone.CsInsn ins) {
		err_println(String.format("0x%x: %s %s", ins.address, ins.mnemonic, ins.opStr));

		StringBuilder sb = new StringBuilder();
		Arm.OpInfo operands = (Arm.OpInfo) ins.operands;
		boolean opt = false;
		if (operands.op.length != 0) {
			sb.append(String.format("// op_count: %d\n", operands.op.length));
			opt = true;
			for (int c=0; c<operands.op.length; c++) {
				Arm.Operand i = operands.op[c];
				if (i.type == Arm_const.ARM_OP_SYSREG)
					sb.append(String.format("// \toperands[%d].type: SYSREG = %d\n", c, i.value.reg));
				if (i.type == Arm_const.ARM_OP_REG)
					sb.append(String.format("// \toperands[%d].type: REG = %s\n", c, ins.regName(i.value.reg)));
				if (i.type == Arm_const.ARM_OP_IMM)
					sb.append(String.format("// \toperands[%d].type: IMM = 0x%x\n", c, i.value.imm));
				if (i.type == Arm_const.ARM_OP_PIMM)
					sb.append(String.format("// \toperands[%d].type: P-IMM = %d\n", c, i.value.imm));
				if (i.type == Arm_const.ARM_OP_CIMM)
					sb.append(String.format("// \toperands[%d].type: C-IMM = %d\n", c, i.value.imm));
				if (i.type == Arm_const.ARM_OP_SETEND)
					sb.append(String.format("// \toperands[%d].type: SETEND = %s\n", c, i.value.setend == Arm_const.ARM_SETEND_BE? "be" : "le"));
				if (i.type == Arm_const.ARM_OP_FP)
					sb.append(String.format("// \toperands[%d].type: FP = %f\n", c, i.value.fp));
				if (i.type == Arm_const.ARM_OP_MEM) {
					sb.append(String.format("// \toperands[%d].type: MEM\n",c));
					String base = ins.regName(i.value.mem.base);
					String index = ins.regName(i.value.mem.index);
					if (base != null)
						sb.append(String.format("// \t\toperands[%d].mem.base: REG = %s\n", c, base));
					if (index != null)
						sb.append(String.format("// \t\toperands[%d].mem.index: REG = %s\n", c, index));
					if (i.value.mem.scale != 1)
						sb.append(String.format("// \t\toperands[%d].mem.scale: %d\n", c, (i.value.mem.scale)));
					if (i.value.mem.disp != 0)
						sb.append(String.format("// \t\toperands[%d].mem.disp: 0x%x\n", c, (i.value.mem.disp)));
				}
				if (i.vector_index > 0)
					sb.append(String.format("// \t\toperands[%d].vector_index = %d\n", c, (i.vector_index)));
				if (i.shift.type != Arm_const.ARM_SFT_INVALID && i.shift.value > 0)
					sb.append(String.format("// \t\tShift: %d = %d\n", i.shift.type, i.shift.value));
				if (i.subtracted)
					sb.append(String.format("// \t\toperands[%d].subtracted = True\n", c));
			}
		}
		if (operands.writeback) {
			sb.append("// Write-back: True\n");
			opt = true;
		}

		if (operands.updateFlags) {
			sb.append("// Update-flags: True\n");
			opt = true;
		}

		if (operands.cc != Arm_const.ARM_CC_AL && operands.cc != Arm_const.ARM_CC_INVALID) {
			sb.append(String.format("// Code condition: %d\n", operands.cc));
			opt = true;
		}

		if (operands.cpsMode > 0) {
			sb.append(String.format("// CPSI-mode: %d\n", operands.cpsMode));
			opt = true;
		}

		if (operands.cpsFlag > 0) {
			sb.append(String.format("// CPSI-flag: %d\n", operands.cpsFlag));
			opt = true;
		}

		if (operands.vectorData > 0) {
			sb.append(String.format("// Vector-data: %d\n", operands.vectorData));
			opt = true;
		}

		if (operands.vectorSize > 0) {
			sb.append(String.format("// Vector-size: %d\n", operands.vectorSize));
			opt = true;
		}

		if (operands.usermode) {
			sb.append("// User-mode: True\n");
			opt = true;
		}

		if (opt) {
			out_println(sb);
		} else {
			out_print(sb);
		}
	}

	public void disasm(int startAddr, int lengthOrEndAddr) {
		int length = lengthOrEndAddr;
		if (lengthOrEndAddr >= startAddr) {
			length = lengthOrEndAddr - startAddr;
		}

		if (length < 1) {
			println("length must big than zero");
			return;
		}

		boolean thumb = (startAddr & 1) == 1;
		if(thumb) {
			startAddr--;
		}
		ByteBuffer memory = this.dexFileManager.dumpMemory(startAddr, length);
		byte[] code = new byte[memory.remaining()];
		memory.get(code);

		Capstone cs = new Capstone(Capstone.CS_ARCH_ARM, thumb ? Capstone.CS_MODE_THUMB : Capstone.CS_MODE_ARM);
		try {
			cs.setDetail(Capstone.CS_OPT_ON);
			Capstone.CsInsn[] insns = cs.disasm(code, startAddr);
			for (Capstone.CsInsn insn : insns) {
				print_ins_detail(insn);
			}
		} finally {
			cs.close();
		}
	}

	// code to be emulated
	private static final byte[] ARM_CODE = {55,0,(byte)0xa0,(byte)0xe3,3,16,66,(byte)0xe0}; // mov r0, #0x37; sub r1, r2, r3
	private static final byte[] THUMB_CODE = {(byte)0x83, (byte)0xb0}; // sub    sp, #0xc

	// memory address where emulation starts
	private static final int ADDRESS = 0x10000;

	private class MyBlockHook implements BlockHook {
		public void hook(Unicorn u, long address, int size, Object user_data) {
			out_print(String.format(">>> Tracing basic block at 0x%x, block size = 0x%x\n", address, size));
		}
	}

	// callback for tracing instruction
	private class MyCodeHook implements CodeHook {
		public void hook(Unicorn u, long address, int size, Object user_data) {
			out_print(String.format(">>> Tracing instruction at 0x%x, instruction size = 0x%x\n", address, size));
		}
	}

	private void test_arm() {
		Long r0 = 0x1234L; // R0 register
		Long r2 = 0x6789L; // R1 register
		Long r3 = 0x3333L; // R2 register
		Long r1;     // R1 register

		out_print("Emulate ARM code\n");

		// Initialize emulator in ARM mode
		Unicorn u = new Unicorn(Unicorn.UC_ARCH_ARM, Unicorn.UC_MODE_ARM);

		out_print("Unicorn created\n");

		// map 2MB memory for this emulation
		u.mem_map(ADDRESS, 2 * 1024 * 1024, Unicorn.UC_PROT_ALL);

		// write machine code to be emulated to memory
		u.mem_write(ADDRESS, ARM_CODE);

		// initialize machine registers
		u.reg_write(Unicorn.UC_ARM_REG_R0, r0);
		u.reg_write(Unicorn.UC_ARM_REG_R2, r2);
		u.reg_write(Unicorn.UC_ARM_REG_R3, r3);

		// tracing all basic blocks with customized callback
		u.hook_add(new MyBlockHook(), 1, 0, null);

		// tracing one instruction at ADDRESS with customized callback
		u.hook_add(new MyCodeHook(), ADDRESS, ADDRESS, null);

		out_print("Unicorn before start\n");

		// emulate machine code in infinite time (last param = 0), or when
		// finishing all the code.
		u.emu_start(ADDRESS, ADDRESS + ARM_CODE.length, 0, 0);

		// now print out some registers
		out_print(">>> Emulation done. Below is the CPU context\n");

		r0 = (Long)u.reg_read(Unicorn.UC_ARM_REG_R0);
		r1 = (Long)u.reg_read(Unicorn.UC_ARM_REG_R1);
		out_print(String.format(">>> R0 = 0x%x\n", r0.intValue()));
		out_print(String.format(">>> R1 = 0x%x\n", r1.intValue()));

		u.closeAll();
	}

	private void test_thumb() {
		Long sp = 0x1234L; // R0 register

		out_print("Emulate THUMB code\n");

		// Initialize emulator in ARM mode
		Unicorn u = new Unicorn(Unicorn.UC_ARCH_ARM, Unicorn.UC_MODE_THUMB);

		out_print("Unicorn created\n");

		// map 2MB memory for this emulation
		u.mem_map(ADDRESS, 2 * 1024 * 1024, Unicorn.UC_PROT_ALL);

		// write machine code to be emulated to memory
		u.mem_write(ADDRESS, THUMB_CODE);

		// initialize machine registers
		u.reg_write(Unicorn.UC_ARM_REG_SP, sp);

		// tracing all basic blocks with customized callback
		u.hook_add(new MyBlockHook(), 1, 0, null);

		// tracing one instruction at ADDRESS with customized callback
		u.hook_add(new MyCodeHook(), ADDRESS, ADDRESS, null);

		out_print("Unicorn before start\n");

		// emulate machine code in infinite time (last param = 0), or when
		// finishing all the code.
		u.emu_start(ADDRESS | 1, ADDRESS + THUMB_CODE.length, 0, 0);

		// now print out some registers
		out_print(">>> Emulation done. Below is the CPU context\n");

		sp = (Long)u.reg_read(Unicorn.UC_ARM_REG_SP);
		out_print(String.format(">>> SP = 0x%x\n", sp.intValue()));

		u.closeAll();
	}

	public void test() {
		for(int i = 0; i < 50; i++) {
			test_arm();
			out_print(i + "==========================\n");
			test_thumb();
		}
	}

	@Override
	public Emulator emuArm() {
		return EmulatorFactory.createARMEmulator(this);
	}

	@Override
	public Emulator emuThumb() {
		return EmulatorFactory.createThumbEmulator(this);
	}

	private static String getProcessNameByPid(Context context, int pid) {
		for (ActivityManager.RunningAppProcessInfo info : AndroidProcesses.getRunningAppProcessInfo(context)) {
			if (info.pid == pid) {
				return info.processName;
			}
		}
		return null;
	}

	@SuppressWarnings("unused")
	private void startStetho() {
		startStetho(null);
	}

	@Override
	public boolean startStetho(Stetho.Initializer initializer) {
		try {
			if (appContext != null) {
				if (initializer == null) {
					initializer = new Stetho.Initializer(appContext) {
						@Override
						protected Iterable<DumperPlugin> getDumperPlugins() {
							return new Stetho.DefaultDumperPluginsBuilder(appContext).finish();
						}
						@Override
						protected Iterable<ChromeDevtoolsDomain> getInspectorModules() {
							return new Stetho.DefaultInspectorModulesBuilder(appContext).finish();
						}
					};
				}
				new StethoInitializer().initialize(initializer, context);
				println("start stetho successfully.");
				return true;
			} else {
				err_println("start stetho failed as app context is null.");
			}
		} catch (Throwable e) {
			println(e);
		}
		return false;
	}
}
