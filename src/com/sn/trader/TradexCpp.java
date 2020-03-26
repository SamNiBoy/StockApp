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
    private native String cancelOrder(int order_id);
    private native String queryStockInHand(String ID);
    
    private native String loadAcnt();
    public static void main(String[] args) throws Exception {
        
        TradexCpp t = new TradexCpp();
        
        t.doLogin("abc", "def");
        TradexBuySellResult tbs = t.processBuyOrder("000975", "sz", 100, 10.4);
        
        System.out.println(tbs.toString());
        
        t.processCancelOrder(tbs.getOrder_id());
        
        TradexBuySellResult tbs2 = t.processSellOrder("000975", "sz", 100, 11.4);
        
        System.out.println(tbs2.toString());
        
        
        TradexStockInHand a = t.processQueryStockInHand("000975");
        
        System.out.println(a.toString());
        
        TradexAccount ta = t.processLoadAcnt();
        System.out.println(ta.toString());
        
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
    
    public TradexStockInHand processQueryStockInHand(String ID) {
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
    
    public boolean processCancelOrder(int order_id) {
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
    
    public TradexAccount processLoadAcnt() {
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
/*
 *  trade_unit_t trade_unit;  // 交易单元
    symbol_t symbol;          // 交易标的代码，需填入交易所认可的交易标的代码
    TRXSide side;             // 买卖方向，期货及期权
    TRXHedgeFlag hedge_flag;  // 股指期货投机套保标志
    TRXMarket market;         // 此持仓的交易市场
    quantity_t yesterday_qty; // 昨日持仓
    quantity_t latest_qty;    // 最新持仓
    quantity_t available_qty; // 可用数量
    quantity_t frozen_qty;    // 冻结数量
    money_t margin;           // 保证金(期货、期权)
 */
class TradexStockInHand {
    private String acntID;
    private String stockID;
    private int side;
    private int yesterday_qty;
    private int latest_qty;
    private int available_qty;
    private int frozen_qty;
    
    private int error_code;
    private String error_msg;
    
    static Logger log = Logger.getLogger(TradexStockInHand.class);
    
    TradexStockInHand(int errcod,
            String errmsg)
    {
    error_code = errcod;
    error_msg = errmsg;
    }
    
    public TradexStockInHand(String acntID, String stockID, int side,
            int yesterday_qty, int latest_qty, int available_qty,
            int frozen_qty) {
        this.acntID = acntID;
        this.stockID = stockID;
        this.side = side;
        this.yesterday_qty = yesterday_qty;
        this.latest_qty = latest_qty;
        this.available_qty = available_qty;
        this.frozen_qty = frozen_qty;
    }
    
    public String getAcntID() {
        return acntID;
    }
    public void setAcntID(String acntID) {
        this.acntID = acntID;
    }
    public String getStockID() {
        return stockID;
    }
    public void setStockID(String stockID) {
        this.stockID = stockID;
    }
    public int getSide() {
        return side;
    }
    public void setSide(int side) {
        this.side = side;
    }
    public int getYesterday_qty() {
        return yesterday_qty;
    }
    public void setYesterday_qty(int yesterday_qty) {
        this.yesterday_qty = yesterday_qty;
    }
    public int getLatest_qty() {
        return latest_qty;
    }
    public void setLatest_qty(int latest_qty) {
        this.latest_qty = latest_qty;
    }
    public int getAvailable_qty() {
        return available_qty;
    }
    public void setAvailable_qty(int available_qty) {
        this.available_qty = available_qty;
    }
    public int getFrozen_qty() {
        return frozen_qty;
    }
    public void setFrozen_qty(int frozen_qty) {
        this.frozen_qty = frozen_qty;
    }
    
    public boolean isTranSuccess() {
        return error_code == 0;
    }
    
    public int getError_code() {
        return error_code;
    }

    public void setError_code(int error_code) {
        this.error_code = error_code;
    }

    public String getError_msg() {
        return error_msg;
    }

    public void setError_msg(String error_msg) {
        this.error_msg = error_msg;
    }
    
    public String toString() {
        log.info("Print TradexStockInHand:");
        
        /*
         *         this.stockID = stockID;
        this.side = side;
        this.yesterday_qty = yesterday_qty;
        this.latest_qty = latest_qty;
        this.available_qty = available_qty;
        this.frozen_qty = frozen_qty;
         */
        
        String rts = "";
        if (this.isTranSuccess()) {
            log.info("acntID:" + acntID);
            log.info("stockID:" + stockID);
            log.info("side:" + side);
            log.info("yesterday_qty:" + yesterday_qty);
            log.info("latest_qty:" + latest_qty);
            log.info("available_qty:" + available_qty);
            log.info("frozen_qty:" + frozen_qty);
            
            rts = "acntID:" + acntID + "\n"
            + "stockID:" + stockID + "\n"
            + "side:" + side + "\n"
            + "yesterday_qty:" + yesterday_qty + "\n"
            + "latest_qty:" + latest_qty + "\n"
            + "available_qty:" + available_qty + "\n"
            + "frozen_qty:" + frozen_qty;
        }
        else {
            log.info("Trade errored with error code:" + error_code + ", error_message:" + error_msg);
            rts = "Trade errored with error code:" + error_code + ", error_message:" + error_msg;
        }
        return rts;
    }
}

class TradexAccount {
    private String acntID;
    private double init_mny;
    private double usable_mny;
    private double fetchable_mny;
    private double stock_value;
    private double total_value;
    
    private int splitNum = 1000;
    
    private int error_code;
    private String error_msg;
    
    static Logger log = Logger.getLogger(TradexAccount.class);

    TradexAccount(
            String acntID1,
            double init_mny1,
            double usable_mny1,
            double fetchable_mny1,
            double stock_value1,
            double total_value1) {
        acntID = acntID1;
        init_mny = init_mny1;
        usable_mny = usable_mny1;
        fetchable_mny = fetchable_mny1;
        stock_value = stock_value1;
        total_value = total_value1;
    }
    
    TradexAccount(int errcod,
                        String errmsg)
    {
        error_code = errcod;
        error_msg = errmsg;
    }

    public String getAcntID() {
        return acntID;
    }

    public void setAcntID(String acntID) {
        this.acntID = acntID;
    }

    public double getInit_mny() {
        return init_mny;
    }

    public void setInit_mny(double init_mny) {
        this.init_mny = init_mny;
    }

    public double getUsable_mny() {
        return usable_mny;
    }

    public void setUsable_mny(double usable_mny) {
        this.usable_mny = usable_mny;
    }

    public double getFetchable_mny() {
        return fetchable_mny;
    }

    public void setFetchable_mny(double fetchable_mny) {
        this.fetchable_mny = fetchable_mny;
    }

    public double getStock_value() {
        return stock_value;
    }

    public void setStock_value(double stock_value) {
        this.stock_value = stock_value;
    }

    public double getTotal_value() {
        return total_value;
    }

    public void setTotal_value(double total_value) {
        this.total_value = total_value;
    }

    public int getSplitNum() {
        return splitNum;
    }

    public void setSplitNum(int splitNum) {
        this.splitNum = splitNum;
    }
    
    public boolean isTranSuccess() {
        return error_code == 0;
    }
    
    public int getError_code() {
        return error_code;
    }

    public void setError_code(int error_code) {
        this.error_code = error_code;
    }

    public String getError_msg() {
        return error_msg;
    }

    public void setError_msg(String error_msg) {
        this.error_msg = error_msg;
    }
    
    public String toString() {
        log.info("Print TradexAccount:");
        
        String rts = "";
        if (this.isTranSuccess()) {
            log.info("acntID:" + acntID);
            log.info("init_mny:" + init_mny);
            log.info("usable_mny:" + usable_mny);
            log.info("fetchable_mny:" + fetchable_mny);
            log.info("stock_value:" + stock_value);
            log.info("total_value:" + total_value);
            log.info("splitNum:" + splitNum);
            
            rts = "acntID:" + acntID + "\n"
            + "init_mny:" + init_mny + "\n"
            + "usable_mny:" + usable_mny + "\n"
            + "fetchable_mny:" + fetchable_mny + "\n"
            + "stock_value:" + stock_value + "\n"
            + "total_value:" + total_value + "\n"
            + "splitNum:" + splitNum;
        }
        else {
            log.info("Trade errored with error code:" + error_code + ", error_message:" + error_msg);
            rts = "Trade errored with error code:" + error_code + ", error_message:" + error_msg;
        }
        return rts;
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
   
   public String getInsertSQL(String acntID) {
       String sql = "insert into tradedtl (seqnum, acntId, stkId, order_id, price, amount, dl_dt, buy_flg) values(select "
               + " max(seqnum) + 1,"+ acntID + "," + id + "," + order_id + "," + trade_price + "," + trade_amount + ",sysdate(), " + is_buy_flg+ " from tradedtl where acntId = '" + acntID + "')";
       return sql;
   }
}