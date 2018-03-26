package com.fuzhu8.inspector.raw.dex.smali;

import android.annotation.SuppressLint;

public class DexFileHeadersPointer {
	
    private  int  baseAddr;
    private  int  pStringIds;
    private  int  pTypeIds;
    private  int  pFieldIds;
    private  int  pMethodIds;
    private  int  pProtoIds;
    private  int  pClassDefs;
    private  int  classCount;
    
	public int getClassCount() {
		return classCount;
	}
	public void setClassCount(int classCount) {
		this.classCount = classCount;
	}
	public void setBaseAddr(int baseAddr) {
		this.baseAddr = baseAddr;
	}
	public void setStringIds(int pStringIds) {
		this.pStringIds = pStringIds;
	}
	public void setTypeIds(int pTypeIds) {
		this.pTypeIds = pTypeIds;
	}
	public void setFieldIds(int pFieldIds) {
		this.pFieldIds = pFieldIds;
	}
	public void setMethodIds(int pMethodIds) {
		this.pMethodIds = pMethodIds;
	}
	public void setProtoIds(int pProtoIds) {
		this.pProtoIds = pProtoIds;
	}
	public void setClassDefs(int pClassDefs) {
		this.pClassDefs = pClassDefs;
	}
	public int getBaseAddr() {
		return baseAddr;
	}
	public int getStringIds() {
		return pStringIds;
	}
	public int getTypeIds() {
		return pTypeIds;
	}
	public int getFieldIds() {
		return pFieldIds;
	}
	public int getMethodIds() {
		return pMethodIds;
	}
	public int getProtoIds() {
		return pProtoIds;
	}
	public int getClassDefs() {
		return pClassDefs;
	}
	
	@SuppressLint("DefaultLocale") public String toString(){
		return "baseAddr:0x"+Integer.toHexString(baseAddr).toUpperCase()+
				";pStringIds:0x"+Integer.toHexString(pStringIds).toUpperCase() +
				";pTypeIds:0x"+Integer.toHexString(pTypeIds).toUpperCase()+
				";pFieldIds:0x"+Integer.toHexString(pFieldIds).toUpperCase()+
				";pMethodIds:0x"+Integer.toHexString(pMethodIds).toUpperCase()+
				";pProtoIds:0x"+Integer.toHexString(pProtoIds).toUpperCase()+
				";pClassDefs:0x"+Integer.toHexString(pClassDefs).toUpperCase()+
				";classCount:" + classCount;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + baseAddr;
		result = prime * result + classCount;
		result = prime * result + pClassDefs;
		result = prime * result + pFieldIds;
		result = prime * result + pMethodIds;
		result = prime * result + pProtoIds;
		result = prime * result + pStringIds;
		result = prime * result + pTypeIds;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DexFileHeadersPointer other = (DexFileHeadersPointer) obj;
		if (baseAddr != other.baseAddr)
			return false;
		if (classCount != other.classCount)
			return false;
		if (pClassDefs != other.pClassDefs)
			return false;
		if (pFieldIds != other.pFieldIds)
			return false;
		if (pMethodIds != other.pMethodIds)
			return false;
		if (pProtoIds != other.pProtoIds)
			return false;
		if (pStringIds != other.pStringIds)
			return false;
		if (pTypeIds != other.pTypeIds)
			return false;
		return true;
	}

}
