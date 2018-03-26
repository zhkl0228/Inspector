#define LOG_TAG "Inspector"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <android/log.h>

#define LOG_BUF_SIZE	1024

#include "inspector.h"

static JavaVM *g_vm = NULL;
static jobject g_inspector = NULL;

static jmethodID gPrint = NULL;
static jmethodID gInspect = NULL;
static jmethodID gCollectDexFile = NULL;

static void init_inspector(JNIEnv *env, jobject inspector) {
	if(env->GetJavaVM(&g_vm) != JNI_OK) {
		LOGI("GetJavaVM failed");
		return;
	}
	g_inspector = env->NewGlobalRef(inspector);

	jclass clazz = env->GetObjectClass(g_inspector);
	if(clazz == NULL) {
		LOGI("Get inspector class failed");
		return;
	}

	gPrint = env->GetMethodID(clazz, "println", "(Ljava/lang/Object;)V");
	gInspect = env->GetMethodID(clazz, "inspect", "([BLjava/lang/String;)V");
	gCollectDexFile = env->GetMethodID(clazz, "collectDexFile", "([BLjava/lang/String;)V");
}

JNIEXPORT void JNICALL Java_com_fuzhu8_inspector_jni_InspectorNative__1initializeNative
  (JNIEnv *env, jobject in, jobject inspector) {
	init_inspector(env, inspector);
}

/*JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void *reserved) {
	g_vm = vm;
	return JNI_VERSION_1_4;
}*/

void inspect(const char *fmt, ...) {
	if(g_inspector == NULL || g_vm == NULL || gPrint == NULL) {
		return;
	}

	jboolean isAttached = JNI_FALSE;
	JNIEnv* env = NULL;
	if(g_vm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK) {
		if(g_vm->AttachCurrentThread(&env, NULL) != JNI_OK) {
			return;
		}
		isAttached = JNI_TRUE;
	}

	va_list ap;
	char buf[LOG_BUF_SIZE];
	memset(buf, 0, LOG_BUF_SIZE);

	va_start(ap, fmt);
	vsnprintf(buf, LOG_BUF_SIZE, fmt, ap);
	va_end(ap);

	jstring str = env->NewStringUTF(buf);
	env->CallVoidMethod(g_inspector, gPrint, str);

	if(isAttached) {
		g_vm->DetachCurrentThread();
	}
}

void inspect(const void *data, size_t size, const char *fmt, ...) {
	if(g_inspector == NULL || g_vm == NULL || data == NULL || size < 1 || gInspect == NULL) {
		return;
	}

	jboolean isAttached = JNI_FALSE;
	JNIEnv* env = NULL;
	if(g_vm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK) {
		if(g_vm->AttachCurrentThread(&env, NULL) != JNI_OK) {
			return;
		}
		isAttached = JNI_TRUE;
	}

	va_list ap;
	char buf[LOG_BUF_SIZE];
	memset(buf, 0, LOG_BUF_SIZE);

	va_start(ap, fmt);
	vsnprintf(buf, LOG_BUF_SIZE, fmt, ap);
	va_end(ap);

	jbyteArray bytes = env->NewByteArray(size);
	env->SetByteArrayRegion(bytes, 0, size, (const jbyte*) data);
	jstring str = env->NewStringUTF(buf);
	env->CallVoidMethod(g_inspector, gInspect, bytes, str);

	if(isAttached) {
		g_vm->DetachCurrentThread();
	}
}

void collectDexFile(const void *data, size_t size, const char *name) {
	if(g_inspector == NULL || g_vm == NULL || data == NULL || size < 1 || gCollectDexFile == NULL) {
		return;
	}

	jboolean isAttached = JNI_FALSE;
	JNIEnv* env = NULL;
	if(g_vm->GetEnv((void**)&env, JNI_VERSION_1_4) != JNI_OK) {
		if(g_vm->AttachCurrentThread(&env, NULL) != JNI_OK) {
			return;
		}
		isAttached = JNI_TRUE;
	}

	jbyteArray bytes = env->NewByteArray(size);
	env->SetByteArrayRegion(bytes, 0, size, (const jbyte*) data);
	jstring str = env->NewStringUTF(name);
	env->CallVoidMethod(g_inspector, gCollectDexFile, bytes, str);

	if(isAttached) {
		g_vm->DetachCurrentThread();
	}
}
