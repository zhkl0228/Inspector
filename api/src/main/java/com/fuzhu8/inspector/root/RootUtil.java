package com.fuzhu8.inspector.root;

import java.util.List;

public interface RootUtil {

	boolean startShell();

	/**
	 * Starts an interactive shell with root permissions.
	 * Does nothing if already running.
	 *
	 * @return true if root access is available, false otherwise
	 */
	boolean startShell(int watchdogTimeout, LineListener lineListener);

	void killAll();

	/**
	 * Executes a single command, waits for its termination and returns the result
	 */
	int execute(String command, List<String> output);

	void executeAsync(String command);
	
	boolean isLocalRoot();

}