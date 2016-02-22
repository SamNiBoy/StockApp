package com.sn.sim.strategy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.db.DBManager;
import com.sn.stock.Stock;

public interface ITradeStrategy {

    /**
     * @param args
     */
    public boolean isGoodStockToSelect(Stock s);

    public boolean isGoodPointtoBuy(Stock s);

    public boolean isGoodPointtoSell(Stock s);
}
