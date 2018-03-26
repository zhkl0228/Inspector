LOCAL_PATH		:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE    :=  MSHook
MSHOOK_SOURCES  := 	hook.cpp ARM.cpp Thumb.cpp x86.cpp x86_64.cpp \
					Debug.cpp Hooker.cpp PosixMemory.cpp util.cpp
LOCAL_SRC_FILES := 	$(MSHOOK_SOURCES)
include $(BUILD_STATIC_LIBRARY)
