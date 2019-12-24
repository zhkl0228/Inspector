/**
 * 
 */
package com.fuzhu8.inspector.bridge;

import com.fuzhu8.inspector.advisor.AbstractUnhook;

import java.lang.reflect.Member;
import java.util.List;
import java.util.Set;

import cn.android.bridge.XC_MethodHook;

/**
 * @author zhkl0228
 *
 */
public class BridgeUnhook extends AbstractUnhook implements com.fuzhu8.inspector.advisor.Unhook {
	
	private final XC_MethodHook.Unhook unhook;

	public BridgeUnhook(boolean userHook, XC_MethodHook.Unhook unhook, List<com.fuzhu8.inspector.advisor.Unhook> hookList, Set<Member> hookedSet) {
		super(userHook, hookList, hookedSet);
		this.unhook = unhook;
	}

	@Override
	public String toString() {
		return unhook.getHookedMethod().toString();
	}

	@Override
	protected void unhookInternal() {
		unhook.unhook();
	}

	@Override
	protected Member getHookedMethod() {
		return unhook.getHookedMethod();
	}

}
