package com.sn.trade.strategy.selector.stock;

import org.apache.log4j.Logger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.stock.Stock;
import com.sn.trade.strategy.imp.STConstants;

public class AvgPriSellModeSelector implements IStockSelector {

    static Logger log = Logger.getLogger(AvgPriSellModeSelector.class);
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
        boolean Down5DaysCross10Days = false;
        Double avgPri1 = s.getAvgYtClsPri(5, 0);
        Double avgPri2 = s.getAvgYtClsPri(5, 1);
        
        Double avg10Pri1 = s.getAvgYtClsPri(10, 0);
        Double avg10Pri2 = s.getAvgYtClsPri(10, 1);
        
        if ((avg10Pri1 != null && avg10Pri2 != null && avgPri1 != null && avgPri2 != null) && (avg10Pri1 > avgPri1 + 0.01) && (0.01 + avg10Pri2 < avgPri2)) {
            log.info("cur 5 avgPrice is crossover 10 days avg cls price");
            Down5DaysCross10Days = true;
        }
        
        if (Down5DaysCross10Days) {
            log.info("cur price is up 5 days avg cls price, and 5 avgCls Price is crossover 10 days avgprice, select stock for trade.");
            return true;
        }
        else {
            log.info("cur price is NOT up 5 days avg cls price, and 5 avgCls Price is crossover 10 days avgprice, NOT select stock for trade.");
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
        return true;
    }
    @Override
    public boolean adjustCriteria(boolean harder) {
        // TODO Auto-generated method stub
        log.info("5/10 days cls avg price can not be adjusted");
        return true;
    }
    @Override
    public Integer getTradeModeId() {
        // TODO Auto-generated method stub
        return null;
    }
}
