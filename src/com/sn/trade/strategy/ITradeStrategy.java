package com.sn.trade.strategy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.TimeUnit;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.StockBuySellEntry;

public interface ITradeStrategy {

    /**
     * @param args
     */
    public boolean isGoodPointtoBuy(Stock s);
    public boolean isGoodPointtoSell(Stock s);
    
    public boolean sellStock(Stock s);
    public boolean buyStock(Stock s);
    public boolean calProfit(String ForDt, Map<String, Stock>stockSet);
    
    public boolean reportTradeStat();
    public ICashAccount getCashAccount();
    public void setCashAccount(ICashAccount ca);
    public boolean initAccount();
    
    public boolean performTrade(Stock s);
    public StockBuySellEntry getLstTradeRecord(Stock s);
    public void enableSimulationMode(boolean yes);
}
