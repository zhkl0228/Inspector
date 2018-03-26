/**
 * 
 */
package com.fuzhu8.inspector.dex.vm.dvm;

/**
 * @author zhkl0228
 *
 */
public enum ClassStatus {
	
	CLASS_ERROR(-1),
	
	CLASS_NOTREADY(0),
	
	CLASS_IDX(1),
	
	CLASS_LOADED(2),
	
	CLASS_RESOLVED(3),
	
	CLASS_VERIFYING(4),
	
	CLASS_VERIFIED(5),
	
	CLASS_INITIALIZING(6),
	
	CLASS_INITIALIZED(7);

	private final int status;

	private ClassStatus(int status) {
		this.status = status;
	}

	public int getStatus() {
		return status;
	}
	
	public static ClassStatus valueOf(int status) {
		for(ClassStatus classStatus : values()) {
			if(classStatus.status == status) {
				return classStatus;
			}
		}
		throw new RuntimeException("Unknown class status: " + status);
	}

}
