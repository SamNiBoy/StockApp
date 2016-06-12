package com.sn.trade.strategy.selector.stock;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.stock.Stock;
import com.sn.work.task.SuggestStock;

public class BadTradeSellModeSelector implements IStockSelector {

    static Logger log = Logger.getLogger(BadTradeSellModeSelector.class);
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
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
