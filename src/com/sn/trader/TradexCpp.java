package com.sn.trader;

/*
 * This java file is used for calling cpp code to communicate to Tradex system for trading.
 * Steps to build:
 * 1. Update this java code.
 * 2. Goto dir 'cppTrader'.
 * 3. type 'make clean' to cleanup.
 * 4. type 'make' to build the tradex executable.
 * 5. type 'make build_java_so' to build the libTradexCpp.so under 'cppTrader/lib'.
 * 
 * Note: check the makefile under 'cppTrader' for more detail.
 */
public class TradexCpp
{
    static {
        System.loadLibrary("TradexCpp");
    }
    public native boolean doLogin(String account, String password);
    public native String placeOrder();
    public static void main(String[] args) {
        String account = "nxj";
        String password = "123456";
        TradexCpp t = new TradexCpp();
        String msg = t.placeOrder();
        System.out.println("Got message from native code:" + msg);
        if(t.doLogin(account, password)) {
            System.out.println("in Java, you login success with:" + account + ", password:" + password);
        }
        else {
            System.out.println("in Java, you login failed with:" + account + ", password:" + password);
        }
    }
}
