package com.sn.trade.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;

public interface IStockSelector {

    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac);
    public boolean isORCriteria();
    public boolean isMandatoryCriteria();
    public boolean adjustCriteria(boolean harder);
    public Integer getTradeModeId();
}
