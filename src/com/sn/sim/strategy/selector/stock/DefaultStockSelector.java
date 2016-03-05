package com.sn.sim.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.sim.strategy.imp.TradeStrategyImp;
import com.sn.stock.Stock;
import com.sn.stock.Stock2;

public class DefaultStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(DefaultStockSelector.class);
    /**
     * @param args
     */
    public boolean isGoodStock(Stock2 s) {
        if (true) {//s.getSd().keepDaysClsPriLost(5, 0.01)) {
            log.info("DefaultStockSelector returned ture for isGoodStock()");
            return true;
        }
        log.info("DefaultStockSelector returned false for isGoodStock()");
        return false;
    }
}
