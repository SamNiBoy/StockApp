package com.sn.strategy.algorithm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock2;

public interface IBuyPointSelector {

    /**
     * @param args
     */
    public boolean isGoodBuyPoint(Stock2 s, ICashAccount ac);
    public int getBuyQty(Stock2 s, ICashAccount ac);
    public boolean isSimMode();
}
