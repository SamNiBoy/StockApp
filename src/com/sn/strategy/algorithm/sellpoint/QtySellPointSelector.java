package com.sn.strategy.algorithm.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
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

public class QtySellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(QtySellPointSelector.class);

    private String selector_name = "QtySellPointSelector";
    private String selector_comment = "";
    
    private boolean sim_mode;
    
    
    public QtySellPointSelector(boolean sm)
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
        
		
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
        double marketDegree = StockMarket.getDegree(stk.getDl_dt());
//        double baseThresh = ParamManager.getFloatParam("SELL_BASE_TRADE_THRESH", "TRADING", stk.getID());
//        
//		if (sbs != null && sbs.is_buy_point) {
//			if ((stk.getCur_pri() - sbs.price) / yt_cls_pri < -baseThresh / 3) {
//			    log.info("previous bought at price:" + sbs.price + ", now price:" + stk.getCur_pri() + "(stk.getCur_pri() - sbs.price) / yt_cls_pri < -baseThresh / 3 ?" + ((stk.getCur_pri() - sbs.price) / yt_cls_pri < -baseThresh / 3) + ", sell it out.");
//                stk.setTradedBySelector(this.selector_name);
//                stk.setTradedBySelectorComment("previous bought at price:" + sbs.price + ", now price:" + stk.getCur_pri() + "(stk.getCur_pri() - sbs.price) / yt_cls_pri < -baseThresh / 3 ?" + ((stk.getCur_pri() - sbs.price) / yt_cls_pri < -baseThresh / 3) + ", sell it out.");
//			    return true;
//			}
//		}
		
//		if (marketDegree < -1.0) {
//			if (sbs != null && sbs.is_buy_point) {
//			    log.info("MarketDegree is 1% decrease, we have bought unbalance, sold it out.");
//                stk.setTradedBySelector(this.selector_name);
//                stk.setTradedBySelectorComment("MarketDegree is " + marketDegree + "% decrease, we have bought unbalance, sold it out.");
//			    return true;
//			}
//			else {
//				log.info("MarketDegree is 1% decrease, stop sell.");
//				return false;
//			}
//		}
//		else if (marketDegree > 1.0) {
//			log.info("MarketDegree is 1% increase, no sell.");
//			return false;
//		}

        Timestamp t1 = stk.getDl_dt();
        
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
        
        double pct = (cur_pri - yt_cls_pri) / yt_cls_pri;
        
        double stop_trade_for_max_pct = ParamManager.getFloatParam("STOP_BREAK_BALANCE_IF_CURPRI_REACHED_PCT", "TRADING", stk.getID());
        
        log.info("check stock:" + stk.getID() + " reached no trading margin:" + stop_trade_for_max_pct + " with actual pct:" + pct);
        
        if (Math.abs(pct) >= stop_trade_for_max_pct && (sbs == null || !sbs.is_buy_point))
        {
           log.info("Stock:" + stk.getID() + " cur_pri:" + stk.getCur_pri() + " ytClsPri:" + stk.getYtClsPri() +", increase pct:" + pct
                   + " is exceeding " + stop_trade_for_max_pct + " stop trading to increase unbalance.");
            return false;
        }
        
        boolean csd = SellModeWatchDog.isStockInStopTradeMode(stk);
        
        if (csd == true && (sbs == null || !sbs.is_buy_point))
        {
            log.info("Stock:" + stk.getID() + " is in stop trade mode and in balance/sold, no need to break balance.");
            return false;
        }
		
		double tradeThresh = 0;
        double margin_pct = ParamManager.getFloatParam("MARGIN_PCT_TO_TRADE_THRESH", "TRADING", stk.getID());
		if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {

			// If we bought before with lower price, use it as minPri.
			if (sbs != null && sbs.is_buy_point && sbs.price < minPri) {
				log.info("stock:" + sbs.id + " bought with price:" + sbs.price + " which is lower than:" + minPri + ", use it as minPri.");
				minPri = sbs.price;
			}
			
			tradeThresh = getSellThreshValueByDegree(marketDegree, stk);
			
			double maxPct = (maxPri - minPri) / yt_cls_pri;
			double curPct = (cur_pri - minPri) / yt_cls_pri;
			
			boolean con1 = maxPct > tradeThresh && curPct > maxPct * (1 - margin_pct);
			boolean con2 = stk.isLstQtyPlused(2);
			boolean priceTurnedAround = stk.priceDownAfterSharpedUp(2);
			
			log.info("Check Sell:" + stk.getDl_dt() + " stock:" + stk.getID() + "yt_cls_pri:" + yt_cls_pri + " maxPri:" + maxPri + " minPri:"
					+ minPri + " maxPct:" + maxPct + " curPct:" + curPct + " curPri:" + cur_pri + " tradeThresh:" + tradeThresh + " marginPct:" + (1-margin_pct));
			log.info("price is reaching top margin:" + con1 + " priceTurnedAround is:" + priceTurnedAround + " isLstQtyPlused:" + con2);
			if (con1 && con2 && priceTurnedAround) {
                stk.setTradedBySelector(this.selector_name);
                stk.setTradedBySelectorComment("Price range:[" + minPri + ", " + maxPri + "] /" + yt_cls_pri + " > tradeThresh:" + tradeThresh + " and in margin pct:" + (1 - margin_pct) + " also priceTurnedAround:" + priceTurnedAround);
				return true;
			}
            
//			if (sbs != null && sbs.is_buy_point) {
//			    curPct = (cur_pri - sbs.price) / yt_cls_pri;
//			    if (curPct >= tradeThresh) {
//			        log.info("We have bought unbalance, price reached tradeThresh:" + tradeThresh + ", sell it.");
//                    stk.setTradedBySelector(this.selector_name);
//                    stk.setTradedBySelectorComment("cur_pri > pre_buy_pri:[" + cur_pri + "," + sbs.price + "], curPct:" + curPct + " > tradeTresh:" + tradeThresh);
//                    return true;
//			    }
//			}
			
		} else {
			log.info("isGoodSellPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
		}
		return false;
	}
	
    public double getSellThreshValueByDegree(double Degree, Stock2 stk) {
    	
    	double baseThresh = ParamManager.getFloatParam("SELL_BASE_TRADE_THRESH", "TRADING", stk.getID());
    	
//    	Timestamp tm = stk.getDl_dt();
//        String deadline = null;
//        if (tm == null) {
//        	deadline = "sysdate()";
//        }
//        else {
//        	deadline = "str_to_date('" + stk.getDl_dt().toString() + "', '%Y-%m-%d %H:%i:%s') - interval 1 day ";
//        }
//        
//    	try {
//    		Connection con = DBManager.getConnection();
//    		Statement stm = con.createStatement();
//    		String sql = "select (max(td_hst_pri) - min(td_lst_pri)) / max(yt_cls_pri) / 2.0 yt_shk_hlf_pct "
//    				   + "  from stkdat2 "
//    				   + " where id ='" + stk.getID() + "'"
//    				   + "   and left(dl_dt, 10) = left(" + deadline + ", 10)";
//    		log.info(sql);
//    		ResultSet rs = stm.executeQuery(sql);
//    		if (rs.next()) {
//    			double yt_shk_hlf_pct = rs.getDouble("yt_shk_hlf_pct");
//    			log.info("yt_shk_hlf_pct calculated for stock:" + stk.getID() + " is:" + yt_shk_hlf_pct + " vs baseThresh:" + baseThresh);
//    			if (yt_shk_hlf_pct > baseThresh) {
//    				baseThresh = yt_shk_hlf_pct;
//    			}
//    		}
//    		rs.close();
//    		stm.close();
//    		con.close();
//    	}
//    	catch(Exception e) {
//    		e.printStackTrace();
//    	}
//    	
//    	log.info("Calculate buy thresh value with Degree:" + Degree + ", final baseThresh:" + baseThresh);
    	
    	return baseThresh;
    }

	@Override
	public int getSellQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        int sellMnt = 0;
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(s.getID());
	    if (sbs != null && sbs.is_buy_point)
	    {
	    	sellMnt = sbs.quantity;
	    	log.info("stock:" + s.getID() + " bought qty:" + sbs.quantity + " already, sell same out");
	    }
	    else if (ac != null) {
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
		return sellMnt;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
