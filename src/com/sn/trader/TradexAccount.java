package com.sn.trader;

import org.apache.log4j.Logger;

public class TradexAccount {
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

