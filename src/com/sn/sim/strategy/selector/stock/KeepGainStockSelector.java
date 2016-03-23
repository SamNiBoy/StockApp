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

public class KeepGainStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(KeepGainStockSelector.class);
    /**
     * @param args
     */
    public boolean isGoodStock(Stock2 s, ICashAccount ac) {
        if (s.getSd().keepDaysClsPriGain(3, 0.01)) {
                    log.info("returned true because keep 3 days gain 0.02.");
                    return true;
        }
        log.info("returned false for isGoodStock()");
        return false;
    }
	@Override
	public boolean isORCriteria() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean isMandatoryCriteria() {
		// TODO Auto-generated method stub
		return false;
	}
}
