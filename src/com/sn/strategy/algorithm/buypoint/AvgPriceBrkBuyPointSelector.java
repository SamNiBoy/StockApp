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

public class AvgPriceBrkBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(AvgPriceBrkBuyPointSelector.class);
	
    private boolean sim_mode;
    private String selector_name = "AvgPriceBrkBuyPointSelector";
    private String selector_comment = "";
    
    private ThreadLocal<Double> avgpri5 = new ThreadLocal<Double>();
    private ThreadLocal<Double> avgpri10 = new ThreadLocal<Double>();
    private ThreadLocal<Double> avgpri30 = new ThreadLocal<Double>();
    private ThreadLocal<Double> td_open_pri = new ThreadLocal<Double>();
    private ThreadLocal<Double> td_cls_pri = new ThreadLocal<Double>();
    private ThreadLocal<Double> td_high = new ThreadLocal<Double>();
    private ThreadLocal<Double> td_low = new ThreadLocal<Double>();
    
    public AvgPriceBrkBuyPointSelector(boolean sm)
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
        
        SellModeWatchDog.processInHandStockModeSetup(stk.getDl_dt().toString().substring(0, 10));
//        int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING", stk.getID());
//        int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING", stk.getID());
//        
//        if ((hour * 100 + minutes) >= (hour_for_balance * 100 + mins_for_balance))
//        {
//            if (sbs == null || (sbs != null && sbs.is_buy_point))
//            {
//                log.info("Hour:" + hour + ", Minute:" + minutes);
//                log.info("Close to market shutdown time, no need to break balance");
//                return false;
//            }
//        }
        
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
        
        double threshPct = 0.01;
        
        boolean con1_0 = getAvgPriceFromSina(stk, ac, 0) && ((td_cls_pri.get() - avgpri5.get()) / td_cls_pri.get() > threshPct);
        boolean con1_1 = avgpri5.get() > avgpri10.get() && avgpri10.get() > avgpri30.get();
        boolean con1_2 = td_cls_pri.get() > td_open_pri.get();
        boolean con1_3 = avgpri5.get() <= td_high.get() && avgpri5.get() >= td_low.get();
        
        boolean con1 = con1_0 && con1_1 && con1_2 && con1_3;
        
        if (!con1) {
        	log.info("stock:" + stk.getID() + " con1 false.");
        	return false;
        }
        boolean con2 = getAvgPriceFromSina(stk, ac, 1) && ((avgpri5.get() - td_cls_pri.get()) / td_cls_pri.get() > threshPct);
        
        if (!con2) {
        	log.info("stock:" + stk.getID() + " con2 false.");
        	return false;
        }
        
        boolean con3 = getAvgPriceFromSina(stk, ac, 2) && ((avgpri5.get() - td_cls_pri.get()) / td_cls_pri.get() > threshPct);
        
        if (!con3) {
        	log.info("stock:" + stk.getID() + " con3 false.");
        	return false;
        }
        
        boolean con4 = getAvgPriceFromSina(stk, ac, 3) && ((avgpri5.get() - td_cls_pri.get()) / td_cls_pri.get() > threshPct);
        
        if (!con4) {
        	log.info("stock:" + stk.getID() + " con4 false.");
        	return false;
        }
        
        if (con1 && con2 && con3 && con4)
        {
		    stk.setTradedBySelector(this.selector_name);
		    stk.setTradedBySelectorComment("past 3 days lower than 5 days avipri, now bigger than 5 days avgpri, buy!");
		    return true;
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
