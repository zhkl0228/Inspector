/**
 * 
 */
package com.fuzhu8.inspector.advisor;

import java.lang.reflect.Member;
import java.util.List;
import java.util.Set;

/**
 * @author zhkl0228
 *
 */
public abstract class AbstractUnhook implements com.fuzhu8.inspector.advisor.Unhook {
	
	private final boolean userHook;
	private final List<com.fuzhu8.inspector.advisor.Unhook> hookList;
	private final Set<Member> hookedSet;

	public AbstractUnhook(boolean userHook, List<com.fuzhu8.inspector.advisor.Unhook> hookList, Set<Member> hookedSet) {
		super();
		this.userHook = userHook;
		this.hookList = hookList;
		this.hookedSet = hookedSet;
	}

	@Override
	public final void unhook() {
		unhookInternal();
		hookList.remove(this);
		if(hookedSet != null) {
			hookedSet.remove(getHookedMethod());
		}
	}

	protected abstract void unhookInternal();

	@Override
	public boolean isUserHook() {
		return userHook;
	}
	
	protected abstract Member getHookedMethod();

}
