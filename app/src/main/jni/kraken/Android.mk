LOCAL_PATH		:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../libpcap
LOCAL_STATIC_LIBRARIES := libpcap
LOCAL_LDFLAGS 	:= -shared -Wall -fPIC
LOCAL_MODULE	:= kpcap
LOCAL_SRC_FILES := kpcap.c routingtable.c
include $(BUILD_SHARED_LIBRARY)