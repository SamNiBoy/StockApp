#include "./include/com_sn_trader_TradexCpp.h"
#include "tradex_sample.hpp"

#include <thread>
#include <chrono>
#include <iostream>
#include <string.h>
#include <sstream>

using namespace com::tradex::sample;

//Global var area:
TradeXSample sample;
trade_unit_t my_trade_unit = 6001045;
trade_unit_t my_credit_account = 7001004;
client_id_t my_client_id = 5001093;
std::string my_client_psd = "Admin@12345";
std::string my_login_url = "219.143.244.232:5570";
std::string my_hard_drive = "ST1000DM010-2EP102";
std::string my_mac = "8C:EC:4B:52:62:F4";
std::string my_ip = "124.127.131.218";
std::string my_log_path = "/usr/share/tomcat/logs/";

jstring placeOrder(JNIEnv * env, jobject thiz, jstring ID, jstring area, jint qty, jdouble price, jboolean is_buy)
{

    std::cout<<"Now Place "<<(is_buy? "buy":"Sell") <<" order..."<<std::endl;
    char * stock_id_p = NULL;
    char * area_p = NULL;
    //std::cout << "Now place order from Cpp!" << std::endl;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("utf-8");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    
    jbyteArray stock_id_arr= (jbyteArray)env->CallObjectMethod(ID, mid, strencode);
    jsize stock_id_len = env->GetArrayLength(stock_id_arr);
    jbyte* stock_id_ba = env->GetByteArrayElements(stock_id_arr, JNI_FALSE);
 
    if (stock_id_len > 0)
    {
        stock_id_p = (char*)malloc(stock_id_len + 1);
 
        memcpy(stock_id_p, stock_id_ba, stock_id_len);
        stock_id_p[stock_id_len] = 0;
    }
    env->ReleaseByteArrayElements(stock_id_arr, stock_id_ba, 0);
    
    
    jbyteArray area_arr= (jbyteArray)env->CallObjectMethod(area, mid, strencode);
    jsize area_len = env->GetArrayLength(area_arr);
    jbyte* area_ba = env->GetByteArrayElements(area_arr, JNI_FALSE);
 
    if (area_len > 0)
    {
        area_p = (char*)malloc(area_len + 1);
 
        memcpy(area_p, area_ba, area_len);
        area_p[area_len] = 0;
    }
    env->ReleaseByteArrayElements(area_arr, area_ba, 0);
    
    uint64_t client_order_id = timestamp_now();
    sample.update_order_id(client_order_id, 0);

    TRXSingleOrder order;
    memset(&order, 0, sizeof(order));
    strcpy(order.symbol, stock_id_p);

    if (strcmp(area_p, "sh") == 0)
    {
        order.market = TRXMarket::SH_A;
    }
    else
    {
        order.market = TRXMarket::SZ_A;
    }
    
    delete area_p;
    delete stock_id_p;
    //order.trade_unit = my_trade_unit;
    order.trade_unit = my_trade_unit;
    order.client_order_id = client_order_id;
    
    order.price = (double)price;
    order.quantity = (int)qty;
    order.side = (is_buy ? TRXSide::Buy : TRXSide::Sell);
    order.price_type = TRXPriceType::LIMIT;

    int rtn = sample.PlaceOrder(&order);
    
    std::string rtstr = sample.getTranStatus();
    
   
    /*TRXTradeQueryRequest request;
    memset(&request, 0, sizeof(request));

    request.trade_unit = my_trade_unit;
    request.request_id = timestamp_now();
    request.client_order_id = client_order_id;

    std::cout<<"sending QueryTrade with request_id:" << request.request_id<<std::endl;
    rtn = sample.getAPI()->QueryTrade(&request);
    if (!rtn) {
        std::cout << rtn << std::endl;
    }
    
    std::string rtstr = sample.get_queryTrade(client_order_id, request.request_id);*/
    
    jmethodID ctorID = env->GetMethodID(clsstring, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = env->NewByteArray(strlen(rtstr.c_str()));
    env->SetByteArrayRegion(bytes, 0, strlen(rtstr.c_str()), (jbyte*)rtstr.c_str());
    jstring encoding = env->NewStringUTF("utf-8");
    jstring result = (jstring)env->NewObject(clsstring, ctorID, bytes, encoding);

    std::cout<<"Return "<<(is_buy? "buy":"Sell") <<" order result:"<<rtstr<<std::endl;
    /*std::this_thread::sleep_for(std::chrono::seconds(5));

    order_id_t order_id = find_order_id(client_order_id);
    if (order_id > 0) {
        TRXOrderCancelRequest cancel;
        cancel.order_id = order_id;
        cancel.trade_unit = my_trade_unit;

        api->CancelOrder(&cancel);
    }

    
    sample.QueryTrades();*/
    return result;
}


JNIEXPORT jboolean JNICALL Java_com_sn_trader_TradexCpp_doLogin(JNIEnv * env, jclass tclz, jstring account, jstring password, jstring trade_unit)
{
    char* account_p = NULL;
    char* password_p = NULL;
    char* trade_unit_p = NULL;
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
    
    
    jbyteArray trade_unit_arr= (jbyteArray)env->CallObjectMethod(trade_unit, mid, strencode);
    jsize trade_unit_len = env->GetArrayLength(trade_unit_arr);
    jbyte* trade_unit_ba = env->GetByteArrayElements(trade_unit_arr, JNI_FALSE);
 
    if (trade_unit_len > 0)
    {
        trade_unit_p = (char*)malloc(trade_unit_len + 1);
 
        memcpy(trade_unit_p, trade_unit_ba, trade_unit_len);
        trade_unit_p[trade_unit_len] = 0;
    }
    env->ReleaseByteArrayElements(trade_unit_arr, trade_unit_ba, 0);
    
    my_client_id = atoi(account_p);
    my_client_psd = password_p;
    my_trade_unit = atoi(trade_unit_p);
    
    delete account_p;
    delete password_p;
    delete trade_unit_p;

    sample.initialize(my_log_path);
    sample.set_normal_account(my_trade_unit);
    sample.set_credit_account(my_credit_account);

    client_id_t cleint_id = my_client_id;
    std::string pwd = my_client_psd;
    std::string login_url = my_login_url;
    std::string hard_drive = my_hard_drive;
    std::string mac = my_mac;
    std::string ip = my_ip;

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

    int max_attempts = 5;
    
    while (!sample.is_login_ready() && --max_attempts > 0) {
        std::this_thread::sleep_for(std::chrono::seconds(1));
        std::cout <<"looping on login one second..."<<std::endl;
    }

    if (max_attempts <= 0)
    {
        return (jboolean) false;
    }
    //sample.AlgoOrder();
    //sample.SHOrder();
    //sample.QueryTrades();

    return (jboolean)true;
}

JNIEXPORT jboolean JNICALL Java_com_sn_trader_TradexCpp_doLogout(JNIEnv *env, jclass clz)
{
    std::cout << "Now logout user from tradex." << std::endl;
    if (sample.is_login_ready())
    {
        sample.logout();
        sample.close();
    }
    else {
        std::cout << "logout didn't doing anything as didn't login success." <<std::endl;
        return (jboolean) false;
    }
    return (jboolean)true;
}

JNIEXPORT jboolean JNICALL Java_com_sn_trader_TradexCpp_checkLoginAlready(JNIEnv *env, jclass clz)
{
    std::cout << "Now checkLoginAlready..." << std::endl;
    return sample.is_login_ready();
}

/*
 * Return a string to java:
    jclass strClass = env->FindClass("Ljava/lang/String;");
    jmethodID ctorID = env->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = env->NewByteArray(strlen("This is from Cpp message"));
    env->SetByteArrayRegion(bytes, 0, strlen("This is from Cpp message"), (jbyte*)"This is from Cpp message");
    jstring encoding = env->NewStringUTF("utf-8");
    (jstring)env->NewObject(strClass, ctorID, bytes, encoding);
    
 */

/* Return value: rtncod,order_client_id
 * 
 */
JNIEXPORT jstring JNICALL Java_com_sn_trader_TradexCpp_placeBuyOrder
(JNIEnv * env, jclass thiz, jstring ID, jstring area, jint qty, jdouble price)
{
    placeOrder(env, thiz, ID, area, qty, price, true);
}

JNIEXPORT jstring JNICALL Java_com_sn_trader_TradexCpp_placeSellOrder
(JNIEnv * env, jclass thiz, jstring ID, jstring area, jint qty, jdouble price)
{
    placeOrder(env, thiz, ID, area, qty, price, false);
}

JNIEXPORT jstring JNICALL Java_com_sn_trader_TradexCpp_loadAcnt
(JNIEnv * env, jclass thizCls)
{
    TRXBalanceQueryRequest request;
    memset(&request, 0, sizeof(request));

    request.trade_unit = my_trade_unit;
    request.request_id = timestamp_now();

    int rtn = sample.QueryBalance(&request);
    //sample.QueryCash();
    std::string rtstr = sample.getTranStatus();
    
    
    jclass clsstring = env->FindClass("java/lang/String");
    jmethodID ctorID = env->GetMethodID(clsstring, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = env->NewByteArray(strlen(rtstr.c_str()));
    env->SetByteArrayRegion(bytes, 0, strlen(rtstr.c_str()), (jbyte*)rtstr.c_str());
    jstring encoding = env->NewStringUTF("utf-8");
    jstring result = (jstring)env->NewObject(clsstring, ctorID, bytes, encoding);
    
    return result;
}

JNIEXPORT jstring JNICALL Java_com_sn_trader_TradexCpp_cancelOrder
(JNIEnv * env, jclass thiz, jint order_id)
{
    TRXOrderCancelRequest cancel;
    cancel.order_id = order_id;
    cancel.trade_unit = my_trade_unit;
    sample.CancelOrder(&cancel);
    
    std::string rtstr = sample.getTranStatus();
    
    jclass clsstring = env->FindClass("java/lang/String");
    jmethodID ctorID = env->GetMethodID(clsstring, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = env->NewByteArray(strlen(rtstr.c_str()));
    env->SetByteArrayRegion(bytes, 0, strlen(rtstr.c_str()), (jbyte*)rtstr.c_str());
    jstring encoding = env->NewStringUTF("utf-8");
    jstring result = (jstring)env->NewObject(clsstring, ctorID, bytes, encoding);
    
    return result;
}

JNIEXPORT jstring JNICALL Java_com_sn_trader_TradexCpp_queryStockInHand
(JNIEnv * env, jclass thiz, jstring ID)
{
    
    std::cout<<"Now queryStockInHand "<<" for stock:"<<ID<<std::endl;
    char * symbol_p = NULL;
    //std::cout << "Now place order from Cpp!" << std::endl;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("utf-8");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    
    jbyteArray symbol_arr= (jbyteArray)env->CallObjectMethod(ID, mid, strencode);
    jsize symbol_len = env->GetArrayLength(symbol_arr);
    jbyte* symbol_ba = env->GetByteArrayElements(symbol_arr, JNI_FALSE);
 
    if (symbol_len > 0)
    {
        symbol_p = (char*)malloc(symbol_len + 1);
 
        memcpy(symbol_p, symbol_ba, symbol_len);
        symbol_p[symbol_len] = 0;
    }
    env->ReleaseByteArrayElements(symbol_arr, symbol_ba, 0);
    
    
    TRXPositionQueryRequest request;
    memset(&request, 0, sizeof(request));

    request.trade_unit = my_trade_unit;
    request.request_id = timestamp_now();
    strcpy(request.symbol, symbol_p);
    
    delete symbol_p;
    
    std::cout<<"Query stockinhand for:"<<request.symbol<<std::endl;

    int rtn = sample.QueryStockInHand(&request);
    
    std::string rtstr = sample.getTranStatus();
    
    
    jmethodID ctorID = env->GetMethodID(clsstring, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = env->NewByteArray(strlen(rtstr.c_str()));
    env->SetByteArrayRegion(bytes, 0, strlen(rtstr.c_str()), (jbyte*)rtstr.c_str());
    jstring encoding = env->NewStringUTF("utf-8");
    jstring result = (jstring)env->NewObject(clsstring, ctorID, bytes, encoding);
    
    return result;
}