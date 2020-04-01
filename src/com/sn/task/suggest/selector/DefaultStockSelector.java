package com.sn.task.suggest.selector;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.strategy.TradeStrategyImp;
import com.sn.task.IStockSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class DefaultStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(DefaultStockSelector.class);
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	if (s == null) {
    		log.info("s is null!, possible?");
    	}
    	else {
    		log.info("s is not null, s.getDl_dt() is:" + s.getDl_dt());
    	}
        if (StockMarket.isMarketTooCold(s.getDl_dt()) &&
                !StockMarket.hasMoreIncStock()) {
                    log.info("returned false because market is too cool.");
                    return false;
        }
        log.info("returned true for isGoodStock()");
        return true;
    }
	@Override
	public boolean isORCriteria() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isMandatoryCriteria() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		log.info("Mandatory criteria can not be adjusted");
		return true;
	}
}
