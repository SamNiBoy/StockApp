package com.sn.sim.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock2;

public interface IStockSelector {

    /**
     * @param args
     */
    public boolean isGoodStock(Stock2 s, ICashAccount ac);
    public boolean isORCriteria();
    public boolean isMandatoryCriteria();
}
