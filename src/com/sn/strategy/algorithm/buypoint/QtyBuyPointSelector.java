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

public class QtyBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(QtyBuyPointSelector.class);
	
    private boolean sim_mode;
    private String selector_name = "QtyBuyPointSelector";
    private String selector_comment = "";
    
    
    public QtyBuyPointSelector(boolean sm)
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
        
        double pct = (cur_pri - yt_cls_pri) / yt_cls_pri;
        
        double stop_trade_for_max_pct = ParamManager.getFloatParam("STOP_BREAK_BALANCE_IF_CURPRI_REACHED_PCT", "TRADING", stk.getID());
        
        log.info("check stock:" + stk.getID() + " reached no trading margin:" + stop_trade_for_max_pct + " with actual pct:" + pct);
        if (Math.abs(pct) >= stop_trade_for_max_pct && (sbs == null || sbs.is_buy_point))
        //if (Math.abs(pct) >= stop_trade_for_max_pct)
        {
           log.info("Stock:" + stk.getID() + " cur_pri:" + stk.getCur_pri() + " ytClsPri:" + stk.getYtClsPri() +", increase pct:" + pct
                   + " is exceeding " + stop_trade_for_max_pct + " stop trading to increase unbalance.");
            return false;
        }
        
        
        boolean csd = SellModeWatchDog.isStockInStopTradeMode(stk);
        
        if (csd == true && (sbs == null || sbs.is_buy_point))
        {
            log.info("Stock:" + stk.getID() + " is in sell mode and in balance/bought, no need to break balance.");
            return false;
        }
    

		double tradeThresh = 0;
		double margin_pct = ParamManager.getFloatParam("MARGIN_PCT_TO_TRADE_THRESH", "TRADING", stk.getID());

     	tradeThresh = getBuyThreshValueByDegree(marketDegree, stk);
         
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
				boolean priceTurnedAround = stk.priceUpAfterSharpedDown(2);
				
				log.info("maxPct:" + maxPct + ", tradeThresh:" + tradeThresh + ", curPct:" + curPct + ", priceTurnedAround:" + priceTurnedAround);
				
				if (maxPct >= tradeThresh && curPct < maxPct * margin_pct && priceTurnedAround) {
					log.info("isGoodBuyPoint true says Check Buy:" + stk.getDl_dt() + " stock:" + stk.getID()
							+ " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " curPri:" + cur_pri + " margin_pct:" + margin_pct);
                    
					stk.setTradedBySelector(this.selector_name);
					stk.setTradedBySelectorComment("Price range:[" + minPri + ", " + maxPri + "] /" + yt_cls_pri + " > tradeThresh:" + tradeThresh + " and in margin pct:" + margin_pct + " also priceTurnedAround:" + priceTurnedAround);
					return true;
				}
//				else {
//		            if (sbs != null && !sbs.is_buy_point) {
//		                curPct = (sbs.price - cur_pri) / yt_cls_pri;
//		                if (curPct >= tradeThresh) {
//		                    log.info("We have sold unbalance, price reached tradeThresh:" + tradeThresh + ", buy it.");
//		                    stk.setTradedBySelector(this.selector_name);
//		                    stk.setTradedBySelectorComment("cur_pri < pre_sold_pri:[" + cur_pri + "," + sbs.price + "], curPct:" + curPct + " > tradeTresh:" + tradeThresh);
//		                    return true;
//		                }
//		            }
//				}
			} else {
				log.info("isGoodBuyPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
			}
		}
		return false;
	}
	
    public double getBuyThreshValueByDegree(double Degree, Stock2 stk) {
    	
    	double baseThresh = ParamManager.getFloatParam("BUY_BASE_TRADE_THRESH", "TRADING", stk.getID());
    	
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
