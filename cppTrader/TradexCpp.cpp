#include "./include/com_sn_trader_TradexCpp.h"
#include "tradex_sample.hpp"

#include <thread>
#include <chrono>
#include <iostream>
#include <string.h>
#include <sstream>

using namespace com::tradex::sample;

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
    
    TradeXSample sample;

    sample.initialize("/usr/share/tomcat/logs/");
    sample.set_normal_account(7001028);
    sample.set_credit_account(7001004);

    client_id_t cleint_id = 5001093;
    std::string pwd = "Admin@123";
    std::string login_url = "219.143.244.232:5570";
    std::string hard_drive = "ST1000DM010-2EP102";
    std::string mac = "8C:EC:4B:52:62:F4";
    std::string ip = "124.127.131.218";

    /*client_id_t cleint_id = 5001093;
    std::string password = "Admin@123";
    std::string login_url = "219.143.244.232:5570";
    std::string hard_drive = "NVMe SAMSUNG MZFLV512";
    std::string mac = "88:B1:11:C9:5B:21";
    std::string ip = "192.168.0.100";*/

    TRXLoginRequest l;

    memset(&l, 0, sizeof(TRXLoginRequest));

    l.client_id = cleint_id;
    strncpy(l.password,pwd.c_str(),pwd.size());
    strncpy(l.login_url,login_url.c_str(),login_url.size());
    strncpy(l.hard_drive,hard_drive.c_str(),hard_drive.size());
    strncpy(l.mac,mac.c_str(),mac.size());
    strncpy(l.ip,ip.c_str(),ip.size());

    sample.login(&l);

    while (!sample.is_login_ready()) {
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }

    //sample.AlgoOrder();
    //sample.SHOrder();

    sample.QueryTrades();

    int x;
    std::cin >> x;

    sample.close();

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
