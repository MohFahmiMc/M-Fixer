#include <jni.h>
#include <string>
#include <sys/stat.h>

extern "C" JNIEXPORT jboolean JNICALL
Java_com_scarilyid_mfixer_MainActivity_checkMinecraftInstalled(JNIEnv* env, jobject) {
    const char* path = "/sdcard/Android/data/com.mojang.minecraftpe";
    struct stat info;
    if (stat(path, &info) == 0 && (info.st_mode & S_IFDIR)) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}
