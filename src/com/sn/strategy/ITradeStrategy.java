package com.sn.strategy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.TimeUnit;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;

public interface ITradeStrategy {

    /**
     * @param args
     */
    public boolean isGoodPointtoBuy(Stock2 s);
    public boolean isGoodPointtoSell(Stock2 s);
    
    public boolean sellStock(Stock2 s);
    public boolean buyStock(Stock2 s);
    
    public void resetStrategyStatus();
    public boolean reportTradeStat();
    public ICashAccount getCashAccount();
    public void setCashAccount(ICashAccount ca);
    public boolean initAccount();
    
    public boolean performTrade(Stock2 s);
    public StockBuySellEntry getLstTradeRecord(Stock2 s);
    public void enableSimulationMode(boolean yes);
    
    public String getTradeStrategyName();
}
