package com.sn.sim.strategy.selector.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.sim.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.stock.Stock;
import com.sn.stock.Stock2;

public class DefaultSellPointSelector implements ISellPointSelector {

    static Logger log = Logger.getLogger(DefaultSellPointSelector.class);
    
    /**
     * @param args
     */
    public boolean isGoodSellPoint(Stock2 s) {
        int periods = 5;
        int upTimes = 4;
        if (true) {//s.getSd().priceDownAfterSharpedUp(periods, upTimes)) {
            log.info("DefaultSellPointSelector returned ture for isGoodStock()");
            return true;
        }
        log.info("DefaultSellPointSelector returned ture for isGoodStock()");
        return false;
    }
}
