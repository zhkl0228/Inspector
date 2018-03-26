package com.fuzhu8.inspector.dex;

import com.fuzhu8.inspector.Inspector;

public interface ClassMethod {
	
	void dump(String label, Inspector inspector);
	
	boolean isBytecodeMethod();
	
	short[] getInstructions();

}