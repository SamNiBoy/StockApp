package com.sn.trade.strategy.selector.sellpoint;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.stock.Stock;
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.selector.stock.TopGainStockSelector;

public class IncStopSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(IncStopSellPointSelector.class);

    public static boolean matchIncStopMode(Stock s){

	    TopGainStockSelector selector = new TopGainStockSelector();
	    if (selector.isTargetStock(s, null)) {
	        log.info("TopGainStockSelector determined the stock is good, return true for matchIncStopMode.");
	        return true;
	    }

	    log.info("IncStopSellPointSelector return false for matchIncStopMode.");
	    return false;
    }
    
    private boolean isGoodTime() {
        LocalDateTime lt = LocalDateTime.now();
        int hr = lt.getHour();
        int mnt = lt.getMinute();
        int timeStm = hr*100 + mnt;
        if (timeStm >= 930 && timeStm <= 1100) {
            log.info("Time is after 9:30 and before 11:00, good time for matchIncStopMode to sell.");
            return true;
        }
        return false;
    }
	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock stk, ICashAccount ac) {

//	    if (!matchIncStopMode(stk)) {
//	        return false;
//	    }
//	    else {
//	        if (!isGoodTime()) {
//	            LocalDateTime lt = LocalDateTime.now();
//	            int hr = lt.getHour();
//	            int mnt = lt.getMinute();
//	            int timeStm = hr*100 + mnt;
//	            //For incStopMode, we only sell before 14:59, buy after 14:30.
//	            if (timeStm >= 1459) {
//	                log.info("good time passed for stock: " + stk.getID() + ", sell it anyway.");
//	                return true;
//	            }
//	        }
//	    }
//	    
//		Double maxPri = stk.getMaxCurPri();
//		Double minPri = stk.getMinCurPri();
//		Double yt_cls_pri = stk.getYtClsPri();
//		Double cur_pri = stk.getCur_pri();
//		double tradeThresh = STConstants.TOP_INCSTOP_BASE_TRADE_THRESH;
//		
//		if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {
//
//			double maxPct = (maxPri - minPri) / yt_cls_pri;
//			double curPct = (cur_pri - minPri) / yt_cls_pri;
//			
//			boolean con1 = maxPct > tradeThresh && curPct > maxPct * 9.0 / 10.0;
//			
//			log.info("Check Sell:" + stk.getDl_dt() + " stock:" + stk.getID() + "yt_cls_pri:" + yt_cls_pri + " maxPri:" + maxPri + " minPri:"
//					+ minPri + " maxPct:" + maxPct + " curPct:" + curPct + " curPri:" + cur_pri + " tradeThresh:" + tradeThresh);
//			log.info("con1 is:" + con1);
//			if (con1) {
//				return true;
//			}
//			log.info("IncStopSellPointSelector common bad sell point.");
//		} else {
//			log.info("isGoodSellPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
//		}
		return false;
	}
	
	@Override
	public int getSellQty(Stock s, ICashAccount ac) {
		// TODO Auto-generated method stub
        int sellMnt = 0;
        
        if (ac != null) {
            String dt = s.getDl_dt().toString().substring(0, 10);
            int sellableAmt = ac.getSellableAmt(s.getID(), dt);
            
            if (sellableAmt >= 400) {
            	sellMnt = sellableAmt / 2;
            	sellMnt = sellMnt - sellMnt % 100;
            }
            else {
            	sellMnt = sellableAmt;
            }
            log.info("getSellQty, sellableAmt:" + sellableAmt + " sellMnt:" + sellMnt);
        }
        else {
        	if (s.getCur_pri() <= 10) {
        		sellMnt = 200;
        	}
        	else {
        		sellMnt = 100;
        	}
        	log.info("getSellQty, cur_pri:" + s.getCur_pri() + " sellMnt:" + sellMnt);
        }
		return sellMnt;
	}

    @Override
    public boolean matchTradeModeId(Stock s) {
        // TODO Auto-generated method stub
        return false;
    }
}
