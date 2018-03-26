LOCAL_PATH		:= $(call my-dir)

ifeq ($(TARGET_ARCH),arm)
	include $(CLEAR_VARS)
	LOCAL_MODULE    := launcher
	LOCAL_STATIC_LIBRARIES := libdalvikhook
	DALVIK_SOURCES	:= launcher.c
	LOCAL_SRC_FILES := $(DALVIK_SOURCES)
	LOCAL_LDLIBS    := -ldl -llog
	LOCAL_CFLAGS	:= -g
	include $(BUILD_SHARED_LIBRARY)
endif