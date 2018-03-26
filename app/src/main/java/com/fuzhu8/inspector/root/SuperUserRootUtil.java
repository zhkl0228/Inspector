package com.fuzhu8.inspector.root;

import java.util.List;

import android.os.Handler;
import android.os.HandlerThread;
import eu.chainfire.libsuperuser.Shell;
import eu.chainfire.libsuperuser.Shell.Builder;
import eu.chainfire.libsuperuser.Shell.OnCommandResultListener;
import eu.chainfire.libsuperuser.StreamGobbler.OnLineListener;

public class SuperUserRootUtil implements RootUtil {
	private Shell.Interactive mShell = null;
	private HandlerThread mCallbackThread = null;

	private boolean mCommandRunning = false;
	private int mLastExitCode = -1;
	private List<String> mLastOutput = null;
	
	private final boolean useSU;

	public SuperUserRootUtil() {
		this(true);
	}

	public SuperUserRootUtil(boolean useSU) {
		super();
		
		this.useSU = useSU;
	}

	private OnCommandResultListener commandResultListener = new OnCommandResultListener() {
		@Override
		public void onCommandResult(int commandCode, int exitCode, List<String> output) {
			mLastExitCode = exitCode;
			mLastOutput = output;
			synchronized (mCallbackThread) {
				mCommandRunning = false;
				mCallbackThread.notifyAll();
			}
		}
	};

	private void waitForCommandFinished() {
		synchronized (mCallbackThread) {
			while (mCommandRunning) {
				try {
					mCallbackThread.wait();
				} catch (InterruptedException ignored) {}
			}
		}

		if (mLastExitCode == OnCommandResultListener.WATCHDOG_EXIT
		   || mLastExitCode == OnCommandResultListener.SHELL_DIED)
			dispose();
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.root.Root#startShell()
	 */
	@Override
	public boolean startShell() {
		return startShell(10, null);
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.root.Root#startShell(int, eu.chainfire.libsuperuser.StreamGobbler.OnLineListener)
	 */
	@Override
	public synchronized boolean startShell(int watchdogTimeout, final LineListener onLineListener) {
		if (mShell != null) {
			if (mShell.isRunning())
				return true;
			else
				dispose();
		}

		mCallbackThread = new HandlerThread("su callback listener");
		mCallbackThread.start();

		mCommandRunning = true;
		Builder builder = new Builder();
		if(useSU) {
			builder.useSU();
		} else {
			builder.useSH();
		}
		final OnLineListener listener = new OnLineListener() {
			@Override
			public void onLine(String line) {
				if(onLineListener != null) {
					onLineListener.notifyLine(line);
				}
			}
		};
		mShell = builder.setHandler(new Handler(mCallbackThread.getLooper()))
			.setWantSTDERR(true)
			.setWatchdogTimeout(watchdogTimeout)
			.setOnSTDOUTLineListener(listener)
			.setOnSTDERRLineListener(listener)
			.open(commandResultListener);

		waitForCommandFinished();

		if (mLastExitCode != OnCommandResultListener.SHELL_RUNNING) {
			dispose();
			return false;
		}

		return true;
	}

	/**
	 * Closes all resources related to the shell.
	 */
	private synchronized void dispose() {
		if (mShell == null)
			return;

		try {
			mShell.close();
		} catch (Exception ignored) {}
		mShell = null;

		mCallbackThread.quit();
		mCallbackThread = null;
	}
	
	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.root.Root#killAll()
	 */
	@Override
	public void killAll() {
		if(mShell == null) {
			return;
		}
		
		mShell.kill();
		mShell = null;
		
		synchronized (mCallbackThread) {
			mCommandRunning = false;
			mCallbackThread.notifyAll();
		}
		mCallbackThread.quit();
		mCallbackThread = null;
	}

	/* (non-Javadoc)
	 * @see com.fuzhu8.inspector.root.Root#execute(java.lang.String, java.util.List)
	 */
	@Override
	public synchronized int execute(String command, List<String> output) {
		if (mShell == null)
			throw new IllegalStateException("shell is not running");

		mCommandRunning = true;
		mShell.addCommand(command, 0, commandResultListener);
		waitForCommandFinished();

		if (output != null) {
			output.addAll(mLastOutput);
		}

		return mLastExitCode;
	}

	@Override
	public void executeAsync(String command) {
		if (mShell == null) {
			throw new IllegalStateException("shell is not running");
		}

		mCommandRunning = true;
		mShell.addCommand(command, 0, commandResultListener);
	}

	@Override
	protected void finalize() throws Throwable {
		dispose();
	}

	@Override
	public boolean isLocalRoot() {
		return false;
	}
}
