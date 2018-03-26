package com.fuzhu8.inspector.xposed;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.commons.io.IOUtils;

import com.fuzhu8.inspector.root.LineListener;
import com.fuzhu8.inspector.root.RootUtil;
import com.fuzhu8.inspector.root.SuperUserRootUtil;

/**
 * @author zhkl0228
 *
 */
public class RootShellHandler implements Runnable, LineListener {
	
	private final RootUtil rootUtil = new SuperUserRootUtil(false);
	private final Socket socket;
	
	private final BufferedReader reader;
	private final PrintWriter writer;

	RootShellHandler(Socket socket, InputStream inputStream, OutputStream outputStream) {
		super();
		this.socket = socket;
		reader = new BufferedReader(new InputStreamReader(inputStream));
		writer = new PrintWriter(outputStream, true);
	}
	
	void startHandle() {
		if(!rootUtil.startShell(0, this)) {
			throw new UnsupportedOperationException("startShell");
		}
		
		Thread thread = new Thread(this, "RootShellHandler#" + socket.getLocalPort());
		thread.setDaemon(true);
		thread.start();
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			String line;
			while((line = reader.readLine()) != null) {
				rootUtil.execute(line, null);
			}
		} catch(IOException ignored) {}
		
		close();
	}

	private void close() {
		IOUtils.closeQuietly(reader);
		IOUtils.closeQuietly(writer);
		IOUtils.closeQuietly(socket);
	}

	@Override
	public void notifyLine(String line) {
		writer.println(line);
	}

}
