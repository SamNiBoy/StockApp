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

public class ClosePriceTrendStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(ClosePriceTrendStockSelector.class);
    /**
     * @param args
     */
    public boolean isGoodStock(Stock2 s, ICashAccount ac) {
    	Double maxYtClsPri = s.getMaxYtClsPri(7);
    	Double minYtClsPri = s.getMinYtClsPri(7);
    	Double curPri = s.getCur_pri();
    	
    	if (maxYtClsPri == null || minYtClsPri == null || curPri == null) {
    		log.info("ClosePriceTrendStockSelector return false because null max/min price.");
    		return false;
    	}
    	
    	double maxPct = (maxYtClsPri - minYtClsPri) / minYtClsPri;
    	double curPct = (curPri - minYtClsPri) / minYtClsPri;
    	
    	log.info("maxPct:" + maxPct + " curPct:" + curPct);
    	
        if (maxPct < 1 && maxPct >= 0.1 && curPct <= maxPct * 0.5) {
            log.info("Now, price plused by " + maxPct + " pct, and curPri is 1/2 close to minYtClsPri. return true.");
            return true;
        }
        else if (maxPct <= 0.05 && curPct >= maxPct * 0.5) {
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
}
