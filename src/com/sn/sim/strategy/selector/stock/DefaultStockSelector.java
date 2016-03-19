package com.sn.sim.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.imp.TradeStrategyImp;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class DefaultStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(DefaultStockSelector.class);
    /**
     * @param args
     */
    public boolean isGoodStock(Stock2 s, ICashAccount ac) {
        if (StockMarket.isMarketTooCold(s.getDl_dt()) &&
                !StockMarket.hasMoreIncStock()) {
                    log.info("returned false because market is too cool.");
                    return false;
        }
        log.info("returned true for isGoodStock()");
        return true;
    }
}
