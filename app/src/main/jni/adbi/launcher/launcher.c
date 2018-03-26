/*
 *  Collin's Binary Instrumentation Tool/Framework for Android
 *  Collin Mulliner <collin[at]mulliner.org>
 *  http://www.mulliner.org/android/
 *
 *  (c) 2012,2013
 *
 *  License: LGPL v2.1
 *
 */

#define _GNU_SOURCE
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/select.h>
#include <string.h>
#include <termios.h>
#include <pthread.h>
#include <sys/epoll.h>

#include <jni.h>
#include <stdlib.h>

#include <android/log.h>

#include "../base/hook.h"
#include "../base/base.h"
#include "../ddi/dexstuff.h"
#include "../ddi/dalvik_hook.h"

#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, "Inspector", fmt, ##args)


// this file is going to be compiled into a thumb mode binary

void __attribute__ ((constructor)) my_init(void);

/*  
 *  log function to pass to the hooking library to implement central loggin
 *
 *  see: set_logfunction() in base.h
 */
static void my_log(char *msg)
{
	LOGD("%s", msg);
}

JNIEnv *findJniEnv(const char *pLibpath) {
	void *pLibVm = dlopen(pLibpath, RTLD_LAZY);

	if (!pLibVm) {
		return NULL;
	}

	jint (*JNI_GetCreatedJavaVMs)(JavaVM**, jsize, jsize*) = dlsym(pLibVm, "JNI_GetCreatedJavaVMs");
	if (!JNI_GetCreatedJavaVMs) {
		LOGD("error finding JNI_GetCreatedJavaVMs - %s", pLibpath);
		dlclose(pLibVm);
		return NULL;
	}

	JavaVM *jvm;
	jsize sz;

	if (JNI_GetCreatedJavaVMs(&jvm, 1, &sz) != JNI_OK) {
		LOGD("error in JNI_GetCreatedJavaVMs - %s", pLibpath);
		dlclose(pLibVm);
		return NULL;
	}
	if (sz == 0 || !jvm) {
		LOGD("didn't find a VM - %s", pLibpath);
		dlclose(pLibVm);
		return NULL;
	}

	JNIEnv *env;
	if ((*jvm)->GetEnv(jvm, (void**)&env, JNI_VERSION_1_6) != JNI_OK || !env) {
		LOGD("error in JVM::GetEnv - %s", pLibpath);
		dlclose(pLibVm);
		return NULL;
	}

	return env;
}

static int is_file_exists(char* path) {
	FILE* test = fopen(path, "rb");
	if(test) {
		fclose(test);
		return 1;
	} else {
		return 0;
	}
}

static char* find_apk() {
	if(is_file_exists("/data/app/com.fuzhu8.inspector-1.apk")) {
		return "/data/app/com.fuzhu8.inspector-1.apk";
	}
	if(is_file_exists("/data/app/com.fuzhu8.inspector-2.apk")) {
		return "/data/app/com.fuzhu8.inspector-2.apk";
	}
	if(is_file_exists("/data/app/com.fuzhu8.inspector-3.apk")) {
		return "/data/app/com.fuzhu8.inspector-3.apk";
	}

	if(is_file_exists("/data/app/com.fuzhu8.inspector-1/base.apk")) {
		return "/data/app/com.fuzhu8.inspector-1/base.apk";
	}
	if(is_file_exists("/data/app/com.fuzhu8.inspector-2/base.apk")) {
		return "/data/app/com.fuzhu8.inspector-2/base.apk";
	}
	if(is_file_exists("/data/app/com.fuzhu8.inspector-3/base.apk")) {
		return "/data/app/com.fuzhu8.inspector-3/base.apk";
	}

	LOGD("find apk failed.");
	return NULL;
}

static struct dexstuff_t my_dexstuff;

static int loadDexposed(JNIEnv *env, jclass DexposedLoader, char* apk) {
	if(!DexposedLoader) {
		LOGD("liblauncher: FindClass = %p\n", DexposedLoader);
		return 0;
	}
	jmethodID load = (*env)->GetStaticMethodID(env, DexposedLoader, "load", "(Ljava/lang/String;)V");
	if(!load) {
		LOGD("liblauncher: load method not found!, %p\n", load);
		return 0;
	}

	jstring apkStr = (*env)->NewStringUTF(env, apk);
	LOGD("liblauncher: new apkStr = %s\n", apk);
	(*env)->CallStaticVoidMethod(env, DexposedLoader, load, apkStr);
	LOGD("liblauncher: CallStaticVoidMethod, load=%p.\n", load);
	(*env)->DeleteLocalRef(env, apkStr);

	LOGD("liblauncher: load successfully!\n");
	return 1;
}

static int ddi_loadDexposedModule(JNIEnv *env) {
	char* apk = find_apk();
	if(!apk) {
		return 0;
	}

	// load dex classes
	int cookie = dexstuff_loaddex(&my_dexstuff, apk);
	LOGD("liblauncher loaddex res = 0x%x\n", cookie);
	if (!cookie) {
		LOGD("liblauncher make sure /data/dalvik-cache/ is world writable and delete data@app@com.fuzhu8.inspector-*.apk@classes.dex\n");
		return 0;
	}

	void *clazz = dexstuff_defineclass(&my_dexstuff, "com/fuzhu8/inspector/DexposedLoader", cookie);
	if(!clazz) {
		LOGD("liblauncher: defineclass = %p\n", clazz);
		return 0;
	}

	jclass DexposedLoader = (*env)->FindClass(env, "com/fuzhu8/inspector/DexposedLoader");
	return loadDexposed(env, DexposedLoader, apk);
}

static struct dalvik_hook_t my_hook;

static jobject my_toString(JNIEnv *env, jobject obj, jobjectArray pdu) {
	dalvik_prepare(&my_dexstuff, &my_hook, env);
	jobject ret = (*env)->CallObjectMethod(env, obj, my_hook.mid);

	ddi_loadDexposedModule(env);

	return ret;
}

static jclass loadClassFromDex(JNIEnv *env, const char *classNameSlash, const char *classNameDot, const char *dexPath) {
	jclass clTargetClass = (*env)->FindClass(env, classNameSlash);
	if(clTargetClass) {
		return clTargetClass;
	}

	(*env)->ExceptionClear(env); // FindClass() complains if there's an exception already

	// Load my class with BaseDexClassLoader
	// See RilExtenderCommandsInterface.java:
	// new BaseDexClassLoader(rilExtenderDex.getAbsolutePath(), rilExtenderDexCacheDir, null, ClassLoader.getSystemClassLoader())

	jclass clDexClassLoader = (*env)->FindClass(env, "dalvik/system/BaseDexClassLoader");
	if(!clDexClassLoader) {
		LOGD("clDexClassLoader is NULL");
		return NULL;
	}

	jmethodID mClassLoaderConstructor = (*env)->GetMethodID(env, clDexClassLoader, "<init>", "(Ljava/lang/String;Ljava/io/File;Ljava/lang/String;Ljava/lang/ClassLoader;)V");
	if(!mClassLoaderConstructor) {
		LOGD("mClassLoaderConstructor is NULL");
		return NULL;
	}

	jmethodID mLoadClass = (*env)->GetMethodID(env, clDexClassLoader, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
	if(!mLoadClass) {
		LOGD("mLoadClass is NULL");
		return NULL;
	}

	jmethodID mGetSystemClassLoader = (*env)->GetStaticMethodID(env, clDexClassLoader, "getSystemClassLoader", "()Ljava/lang/ClassLoader;");
	if(!mGetSystemClassLoader) {
		LOGD("clDexClassLoader = %p", clDexClassLoader);
		return NULL;
	}

	jobject systemClassLoader = (*env)->CallStaticObjectMethod(env, clDexClassLoader, mGetSystemClassLoader);
	if(!systemClassLoader) {
		LOGD("systemClassLoader is NULL");
		return NULL;
	}

	jobject classloaderobj = (*env)->NewObject(env, clDexClassLoader, mClassLoaderConstructor,
			(*env)->NewStringUTF(env, dexPath), NULL, NULL,
			systemClassLoader);

	if ((*env)->ExceptionOccurred(env)) {
		LOGD("New ClassLoader threw an exception");
		(*env)->ExceptionDescribe(env);
		return NULL;
	}

	if(!classloaderobj) {
		LOGD("classloader object not found: %s", dexPath);
		return NULL;
	}

	clTargetClass = (*env)->CallObjectMethod(env, classloaderobj, mLoadClass, (*env)->NewStringUTF(env, classNameDot));
	if ((*env)->ExceptionOccurred(env)) {
		LOGD("loadClass() threw an exception");
		(*env)->ExceptionDescribe(env);
	} else {
		// this is enough to get Dalvik to execute <clinit> (class static initialization block) for us
		// (*env)->GetStaticMethodID(env, clTargetClass, "<clinit>", "()V");
	}

	LOGD("clTargetClass = %p", clTargetClass);
	return clTargetClass;
}

static int env_loadDexposedModule(JNIEnv *env) {
	char* apk = find_apk();
	if(!apk) {
		return 0;
	}

	jclass DexposedLoader = loadClassFromDex(env, "com/fuzhu8/inspector/DexposedLoader", "com.fuzhu8.inspector.DexposedLoader", apk);
	return loadDexposed(env, DexposedLoader, apk);
}

void my_init(void) {
	LOGD("%s started\n", __FILE__);
 
	set_logfunction(my_log);
	dalvikhook_set_logfunction(my_log);

	JNIEnv *env = findJniEnv("libdvm.so");
	if (!env) {
		env = findJniEnv("libart.so");
	}
	if (env && env_loadDexposedModule(env)) {
		return;
	}

	// resolve symbols from DVM
	dexstuff_resolv_dvm(&my_dexstuff);

	// hook
	dalvik_hook_setup(&my_hook, "Ljava/lang/String;", "toString", "()Ljava/lang/String;", 1, my_toString);
	my_hook.debug_me = 1;
	dalvik_hook(&my_dexstuff, &my_hook);
}

