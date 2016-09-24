package com.sn.work.task;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.mail.reporter.SellModeStockObserverable;
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.selector.stock.AvgClsPriSellModeSelector;
import com.sn.trade.strategy.selector.stock.AvgPriSellModeSelector;
import com.sn.trade.strategy.selector.stock.BadTradeSellModeSelector;
import com.sn.trade.strategy.selector.stock.CurPriLostSellModeSelector;
import com.sn.trade.strategy.selector.stock.DefaultSellModeSelector;
import com.sn.trade.strategy.selector.stock.IStockSelector;
import com.sn.trade.strategy.selector.stock.QtyDisableTradeStockSelector;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class SellModeWatchDog implements IWork {

	/*
	 * Initial delay before executing work.
	 */
	static long initDelay = 0;

	/*
	 * Seconds delay befor executing next work.
	 */
	static long delayBeforNxtStart = 5;

	static TimeUnit tu = TimeUnit.MILLISECONDS;

	static public String resMsg = "Initial msg for work TradeWatchDog.";

	static List<IStockSelector> selectors = new LinkedList<IStockSelector>();

	static SellModeWatchDog self = null;
	
	static List<Stock> stocksSellModeWaitForMail = new LinkedList<Stock>();
	static List<Stock> stocksUnSellModeWaitForMail = new LinkedList<Stock>();
	
	static SellModeStockObserverable rso = new SellModeStockObserverable();

	static Logger log = Logger.getLogger(SellModeWatchDog.class);

	public static String getResMsg() {
		return resMsg;
	}

	public static void setResMsg(String resMsg) {
		SellModeWatchDog.resMsg = resMsg;
	}

	static public boolean start() {
		self = new SellModeWatchDog(0, 5 * 60000);
		if (WorkManager.submitWork(self)) {
			resMsg = "Newly created TradeWatchDog and started!";
			return true;
		}
		return false;
	}

	static public boolean stop() {
		if (WorkManager.cancelWork(self.getWorkName())) {
			resMsg = "TradeWatchDog is cancelled successfully.";
			return true;
		}
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SellModeWatchDog.start();
	}

	public SellModeWatchDog(long id, long dbn) {
		initDelay = id;
		delayBeforNxtStart = dbn;
		//selectors.add(new DefaultSellModeSelector());
		selectors.add(new AvgClsPriSellModeSelector());
		selectors.add(new CurPriLostSellModeSelector());
		selectors.add(new AvgPriSellModeSelector());
		//selectors.add(new BadTradeSellModeSelector());
	}

	public void run() {
		// TODO Auto-generated method stub
		try {
	        Map<String, Stock> stks = StockMarket.getGzstocks();
	        for (String stk : stks.keySet()) {
	        	Stock s = stks.get(stk);
	        	
	        	if (!isStockInSellMode(s) && stockMatchSellMode(s)) {
	        		setStockSellMode(s, true);
	        		stocksSellModeWaitForMail.add(s);
	        	}
	        	else if (isStockInSellMode(s) && stockMatchUnSellMode(s)) {
	        		setStockSellMode(s, false);
	        		stocksUnSellModeWaitForMail.add(s);
	        	}
	       }

	       rso.addStockToSellMode(stocksSellModeWaitForMail);
	       rso.addStockToUnsellMode(stocksUnSellModeWaitForMail);
	       rso.update();
	       stocksSellModeWaitForMail.clear();
	       stocksUnSellModeWaitForMail.clear();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    log.info("TradeWatchDog Now exit!!!");
	}
	
	private boolean stockMatchSellMode(Stock s) {
        boolean suggest_flg = false;

        for (IStockSelector slt : selectors) {
           	if (slt.isMandatoryCriteria() && !slt.isTargetStock(s, null)) {
           		log.info("stockMatchSellMode mandatory criteria not pass, return false.");
           		suggest_flg = false;
           		break;
           	}
           	else {
           		suggest_flg = true;
           	}
        }
        if (suggest_flg) {
        	for (IStockSelector slt : selectors) {
        		if (slt.isMandatoryCriteria()) {
        			continue;
        		}
        		if (slt.isTargetStock(s, null)) {
        			suggest_flg = true;
        			if (slt.isORCriteria()) {
        				log.info("Or criteria matched, suggest the stock:" + s.getID());
        				break;
        			}
        		}
        		else {
        			if (slt.isORCriteria()) {
        				log.info("Or criteria not matched, continue next criteira.");
        				suggest_flg = false;
        				continue;
        			} else {
        				suggest_flg = false;
        				break;
        			}
        		}
        	}
        }
        return suggest_flg;
	}
	 
	private boolean stockMatchUnSellMode(Stock s) {
        boolean suggest_flg = true;

        boolean pre_sell_mode = isStockInSellMode(s);
       for (IStockSelector slt : selectors) {
       	   if (slt.isTargetStock(s, null)) {
       	   	   log.info("unset sell mode return false for non mandatory criteria.");
       	   	   suggest_flg = false;
       	   	   break;
       	   }
       }
       
       if (SuggestStock.shouldStockExitTrade(s.getID())) {
    	   log.info("Stock:" + s.getID() + ":" + s.getName() + " matches shouldStockExitTrade(), can not disable sell mode.");
    	   suggest_flg = false;
       }
       
       //If we are going to disable sell mode, make more safe check.
       if (pre_sell_mode && suggest_flg) {
           Double ytclspri = s.getYtClsPri();
           Double curPri = s.getCur_pri();
           Double opnPri = s.getOpen_pri();
           double incPct = 0.0;
           
           if (ytclspri != null && curPri != null && opnPri != null && ytclspri > 0) {
               incPct = (curPri - opnPri)/ ytclspri;
               log.info("got incPct:" + incPct);
           }
           if (incPct < STConstants.MAX_GAIN_PCT_FOR_DISABLE_SELL_MODE) {
               log.info("cur price is incPct:" + incPct + " which is less " + STConstants.MAX_GAIN_PCT_FOR_DISABLE_SELL_MODE + " yt_cls_pri, not suggest disable sell mode.");
               suggest_flg = false;
           }
       }
        return suggest_flg;
	}
	
	public static boolean isStockInSellMode(Stock s) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		boolean is_in_sell_mode = false;
		try {
			sql = "select sell_mode_flg from usrStk where openID = '" + STConstants.openID + "' and id = '" + s.getID() + "' and suggested_sellmode_by in ('SuggestStock', 'SellModeWatchDog')";
			log.info(sql);
			stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);
			if (rs.next()) {
			    is_in_sell_mode = (rs.getInt("sell_mode_flg") == 1);
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
			log.info("stock " + s.getName() + " is not currently in sell mode!");
		}
		finally {
		    try {
		    	//log.info("Closing statement and connection!");
				stm.close();
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log.info("Stock " + s.getName() + "'s sell mode is:" + is_in_sell_mode);
		return is_in_sell_mode;
	}

	private void setStockSellMode(Stock s, boolean to_sell_mode) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		try {
			sql = "update usrStk set sell_mode_flg = " + (to_sell_mode ?  "1": "0") + ", suggested_sellmode_by = '" + (to_sell_mode ?  "SellModeWatchDog": "") + "'" + " where openID = '" + STConstants.openID + "' and id = '" + s.getID() + "'";
			log.info(sql);
			stm = con.createStatement();
			stm.execute(sql);
			con.commit();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getWorkResult() {
		return "";
	}

	public String getWorkName() {
		return "SellModeWatchDog";
	}

	public long getInitDelay() {
		return initDelay;
	}

	public long getDelayBeforeNxt() {
		return delayBeforNxtStart;
	}

	public TimeUnit getTimeUnit() {
		return tu;
	}

	public boolean isCycleWork() {
		return true;
	}

}
