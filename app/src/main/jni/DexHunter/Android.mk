LOCAL_PATH		:= $(call my-dir)

ifneq ($(TARGET_ARCH_ABI),arm64-v8a)
	include $(CLEAR_VARS)
	LOCAL_C_INCLUDES += $(LOCAL_PATH)/../android \
						$(LOCAL_PATH)/../inspector
	LOCAL_MODULE	:= DexHunter
	LOCAL_SHARED_LIBRARIES := Inspector
	LOCAL_SRC_FILES := DexHunter.cpp
	LOCAL_LDLIBS	:= -L$(LOCAL_PATH)/../so/$(TARGET_ARCH_ABI) -ldvm -llog
	include $(BUILD_SHARED_LIBRARY)
endif
