LOCAL_PATH		:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE    := capstone
LOCAL_CFLAGS := -O2 -g -I$(LOCAL_PATH)/include -DCAPSTONE_USE_SYS_DYN_MEM
LOCAL_SRC_FILES := cs.c utils.c SStream.c MCInstrDesc.c MCRegisterInfo.c MCInst.c
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
    LOCAL_CFLAGS += -DCAPSTONE_HAS_ARM64
    LOCAL_SRC_FILES += arch/AArch64/AArch64BaseInfo.c arch/AArch64/AArch64Disassembler.c arch/AArch64/AArch64InstPrinter.c arch/AArch64/AArch64Mapping.c arch/AArch64/AArch64Module.c
endif
ifeq ($(TARGET_ARCH_ABI),armeabi)
    LOCAL_CFLAGS += -DCAPSTONE_HAS_ARM
    LOCAL_SRC_FILES += arch/ARM/ARMDisassembler.c arch/ARM/ARMInstPrinter.c arch/ARM/ARMMapping.c arch/ARM/ARMModule.c
endif
ifeq ($(TARGET_ARCH_ABI),x86)
    LOCAL_CFLAGS += -DCAPSTONE_HAS_X86
    LOCAL_SRC_FILES += arch/X86/X86ATTInstPrinter.c arch/X86/X86Disassembler.c arch/X86/X86DisassemblerDecoder.c arch/X86/X86IntelInstPrinter.c arch/X86/X86Mapping.c arch/X86/X86Module.c
endif
include $(BUILD_SHARED_LIBRARY)
