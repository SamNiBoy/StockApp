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
import com.sn.strategy.algorithm.IBuyPointSelector;
import com.sn.strategy.algorithm.ISellPointSelector;

public interface ITradeStrategy {

    /**
     * @param args
     */
    public boolean isGoodPointtoBuy(Stock2 s, IBuyPointSelector bs);
    public boolean isGoodPointtoSell(Stock2 s, ISellPointSelector ss);
    
    public boolean sellStock(Stock2 s, ISellPointSelector ss);
    public boolean buyStock(Stock2 s, IBuyPointSelector bs);
    
    public void resetStrategyStatus();
    public void resetStrategyStatusForStock(String stk);
    public boolean reportTradeStat();
    public ICashAccount getCashAccount();
    public void setCashAccount(ICashAccount ca);
    public boolean initAccount();
    
    public boolean performTrade(Stock2 s);
    public StockBuySellEntry getLstTradeRecord(Stock2 s);
    public void enableSimulationMode(boolean yes);
    
    public String getTradeStrategyName();
}
