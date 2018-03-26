LOCAL_PATH		:= $(call my-dir)

ifeq ($(TARGET_ARCH),arm)
	include $(CLEAR_VARS)
	LOCAL_MODULE    := dalvikhook
	LOCAL_STATIC_LIBRARIES := libbase
	DALVIK_HOOK_SOURCES := dexstuff.c.arm dalvik_hook.c
	LOCAL_SRC_FILES := $(DALVIK_HOOK_SOURCES)
	LOCAL_LDLIBS    := -ldl -ldvm
	include $(BUILD_STATIC_LIBRARY)
endif
