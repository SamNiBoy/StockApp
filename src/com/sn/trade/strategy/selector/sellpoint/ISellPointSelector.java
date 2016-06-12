package com.sn.trade.strategy.selector.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;

public interface ISellPointSelector {

    /**
     * @param args
     */
    public boolean isGoodSellPoint(Stock s, ICashAccount ac);
    public int getSellQty(Stock s, ICashAccount ac);
}
