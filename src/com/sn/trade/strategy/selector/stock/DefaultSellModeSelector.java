package com.sn.trade.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.trade.strategy.imp.TradeStrategyImp;

public class DefaultSellModeSelector implements IStockSelector {

    static Logger log = Logger.getLogger(DefaultSellModeSelector.class);
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
        if (StockMarket.isMarketTooCold(s.getDl_dt()) &&
            StockMarket.hasMostDecStock()) {
            log.info("when market is too cool, set to sell mode.");
            return true;
        }
        log.info("market is not too cool, do not set to sell mode.");
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
		log.info("Mandatory criteria can not be adjusted");
		return true;
	}
}
