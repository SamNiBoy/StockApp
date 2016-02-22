package com.sn.sim.strategy.selector.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.db.DBManager;
import com.sn.stock.Stock;

public class DefaultSellPointSelector implements ISellPointSelector {

    /**
     * @param args
     */
    public boolean isGoodSellPoint(Stock s) {
        if (s.getIncPriCnt() > 2 * s.getDesPriCnt()) {
            return false;
        }
        return true;
    }
}
