package com.sn.sim.strategy.selector.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class DefaultBuyPointSelector implements IBuyPointSelector{

    /**
     * @param args
     */
    public boolean isGoodBuyPoint(Stock2 s) {
        int periods = 5;
        int ratio = 10;
        int downTimes = 3;
        if (s.getSd().detQtyPlused(periods, ratio) &&
            s.getSd().priceUpAfterSharpedDown(periods, downTimes) &&
            !StockMarket.isMarketTooCold() &&
            StockMarket.hasMoreIncStock()) {
            return true;
        }
        return false;
    }
}
