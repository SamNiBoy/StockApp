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
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.imp.TradeStrategyImp;

public class ClosePriceTrendStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(ClosePriceTrendStockSelector.class);
    int days = 60;
	double curPctLowLvl = 0.1;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
    	Double maxYtClsPri = s.getMaxDlyTdClsPri(days);
    	Double minYtClsPri = s.getMinDlyTdClsPri(days);
    	Double curPri = s.getCur_pri();
    	
    	if (maxYtClsPri == null || minYtClsPri == null || curPri == null) {
    		log.info("ClosePriceTrendStockSelector return false because null max/min price.");
    		return false;
    	}
    	
    	double maxPct = (maxYtClsPri - minYtClsPri) / minYtClsPri;
    	double curPct = (curPri - minYtClsPri) / minYtClsPri;
    	
    	log.info("maxPct:" + maxPct + " curPct:" + curPct);
    	
        if (curPct <= maxPct * curPctLowLvl) {
            log.info("Now, price plused by " + maxPct + " pct, and curPri is 1/2 close to minYtClsPri. return true.");
            return true;
        }
        log.info("returned false for isGoodStock()");
        return false;
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
		log.info("curPctLowLvl:" + curPctLowLvl);
		if (harder) {
			days++;
			if (days >= 90) {
				days = 90;
			}
			curPctLowLvl = curPctLowLvl - 0.01;
			if (curPctLowLvl <= 0.01) {
				curPctLowLvl = 0.01;
			}
		}
		else {
			curPctLowLvl = curPctLowLvl + 0.01;
			days--;
			if (days < 20) {
				days = 20;
			}
			curPctLowLvl = curPctLowLvl + 0.01;
			if (curPctLowLvl >= 0.2) {
				curPctLowLvl = 0.2;
			}
		}
		log.info("after try " + (harder ? " harder" : " loose"));
		log.info("curPctLowLvl:" + curPctLowLvl);
		return true;
	}
    @Override
    public Integer getTradeModeId() {
        // TODO Auto-generated method stub
        return STConstants.TRADE_MODE_ID_MANUAL;
    }
}
