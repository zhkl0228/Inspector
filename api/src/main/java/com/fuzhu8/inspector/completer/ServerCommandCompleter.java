package com.fuzhu8.inspector.completer;

/**
 * @author zhkl0228
 *
 */
public interface ServerCommandCompleter {
	
	void addCommandHelp(String command, String...help);
	
	void commit();
	
	String describeHelp();

}
