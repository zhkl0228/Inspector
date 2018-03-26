//
// Created by 廖正凯 on 2017/10/23.
//

#include "com_freakishfox_xAnSo_xAnSoUnpacker.h"
#include <fstream>

#include "fix/section_fix.h"

JNIEXPORT jint JNICALL Java_com_freakishfox_xAnSo_xAnSoUnpacker_fix_1section
        (JNIEnv *env, jclass clazz, jstring so_path, jstring out_path) {
    const char *target_file = env->GetStringUTFChars(so_path, NULL);
    const char *out_file = env->GetStringUTFChars(out_path, NULL);

    // fix section
    section_fix fixer_;
    if (!fixer_.fix(target_file)){
        return 1;
    }

    std::string fixed_name_ = std::string(out_file);
    if (fixer_.save_as(fixed_name_)) {
        return 0;
    }
    return 3;
}
