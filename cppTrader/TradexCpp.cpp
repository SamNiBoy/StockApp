#include "./include/com_sn_trader_TradexCpp.h"
#include "tradex_sample.hpp"

#include <thread>
#include <chrono>
#include <iostream>
#include <string.h>
#include <sstream>

JNIEXPORT jboolean JNICALL Java_com_sn_trader_TradexCpp_doLogin(JNIEnv * env, jobject thiz, jstring account, jstring password)
{
    char* account_p = NULL;
    char* password_p = NULL;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("utf-8");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    
    
    jbyteArray account_arr= (jbyteArray)env->CallObjectMethod(account, mid, strencode);
    jsize account_len = env->GetArrayLength(account_arr);
    jbyte* account_ba = env->GetByteArrayElements(account_arr, JNI_FALSE);
 
    if (account_len > 0)
    {
        account_p = (char*)malloc(account_len + 1);
 
        memcpy(account_p, account_ba, account_len);
        account_p[account_len] = 0;
    }
    env->ReleaseByteArrayElements(account_arr, account_ba, 0);
    
    
    jbyteArray password_arr= (jbyteArray)env->CallObjectMethod(password, mid, strencode);
    jsize password_len = env->GetArrayLength(password_arr);
    jbyte* password_ba = env->GetByteArrayElements(password_arr, JNI_FALSE);
 
    if (password_len > 0)
    {
        password_p = (char*)malloc(password_len + 1);
 
        memcpy(password_p, password_ba, password_len);
        password_p[password_len] = 0;
    }
    env->ReleaseByteArrayElements(password_arr, password_ba, 0);
    std::cout << "in Cpp got account:"<<account_p<<", password:"<<password_p<< std::endl;
    return (jboolean)false;
}

JNIEXPORT jstring JNICALL Java_com_sn_trader_TradexCpp_placeOrder(JNIEnv *env, jobject obj)
{
    std::cout << "Now place order from Cpp!" << std::endl;
    jclass strClass = env->FindClass("Ljava/lang/String;");
    jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = env->NewByteArray(strlen("This is from Cpp message"));
    env->SetByteArrayRegion(bytes, 0, strlen("This is from Cpp message"), (jbyte*)"This is from Cpp message");
    jstring encoding = env->NewStringUTF("utf-8");
    return (jstring)env->NewObject(strClass, ctorID, bytes, encoding);
}
