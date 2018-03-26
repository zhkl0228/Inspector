package com.fuzhu8.inspector.xposed;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import org.apache.commons.io.IOUtils;

import com.fuzhu8.inspector.root.RootUtilServer;

import android.os.Process;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author zhkl0228
 *
 */
public class XposedRootUtilServer implements RootUtilServer {
	
	private final int port;
	
	private XposedRootUtilServer(int port) {
		super();

		this.port = port;
	}

	private static final int PORT_MIN = 10000;
	
	public static XposedRootUtilServer startRootUtilServer() {
		if(Process.myUid() != 0) {
			XposedBridge.log("startRootUtilServer uid=" + Process.myUid());
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
			XposedRootUtilServer rootUtilServer = new XposedRootUtilServer(port);
			Integer pid = (Integer) XposedHelpers.callStaticMethod(XposedRootUtilServer.class.getClassLoader().loadClass("dalvik.system.Zygote"), "fork");
			if(pid == 0) {
				rootUtilServer.startServer();
			}
			return rootUtilServer;
		} catch(Throwable t) {
			XposedBridge.log(t);
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
			XposedBridge.log("start XposedRootUtilServer on port " + port + " with pid " + Process.myPid() + " successfully! ");
		} catch(Throwable e) {
			XposedBridge.log(e);
			
			IOUtils.closeQuietly(server);
			this.server = null;
		}
		
		while(server != null) {
			try {
				acceptSocket(server);
			} catch(Throwable t) {
				XposedBridge.log(t);
			}
		}
		
		IOUtils.closeQuietly(server);
		this.server = null;
		XposedBridge.log("stop XposedRootUtilServer successfully! ");
		Process.killProcess(Process.myPid());
	}

	private void acceptSocket(ServerSocket serverSocket) throws IOException {
		Socket socket = serverSocket.accept();
		new RootShellHandler(socket, socket.getInputStream(), socket.getOutputStream()).startHandle();
	}

}
