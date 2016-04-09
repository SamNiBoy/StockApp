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

public class AvgClsPriSellModeSelector implements IStockSelector {

    static Logger log = Logger.getLogger(AvgClsPriSellModeSelector.class);
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	Double avgPri = s.getAvgYtClsPri(10, 0);
    	Double curPri = s.getCur_pri();
        if ((avgPri != null && curPri != null) && (avgPri >= curPri)) {
            log.info("cur price is lower than 10 days avg cls price, set to sell mode.");
            return true;
        }
        log.info("cur price is not lower then 10 days avg cls price, do not set to sell mode.");
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
		log.info("10 days cls avg price can not be adjusted");
		return true;
	}
}
