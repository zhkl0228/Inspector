LOCAL_PATH		:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE	:= Inspector
LOCAL_SRC_FILES := inspector.cpp
LOCAL_LDLIBS	:= -llog
include $(BUILD_SHARED_LIBRARY)
