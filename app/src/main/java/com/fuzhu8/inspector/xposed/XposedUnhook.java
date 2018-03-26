/**
 * 
 */
package com.fuzhu8.inspector.xposed;

import java.lang.reflect.Member;
import java.util.List;
import java.util.Set;

import com.fuzhu8.inspector.advisor.AbstractUnhook;

import de.robv.android.xposed.XC_MethodHook.Unhook;

/**
 * @author zhkl0228
 *
 */
public class XposedUnhook extends AbstractUnhook implements com.fuzhu8.inspector.advisor.Unhook {
	
	private final Unhook unhook;

	public XposedUnhook(boolean userHook, Unhook unhook, List<com.fuzhu8.inspector.advisor.Unhook> hookList, Set<Member> hookedSet) {
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
