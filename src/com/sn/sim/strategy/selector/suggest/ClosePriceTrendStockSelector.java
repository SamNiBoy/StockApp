package com.sn.sim.strategy.selector.suggest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.imp.TradeStrategyImp;
import com.sn.sim.strategy.selector.IStockSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class ClosePriceTrendStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(ClosePriceTrendStockSelector.class);
    int days = 7;
	double maxPctUpper = 2;
	double maxPctLower = 0.1;
	double curPctLowLvl = 0.2;
	double curPctUpLvl = 0.8;
	double maxPctLower2 = 0.02;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	Double maxYtClsPri = s.getMaxYtClsPri(days);
    	Double minYtClsPri = s.getMinYtClsPri(days);
    	Double curPri = s.getCur_pri();
    	
    	if (maxYtClsPri == null || minYtClsPri == null || curPri == null) {
    		log.info("ClosePriceTrendStockSelector return false because null max/min price.");
    		return false;
    	}
    	
    	double maxPct = (maxYtClsPri - minYtClsPri) / minYtClsPri;
    	double curPct = (curPri - minYtClsPri) / minYtClsPri;
    	
    	log.info("maxPct:" + maxPct + " curPct:" + curPct);
    	
        if (maxPct < maxPctUpper && maxPct >= maxPctLower && curPct <= maxPct * curPctLowLvl) {
            log.info("Now, price plused by " + maxPct + " pct, and curPri is 1/2 close to minYtClsPri. return true.");
            return true;
        }
        else if (maxPct <= maxPctLower2 && curPct >= maxPct * curPctUpLvl) {
            log.info("Now, price plused by " + maxPct + " pct, and curPri is more than 1/2 close to maxYtClsPri. return true.");
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
		log.info("maxPctUpper:" + maxPctUpper + "maxPctLower:" + maxPctLower + "curPctLowLvl:" + curPctLowLvl + "maxPctLower2:" + maxPctLower2 + "curPctUpLvl:" + curPctUpLvl);
		if (harder) {
			days--;
			if (days < 3) {
				days = 3;
			}
			maxPctUpper = maxPctUpper - 0.2;
			if (maxPctUpper <= 1) {
				maxPctUpper = 1;
			}
			maxPctLower = maxPctLower * 1.1;
			if (maxPctLower >= 0.2) {
				maxPctLower = 0.2;
			}
			curPctLowLvl = curPctLowLvl - 0.01;
			if (curPctLowLvl <= 0.1) {
				curPctLowLvl = 0.1;
			}
			maxPctLower2 = maxPctLower2 * 0.9;
			if (maxPctLower2 <= 0.01) {
				maxPctLower2 = 0.01;
			}
			curPctUpLvl = curPctUpLvl * 0.9;
			if (curPctUpLvl <= 0.6) {
				curPctUpLvl = 0.6;
			}
		}
		else {
			maxPctUpper = maxPctUpper + 0.2;
			maxPctLower = maxPctLower * 0.9;
			curPctLowLvl = curPctLowLvl + 0.01;
			maxPctLower2 = maxPctLower2 * 1.1;
			curPctUpLvl = curPctUpLvl * 0.9;
			days++;
			if (days >= 14) {
				days = 14;
			}
			maxPctUpper = maxPctUpper + 0.2;
			if (maxPctUpper >= 3) {
				maxPctUpper = 3;
			}
			maxPctLower = maxPctLower * 0.9;
			if (maxPctLower <= 0.01) {
				maxPctLower = 0.01;
			}
			curPctLowLvl = curPctLowLvl + 0.01;
			if (curPctLowLvl >= 0.3) {
				curPctLowLvl = 0.3;
			}
			maxPctLower2 = maxPctLower2 * 1.1;
			if (maxPctLower2 >= 0.05) {
				maxPctLower2 = 0.05;
			}
			curPctUpLvl = curPctUpLvl * 1.1;
			if (curPctUpLvl >= 0.9) {
				curPctUpLvl = 0.9;
			}
		}
		log.info("after try " + (harder ? " harder" : " loose"));
		log.info("maxPctUpper:" + maxPctUpper + "maxPctLower:" + maxPctLower + "curPctLowLvl:" + curPctLowLvl + "maxPctLower2:" + maxPctLower2 + "curPctUpLvl:" + curPctUpLvl);
		return true;
	}
}
