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

            boolean con6 = con3 && (td_cls_pri1 - td_cls_pri2) / td_cls_pri1 <= -0.055;
            
            if (con1) {
                if (con2)
                {
    	    	    stk.setTradedBySelector(this.selector_name);
    	    	    stk.setTradedBySelectorComment("yt_cls_pri 10 pct lower than 5 days avgpri, sell!");
    	    	    return true;
                }
                else if (con6) {
    	    	    stk.setTradedBySelector(this.selector_name);
    	    	    stk.setTradedBySelectorComment("close price at least 5.5 pct drop, sell!");
    	    	    return true;
                }
                
                //we do sell if raise 3 days only when there was big drop before bought and bought more than 3 days.
                if (stockBoughtMoreThanDays(stk, sbs, 3) && stockHadBigDropOrIncBefore(stk, sbs)) {
                	
                    boolean con4 = getAvgPriceFromSina(stk, ac, 2);
                    double td_cls_pri3 = td_cls_pri.get();
                    
                    boolean con7 = con3 && con4 && td_cls_pri3 < td_cls_pri2 && td_cls_pri2 < td_cls_pri1 && td_cls_pri1 < stk.getCur_pri();
                    
                    if (con7) {
    	    	        stk.setTradedBySelector(this.selector_name);
    	    	        stk.setTradedBySelectorComment("Win 3 days, sell!");
    	    	        return true;
                    }
                }
                
                if (checkClosePriceLostDays(stk, 2)) {
    	    	    stk.setTradedBySelector(this.selector_name);
    	    	    stk.setTradedBySelectorComment("Lost 2 days, sell!");
    	    	    return true;
                }
            }
        }
       
       //if yt_close_price close the gap, and it is not reaching a bottom support line.
        if (checkPriceBrkLatestGapDB(stk) && !reachedBottomLine(stk, sbs, 5, 2)) {
   		   log.info("Stock:" + stk.getID() + " has broken previous price gap, sell it.");
   	       stk.setTradedBySelector(this.selector_name);
   	       stk.setTradedBySelectorComment("Previous gap broken, sell!");
   	       return true;
       }
        
//       double lostPct = (stk.getCur_pri() - sbs.price) / sbs.price;
//       if (lostPct < -0.03) {
//		   log.info("Stock:" + stk.getID() + " has broken bottom price 3 pct, sell it.");
//	       stk.setTradedBySelector(this.selector_name);
//	       stk.setTradedBySelectorComment("bottom buy price broken, sell!");
//	       return true;
//       }
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
	
	private boolean stockHadBigDropOrIncBefore(Stock2 s, StockBuySellEntry sbs) {
		
		Connection con = DBManager.getConnection();
		
		boolean bigRisk = false;
		int daysToChk = 7;
    	try {
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    	    String sql = "select close, add_dt from stkAvgPri where id = '" + s.getID() + "' and add_dt < '" + sbs.dl_dt.toString().substring(0, 10)
    	    		+ "' order by add_dt desc";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		int lostCnt = 0;
    		int conWinCnt = 0;
    		
    		double pre_close = -1;
    		while (rs.next() && daysToChk > 0) {
    			
    			daysToChk--;
    			
    			double curClose = rs.getDouble("close");
    			String on_dte = rs.getString("add_dt");
    			
    			if (pre_close == -1) {
    				pre_close = curClose;
    				continue;
    			}
    			
    			double pct = (pre_close - curClose) / pre_close;
    			
    			log.info("check stock:" + s.getID() + " pre_close:" + pre_close + ", cur close:" + curClose + " for date:" + on_dte + " with pct:" + pct);
    			
    			if (pct <= -0.05) {
    				log.info("more than 5 pct drop, big drop.");
    				bigRisk = true;
    				break;
    			}
    			else if (pct <= -0.01){
    				conWinCnt = 0;
    				lostCnt++;
    			}
    			else if (pct > 0.01) {
    				lostCnt = 0;
    				conWinCnt++;
    			}
    			else {
    				lostCnt = 0;
    				conWinCnt = 0;
    			}
    			
    			if (lostCnt >= 3 || conWinCnt >= 3)
    				break;
    			
    			pre_close = curClose;
    		}
    		
    		if (lostCnt >= 3 || conWinCnt >= 3) {
    			log.info("in previous 7 days, continue lost or win at lest 3 times.");
    			bigRisk = true;
    		}
    		
    		rs.close();
    		stm.close();
    		
    		log.info("stock:" + s.getID() + " price:" + s.getCur_pri() + " had big risk? " + bigRisk);
    		
    		return bigRisk;
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
    	return bigRisk;
	}
	
	private boolean stockBoughtMoreThanDays(Stock2 s, StockBuySellEntry sbs, int daysToChk) {
		
		Connection con = DBManager.getConnection();
    	try {
    		
    		int dayCnt = 0;
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    	    String sql = "select count(*) dayCnt from stkAvgPri where id = '" + s.getID() + "' and add_dt < '" + s.getDl_dt().toString().substring(0, 10)
    	    		+ "' and add_dt >= '" + sbs.dl_dt.toString().substring(0, 10) + "' ";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		if (rs.next()) {
    			dayCnt = rs.getInt("dayCnt");
    		}
    		
    		rs.close();
    		stm.close();
    		
    		log.info("stock:" + s.getID() + " bought with " + dayCnt + " days.");
    		
    		return dayCnt >= daysToChk;
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
    
private boolean checkClosePriceLostDays(Stock2 s, int daysChk) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean gotDataSuccess = false;
    	double highest = 0;
    	double close = 0;
    	String on_dte = "";

    	try {
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    	    String sql = "select * from stkAvgPri where id = '" + s.getID() + "' and add_dt < '" + s.getDl_dt().toString().substring(0, 10)
    	    		+ "' order by add_dt desc";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		int cnt = 0;
    		int loopLimit = daysChk + 1;
    		while (rs.next()) {
    			
    			cnt++;
    			
    			if (highest == 0) {
    				highest = rs.getDouble("close");
        			on_dte = rs.getString("add_dt");
        			continue;
    			}
    			else {
    				close = rs.getDouble("close");
    			}
    			
    			if (highest < close && cnt < loopLimit) {
    				highest = close;
    			}
    			
    			log.info("stock:" + s.getID() + " daysChk:" + daysChk + " on date:" + on_dte + ", highest:" + highest + ", last close:" + close);
    			
    			if (cnt == loopLimit && highest < close) {
    				log.info("stock:" + s.getID() + " daysChk:" + daysChk + " lost check success, on date:" + on_dte);
    				gotDataSuccess = true;
    				break;
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
    	double yt_cls_pri = 0;
    	double yt_open_pri = 0;

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
    				yt_cls_pri = rs.getDouble("close");
    				yt_open_pri = rs.getDouble("open");
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
    		
//    		double open_pri = s.getOpen_pri();
//    		if (gotDataSuccess && previous_high >= open_pri) {
//    			log.info("open_pri broken the gap already, open_pri:" + open_pri + " < previous_high:" + previous_high);
//    			gotDataSuccess = true;
//    		}
    		if (gotDataSuccess)
    		{
    			log.info("found gap price success, previous_high:" + previous_high + ", current_low:" + current_low + " on date:" + on_dte);
    			
    			if (yt_cls_pri < previous_high) {
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
    				log.info("stock:" + s.getID() + " on date:" + on_dte + " yt_cls_pri:" + yt_cls_pri + " is not lower than previous_high:" + previous_high + " do not consider gap broken.");
    				gotDataSuccess = false;
    			}
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
    
    private boolean reachedBottomLine(Stock2 s, StockBuySellEntry sbs, int chkDayNum, int cfmNum) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean passChk = false;
    	double previous_low = -1;
    	double current_low = -1;
    	double current_close = -1;
    	String on_dte = "";
    	
    	int dayNum = chkDayNum;

    	try {
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    	    String sql = "select * from stkAvgPri where id = '" + s.getID() + "' and add_dt < '" + s.getDl_dt().toString().substring(0, 10)
    	    		+ "' order by add_dt desc";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		int alignedCnt = 0;
    		double lowestPri = 100000;
    		
    		while (rs.next() && dayNum > 0) {
    			
    			dayNum--;
    			
    			if (current_low == -1) {
    				current_low = rs.getDouble("low");
    				current_close = rs.getDouble("close");
    				on_dte = rs.getString("add_dt");
    				lowestPri = current_low;
    				alignedCnt = 1;
    				continue;
    			}
    			else {
    				previous_low = rs.getDouble("low");
    			}
    			
    			log.info("previous_low:" + previous_low + ", lowestPri:" + lowestPri + " check for stock:" + s.getID() + ", on_dte:" + on_dte);
    			if ((previous_low - lowestPri) / lowestPri < -0.005) {
    				lowestPri = previous_low;
    				alignedCnt = 1;
    			}
    			else if (Math.abs(previous_low - lowestPri) / lowestPri < 0.005) {
    				alignedCnt++;
    			}
    		}
    		
    		if ((current_close - lowestPri) / lowestPri > 0.03) {
    			log.info(" last day close price is 3 pct away from bottom line, skip alignment check.");
    			alignedCnt = 0;
    		}
    		
    		log.info("In past " + chkDayNum + " days, has " + alignedCnt + " times aligned for date:" + on_dte);
    		if (dayNum == 0 && alignedCnt >= cfmNum) {
    			passChk = true;
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
    	return passChk;
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
