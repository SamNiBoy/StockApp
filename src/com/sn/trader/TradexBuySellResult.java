package com.sn.trader;

import org.apache.log4j.Logger;

public class TradexBuySellResult
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
   
   public TradexBuySellResult(
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