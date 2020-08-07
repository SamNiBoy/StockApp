package com.sn.cashAcnt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.mail.StockObserverable;
import com.sn.stock.Stock2;

public interface ICashAccount {

    static Logger log = Logger.getLogger(ICashAccount.class);
    
    /**
     * @param args
     */
    public double getMaxMnyForTrade();
    public String getActId();
    public double getInitMny();
    public double getUsedMny();
    public double getUsedMnyHrs();
    public void setUsedMny(double usedMny);
    public double getPftMny();
    public void printAcntInfo();
    public void printTradeInfo();
    public int getSellableAmt(String stkId, String sellDt);
    public int getUnSellableAmt(String stkId, String sellDt);
    public boolean calProfit();
    public boolean refreshProfitWithCurPri(Stock2 s);
    public boolean initAccount();
    public boolean hasStockInHand(Stock2 s);
    public double getInHandStockCostPrice(Stock2 s);
    public Double getLstBuyPri(Stock2 s);
    public double getStockCostRatio(Stock2 s);
}
