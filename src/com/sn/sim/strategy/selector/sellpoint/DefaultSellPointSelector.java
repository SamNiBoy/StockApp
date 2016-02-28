package com.sn.sim.strategy.selector.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.Stock2;

public class DefaultSellPointSelector implements ISellPointSelector {

    /**
     * @param args
     */
    public boolean isGoodSellPoint(Stock2 s) {
        int periods = 5;
        int upTimes = 4;
        if (s.getSd().priceDownAfterSharpedUp(periods, upTimes)) {
            return true;
        }
        return false;
    }
}
