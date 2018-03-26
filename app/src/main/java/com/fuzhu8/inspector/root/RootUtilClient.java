package com.fuzhu8.inspector.root;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import org.apache.commons.io.IOUtils;

import android.util.Log;

/**
 * @author zhkl0228
 *
 */
public class RootUtilClient implements RootUtil {
	
	private final int port;

	public RootUtilClient(int port) {
		super();
		this.port = port;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.root.RootUtil#startShell()
	 */
	@Override
	public boolean startShell() {
		return startShell(10, null);
	}
	
	private Socket socket;
	private PrintWriter writer;
	
	private class InputHandler implements Runnable {
		final BufferedReader reader;
		final LineListener onLineListener;
		InputHandler(BufferedReader reader, LineListener lineListener) {
			super();
			this.reader = reader;
			this.onLineListener = lineListener;
			
			Thread thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		}
		@Override
		public void run() {
			try {
				String line;
				while((line = reader.readLine()) != null) {
					if(onLineListener != null) {
						onLineListener.notifyLine(line);
					}
				}
			} catch(IOException e) {
				killAll();
			}
		}
		void close() {
			IOUtils.closeQuietly(reader);
		}
	}
	
	private InputHandler handler;

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.root.RootUtil#startShell(int, eu.chainfire.libsuperuser.StreamGobbler.OnLineListener, eu.chainfire.libsuperuser.StreamGobbler.OnLineListener)
	 */
	@Override
	public synchronized boolean startShell(int watchdogTimeout, LineListener lineListener) {
		if(socket != null) {
			return true;
		}
		
		socket = new Socket();
		try {
			socket.connect(new InetSocketAddress("localhost", port), 1000);
			writer = new PrintWriter(socket.getOutputStream(), true);
			handler = new InputHandler(new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8")), lineListener);
			return true;
		} catch (IOException e) {
			Log.d("Inspector", e.getMessage(), e);
			IOUtils.closeQuietly(socket);
			socket = null;
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.root.RootUtil#killAll()
	 */
	@Override
	public void killAll() {
		if(handler != null) {
			handler.close();
		}
		IOUtils.closeQuietly(writer);
		IOUtils.closeQuietly(socket);
		socket = null;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.root.RootUtil#execute(java.lang.String, java.util.List)
	 */
	@Override
	public int execute(String command, List<String> output) {
		if (writer == null)
			throw new IllegalStateException("shell is not running");
		writer.println(command);
		return 0;
	}

	@Override
	public boolean isLocalRoot() {
		return true;
	}

	@Override
	public void executeAsync(String command) {
		throw new UnsupportedOperationException();
	}
}
