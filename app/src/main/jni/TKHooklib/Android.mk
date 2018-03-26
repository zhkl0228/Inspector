LOCAL_PATH		:= $(call my-dir)

ifeq ($(TARGET_ARCH),arm)
	include $(CLEAR_VARS)
	LOCAL_MODULE	:= TKHooklib
	LOCAL_SRC_FILES := armeabi/libTKHooklib.so
	include $(PREBUILT_SHARED_LIBRARY)
endif

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
	include $(CLEAR_VARS)
	LOCAL_MODULE	:= TKInlineHook64
	LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libTKInlineHook64.so
	include $(PREBUILT_SHARED_LIBRARY)
endif
