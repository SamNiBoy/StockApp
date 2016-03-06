package com.sn.sim.strategy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.TimeUnit;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.Stock2;

public interface ITradeStrategy {

    /**
     * @param args
     */
    public boolean isGoodStockToSelect(Stock2 s);
    public boolean isGoodPointtoBuy(Stock2 s);
    public boolean isGoodPointtoSell(Stock2 s);
    
    public boolean sellStock(Stock2 s);
    public boolean buyStock(Stock2 s);
    public boolean calProfit(String ForDt, Map<String, Stock2>stockSet);
    
    public boolean reportTradeStat();
    public ICashAccount getCashAccount();
    public void setCashAccount(ICashAccount ca);
    public boolean initAccount();
}
