package com.fuzhu8.inspector.dex;

public interface BytecodeMethod extends ClassMethod {

	int getRegistersSize();

	int getInsSize();

	int getOutsSize();

	int getDebugInfoOff();

}