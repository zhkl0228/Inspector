/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_fuzhu8_inspector_jni_DvmUtil */

#ifndef _Included_com_fuzhu8_inspector_jni_DvmUtil
#define _Included_com_fuzhu8_inspector_jni_DvmUtil
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_fuzhu8_inspector_jni_DvmUtil
 * Method:    findClassId
 * Signature: (Ljava/lang/Class;)I
 */
JNIEXPORT jint JNICALL Java_com_fuzhu8_inspector_jni_DvmUtil_findClassId
  (JNIEnv *, jobject, jclass);

/*
 * Class:     com_fuzhu8_inspector_jni_DvmUtil
 * Method:    getDvmGlobalsPointer
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_fuzhu8_inspector_jni_DvmUtil_getDvmGlobalsPointer
  (JNIEnv *, jobject);

/*
 * Class:     com_fuzhu8_inspector_jni_DvmUtil
 * Method:    findMethodId
 * Signature: (Ljava/lang/Class;Ljava/lang/reflect/Member;I)I
 */
JNIEXPORT jint JNICALL Java_com_fuzhu8_inspector_jni_DvmUtil_findMethodId
  (JNIEnv *, jobject, jclass, jobject, jint);

#ifdef __cplusplus
}
#endif
#endif