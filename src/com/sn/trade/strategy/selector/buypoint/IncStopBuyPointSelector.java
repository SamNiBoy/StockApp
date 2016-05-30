package com.sn.trade.strategy.selector.buypoint;

import java.time.LocalDateTime;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.trade.strategy.selector.stock.TopGainStockSelector;

public class IncStopBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(IncStopBuyPointSelector.class);
	
	private double BASE_TRADE_THRESH = 0.09;

	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {

	    //First make sure the stock is matching the incStop mode.
	    if (!matchIncStopMode(stk)) {
	        return false;
	    }
	    
//		double tradeThresh = BASE_TRADE_THRESH;
//	    Double maxPri = stk.getMaxCurPri();
//	    Double minPri = stk.getMinCurPri();
//	    Double yt_cls_pri = stk.getYtClsPri();
//	    Double cur_pri = stk.getCur_pri();
//        
//	    if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null && !StockMarket.isMarketTooCold(null)) {
//        
//	    	double maxPct = (maxPri - minPri) / yt_cls_pri;
//	    	double curPct =(cur_pri - minPri) / yt_cls_pri;
//        
//	    	boolean qtyPlused = stk.isLstQtyPlused();
//	    	
//	    	log.info("maxPct:" + maxPct + ", tradeThresh:" + tradeThresh + ", curPct:" + curPct + ", isQtyPlused:" + qtyPlused);
//	    	
//	    	//only when the stock is increased to stop, then buy it.
//	    	if (maxPct >= tradeThresh && curPct >= maxPct * 9.0 / 10.0 && stk.isLstQtyPlused()) {
//	    		log.info("IncStopBuyPointSelector isGoodBuyPoint true says Check Buy:" + stk.getDl_dt() + " stock:" + stk.getID()
//	    				+ " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " curPri:" + cur_pri);
//	    		return true;
//	    	}
//	    	else {
//	    		log.info("IncStopBuyPointSelector isGoodBuyPoint false Check Buy:" + stk.getDl_dt() + " stock:" + stk.getID()
//	    				+ " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " curPri:" + cur_pri + " tradeThresh:" + tradeThresh);
//	    	}
//	    } else {
//	    	log.info("IncStopBuyPointSelector isGoodBuyPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, isMarketTooCold is true return false");
//	    }
		return true;
	}
	
	@Override
	public int getBuyQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        double useableMny = 0;
        int buyMnt = 0;
        int maxMnt = 0;
        
        if (ac != null) {
            useableMny = ac.getMaxAvaMny();
            maxMnt = (int)(useableMny/s.getCur_pri()) / 100 * 100;
            
            if (maxMnt >= 400) {
            	buyMnt = maxMnt / 2;
            	buyMnt = buyMnt - buyMnt % 100;
            }
            else {
            	buyMnt = maxMnt;
            }
            log.info("getBuyQty, useableMny:" + useableMny + " buyMnt:" + buyMnt + " maxMnt:" + maxMnt);
        }
        else {
        	if (s.getCur_pri() <= 10) {
        		buyMnt = 200;
        	}
        	else {
        		buyMnt = 100;
        	}
        	log.info("getBuyQty, cur_pri:" + s.getCur_pri() + " buyMnt:" + buyMnt);
        }
		return buyMnt;
	}

	   private boolean matchIncStopMode(Stock2 s){
//	        LocalDateTime lt = LocalDateTime.now();
//	        int hr = lt.getHour();
//	        int mnt = lt.getMinute();
//	        int timeStm = hr*100 + mnt;
//	        if (timeStm < 1430 || timeStm > 1500) {
//	            log.info("matchIncStopMode says false, timeStm: " + timeStm);
//	            return false;
//	        }
//	        log.info("Time is after 14:30 and before 15:00, good time for matchIncStopMode to buy.");
	        
	        TopGainStockSelector selector = new TopGainStockSelector(false);
	        if (selector.isTargetStock(s, null)) {
	            log.info("TopGainStockSelector determined the stock is good, return true for matchIncStopMode to buy.");
	            return true;
	        }
	        log.info("TopGainStockSelector determined the stock is not good, return false for matchIncStopMode.");
	        
	        return false;
	    }
}
