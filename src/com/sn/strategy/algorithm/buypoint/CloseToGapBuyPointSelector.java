package com.sn.strategy.algorithm.buypoint;

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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONArray;

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

public class CloseToGapBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(CloseToGapBuyPointSelector.class);
	
    private boolean sim_mode;
    private String selector_name = "CloseToGapBuyPointSelector";
    private String selector_comment = "";
    
    private ThreadLocal<Double> avgpri5 = new ThreadLocal<Double>();
    private ThreadLocal<Double> avgpri10 = new ThreadLocal<Double>();
    private ThreadLocal<Double> avgpri30 = new ThreadLocal<Double>();
    private ThreadLocal<Double> td_open_pri = new ThreadLocal<Double>();
    private ThreadLocal<Double> td_cls_pri = new ThreadLocal<Double>();
    private ThreadLocal<Double> td_high = new ThreadLocal<Double>();
    private ThreadLocal<Double> td_low = new ThreadLocal<Double>();
    
    private ThreadLocal<Integer> jump_cnt = new ThreadLocal<Integer>();
    private ThreadLocal<String> jump_cnt_for_date = new ThreadLocal<String>();
    
    public CloseToGapBuyPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {
        
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
        Timestamp t1 = stk.getDl_dt();
        
        long hour = t1.getHours();
        long minutes = t1.getMinutes();
        
        boolean csd = SellModeWatchDog.isStockInStopTradeMode(stk);
        
        if (csd == true && (sbs == null || sbs.is_buy_point))
        {
            log.info("Stock:" + stk.getID() + " is in sell mode and in balance/bought, no need to break balance.");
            return false;
        }
        
        boolean snf = SellModeWatchDog.isStockInSellNowMode(stk);
        
        if (snf) {
        	log.info("stock:" + stk.getID() + " is in sell now mode, can not buy!");
		    return false;
        }
        
        if (TradeStrategyImp.checkBuyBackTempSoldStock(stk)) {
		    stk.setTradedBySelector(this.selector_name);
		    stk.setTradedBySelectorComment("buy back stock from sellnowstock table.");
		    return true;
        }
        else if (TradeStrategyImp.checkOtherBuyBackTempSoldStock()) {
        	log.info("has other sellnowrecord pending for buy back, skip buy.");
        	return false;
        }
        
        boolean con0 = getAvgPriceFromSina(stk, ac, 0);
        
        double avgpri1 = avgpri30.get();
        
//        boolean c2 = getAvgPriceFromSina(stk, ac, 1);
//        
//        double avgpri2 = avgpri30.get();
        
        if (!(con0 && avgpri10.get() > avgpri30.get())) {
        	log.info("avg price is not good, skip buy.");
        	return false;
        }
        
        boolean con1 = checkBottomPriceReached(stk, 7, 2);
        if (con1)
        {
		    stk.setTradedBySelector(this.selector_name);
		    stk.setTradedBySelectorComment("Price shapped a 7 days bottom and reached gap, buy");
		    return true;
        }
        
        boolean con3 = checkCloseToLatestGap(stk);
        if (con3)
        {
		    stk.setTradedBySelector(this.selector_name);
		    stk.setTradedBySelectorComment("A price gap found, buy");
		    return true;
        }
        


		return false;
	}
	
    private boolean checkCloseToLatestGap(Stock2 s) {
    	
    	boolean gotDataSuccess = false;
    	
    	try {
    		
    		if (!checkCloseToLatestGapDB(s))
    		{
    			gotDataSuccess = false;
    		}
    		else {
    			gotDataSuccess = true;
    		}
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    	}
        return gotDataSuccess;
    }
    
    private boolean checkCloseToLatestGapDB(Stock2 s) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean gotDataSuccess = false;
    	double previous_high = -1;
    	double current_low = -1;
    	double close1 = 0;
    	double open1 = 0;
    	
    	double close2 = 0;
    	String on_dte = "";
    	
//    	if (checkJumpCountForTheDay(s)) {
//    		log.info("Did not pass mininum jump count check, skip buy.");
//    		return false;
//    	}
    	boolean onlyLookGapYesterday = true;
    	
    	try {
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    	    String sql = "select * from stkAvgPri where id = '" + s.getID() + "' and add_dt < '" + s.getDl_dt().toString().substring(0, 10)
    	    		+ "' order by add_dt desc";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		int cnt = (onlyLookGapYesterday ? 2 : 100000);
    		
    		while (rs.next() && cnt > 0) {
    			
    			cnt--;
    			
    			if (current_low == -1) {
    				current_low = rs.getDouble("low");
    				close1 = rs.getDouble("close");
    				open1 = rs.getDouble("open");
    				continue;
    			}
    			else {
    				previous_high = rs.getDouble("high");
    				close2 = rs.getDouble("close");
    				on_dte = rs.getString("add_dt");
    			}
    			
    			//not only gap, but also with raise K.
    			if ((current_low - previous_high) > 0 && close1 > open1 && (close1 - close2) / close2 < 0.03) {
    				gotDataSuccess = true;
    				break;
    			}
    			else {
    				current_low = rs.getDouble("low");
    			}
    		}
    		
    		rs.close();
    		stm.close();
    		
    		if (gotDataSuccess)
    		{
    			log.info("found gap price success, previous_high:" + previous_high + ", current_low:" + current_low + "close1:" +close1 + ", open1:" + open1 + " on date:" + on_dte);
    			
    			sql = "select 'x' from stkAvgPri where id = '" + s.getID() + "' and add_dt > '" + on_dte + "' and add_dt < '" + s.getDl_dt().toString().substring(0, 10) + "' and close <= " + previous_high;
    			
    			log.info(sql);
    			
    			stm = con.createStatement();
    			rs = stm.executeQuery(sql);
    			
    			//no date with low price lower than previous_high.
    			if (!rs.next()) {
    				gotDataSuccess = true;
    				
    				double cur_pri = s.getCur_pri();
    				double pct = (cur_pri - current_low) / current_low;
    				log.info("validated the gap is a valid gap, now check cur_pri close to it, cur_pri:" + cur_pri + " vs previous_high:" + previous_high + " with pct:" + pct);
    				
    				if (cur_pri >= current_low && pct < 0.01) {
    					log.info("great, passed all test!");
    					gotDataSuccess = true;
    				}
    				else {
    					log.info("did not pass all test.");
    					gotDataSuccess = false;
    				}
    			}
    			else {
    				log.info("not a vaid gap.");
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
    
    double getPreviousGapTopPrice (Stock2 s) {

    	
    	Connection con = DBManager.getConnection();
    	
    	boolean gotDataSuccess = false;
    	double previous_high = -1;
    	double current_low = -1;
    	double close1 = 0;
    	double open1 = 0;
    	
    	double close2 = 0;
    	
    	double targetPrice = -1;
    	String on_dte = "";
    	
    	boolean onlyLookGapYesterday = false;
    	
    	try {
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    	    String sql = "select * from stkAvgPri where id = '" + s.getID() + "' and add_dt < '" + s.getDl_dt().toString().substring(0, 10)
    	    		+ "' order by add_dt desc";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		int cnt = (onlyLookGapYesterday ? 2 : 100000);
    		
    		while (rs.next() && cnt > 0) {
    			
    			cnt--;
    			
    			if (current_low == -1) {
    				current_low = rs.getDouble("low");
    				close1 = rs.getDouble("close");
    				open1 = rs.getDouble("open");
    				continue;
    			}
    			else {
    				previous_high = rs.getDouble("high");
    				close2 = rs.getDouble("close");
    				on_dte = rs.getString("add_dt");
    			}
    			
    			//not only gap, but also with raise K.
    			if ((current_low - previous_high) > 0) {
    				gotDataSuccess = true;
    				break;
    			}
    			else {
    				current_low = rs.getDouble("low");
    			}
    		}
    		
    		rs.close();
    		stm.close();
    		
    		if (gotDataSuccess)
    		{
    			log.info("found gap price success, previous_high:" + previous_high + ", current_low:" + current_low + "close1:" +close1 + ", open1:" + open1 + " on date:" + on_dte);
    			
    			sql = "select 'x' from stkAvgPri where id = '" + s.getID() + "' and add_dt > '" + on_dte + "' and add_dt < '" + s.getDl_dt().toString().substring(0, 10) + "' and low <= " + previous_high +
    				  " union select 'x' from stkAvgPri s where s.id = '" + s.getID() + "' and s.add_dt < '" + on_dte + "' and s.high >= " + current_low;
    			
    			log.info(sql);
    			
    			stm = con.createStatement();
    			rs = stm.executeQuery(sql);
    			
    			//no date with low price lower than previous_high.
    			if (!rs.next()) {
    				gotDataSuccess = true;
    				
    				log.info("great, validated the gap is a valid gap, now check cur_pri close to it vs previous_high:" + previous_high);
    				
    				targetPrice = previous_high;
    			}
    			else {
    				log.info("not a vaid gap.");
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
    	return targetPrice;
    
    }
    private boolean checkBottomPriceReached(Stock2 s, int numDays, int numSamePri) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean gotDataSuccess = false;
    	double preLowPri = -1;
    	double curLowPri = -1;
    	double open = 0;
    	double high = 0;
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
    		
    	    boolean stopProcess = false;
    	    
    	    double min = 100000;
    	    double max = 0;
    	    double other_close = 0;
    		
    		while (rs.next() && cnt < numDays) {
    			
    			cnt++;
    			
    			if (max < rs.getDouble("high")) {
    				max = rs.getDouble("high");
    			}
    			
    			if (min > rs.getDouble("low")) {
    				min = rs.getDouble("low");
    			}
    			
    			if (cnt <= numSamePri) {
    				
    				log.info("check step:" + cnt + " if low price is close enough.");
    				
    				if (preLowPri < 0) {
    					preLowPri = rs.getDouble("low");
    					curLowPri = rs.getDouble("low");
    					open = rs.getDouble("open");
    					close = rs.getDouble("close");
    					high = rs.getDouble("high");
    					on_dte = rs.getString("add_dt");
    					
    					//first day must be a raise K.
    					if (close < open) {
    						log.info("stock:" + s.getID() + " on date:" + on_dte + " is not a raise K, skip.");
    						stopProcess = true;
    						break;
    					}
    					continue;
    				}
    				else {
    					curLowPri = rs.getDouble("low");
    					
    					//both low prices must be 5 cents close.
    					if (Math.abs(preLowPri - curLowPri) / curLowPri > 0.005) {
    						log.info("curLowPri:" + curLowPri + ", preLowPri:" + preLowPri + " gap is more than 5 cents, skip.");
    						stopProcess = true;
    						break;
    					}
    					preLowPri = curLowPri;
    				}
    			}
    			
    			if (cnt <= numSamePri) {
    				continue;
    			}
    			
    			//next check rest of low prices are higher than first numSamePri of days.
                double other_low = rs.getDouble("low");
                other_close = rs.getDouble("close");
                on_dte = rs.getString("add_dt");
                
                log.info("check stock:" + s.getID() + " on date:" + on_dte + ", bottom price:" + preLowPri + " is less than other_low:" + other_low);
                if (preLowPri < other_low) {
                	continue;
                }
                else {
                	log.info("check stock:" + s.getID() + " on date:" + on_dte + " bottom price check failed.");
                	stopProcess = true;
                	break;
                }
    		}
    		
    		rs.close();
    		stm.close();
    		
    		if (cnt == numDays && !stopProcess)
    		{
    			
    			double pregap = getPreviousGapTopPrice(s);
    			
    			if (pregap > 0 && preLowPri >= pregap && (preLowPri - pregap) / pregap <= 0.03)
    			{
    				gotDataSuccess = true;
    			}
//    			double shakepct = (max - min) / min;
//    			
//    			if (shakepct < 0.2 || (close - min) / min > 0.3 * shakepct || (other_close - min) / min < 0.7 * shakepct) {
//    			    log.info("shakepct:" + shakepct + " is less than 20%, min:" + min + ", max:" + max + " or close is not in 30% bottom shaking range:" + (close - min) / min +
//    			    		" or last close pricce is not on top 70% of shaking range.");
//    			    gotDataSuccess = false;
//    			}
//    			else {
//    			    log.info("stock:" + s.getID() + " on date:" + on_dte + " passed bottom shape check!");
//    			    gotDataSuccess = true;
//    			}
    		}
    		else {
    			gotDataSuccess = false;
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
    
    private boolean checkJumpCountForTheDay(Stock2 s) {
    	
    	int minJumpCount = 70;
    	String on_dte = s.getDl_dt().toString().substring(0, 10);
    	
    	String dte = jump_cnt_for_date.get();
    	
    	Integer jumpcnt = null;
    	
    	if (dte != null && dte.length() > 0 && dte.equals(on_dte))
    	{
    		jumpcnt = jump_cnt.get();
    	    if (jumpcnt!= null) {
    	    	
    	    	if (jumpcnt < minJumpCount) {
    	    		log.info("not enough jump count:" + jumpcnt + ", on date:" + on_dte + ", skip buy.");
    	    	    return false;
    	    	}
    	    	else {
    	    		log.info("enough jump count:" + jumpcnt + ", on date:" + on_dte + ", skip buy.");
    	    		return true;
    	    	}
    	    }
    	}
    	//now get jumpcnt from db.
    	Connection con = DBManager.getConnection();
    	
    	boolean gotDataSuccess = false;
    	Integer jc = 0;
    	try {
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    	    String sql = "select count(*) cnt" + 
    	    		      " from stkavgpri s1 " + 
    	    		      " join stkavgpri s2 " + 
    	    		      " on s1.id = s2.id " + 
    	    		      " and s2.add_dt = (select max(add_dt) from stkavgpri s3 where s3.id = s1.id and s3.add_dt < s1.add_dt)" + 
    	    		      " where (s1.low - s2.high) / s2.high > 0.0 " +
    	    		      "   and s1.add_dt = '" + on_dte + "'";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		if (rs.next()) {
    			jc = rs.getInt("cnt");
    			log.info("calculate jump count:" + jc + " for date:" + on_dte);
    			jump_cnt.set(jc);
    			jump_cnt_for_date.set(on_dte);
    		}
    		
    		rs.close();
    		stm.close();
    		
    		return jc >= minJumpCount;
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
	    	int cnt = TradeStrategyImp.getBuySellCount(s.getID(), s.getDl_dt().toString().substring(0, 10), true);
	    	
	    	cnt++;
	    	
            useableMny = ac.getMaxMnyForTrade();
            //maxMnt = (int)(useableMny * cnt/s.getCur_pri()) / 100 * 100;
            maxMnt = (int)(useableMny /s.getCur_pri()) / 100 * 100;
            
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
