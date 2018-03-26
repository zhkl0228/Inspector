/**
 * 
 */
package com.fuzhu8.inspector.script.hook;

import com.taobao.android.dexposed.callbacks.XCMethodPointer;

/**
 * @author zhkl0228
 *
 */
public interface Callable<T, E> extends XCMethodPointer<T, E> {
	
	Object getOriginal();

}
