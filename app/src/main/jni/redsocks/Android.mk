LOCAL_PATH		:= $(call my-dir)

ifneq ($(TARGET_ARCH_ABI),arm64-v8a)
	include $(CLEAR_VARS)
	REDSOCKS_SOURCES := base.c dnstc.c http-connect.c \
		log.c md5.c socks5.c \
		base64.c http-auth.c http-relay.c main.c \
		parser.c redsocks.c socks4.c utils.c
	LOCAL_STATIC_LIBRARIES := libevent
	LOCAL_MODULE := redsocks
	LOCAL_SRC_FILES := $(REDSOCKS_SOURCES)
	LOCAL_CFLAGS := -O2 -std=gnu99 -g -I$(LOCAL_PATH)/../libevent/include \
		-I$(LOCAL_PATH)/../libevent \
		-DIP_ORIGDSTADDR=20 -DIP_RECVORIGDSTADDR=IP_ORIGDSTADDR -DIP_TRANSPARENT=19 \
		-D_XOPEN_SOURCE=600 -D_DEFAULT_SOURCE -D_GNU_SOURCE -Wall
	LOCAL_LDFLAGS += -pie -fPIE
	NDK_APP_DST_DIR := assets/$(TARGET_ARCH_ABI)
	include $(BUILD_EXECUTABLE)
	NDK_APP_DST_DIR := $(NDK_APP_LIBS_OUT)/$(TARGET_ARCH_ABI)
endif