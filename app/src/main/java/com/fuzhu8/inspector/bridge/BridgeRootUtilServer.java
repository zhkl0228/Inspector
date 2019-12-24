package com.fuzhu8.inspector.bridge;

import android.os.Process;

import com.fuzhu8.inspector.root.RootUtilServer;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import cn.android.bridge.AndroidBridge;
import cn.android.bridge.XposedHelpers;

/**
 * @author zhkl0228
 *
 */
public class BridgeRootUtilServer implements RootUtilServer {
	
	private final int port;
	
	private BridgeRootUtilServer(int port) {
		super();

		this.port = port;
	}

	private static final int PORT_MIN = 10000;
	
	public static BridgeRootUtilServer startRootUtilServer() {
		if(Process.myUid() != 0) {
			AndroidBridge.log("startRootUtilServer uid=" + Process.myUid());
			return null;
		}
		
		try {
			ServerSocket server = new ServerSocket();
			Random random = new Random();
			int port = 0;
			byte tryCount = 0;
			while(tryCount++ < 100) {
				port = random.nextInt(30000) + PORT_MIN;
				try {
					server.bind(new InetSocketAddress("localhost", port));
					break;
				} catch(IOException ignored) {}
			}
			IOUtils.closeQuietly(server);
			BridgeRootUtilServer rootUtilServer = new BridgeRootUtilServer(port);
			Integer pid = (Integer) XposedHelpers.callStaticMethod(BridgeRootUtilServer.class.getClassLoader().loadClass("dalvik.system.Zygote"), "fork");
			if(pid == 0) {
				rootUtilServer.startServer();
			}
			return rootUtilServer;
		} catch(Throwable t) {
			AndroidBridge.log(t);
		}
		return null;
	}

	@Override
	public int getPort() {
		return port;
	}
	
	private ServerSocket server;

	private void startServer() {
		try {
			server = new ServerSocket();
			server.bind(new InetSocketAddress("localhost", port));
			server.setSoTimeout(0);
			AndroidBridge.log("start BridgeRootUtilServer on port " + port + " with pid " + Process.myPid() + " successfully! ");
		} catch(Throwable e) {
			AndroidBridge.log(e);
			
			IOUtils.closeQuietly(server);
			this.server = null;
		}
		
		while(server != null) {
			try {
				acceptSocket(server);
			} catch(Throwable t) {
				AndroidBridge.log(t);
			}
		}
		
		IOUtils.closeQuietly(server);
		this.server = null;
		AndroidBridge.log("stop BridgeRootUtilServer successfully! ");
		Process.killProcess(Process.myPid());
	}

	private void acceptSocket(ServerSocket serverSocket) throws IOException {
		Socket socket = serverSocket.accept();
		new RootShellHandler(socket, socket.getInputStream(), socket.getOutputStream()).startHandle();
	}

}
