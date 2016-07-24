package com.sn.trade.strategy.selector.stock;

import org.apache.log4j.Logger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.stock.Stock;
import com.sn.trade.strategy.imp.STConstants;

public class AvgPriStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(AvgPriStockSelector.class);
    
    static int SHORT_DAYS = 5;
    static int LONG_DAYS = 10;
    static int SHIFT_DAYS = 3;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
//        boolean Up5Days = false;
//        boolean Cross10Days = false;
//        Double avgPri1 = s.getAvgTDClsPri(5, 0);
//        Double avgPri2 = s.getAvgTDClsPri(5, 1);
//        Double curPri = s.getCur_pri();
//        if ((avgPri1 != null && curPri != null && avgPri2 != null) && (avgPri1 + 0.01 < curPri) && (avgPri2 + 0.01 < avgPri1)) {
//            log.info("cur price is up 5 days avg cls price");
//            Up5Days = true;
//        }
//        
//        Double avg10Pri1 = s.getAvgTDClsPri(10, 0);
//        Double avg10Pri2 = s.getAvgTDClsPri(10, 1);
//        
//        if ((avg10Pri1 != null && avg10Pri2 != null) && (avg10Pri1 + 0.01 < avgPri1) && (avg10Pri2 > avgPri2 + 0.01)) {
//            log.info("cur 5 avgPrice is crossover 10 days avg cls price");
//            Cross10Days = true;
//        }
//        
//        if (Cross10Days && Up5Days) {
//            log.info("cur price is up 5 days avg cls price, and 5 avgCls Price is crossover 10 days avgprice, select stock for trade.");
//            return true;
//        }
//        else {
//            log.info("cur price is NOT up 5 days avg cls price, and 5 avgCls Price is crossover 10 days avgprice, NOT select stock for trade.");
//            return false;
//        }
        int shrtDays = SHORT_DAYS;
        int lngDays = LONG_DAYS;
        int shftDays = SHIFT_DAYS;
        if (s.isTDClsPriGoldenCross(shrtDays, lngDays, shftDays)) {
            log.info("stock:" + s.getID() + " is gloden across, AvgPriStockSelector return true");
            return true;
        }
        log.info("stock:" + s.getID() + " is NOT gloden across, AvgPriStockSelector return false");
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
        return true;
    }
    @Override
    public boolean adjustCriteria(boolean harder) {
        // TODO Auto-generated method stub
        if (harder) {
            SHORT_DAYS++;
            LONG_DAYS++;
            SHIFT_DAYS++;
            
            if (SHORT_DAYS > 10) {
                SHORT_DAYS = 10;
            }
            if (LONG_DAYS > 30) {
                LONG_DAYS = 30;
            }
            if (SHIFT_DAYS > 7) {
                SHIFT_DAYS = 7;
            }
        }
        else {
            SHORT_DAYS--;
            LONG_DAYS--;
            SHIFT_DAYS--;
            
            if (SHORT_DAYS < 3) {
                SHORT_DAYS = 3;
            }
            if (LONG_DAYS < 7) {
                LONG_DAYS = 7;
            }
            if (SHIFT_DAYS <= 0) {
            	SHIFT_DAYS = 1;
            }
        }
        
        log.info("adjustCriteria: SHORT_DAYS:" + SHORT_DAYS + ", LONG_DAYS:" + LONG_DAYS + ", SHIFT_DAYS:" + SHIFT_DAYS);
        return true;
    }
    @Override
    public Integer getTradeModeId() {
        // TODO Auto-generated method stub
        return STConstants.TRADE_MODE_ID_AVGPRI;
    }
}
