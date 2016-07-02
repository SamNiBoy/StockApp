package com.sn.trade.strategy.selector.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;

public interface IBuyPointSelector {

    /**
     * @param args
     */
    public boolean isGoodBuyPoint(Stock s, ICashAccount ac);
    public int getBuyQty(Stock s, ICashAccount ac);
    public boolean matchTradeModeId(Stock s);
}
