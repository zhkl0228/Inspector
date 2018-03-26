#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <sys/types.h>
#include <android/log.h>
#include "com_fuzhu8_inspector_jni_DvmUtil.h"
#include "dvm.h"

JNIEXPORT jint JNICALL Java_com_fuzhu8_inspector_jni_DvmUtil_findClassId
  (JNIEnv *env, jobject obj, jclass clazz) {
	if (clazz == NULL) {
		dvmThrowIllegalArgumentException("class must not be null");
		return 0;
	}

	ClassObject* declaredClass = (ClassObject*) dvmDecodeIndirectRef(dvmThreadSelf(), clazz);
	return (jint) declaredClass;
}

JNIEXPORT jint JNICALL Java_com_fuzhu8_inspector_jni_DvmUtil_findMethodId
  (JNIEnv *env, jobject obj, jclass clazz, jobject member, jint slot) {
	if (clazz == NULL) {
		dvmThrowIllegalArgumentException("class must not be null");
		return 0;
	}

	ClassObject* declaredClass = (ClassObject*) dvmDecodeIndirectRef(dvmThreadSelf(), clazz);
	Method* method = dvmSlotToMethod(declaredClass, slot);
	return (jint) method;
}

extern struct DvmGlobals gDvm;

#define LOG_TAG "Inspector"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static DvmDex* getDvmDexFromClassPathEntry(ClassPathEntry* cpe) {
    if (cpe->kind == kCpeDex) {
        return ((RawDexFile*) cpe->ptr)->pDvmDex;
    }
    if (cpe->kind == kCpeJar) {
        return ((JarFile*) cpe->ptr)->pDvmDex;
    }
    LOGI("Unknown cpe->kind=%d", cpe->kind);
    return NULL;
}

/*
 * Get the cache file name from a ClassPathEntry.
 */
static const char* getCacheFileName(const ClassPathEntry* cpe) {
    switch (cpe->kind) {
    case kCpeJar:
        return dvmGetJarFileCacheFileName((JarFile*) cpe->ptr);
    case kCpeDex:
        return dvmGetRawDexFileCacheFileName((RawDexFile*) cpe->ptr);
    default:
    	LOGI("getCacheFileName: unexpected cpe kind %d", cpe->kind);
        dvmAbort();
        return NULL;
    }
}

/*
 * Class:     com_fuzhu8_inspector_dex_InspectorNative
 * Method:    getDvmGlobalsPointer
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_fuzhu8_inspector_jni_DvmUtil_getDvmGlobalsPointer
  (JNIEnv *env, jobject obj) {
	return (jint) &gDvm;
}
