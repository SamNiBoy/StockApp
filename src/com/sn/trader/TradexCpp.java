package com.sn.trader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import com.sn.trader.TradexAccount;
import com.sn.trader.TradexBuySellResult;
import com.sn.trader.TradexStockInHand;
import com.sn.db.DBManager;
import com.sn.mail.reporter.GzStockBuySellPointObserverable.BuySellInfoSubscriber;
import com.sn.trader.StockTrader;

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
    
    private static native boolean doLogin(String account, String password, String trade_unit);
    private static native boolean doLogout();
    private static native boolean checkLoginAlready();
    /* Return: order_status,client_order_id,order_id,qty,price*/
    private static native String placeBuyOrder(String ID, String area, int qty, double price);
    private static native String placeSellOrder(String ID, String area, int qty, double price);
    private static native String cancelOrder(int order_id);
    private static native String queryStockInHand(String ID);
    
    private static native String loadAcnt();
    
    private static String client_id = "5001093";
    private static String client_pwd = "Admin@12345";
    private static String trade_unit = "6001045";
    
    
    public static void main(String[] args) throws Exception {
        
        TradexCpp t = new TradexCpp();
        
        t.doLogin("5001093", "Admin@12345", "6001045");
        /*TradexBuySellResult tbs = t.processBuyOrder("000975", "sz", 100, 10.4);
        
        System.out.println(tbs.toString());
        
        t.processCancelOrder(tbs.getOrder_id());
        
        TradexBuySellResult tbs2 = t.processSellOrder("000975", "sz", 100, 11.4);
        
        System.out.println(tbs2.toString());
        
        
        TradexStockInHand a = t.processQueryStockInHand("000975");
        
        System.out.println(a.toString());
        
        TradexAccount ta = t.processLoadAcnt();
        System.out.println(ta.toString());*/
        StockTrader.doTest();
        
        t.doLogout();
    }
    
    public static void findBestClientForTrade(String ID)
    {
        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        try {
            stm = con.createStatement();
            String sql = "select u.client_id, u.client_pwd, u.trade_unit, case when t.in_hand_qty is null then 0 else t.in_hand_qty end in_hand_qty"
                    + "     from usrStk s"
                    + "     join usr u"
                    + "       on s.openID = u.openID "
                    + "left join tradehdr t"
                    + "        on u.trade_unit = t.acntId "
                    + "    where s.gz_flg = 1"
                    + "      and u.buy_sell_enabled = 1"
                    + "      and s.stkId = '" + ID + "'"
                    + "    order by in_hand_qty desc";
            log.info(sql);
            rs = stm.executeQuery(sql);
            if (rs.next()) {
                String nxt_client_id = rs.getString("client_id");
                String nxt_client_pwd = rs.getString("client_pwd");
                String nxt_trade_unit = rs.getString("trade_unit");
                log.info("findBestClientForTrade, client_id:" + client_id + ", client_pwd:" + client_pwd + ", trade_unit:" + trade_unit);
                log.info("findBestClientForTrade, nxt_client_id:" + nxt_client_id + ", nxt_client_pwd:" + nxt_client_pwd + ", nxt_trade_unit:" + nxt_trade_unit);
                if (!client_id.equals(nxt_client_id))
                {
                    log.info("findBestClientForTrade, client_id:" + client_id + " is different with nxt_client_id:" + nxt_client_id + ", login with next client to trade.");
                    if (client_id.length() > 0 && checkLoginAlready())
                    {
                        doLogout();
                    }
                    
                    client_id = nxt_client_id;
                    client_pwd = nxt_client_pwd;
                    trade_unit = nxt_trade_unit;
                    doLogin(nxt_client_id, nxt_client_pwd, nxt_trade_unit);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                rs.close();
                stm.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static void doLoginCheck() throws Exception 
    {
       if (!checkLoginAlready()) 
       {
           boolean is_login_success = doLogin(client_id, client_pwd, trade_unit);
           
           if (!is_login_success)
           {
               throw new Exception("Can not login Thradex success!");
           }
       }
    }
    
    public static TradexStockInHand processQueryStockInHand(String ID) throws Exception {
        
        doLoginCheck();
        
        /*        std::string value = "trade_unit:" + std::to_string(position->trade_unit) + "|symbol:" + position->symbol + "|side:" + std::to_string(position->side) + "|yesterday_qty:" + std::to_string(position->yesterday_qty) +
                            "|latest_qty:" + std::to_string(position->latest_qty) + "|available_qty:" + std::to_string(position->available_qty) + "|frozen_qty:" + std::to_string(position->frozen_qty);*/
        String result = queryStockInHand(ID);
        System.out.println("In Java processQueryStockInHand got:" + result);
        
        TradexStockInHand tsih = null;
        if (result.startsWith("error#")) {
            String res[] = result.split("#");
            int errcod = Integer.valueOf(res[1]);
            String errmsg = res[2];
            tsih = new TradexStockInHand(errcod, errmsg);
            return tsih;
        }
        else {
            /*
             * private String acntID;
               private String stockID;
               private int side;
               private int yesterday_qty;
               private int latest_qty;
               private int available_qty;
               private int frozen_qty;
             */
            String res[] = result.split("#");
            String txtmsg = res[1];
            String dtlary[] = txtmsg.split("\\|");
            String acntID = dtlary[0].split(":")[1];
            String stockID = dtlary[1].split(":")[1];
            int side = Integer.valueOf(dtlary[2].split(":")[1]);
            int yesterday_qty = Integer.valueOf(dtlary[3].split(":")[1]);
            int latest_qty = Integer.valueOf(dtlary[4].split(":")[1]);
            int available_qty = Integer.valueOf(dtlary[5].split(":")[1]);
            int frozen_qty = Integer.valueOf(dtlary[6].split(":")[1]);
            tsih = new TradexStockInHand(acntID, stockID, side, yesterday_qty, latest_qty, available_qty, frozen_qty);
            return tsih;
        }
    }
    
    public static boolean processCancelOrder(int order_id) throws Exception {
        
        doLoginCheck();
        
        String result = cancelOrder(order_id);
        System.out.println("In Java processCancelOrder got:" + result);
        
        if (result.startsWith("error#")) {
            String res[] = result.split("#");
            int errcod = Integer.valueOf(res[1]);
            String errmsg = res[2];
            System.out.println("processCancelOrder error:" + errcod + ", msg:" + errmsg);
            return false;
        }
        else {
            System.out.println("processCancelOrder cancelled order:" + order_id + " success.");
            return true;
        }
    }
    
    public static TradexAccount processLoadAcnt() throws Exception {
        
        doLoginCheck();
        
        String result = loadAcnt();
        System.out.println("In Java got:" + result);
        
        TradexAccount ta = null;
        if (result.startsWith("error#")) {
            /*error#7#251005:证券可用数量不足(p_stock_code=000975,v_enable_amount=0.00)*/
            String res[] = result.split("#");
            int errcod = Integer.valueOf(res[1]);
            String errmsg = res[2];
            ta = new TradexAccount(errcod, errmsg);
            return ta;
        }
        else {
           /*success#trade_unit:6001045|initial_balance:9971784.460000|available_balance:9970104.460000|withdrawable_balance:9970104.460000|market_value:41325.000000|total_asset:10011429.460000*/
            /*String acntID1,
              double init_mny1,
              double usable_mny1,
              double fetchable_mny1,
              double stock_value1,
              double total_value1*/
            String res[] = result.split("#");
            String txtmsg = res[1];
            String dtlary[] = txtmsg.split("\\|");
            int acntID = Integer.valueOf(dtlary[0].split(":")[1]);
            double init_mny1 = Double.valueOf(dtlary[1].split(":")[1]);
            double usable_mny1 = Double.valueOf(dtlary[2].split(":")[1]);
            double fetchable_mny1 = Double.valueOf(dtlary[3].split(":")[1]);
            double stock_value1 = Double.valueOf(dtlary[4].split(":")[1]);
            double total_value1 = Double.valueOf(dtlary[5].split(":")[1]);
            ta = new TradexAccount(String.valueOf(acntID), init_mny1, usable_mny1, fetchable_mny1, stock_value1, total_value1);
            return ta;
        }
    }
    

    public static TradexBuySellResult processBuyOrder(String ID, String area, int qty, double price) throws Exception {
        
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
           String dtlary[] = txtmsg.split("\\|");
           int order_id = Integer.valueOf(dtlary[0].split(":")[1]);
           String id = dtlary[1].split(":")[1];
           double trade_price = Double.valueOf(dtlary[2].split(":")[1]);
           int trade_quantity = Integer.valueOf(dtlary[3].split(":")[1]);
           double trade_amount = Double.valueOf(dtlary[4].split(":")[1]);
           tbs = new TradexBuySellResult(id, trade_price, trade_quantity, trade_amount, order_id, true);
           return tbs;
       }
    }
    public static TradexBuySellResult processSellOrder(String ID, String area, int qty, double price) throws Exception{
        
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
           String dtlary[] = txtmsg.split("\\|");
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
