/**
 * 
 */
package com.fuzhu8.inspector;

import com.fuzhu8.inspector.io.Console;

/**
 * @author zhkl0228
 *
 */
public interface ClientConnectListener {
	
	void onConnected(Console console);
	
	void onClosed(Console console);

}
