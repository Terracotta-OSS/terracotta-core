// GetPid.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include <process.h>
#include "com_tc_util_runtime_GetPid.h"

BOOL APIENTRY DllMain( HANDLE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved
					 )
{
    return TRUE;
}

/*
 * Class:     com_tc_util_runtime_GetPid
 * Method:    getPid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_tc_util_runtime_GetPid_getPid
(JNIEnv *, jobject) {
	return _getpid();	
}




