package com.sn.sim.strategy.selector.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.sim.strategy.selector.stock.DefaultStockSelector;
import com.sn.stock.Stock;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class DefaultBuyPointSelector implements IBuyPointSelector{

    static Logger log = Logger.getLogger(DefaultBuyPointSelector.class);
    
    /**
     * @param args
     */
    public boolean isGoodBuyPoint(Stock2 s) {
        int periods = 5;
        int ratio = 10;
        int downTimes = 3;
        if (true) {//s.getSd().detQtyPlused(periods, ratio) &&
            //s.getSd().priceUpAfterSharpedDown(periods, downTimes)) {
            //!StockMarket.isMarketTooCold(s.getDl_dt()) &&
            //StockMarket.hasMoreIncStock()) {
            log.info("DefaultBuyPointSelector returned ture for isGoodStock()");
            return true;
        }
        log.info("DefaultBuyPointSelector returned false for isGoodStock()");
        return false;
    }
}
