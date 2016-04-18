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
import com.sn.work.task.SuggestStock;

public class BadTradeSellModeSelector implements IStockSelector {

    static Logger log = Logger.getLogger(BadTradeSellModeSelector.class);
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
        if (SuggestStock.shouldStockExitTrade(s.getID())) {
        	log.info("Stock:" + s.getID() + " matches bad trade record, set to sell mode.");
        	return true;
        }
        log.info("Stock:" + s.getID() + " trade record not bad, no sell mode.");
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
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		log.info("BadTradeSellModeSelector can not be adjusted");
		return true;
	}
}
