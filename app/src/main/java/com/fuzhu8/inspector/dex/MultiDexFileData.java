package com.fuzhu8.inspector.dex;

import com.fuzhu8.inspector.Inspector;

/**
 * @author zhkl0228
 *
 */
class MultiDexFileData extends DexFileData {
	
	private final DexFileData[] datas;

	MultiDexFileData(DexFileData...datas) {
		super(null, null, null);
		
		this.datas = datas;
	}

	@Override
	public void writeToConsole(Inspector inspector) {
		for(DexFileData data : datas) {
			data.writeToConsole(inspector);
		}
	}

}
