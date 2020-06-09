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

public class QtySellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(QtySellPointSelector.class);

    private String selector_name = "QtySellPointSelector";
    private String selector_comment = "";
    private double maxrt = 0;
    private String pre_dte = "";
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
        
//        double sbr = stk.getS1B1Ratio();
//        
//        if (pre_dte.length() == 0 || !pre_dte.equals(stk.getDl_dt().toString().substring(0, 10))) {
//            maxrt = getMaxS1B1RationForPastDays(5, stk);
//        }
//        
//        boolean rtmax = (sbr >= maxrt);
//        
//        log.info("S1B1 Num ration:" + sbr + " > 5 days max B1S1 Num Ratio:" + maxrt + "? " + rtmax);
//        if (rtmax) {
//			stk.setTradedBySelector(this.selector_name);
//			stk.setTradedBySelectorComment("S1B1 Num ration:" + sbr + " > 5 days max B1S1 Num Ratio:" + maxrt + " is " + rtmax);
//			return true;
//        }
//        else if (!rtmax){
//        	return false;
//        }

        long hour = t1.getHours();
        long minutes = t1.getMinutes();
        
        double pct = (cur_pri - yt_cls_pri) / yt_cls_pri;
        
        if (hour == 14 && minutes >= 58) {
        	log.info("sell time check: cur_pri:" + cur_pri + ", yt_cls_pri:" + yt_cls_pri);
        	if (pct < -0.01) {
				stk.setTradedBySelector(this.selector_name);
				stk.setTradedBySelectorComment("sell time check: cur_pri:" + cur_pri + ", yt_cls_pri:" + yt_cls_pri + " passed, pct=" + pct);
				return true;
        	}
        }
        
        
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
			
			boolean con2 = stk.isLstQtyPlused(4) || (hour == 9 && minutes <= 30);
			boolean priceTurnedAround = stk.priceDownAfterSharpedUp(4);
			
			log.info("Check Sell:" + stk.getDl_dt() + " stock:" + stk.getID() + "yt_cls_pri:" + yt_cls_pri + " maxPri:" + maxPri + " minPri:"
					+ minPri + " maxPct:" + maxPct + " curPct:" + curPct + " curPri:" + cur_pri + " tradeThresh:" + tradeThresh + " marginPct:" + (1-margin_pct));
			log.info("price is reaching top margin:" + con1 + " priceTurnedAround is:" + priceTurnedAround + " isLstQtyPlused:" + con2);
			if (con1 && con2 && priceTurnedAround) {
                stk.setTradedBySelector(this.selector_name);
                stk.setTradedBySelectorComment("Price range:[" + minPri + ", " + maxPri + "] /" + yt_cls_pri + " > tradeThresh:" + tradeThresh + " and in margin pct:" + (1 - margin_pct) + " also priceTurnedAround:" + priceTurnedAround);
				return true;
			}
		} else {
			log.info("isGoodSellPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
		}
		return false;
	}
	
    public double getMaxS1B1RationForPastDays(int days_to_check, Stock2 stk) {
    	
        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        double maxrt = 0;
        int i = 0;
        try {
            stm = con.createStatement();
            String sql = "select max(s1_num / b1_num) max_rt, left(s1.dl_dt, 10) dte from stkdat2 s1 "
            		+ "  where left(s1.dl_dt, 10) < '" + stk.getDl_dt().toString().substring(0, 10) + "'"
            		+ "    and s1.id = '" + stk.getID() + "'"
            		+ "  group by left(s1.dl_dt, 1) "
            		+ "  order by dte";
            log.info(sql);
            rs = stm.executeQuery(sql);
            
            double currt = 0;
            while (rs.next() && i < days_to_check) {
            	i++;
                log.info("get ratio:" + rs.getDouble("max_rt") + " at date:" + rs.getString("dte"));
                currt = rs.getDouble("max_rt");
                if (currt > maxrt) {
                	maxrt = currt;
                }
            }
            rs.close();
            stm.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally {
            try {
                con.close();
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
        }
        
        if (i == days_to_check) {
        	log.info("got max ration:" + maxrt + " for past " + days_to_check + " days");
        	pre_dte = stk.getDl_dt().toString().substring(0, 10);
        }
        else if (maxrt < 20000){
        	log.info("only has:" + i + " days data to check, use default 20000.");
        	maxrt = 20000;
        }
    	return maxrt;
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
