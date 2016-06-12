package com.sn.trade.strategy.selector.stock;

import org.apache.log4j.Logger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.stock.Stock;

public class KeepGainStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(KeepGainStockSelector.class);
    int days = 2;
    double dayPct = 0.09;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
        if (s.getSd().keepDaysClsPriGain(days, dayPct)) {
             log.info("returned true because keep " + days + " days gain " + dayPct);
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
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		log.info("try " + (harder ? " harder" : " loose") + " days:" + days + " dayPct:" + dayPct);
		return false;
	}
}
