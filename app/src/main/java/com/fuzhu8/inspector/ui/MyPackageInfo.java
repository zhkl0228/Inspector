/**
 * 
 */
package com.fuzhu8.inspector.ui;

/**
 * @author zhkl0228
 *
 */
public class MyPackageInfo {
	
	private final CharSequence label;
	private final int uid;
	private final boolean service;
	
	public MyPackageInfo(CharSequence label, int uid, boolean service) {
		super();
		this.label = label;
		this.uid = uid;
		this.service = service;
	}
	public CharSequence getLabel() {
		return label;
	}
	public int getUid() {
		return uid;
	}
	public boolean isService() {
		return service;
	}

}
