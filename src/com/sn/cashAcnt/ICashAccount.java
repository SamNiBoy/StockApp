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
import com.sn.mail.reporter.StockObserverable;
import com.sn.stock.Stock;

public interface ICashAccount {

    static Logger log = Logger.getLogger(ICashAccount.class);
    
    /**
     * @param args
     */
    public double getMaxAvaMny();
    public String getActId();
    public double getInitMny();
    public double getUsedMny();
    public void setUsedMny(double usedMny);
    public double getPftMny();
    public int getSplitNum();
    public boolean isDftAcnt();
    public void printAcntInfo();
    public void printTradeInfo();
    public int getSellableAmt(String stkId, String sellDt);
    public int getUnSellableAmt(String stkId, String sellDt);
    public boolean calProfit(String ForDt, Map<String, Stock>stockSet);
    public boolean initAccount();
    public boolean hasStockInHand(Stock s);
    public double getInHandStockCostPrice(Stock s);
    public Double getLstBuyPri(Stock s);
    public double getStockCostRatio(Stock s);
}
