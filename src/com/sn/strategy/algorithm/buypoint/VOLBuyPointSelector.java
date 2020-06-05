package com.sn.strategy.algorithm.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.STConstants;
import com.sn.strategy.TradeStrategyImp;
import com.sn.strategy.algorithm.IBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.DefaultBuyPointSelector;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.sellmode.SellModeWatchDog;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trader.StockTrader;
import com.sn.util.StockDataProcess;
import com.sn.util.VOLPRICEHISTRO;

public class VOLBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(VOLBuyPointSelector.class);
	
    private boolean sim_mode;
    private String selector_name = "VOLBuyPointSelector";
    private String selector_comment = "";
    
    
    public VOLBuyPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {
        
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        Double maxPri = stk.getMaxCurPri();
        Double minPri = stk.getMinCurPri();
        Double yt_cls_pri = stk.getYtClsPri();
        Double cur_pri = stk.getCur_pri();
        
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
		double marketDegree = StockMarket.getDegree(stk.getDl_dt());
		
//		VOLPRICEHISTRO v1 = StockDataProcess.getPriceVolHistro(stk, "", 1, 5, 0);
//		
//		if (v1.min_pri > cur_pri) {
//			log.info("skip buy as cur_pri:" + cur_pri + " is less then min_pri of thick line1:" + v1.min_pri);
//			return false;
//		}
		
//		double baseThresh = ParamManager.getFloatParam("BUY_BASE_TRADE_THRESH", "TRADING", stk.getID());
//		
//		if (sbs != null && !sbs.is_buy_point) {
//			if ((stk.getCur_pri() - sbs.price) / yt_cls_pri > baseThresh / 3) {
//			    log.info("previous sold at price:" + sbs.price + ", now price:" + stk.getCur_pri() + "(stk.getCur_pri() - sbs.price) / yt_cls_pri > baseThresh / 3 ? " + ((stk.getCur_pri() - sbs.price) / yt_cls_pri > baseThresh / 3) + ", buy it back");
//                stk.setTradedBySelector(this.selector_name);
//                stk.setTradedBySelectorComment("previous sold at price:" + sbs.price + ", now price:" + stk.getCur_pri() + "(stk.getCur_pri() - sbs.price) / yt_cls_pri > baseThresh / 3 ? " + ((stk.getCur_pri() - sbs.price) / yt_cls_pri > baseThresh / 3) + ", buy it back");
//			    return true;
//			}
//		}
		
//		if (marketDegree > 1.0) {
//			if (sbs != null && !sbs.is_buy_point) {
//			    log.info("MarketDegree is 1% increase, we have sold unbalance, buy it back.");
//                stk.setTradedBySelector(this.selector_name);
//                stk.setTradedBySelectorComment("MarketDegree is " + marketDegree + "% increase, we have sold unbalance, buy it back.");
//			    return true;
//			}
//			else {
//				log.info("MarketDegree is 1% increase, stop buy.");
//				return false;
//			}
//		}
//		else if (marketDegree < -1.0) {
//			log.info("MarketDegree is -1% decrease, no buy.");
//			return false;
//		}
		


        Timestamp t1 = stk.getDl_dt();
        
        long hour = t1.getHours();
        long minutes = t1.getMinutes();
        
        
        int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING", stk.getID());
        int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING", stk.getID());
        
        if ((hour * 100 + minutes) >= (hour_for_balance * 100 + mins_for_balance))
        {
            if (sbs == null || (sbs != null && sbs.is_buy_point))
            {
                log.info("Hour:" + hour + ", Minute:" + minutes);
                log.info("Close to market shutdown time, no need to break balance");
                return false;
            }
        }
        else if (hour == 9 && minutes < 40) {
        	log.info("do not buy before 9:40");
        	return false;
        }
        
        boolean csd = SellModeWatchDog.isStockInStopTradeMode(stk);
        
        if (csd == true && (sbs == null || sbs.is_buy_point))
        {
            log.info("Stock:" + stk.getID() + " is in sell mode and in balance/bought, no need to break balance.");
            return false;
        }
    

		double margin_pct = ParamManager.getFloatParam("MARGIN_PCT_TO_TRADE_THRESH", "TRADING", stk.getID());

		if ((ac != null && !ac.hasStockInHand(stk)) || ac == null) {
            

			if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {
				
				// If we sold before with higher price, use it as maxPri.
				if (sbs != null && !sbs.is_buy_point && sbs.price > maxPri) {
					log.info("stock:" + sbs.id + " sold with price:" + sbs.price + " which is higher than:" + maxPri + ", use it as maxPri.");
					maxPri = sbs.price;
				}
				
				double maxPct = (maxPri - minPri) / yt_cls_pri;
				double curPct =(cur_pri - minPri) / yt_cls_pri;

				//boolean qtyPlused = stk.isLstQtyPlused();
				boolean con2 = stk.isLstQtyOnTopN();
				
				log.info("maxPct:" + maxPct + ", curPct:" + curPct + ", isLstQtyOnTopN:" + con2);
				
				if (curPct < maxPct * margin_pct && con2) {
					log.info("VOL is on topN list, and price is on bottom margin.");
                    
					stk.setTradedBySelector(this.selector_name);
					stk.setTradedBySelectorComment("VOL is on topN list, and price is on bottom margin.");
					return true;
				}
				else if (stk.isLstPriceGoUp() && con2) {

					log.info("VOL is on topN list, and price is going up.");
					stk.setTradedBySelector(this.selector_name);
					stk.setTradedBySelectorComment("VOL is on topN list, and price is going up.");
				}
			} else {
				log.info("isGoodBuyPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
			}
		}
		return false;
	}
	
	@Override
	public int getBuyQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        double useableMny = 0;
        int buyMnt = 0;
        int maxMnt = 0;
        
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(s.getID());
	    if (sbs != null && !sbs.is_buy_point)
	    {
	    	buyMnt = sbs.quantity;
	    	log.info("stock:" + s.getID() + " with qty:" + sbs.quantity + " already, buy same back");
	    }
	    else if (ac != null) {
            useableMny = ac.getMaxMnyForTrade();
            maxMnt = (int)(useableMny/s.getCur_pri()) / 100 * 100;
            
           	buyMnt = maxMnt;
            log.info("getBuyQty, useableMny:" + useableMny + " buyMnt:" + buyMnt + " maxMnt:" + maxMnt);
        }
		return buyMnt;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
    

}
