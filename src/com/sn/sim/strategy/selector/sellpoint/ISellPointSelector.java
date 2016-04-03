package com.sn.sim.strategy.selector.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock2;

public interface ISellPointSelector {

    /**
     * @param args
     */
    public boolean isGoodSellPoint(Stock2 s, ICashAccount ac);
    public int getSellQty(Stock2 s, ICashAccount ac);
}
