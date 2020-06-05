package com.sn.strategy.algorithm.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.STConstants;
import com.sn.strategy.TradeStrategyImp;
import com.sn.strategy.algorithm.ISellPointSelector;
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

public class VOLSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(VOLSellPointSelector.class);

    private String selector_name = "VOLSellPointSelector";
    private String selector_comment = "";
    
    private boolean sim_mode;
    
    
    public VOLSellPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock2 stk, ICashAccount ac) {

		Double maxPri = stk.getMaxCurPri();
		Double minPri = stk.getMinCurPri();
		Double yt_cls_pri = stk.getYtClsPri();
		Double cur_pri = stk.getCur_pri();
        
//		VOLPRICEHISTRO v1 = StockDataProcess.getPriceVolHistro(stk, "", 1, 5, 0);
//		
//		if (v1.max_pri < cur_pri) {
//			log.info("skip sell as cur_pri:" + cur_pri + " is great than max_pri of thick line1:" + v1.max_pri);
//			return false;
//		}
		
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
        double marketDegree = StockMarket.getDegree(stk.getDl_dt());
        
        if (sbs == null || !sbs.is_buy_point) {
        	log.info("only sell stock which was bought yesterday.");
        	return false;
        }
        
        Timestamp t1 = stk.getDl_dt();
        Timestamp t0 = sbs.dl_dt;
        
        log.info("Check if stock:" + stk.getID() + " was bought in past:" + t0.toString() + " at time:" + t1.toString());
        
        long millisec = t1.getTime() - t0.getTime();
        long hrs = millisec / (1000*60*60);
        int can_sell_same_day = ParamManager.getIntParam("CAN_SELL_SAME_DAY", "TRADING", stk.getID());
        
        if (hrs <= 12 && can_sell_same_day != 1) {
        	log.info("can not sell stock which is bought at same day, return false.");
        	return false;
        }

        long hour = t1.getHours();
        long minutes = t1.getMinutes();
        
        int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING", stk.getID());
        int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING", stk.getID());
        if ((hour * 100 + minutes) >= (hour_for_balance * 100 + mins_for_balance))
        {
            if (sbs == null || (sbs != null && !sbs.is_buy_point))
            {
                log.info("Hour:" + hour + ", Minute:" + minutes);
                log.info("Close to market shutdown time, no need to break balance");
                return false;
            }
        }
        
        boolean csd = SellModeWatchDog.isStockInStopTradeMode(stk);
        
        if (csd == true && (sbs == null || !sbs.is_buy_point))
        {
            log.info("Stock:" + stk.getID() + " is in stop trade mode and in balance/sold, no need to break balance.");
            return false;
        }
		
        double margin_pct = ParamManager.getFloatParam("MARGIN_PCT_TO_TRADE_THRESH", "TRADING", stk.getID());
		if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {

			// If we bought before with lower price, use it as minPri.
			if (sbs != null && sbs.is_buy_point && sbs.price < minPri) {
				log.info("stock:" + sbs.id + " bought with price:" + sbs.price + " which is lower than:" + minPri + ", use it as minPri.");
				minPri = sbs.price;
			}
			
			double maxPct = (maxPri - minPri) / yt_cls_pri;
			double curPct = (cur_pri - minPri) / yt_cls_pri;
			
			boolean con1 = curPct > maxPct * (1 - margin_pct);
			boolean con2 = stk.isLstQtyOnTopN();
			
			log.info("maxPct:" + maxPct + ", curPct:" + curPct + ", isLstQtyOnTopN:" + con2);
			
			if (con1 && con2) {
				
				log.info("VOL is on topN list, and price is on top margin.");
                stk.setTradedBySelector(this.selector_name);
                stk.setTradedBySelectorComment("VOL is on topN list, and price is on top margin.");
				return true;
			}
			else if (stk.isLstPriceGoDown() && con2) {
				
				log.info("VOL is on topN list, and price is going down.");
				stk.setTradedBySelector(this.selector_name);
                stk.setTradedBySelectorComment("VOL is on topN list, and price is going down.");
			}
		} else {
			log.info("isGoodSellPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
		}
		return false;
	}
	

	@Override
	public int getSellQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        int sellMnt = 0;
        if (ac != null) {
            int sellableAmt = (int) (ac.getMaxMnyForTrade() / s.getCur_pri());
            sellMnt =  sellableAmt - sellableAmt % 100;
            
            if (!sim_mode)
            {
                  int tradeLocal = ParamManager.getIntParam("TRADING_AT_LOCAL", "TRADING", null);
                  if (tradeLocal == 1)
                  {
                      log.info("Trade at local sellable amount is zero, user max mny per trade to get the qty:" + sellMnt);
                  }
                  else
                  {
                       sellableAmt = ac.getSellableAmt(s.getID(), null);
                       if (sellMnt > sellableAmt)
                       {
                            log.info("Tradex sellable amount:" + sellableAmt + " less than calculated amt:" + sellMnt + " use sellabeAmt.");
                            sellMnt = sellableAmt;
                       }
                  }
             }
             log.info("getSellQty, sellableAmt:" + sellableAmt + " sellMnt:" + sellMnt);
        }
        
        int sellableAmnt = TradeStrategyImp.getSellableMntForStockOnDate(s.getID(), s.getDl_dt());
        
	    if (sellableAmnt > 0)
	    {
	   	    sellMnt = sellableAmnt;
	    }
	    log.info("stock:" + s.getID() + ", calculated sellMnt:" + sellMnt);
		return sellMnt;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
