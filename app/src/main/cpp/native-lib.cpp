#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring

JNICALL
Java_moe_johnny_areyouok_HelloIndianMiFanAct_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


extern "C"
JNIEXPORT jstring

JNICALL
Java_moe_johnny_areyouok_HelloIndianMiFanAct_getUevent(
        JNIEnv *env,
jobject /* this */) {
    FILE *fp;
    char buffer[1024];
    fp=popen("cat /sys/class/power_supply/bms/uevent", "r");
    fgets(buffer,sizeof(buffer),fp);
    return env->NewStringUTF(buffer);
}
