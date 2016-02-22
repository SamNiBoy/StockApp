package com.sn.sim.strategy.imp;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.db.DBManager;
import com.sn.sim.strategy.ITradeStrategy;
import com.sn.stock.Stock;

public class TSIncFast implements ITradeStrategy {

    /**
     * @param args
     */
    public boolean isGoodStockToSelect(Stock s) {
        return true;
    }

    public boolean isGoodPointtoBuy(Stock s) {
        return true;
    }

    public boolean isGoodPointtoSell(Stock s) {
        return true;
    }
}
