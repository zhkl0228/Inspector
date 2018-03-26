LOCAL_PATH		:= $(call my-dir)
include $(CLEAR_VARS)
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
	LOCAL_CFLAGS +=	-DLUA_USE_MKSTEMP
endif
LOCAL_MODULE    := lua
LUA_SOURCES	    := lapi.c lauxlib.c lbaselib.c lcode.c \
				   ldblib.c ldebug.c ldo.c ldump.c \
				   lfunc.c lgc.c linit.c liolib.c llex.c \
				   lmathlib.c lmem.c loadlib.c lobject.c \
				   lopcodes.c loslib.c lparser.c lstate.c \
				   lstring.c lstrlib.c ltable.c ltablib.c \
				   ltm.c lundump.c lvm.c lzio.c
LOCAL_SRC_FILES := $(LUA_SOURCES)
include $(BUILD_STATIC_LIBRARY)
