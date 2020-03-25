package com.sn.trader;

import org.apache.log4j.Logger;

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
        System.out.println(System.getProperty("java.class.path"));
    }
    
    static Logger log = Logger.getLogger(TradexCpp.class);
    
    private native boolean doLogin(String account, String password);
    private native boolean doLogout();
    private native boolean checkLoginAlready();
    /* Return: order_status,client_order_id,order_id,qty,price*/
    private native String placeBuyOrder(String ID, String area, int qty, double price);
    private native String placeSellOrder(String ID, String area, int qty, double price);
    public static void main(String[] args) throws Exception {
        
        TradexCpp t = new TradexCpp();
        
        TradexBuySellResult tbs = t.processBuyOrder("000975", "sz", 100, 10.4);
        
        tbs.toString();
        
        TradexBuySellResult tbs2 = t.processSellOrder("000975", "sz", 100, 11.4);
        
        tbs2.toString();
        
        t.doLogout();
    }
    
    void doLoginCheck() throws Exception 
    {
       if (!checkLoginAlready()) 
       {
           boolean is_login_success = doLogin("dummyUsr", "dummyPswd");
           
           if (!is_login_success)
           {
               throw new Exception("Can not login Thradex success!");
           }
       }
    }
    public TradexBuySellResult processBuyOrder(String ID, String area, int qty, double price) throws Exception {
        
       doLoginCheck();
       
       TradexBuySellResult tbs = null;
       String result =  placeBuyOrder(ID, area, qty, price);
       if (result.startsWith("error#")) {
           /*error#7#251005:证券可用数量不足(p_stock_code=000975,v_enable_amount=0.00)*/
           String res[] = result.split("#");
           int errcod = Integer.valueOf(res[1]);
           String errmsg = res[2];
           tbs = new TradexBuySellResult(errcod, errmsg);
           return tbs;
       }
       else {
           /*success#order:30000521|symbol:000975|price:10.400000|quantity:100|trade_amount:1040.000000*/
           String res[] = result.split("#");
           String txtmsg = res[1];
           String dtlary[] = txtmsg.split("|");
           int order_id = Integer.valueOf(dtlary[0].split(":")[1]);
           String id = dtlary[1].split(":")[1];
           double trade_price = Double.valueOf(dtlary[2].split(":")[1]);
           int trade_quantity = Integer.valueOf(dtlary[3].split(":")[1]);
           double trade_amount = Double.valueOf(dtlary[4].split(":")[1]);
           tbs = new TradexBuySellResult(id, trade_price, trade_quantity, trade_amount, order_id, true);
           return tbs;
       }
    }
    public TradexBuySellResult processSellOrder(String ID, String area, int qty, double price) throws Exception{
        
        doLoginCheck();

        TradexBuySellResult tbs = null;
       String result =  placeSellOrder(ID, area, qty, price);
       if (result.startsWith("error#")) {
           /*error#7#251005:证券可用数量不足(p_stock_code=000975,v_enable_amount=0.00)*/
           String res[] = result.split("#");
           int errcod = Integer.valueOf(res[1]);
           String errmsg = res[2];
           tbs = new TradexBuySellResult(errcod, errmsg);
           return tbs;
       }
       else {
           /*success#order:30000521|symbol:000975|price:10.400000|quantity:100|trade_amount:1040.000000*/
           String res[] = result.split("#");
           String txtmsg = res[1];
           String dtlary[] = txtmsg.split("|");
           int order_id = Integer.valueOf(dtlary[0].split(":")[1]);
           String id = dtlary[1].split(":")[1];
           double trade_price = Double.valueOf(dtlary[2].split(":")[1]);
           int trade_quantity = Integer.valueOf(dtlary[3].split(":")[1]);
           double trade_amount = Double.valueOf(dtlary[4].split(":")[1]);
           tbs = new TradexBuySellResult(id, trade_price, trade_quantity, trade_amount, order_id, false);
           return tbs;
       }
    }
}

class TradexBuySellResult
{
    
   static Logger log = Logger.getLogger(TradexBuySellResult.class);
   private String id;
   private double trade_price;
   private int trade_quantity;
   private double trade_amount;
   private int order_id;
   private boolean is_buy_flg;
   
   private int error_code;
   private String error_msg;
   
   TradexBuySellResult(
           String id1,
           double trade_price1,
           int trade_quantity1,
           double trade_amount1,
           int order_id1,
           boolean is_buy_flg1) {
       id = id1;
       trade_price = trade_price1;
       trade_quantity = trade_quantity1;
       trade_amount = trade_amount1;
       order_id = order_id1;
       is_buy_flg = is_buy_flg1;
   }
   
   TradexBuySellResult(int errcod,
                       String errmsg)
   {
       error_code = errcod;
       error_msg = errmsg;
   }
   
   public boolean isIs_buy_flg() {
    return is_buy_flg;
   }
   public void setIs_buy_flg(boolean is_buy_flg) {
       this.is_buy_flg = is_buy_flg;
   }
   public String getError_msg() {
    return error_msg;
   }
   public void setError_msg(String error_msg) {
       this.error_msg = error_msg;
   }
   public int getError_code() {
       return error_code;
   }
   public void setError_code(int error_code) {
       this.error_code = error_code;
   }
   public String getId() {
       return id;
   }
   public void setId(String id) {
       this.id = id;
   }
   public double getTrade_price() {
       return trade_price;
   }
   public void setTrade_price(double trade_price) {
       this.trade_price = trade_price;
   }
   public int getTrade_quantity() {
       return trade_quantity;
   }
   public void setTrade_quantity(int trade_quantity) {
       this.trade_quantity = trade_quantity;
   }
   public double getTrade_amount() {
       return trade_amount;
   }
   public void setTrade_amount(double trade_amount) {
       this.trade_amount = trade_amount;
   }
   public int getOrder_id() {
       return order_id;
   }
   public void setOrder_id(int order_id) {
       this.order_id = order_id;
   }
   public boolean isTranSuccess() {
       return error_code == 0;
   }
   
   public String toString() {
       log.info("Print TradexBuySellResult:");
       
       String rts = "";
       if (this.isTranSuccess()) {
           log.info("Buy/Sell:" + (this.is_buy_flg? "Buy":"Sell"));
           log.info("Stock ID:" + id);
           log.info("Trade Quantity:" + trade_quantity);
           log.info("Trade Price:" + trade_price);
           log.info("Trade Amount:" + trade_amount);
           log.info("Tradex Order Number:" + order_id);
           
           rts = "Buy/Sell:" + (this.is_buy_flg? "Buy":"Sell")+ "\n" +
                 "Stock ID:" + id+ "\n" +
                 "Trade Quantity:" + trade_quantity+ "\n" +
                 "Trade Price:" + trade_price+ "\n" +
                 "Trade Amount:" + trade_amount+ "\n" +
                 "Tradex Order Number:" + order_id+ "\n";
       }
       else {
           log.info("Trade errored with error code:" + error_code + ", error_message:" + error_msg);
           rts = "Trade errored with error code:" + error_code + ", error_message:" + error_msg;
       }
       return rts;
   }
}