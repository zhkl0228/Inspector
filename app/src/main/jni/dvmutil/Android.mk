LOCAL_PATH		:= $(call my-dir)

ifneq ($(TARGET_ARCH_ABI),arm64-v8a)
	include $(CLEAR_VARS)
	LOCAL_C_INCLUDES += $(LOCAL_PATH)/../android
	LOCAL_MODULE	:= dvmutil
	LOCAL_SRC_FILES := dvmutil.cpp
	LOCAL_LDLIBS	:= -L$(LOCAL_PATH)/../so/$(TARGET_ARCH_ABI) -ldvm -llog
	include $(BUILD_SHARED_LIBRARY)
endif