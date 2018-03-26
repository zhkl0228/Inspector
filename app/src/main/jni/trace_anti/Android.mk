LOCAL_PATH		:= $(call my-dir)

ifeq ($(TARGET_ARCH),arm)
	include $(CLEAR_VARS)
	LOCAL_C_INCLUDES += $(LOCAL_PATH)/../android \
	                    $(LOCAL_PATH)/../TKHooklib/armeabi \
						$(LOCAL_PATH)/../inspector \
						$(LOCAL_PATH)/../MSHook
	LOCAL_SHARED_LIBRARIES := TKHooklib DexHunter Inspector
	LOCAL_CPPFLAGS	:= -std=c++11
	#LOCAL_CFLAGS	:= -mllvm -sub -mllvm -fla -mllvm -bcf
	LOCAL_ARM_MODE	:= arm
	LOCAL_MODULE	:= TraceAnti
	LOCAL_SRC_FILES := trace_anti.cpp arm.S
	LOCAL_STATIC_LIBRARIES := libMSHook
	LOCAL_LDLIBS	:= -L$(LOCAL_PATH)/../so/$(TARGET_ARCH_ABI) -ldvm -llog -fuse-ld=bfd -ldl
	include $(BUILD_SHARED_LIBRARY)
endif