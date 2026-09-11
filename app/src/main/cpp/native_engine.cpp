#include <jni.h>
extern "C" JNIEXPORT jstring JNICALL Java_com_multitrackmemos_MainActivity_nativeEngineStatus(JNIEnv* env, jobject) {
    return env->NewStringUTF("ready");
}
