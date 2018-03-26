/**
 * 
 */
package com.fuzhu8.inspector.dex.vm.dvm;

/**
 * @author zhkl0228
 *
 */
public enum PrimitiveType {
	
	PRIM_NOT,
	
	PRIM_VOID,
	
	PRIM_BOOLEAN,
	
	PRIM_BYTE,
	
	PRIM_SHORT,
	
	PRIM_CHAR,
	
	PRIM_INT,
	
	PRIM_LONG,
	
	PRIM_FLOAT,
	
	PRIM_DOUBLE;
	
	public static PrimitiveType valueOf(int primitiveType) {
		for(PrimitiveType type : values()) {
			if(type.ordinal() == primitiveType) {
				return type;
			}
		}
		throw new RuntimeException("Unknown primitive type: " + primitiveType);
	}

}
