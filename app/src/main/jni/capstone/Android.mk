ifeq ($(TARGET_ARCH),arm)
    LOCAL_PATH		:= $(call my-dir)
    include $(CLEAR_VARS)
    LOCAL_MODULE    := capstone
    LOCAL_CFLAGS := -O2 -g -I$(LOCAL_PATH)/include \
                    -DCAPSTONE_HAS_ARM -DCAPSTONE_USE_SYS_DYN_MEM
    LOCAL_SRC_FILES := cs.c utils.c SStream.c MCInstrDesc.c MCRegisterInfo.c MCInst.c \
                       arch/ARM/ARMDisassembler.c arch/ARM/ARMInstPrinter.c arch/ARM/ARMMapping.c arch/ARM/ARMModule.c
    include $(BUILD_SHARED_LIBRARY)
endif
