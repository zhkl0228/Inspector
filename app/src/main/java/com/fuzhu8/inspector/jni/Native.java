package com.fuzhu8.inspector.jni;

/**
 * @author zhkl0228
 *
 */
public abstract class Native {
	
	protected Native(String...libNames) {
		super();
		
		if(!hasSupported()) {
			return;
		}
		
		for(String libName : libNames) {
			System.loadLibrary(libName);
		}
	}
	
	protected abstract boolean hasSupported();
	
	protected final void checkSupported() {
		if(hasSupported()) {
			return;
		}
		
		throw new UnsupportedOperationException();
	}

}
