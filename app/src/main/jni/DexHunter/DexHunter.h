//
// Created by 陈伟腾 on 15/8/5.
//

#ifndef DUMPAPK_DUMP_H
#define DUMPAPK_DUMP_H

#ifndef LOG_TAG
#define LOG_TAG "cc"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#endif

#include <stdlib.h>
#include <android/log.h>
#include <stdio.h>
#include "dvm.h"
#include "inspector.h"

struct CookieMap {
    int cookie;
    DvmDex *pDvmDex;
    void *header;
    u4 header_len;
    void *data;
    u4 data_len;
    void *dex;
    struct CookieMap* next;
};

struct EncodedField {
    u4 field_idx_diff;
    u4 access_flag;
};

struct EncodedMethod {
    u4 method_idx_diff;
    u4 access_flag;
    u4 code_off;
};

struct ClassDataItem {
    u4 static_field_size;
    u4 instance_field_size;
    u4 direct_method_size;
    u4 virtual_method_size;
    EncodedField* static_fields;
    EncodedField* instance_fields;
    EncodedMethod* direct_methods;
    EncodedMethod* virtual_methods;
};

class MyDexFile{
public:
    DexFile* dexFile;
    MyDexFile(DexFile* dex, bool shouldmalloc){
        if(shouldmalloc){
            dexFile = (DexFile*)malloc(sizeof(DexFile));
            dexFile->pHeader = (DexHeader*)malloc(sizeof(DexHeader));
            memcpy((void*)dexFile->pHeader, dex->pHeader, sizeof(DexHeader));
        }else{
            dexFile = dex;
        }
    }
};

class MyClassDef {
public:
    DexClassDef* classDef;

    MyClassDef(bool shouldmalloc, DexClassDef* defclass){
        if (shouldmalloc){
            classDef = new DexClassDef();
            memcpy(classDef, &defclass, sizeof(DexClassDef));
        }else{
            classDef = defclass;
        }
    }

    void dump(DexFile* dexFile) {
    }
};

struct BytecodeMap {
	const Method *method;
	u4  insnsSize;
	u2 *insns;
    struct BytecodeMap* next;
};

void collectBytecode(const Method* m);
struct BytecodeMap* fixMethodBytecode(const char *descriptor, const Method *method);

#endif //DUMPAPK_DUMP_H
