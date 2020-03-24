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
        System.loadLibrary("tradex");
        System.loadLibrary("TradexCpp");
    }
    public native boolean doLogin(String account, String password);
    public native boolean doLogout();
    /* Return: order_status,client_order_id,order_id,qty,price*/
    public native String placeBuyOrder(String ID, String area, int qty, double price);
    public native String placeSellOrder(String ID, String area, int qty, double price);
    public static void main(String[] args) {
        String account = "nxj";
        String password = "123456";
        TradexCpp t = new TradexCpp();
        if(t.doLogin(account, password)) {
            System.out.println("in Java, you login success with:" + account + ", password:" + password);
        }
        else {
            System.out.println("in Java, you login failed with:" + account + ", password:" + password);
        }
        String buy_rtncode = t.placeBuyOrder("000975", "sz", 100, 10.4);
        System.out.println("Got code from buy place order:" + buy_rtncode );
        String sell_rtncode = t.placeSellOrder("000975", "sz", 100, 11.4);
        System.out.println("Got code from sell place order:" + sell_rtncode );
        t.doLogout();
    }
}
