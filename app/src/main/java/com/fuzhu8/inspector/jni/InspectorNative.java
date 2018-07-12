package com.fuzhu8.inspector.jni;

import com.fuzhu8.inspector.Inspector;

/**
 * @author zhkl0228
 *
 */
public class InspectorNative extends Native {
	
	private static final InspectorNative INSTANCE = new InspectorNative();
	
	private InspectorNative() {
		super("Inspector");
	}
	
	public static InspectorNative getInstance() {
		return INSTANCE;
	}
	
	public void initializeNative(Inspector inspector) {
		_initializeNative(inspector);
	}
	
	private native void _initializeNative(Inspector inspector);

	@Override
	public boolean hasSupported() {
		return true;
	}

}
