package com.sn.trade.strategy.selector.stock;

import org.apache.log4j.Logger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.stock.Stock;

public class AvgClsPriSellModeSelector implements IStockSelector {

    static Logger log = Logger.getLogger(AvgClsPriSellModeSelector.class);
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
        boolean Cross10Days = false;
        boolean Cross20Days = false;
    	Double avgPri1 = s.getAvgYtClsPri(10, 0);
    	Double avgPri2 = s.getAvgYtClsPri(10, 1);
    	Double curPri = s.getCur_pri();
        if ((avgPri1 != null && curPri != null && avgPri2 != null) && (avgPri1 >= curPri) && (avgPri2 <= curPri)) {
            log.info("cur price is crossover 10 days avg cls price");
            Cross10Days = true;
        }
        
    	avgPri1 = s.getAvgYtClsPri(20, 0);
    	avgPri2 = s.getAvgYtClsPri(20, 1);
    	
        if ((avgPri1 != null && curPri != null && avgPri2 != null) && (avgPri1 >= curPri) && (avgPri2 <= curPri)) {
            log.info("cur price is crossover 20 days avg cls price");
            Cross20Days = true;
        }
        
        if (Cross10Days && Cross20Days) {
            log.info("cur price is crossover 10 & 20 days avg cls price, set to sell mode.");
            return true;
        }
        else {
            log.info("cur price is not crossover 10 & 20 days avg cls price, do not set to sell mode.");
            return false;
        }
        
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
		log.info("10/20 days cls avg price can not be adjusted");
		return true;
	}
}
