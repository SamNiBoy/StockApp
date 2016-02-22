package com.sn.sim.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.db.DBManager;
import com.sn.stock.Stock;

public class DefaultStockSelector implements IStockSelector {

    /**
     * @param args
     */
    public boolean isGoodStock(Stock s) {
        if (s.getPct() > s.getPrePct()) {
            return true;
        }
        return false;
    }
}
