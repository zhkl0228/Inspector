LOCAL_PATH		:= $(call my-dir)

ifeq ($(TARGET_ARCH),arm)
	include $(CLEAR_VARS)
	LOCAL_MODULE	:= udog
	LOCAL_CFLAGS	+= -DLINKER_DEBUG=0 \
					   -DNODEBUG \
					   -DUDOG_VERSION=1 \
					   -DANDROID \
					   -DANDROID_ARM_LINKER=1
	UDOG_SOURCES	:= crc.cpp dlfcn.cpp linker_environ.cpp linker_format.cpp \
					   linker_phdr.cpp linker.cpp options.cpp rt.cpp xor.cpp
	LOCAL_SRC_FILES	:= $(UDOG_SOURCES)
	include $(BUILD_SHARED_LIBRARY)
endif