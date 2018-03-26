LOCAL_PATH		:= $(call my-dir)

ifeq ($(TARGET_ARCH),arm)
	include $(CLEAR_VARS)
	LOCAL_MODULE    := base
	ADBI_BASE_SOURCES := util.c hook.c base.c
	LOCAL_SRC_FILES := $(ADBI_BASE_SOURCES)
	include $(BUILD_STATIC_LIBRARY)
endif
