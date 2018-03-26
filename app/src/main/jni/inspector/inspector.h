#include "com_fuzhu8_inspector_jni_InspectorNative.h"

void inspect(const char *fmt, ...);
void inspect(const void *data, size_t size, const char *fmt, ...);

void collectDexFile(const void *data, size_t size, const char *name);
