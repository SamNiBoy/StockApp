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

public class CurPriLostSellModeSelector implements IStockSelector {

    static Logger log = Logger.getLogger(CurPriLostSellModeSelector.class);
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	Double ytclspri = s.getYtClsPri();
    	Double curPri = s.getCur_pri();
    	double lostPct = 0.0;
    	
    	if (ytclspri != null && curPri != null && ytclspri > 0) {
    		lostPct = (curPri - ytclspri)/ ytclspri;
    		log.info("got lost:" + lostPct);
    	}
        if (lostPct < -0.05) {
            log.info("cur price is lost:" + lostPct + " which is over 5% yt_cls_pri, set to sell mode.");
            return true;
        }
        
        log.info("cur price is not lost 5% yt_cls_pri,  do not set to sell mode.");
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
		log.info("CurPriLostSellModeSelector can not be adjusted");
		return true;
	}
}
