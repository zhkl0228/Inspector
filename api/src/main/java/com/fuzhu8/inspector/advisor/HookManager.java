/**
 * 
 */
package com.fuzhu8.inspector.advisor;

import java.util.List;

/**
 * @author zhkl0228
 *
 */
public interface HookManager {
	
	List<Unhook> getHookList();
	
	Unhook getHook(int index);

}
