package com.sn.trader;

import org.apache.log4j.Logger;

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
public class TradexStockInHand {
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

