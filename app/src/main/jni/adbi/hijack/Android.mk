LOCAL_PATH		:= $(call my-dir)

ifeq ($(TARGET_ARCH),arm)
	include $(CLEAR_VARS)
	LOCAL_MODULE    := hijack
	LOCAL_SRC_FILES := hijack.c
	LOCAL_CFLAGS := -g
	LOCAL_LDFLAGS += -pie -fPIE
	NDK_APP_DST_DIR := assets/$(TARGET_ARCH_ABI)
	include $(BUILD_EXECUTABLE)
	NDK_APP_DST_DIR := $(NDK_APP_LIBS_OUT)/$(TARGET_ARCH_ABI)
endif