package com.sn.trade.strategy.selector.stock;

import org.apache.log4j.Logger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.stock.Stock;
import com.sn.trade.strategy.imp.STConstants;

public class CurPriLostSellModeSelector implements IStockSelector {

    static Logger log = Logger.getLogger(CurPriLostSellModeSelector.class);
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
    	Double ytclspri = s.getYtClsPri();
    	Double curPri = s.getCur_pri();
    	Double opnPri = s.getOpen_pri();
    	double lostPct = 0.0;
    	
    	if (ytclspri != null && curPri != null && opnPri != null && ytclspri > 0) {
    		lostPct = (curPri - opnPri)/ ytclspri;
    		log.info("got lost:" + lostPct);
    	}
        if (lostPct < STConstants.MAX_LOST_PCT_FOR_SELL_MODE) {
            log.info("cur price is lost:" + lostPct + " which is over " + STConstants.MAX_LOST_PCT_FOR_SELL_MODE + " yt_cls_pri, set to sell mode.");
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
    @Override
    public Integer getTradeModeId() {
        // TODO Auto-generated method stub
        return null;
    }
	@Override
	public boolean shouldStockExitTrade(String s) {
		// TODO Auto-generated method stub
		return false;
	}
}
