#include <sys/types.h>
#include <unistd.h>
#include "com_tc_util_runtime_GetPid.h"
#include "jni.h"

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) { return JNI_VERSION_1_2; };

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved) { };

/*
 * Class:     com_tc_util_runtime_GetPid
 * Method:    getPid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_tc_util_runtime_GetPid_getPid
(JNIEnv *env, jobject obj) {
  int pid = getpid();
  return pid;
}


