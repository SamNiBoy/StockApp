package com.sn.strategy.algorithm.sellpoint;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.STConstants;
import com.sn.strategy.TradeStrategyImp;
import com.sn.strategy.algorithm.ISellPointSelector;
import com.sn.strategy.algorithm.buypoint.DefaultBuyPointSelector;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.sellmode.SellModeWatchDog;
import com.sn.task.suggest.selector.StddevStockSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trader.StockTrader;
import com.sn.util.StockDataProcess;
import com.sn.util.VOLPRICEHISTRO;

public class CloseToGapSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(CloseToGapSellPointSelector.class);

    private String selector_name = "CloseToGapSellPointSelector";
    private String selector_comment = "";
    private boolean sim_mode;
    
    private ThreadLocal<Double> avgpri5 = new ThreadLocal<Double>();
    private ThreadLocal<Double> avgpri10 = new ThreadLocal<Double>();
    private ThreadLocal<Double> avgpri30 = new ThreadLocal<Double>();
    private ThreadLocal<Double> td_open_pri = new ThreadLocal<Double>();
    private ThreadLocal<Double> td_cls_pri = new ThreadLocal<Double>();
    private ThreadLocal<Double> td_high = new ThreadLocal<Double>();
    private ThreadLocal<Double> td_low = new ThreadLocal<Double>();
    
    public CloseToGapSellPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock2 stk, ICashAccount ac) {

        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
        double marketDegree = StockMarket.getDegree(stk.getDl_dt());
        
        Timestamp t1 = stk.getDl_dt();
        long hour = t1.getHours();
        long minutes = t1.getMinutes();
        
        if (sbs == null || (sbs != null && !sbs.is_buy_point))
        {
            log.info("only sell after buy.");
            return false;
        }
        
        boolean csd = SellModeWatchDog.isStockInStopTradeMode(stk);
        
        if (csd == true && (sbs == null || !sbs.is_buy_point))
        {
            log.info("Stock:" + stk.getID() + " is in stop trade mode and in balance/sold, no need to break balance.");
            return false;
        }
        
        boolean snf = SellModeWatchDog.isStockInSellNowMode(stk);
        
        if (snf) {
		    stk.setTradedBySelector(this.selector_name);
		    stk.setTradedBySelectorComment("stock is in sell now mode, sell!");
		    return true;
        }
        
        //below if else are stop win vs stop lost logic:
       boolean stockWinMost = TradeStrategyImp.isStockWinTopMost(stk, ac, 3);
       if (stockWinMost) {
       	    log.info("think about below criteria for win most stock " + stk.getID());
            double threshPct = 0.1;
            boolean con1 = getAvgPriceFromSina(stk, ac, 0);
            boolean con2 = ((avgpri5.get() - td_cls_pri.get()) / td_cls_pri.get() > threshPct);
            
            double td_cls_pri1 = td_cls_pri.get();
            
            boolean con3 = getAvgPriceFromSina(stk, ac, 1);
            double td_cls_pri2 = td_cls_pri.get();
            
            boolean con4 = con3 && (td_cls_pri1 - td_cls_pri2) / td_cls_pri1 <= -0.055;
            
            if (con1) {
                if (con2)
                {
    	    	    stk.setTradedBySelector(this.selector_name);
    	    	    stk.setTradedBySelectorComment("yt_cls_pri 10 pct lower than 5 days avgpri, sell!");
    	    	    return true;
                }
                else if (con4) {
    	    	    stk.setTradedBySelector(this.selector_name);
    	    	    stk.setTradedBySelectorComment("close price at least 5.5 pct drop, sell!");
    	    	    return true;
                }
                else if (checkClosePriceLostForTwoDays(stk)) {
    	    	    stk.setTradedBySelector(this.selector_name);
    	    	    stk.setTradedBySelectorComment("Lost 2 days, sell!");
    	    	    return true;
                }
            }
        }
        if (checkPriceBrkLatestGapDB(stk)) {
   		   log.info("Stock:" + stk.getID() + " has broken previous price gap, sell it.");
   	       stk.setTradedBySelector(this.selector_name);
   	       stk.setTradedBySelectorComment("Previous gap broken, sell!");
   	       return true;
       }
//       else if (checkPriceDownGap(stk)) {
//   		   log.info("Stock:" + stk.getID() + " found down price gap, sell it.");
//   	       stk.setTradedBySelector(this.selector_name);
//   	       stk.setTradedBySelectorComment("Down price gap, sell!");
//   	       return true;
//       }
        if (TradeStrategyImp.needMakeSpaceForBuy()){
        	log.info("Total buy limit reached, check if stock:" + stk.getID() + " is the most lost stock for which we should sell.");
        	if (TradeStrategyImp.isStockLostMost(stk, ac)) {
        		log.info("Stock:" + stk.getID() + " is the most lost stock, sell it.");
	    	    stk.setTradedBySelector(this.selector_name);
	    	    stk.setTradedBySelectorComment("Most lost stock, sell!");
	    	    return true;
        	}
        }
		return false;
	}
	
    private boolean getAvgPriceFromSina(Stock2 s, ICashAccount ac, int shftDays) {
    	boolean gotDataSuccess = false;
    	
    	try {
    		
    		if (!getAvgPriceFromDb(s, shftDays))
    		{
    			gotDataSuccess = false;
    		}
    		else {
    			gotDataSuccess = true;
    		}
    		
            log.info("stock:" + s.getID() + " got avgpri5:" + avgpri5.get() + ", avgpri10:" + avgpri10.get() + ", avgpri30:" + avgpri30.get() + ", td_cls_pri:" + td_cls_pri.get() + " with shftDays:" + shftDays);
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    	}
        return gotDataSuccess;
    }
    
    private boolean getAvgPriceFromDb(Stock2 s, int shftDays) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean gotDataSuccess = false;
    	try {
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    	    String sql = "select * from stkAvgPri where id = '" + s.getID() + "' and add_dt < '" + s.getDl_dt().toString().substring(0, 10)
    	    		+ "' order by add_dt desc";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		int cnt = shftDays;
    		
    		while(cnt > 0) {
    			rs.next();
    			cnt--;
    		}
    		
    		if (rs.next()) {
                avgpri5.set(rs.getDouble("avgpri1"));
                avgpri10.set(rs.getDouble("avgpri2"));
                avgpri30.set(rs.getDouble("avgpri3"));
                td_open_pri.set(rs.getDouble("open"));
                td_cls_pri.set(rs.getDouble("close"));
                td_high.set(rs.getDouble("high"));
                td_low.set(rs.getDouble("low"));
                gotDataSuccess = true;
    		}
    		
    		rs.close();
    		stm.close();
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    	}
    	finally {
    		try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage(), e);
			}
    	}
    	return gotDataSuccess;
    }
    
private boolean checkClosePriceLostForTwoDays(Stock2 s) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean gotDataSuccess = false;
    	double close1 = 0;
    	double close2 = 0;
    	double close3 = 0;
    	String on_dte = "";
    	

    	try {
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    	    String sql = "select * from stkAvgPri where id = '" + s.getID() + "' and add_dt < '" + s.getDl_dt().toString().substring(0, 10)
    	    		+ "' order by add_dt desc";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		if (rs.next()) {
    			
    			close1 = rs.getDouble("close");
    			on_dte = rs.getString("add_dt");
    			
    		    if (rs.next()) {
    		    	
    		    	close2 = rs.getDouble("close");
    		        if (rs.next()) {
    		        	
    		        	close3 = rs.getDouble("close");
    		            log.info("stock:" + s.getID() + " 3 days close price, close1:" + close1 + ", close2:" + close2 + ", close3:" + close3 + ", on_dte:" + on_dte);
    		            
    		            if (close1 > 0 && close2 > 0 && close3 > 0) {
    		            	
    		            	if (close1 <= close3 && close2 <= close3) {
    		            		log.info("got close price lost 2 days for stock:" + s.getID());
    		            		gotDataSuccess = true;
    		            	}
    		            }
    		        }
    		    }
    		}
    		rs.close();
    		stm.close();
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    	}
    	finally {
    		try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage(), e);
			}
    	}
    	return gotDataSuccess;
    }
    
    private boolean checkPriceBrkLatestGapDB(Stock2 s) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean gotDataSuccess = false;
    	double previous_high = -1;
    	double current_low = -1;
    	String on_dte = "";
    	

    	try {
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    	    String sql = "select * from stkAvgPri where id = '" + s.getID() + "' and add_dt < '" + s.getDl_dt().toString().substring(0, 10)
    	    		+ "' order by add_dt desc";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		while (rs.next()) {
    			
    			if (current_low == -1) {
    				current_low = rs.getDouble("low");
    				continue;
    			}
    			else {
    				previous_high = rs.getDouble("high");
    				on_dte = rs.getString("add_dt");
    			}
    			
    			if (current_low > previous_high) {
    				gotDataSuccess = true;
    				break;
    			}
    			else {
    				current_low = rs.getDouble("low");
    			}
    		}
    		
    		rs.close();
    		stm.close();
    		
    		double open_pri = s.getOpen_pri();
    		if (gotDataSuccess && previous_high >= open_pri) {
    			log.info("open_pri broken the gap already, open_pri:" + open_pri + " < previous_high:" + previous_high);
    			gotDataSuccess = true;
    		}
    		else if (gotDataSuccess)
    		{
    			log.info("found gap price success, previous_high:" + previous_high + ", current_low:" + current_low + " on date:" + on_dte);
    			
    			sql = "select 'x' from stkAvgPri where id = '" + s.getID() + "' and add_dt > '" + on_dte + "' and add_dt < '" + s.getDl_dt().toString().substring(0, 10) + "' and close <= " + previous_high;
    			
    			log.info(sql);
    			
    			stm = con.createStatement();
    			rs = stm.executeQuery(sql);
    			
    			//low price lower than previous_high, breaking true.
    			if (rs.next()) {
    				gotDataSuccess = true;
    			}
    			else {
    				log.info("sell check, still a vaid gap.");
    				gotDataSuccess = false;
    			}
    			
    			rs.close();
    			stm.close();
    		}
    		else {
    			log.info("Did not find any gap price for stock:" + s.getID());
    		}
    		
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    	}
    	finally {
    		try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage(), e);
			}
    	}
    	return gotDataSuccess;
    }
    
    private boolean checkPriceDownGap(Stock2 s) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean gotDataSuccess = false;
    	double previous_low = -1;
    	double current_high = -1;
    	String on_dte = "";
    	try {
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    	    String sql = "select * from stkAvgPri where id = '" + s.getID() + "' and add_dt < '" + s.getDl_dt().toString().substring(0, 10)
    	    		+ "' order by add_dt desc";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		if (rs.next()) {
    			current_high = rs.getDouble("high");
    			if (rs.next()) {
    				previous_low = rs.getDouble("low");
        			on_dte = rs.getString("add_dt");
        			if (previous_low > current_high) {
        				gotDataSuccess = true;
        				log.info("found down price gap, current_high:" + current_high + ", previous_low:" + previous_low + ", on date:" + on_dte);
        			}
    			}
    		}
    		rs.close();
    		stm.close();
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    	}
    	finally {
    		try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage(), e);
			}
    	}
    	return gotDataSuccess;
    }
    
	@Override
	public int getSellQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        int sellMnt = 0;
        if (ac != null) {
        	
	    	int cnt = TradeStrategyImp.getBuySellCount(s.getID(), s.getDl_dt().toString().substring(0, 10), false);
	    	
	    	cnt++;
	    	
            //int sellableAmt = (int) (ac.getMaxMnyForTrade() * cnt/ s.getCur_pri());
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
