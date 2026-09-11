#include <jni.h>

#include "audio_types.h"

static_assert(static_cast<int>(sketch::RoutingMode::BuiltInMic) == 0);

extern "C" JNIEXPORT jstring JNICALL Java_com_multitrackmemos_MainActivity_nativeEngineStatus(JNIEnv* env, jobject) {
    return env->NewStringUTF("ready");
}
