package com.sn.strategy.algorithm.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import com.sn.task.suggest.SuggestStock;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trader.StockTrader;

public class DaBanBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(DaBanBuyPointSelector.class);
	
    private boolean sim_mode;
    private String selector_name = "DaBanBuyPointSelector";
    private String selector_comment = "";
    private double dbban_pct = 0.09;
    private double min_pressure_ratio = 5;
    static private ConcurrentHashMap<String, Integer> top_stks = new ConcurrentHashMap<String, Integer>();
    private static int mny_top_n = 200;
    
    
    public DaBanBuyPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {
        
//		if (top_stks.size() == 0) {
//		    synchronized (top_stks) {
//		    	if (top_stks.size() > 0) {
//		    		log.info("Top expensive stocks loaded, skip reload.");
//		    	}
//		    	else {
//		            Connection con = DBManager.getConnection();
//		            Statement stm = null;
//		            ResultSet rs = null;
//		            try {
//		                stm = con.createStatement();
//		                String sql = "select s1.id from stkdat2 s1 "
//		                		+ " join (select max(ft_id) max_ft_id, id from stkdat2 "
//		                		+ "  where left(dl_dt, 10) < '" + stk.getDl_dt().toString().substring(0, 10) + "') t"
//		                	    + "  on s1.id = t.id "
//		                	    + " and s1.ft_id = t.max_ft_id "
//		                	    + "  order by s1.dl_mny_num desc ";
//		                log.info(sql);
//		                rs = stm.executeQuery(sql);
//		                log.info("Now load mny_top_n:" + mny_top_n + " stocks.");
//		                int i = 0;
//		                while (rs.next() && ++i < mny_top_n) {
//		                    log.info("Load stock:" + rs.getString("id") + " at top:" + i);
//		                    top_stks.put(rs.getString("id"), i);
//		                }
//		                rs.close();
//		            }
//		            catch(SQLException e)
//		            {
//		                e.printStackTrace();
//		            }
//		            finally {
//		                try {
//		                    stm.close();
//		                    con.close();
//		                } catch (SQLException e) {
//		                    // TODO Auto-generated catch block
//		                    e.printStackTrace();
//		                    log.info(e.getMessage() + " errored:" + e.getErrorCode());
//		                }
//		            }
//		            log.info("loading mny_top_n " + mny_top_n + " successed!");
//		    	}
//		    }
//		}
//		else {
//			if (top_stks.get(stk.getID()) == null) {
//				log.info("Stock:" + stk.getID() + "is not in top:" + mny_top_n + " list, skip buy it.");
//				return false;
//			}
//		}
		
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        Double yt_cls_pri = stk.getYtClsPri();
        Double cur_pri = stk.getCur_pri();
		Double minPri = stk.getMinCurPri();
		Double maxPri = stk.getMaxCurPri();
        
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
        
        if (sbs != null && sbs.is_buy_point) {
        	log.info("DaBanBuyPointSelector return false for stock:" + stk.getID() + " because it's already daban success.");
        	return false;
        }
        
//        Timestamp t0 = stk.getDl_dt();
//        
//        long hour = t0.getHours();
//        long minutes = t0.getMinutes();
//        
//        if (!(hour >= 14 && minutes >= 30)) {
//        	log.info("only daban after 14:30");
//        	return false;
//        }
//        
//        if ((maxPri - minPri) / yt_cls_pri < 0.05) {
//        	log.info("Expect min/max price must 0.05 pct covered.");
//        	return false;
//        }
        
//        SuggestStock.setOnDte(stk.getDl_dt().toString().substring(0, 10));
//        
//        if (SuggestStock.calculateStockTrend(stk.getID()) <= 0) {
//        	log.info("We only look at stock with trend up!");
//        	return false;
//        }
        
//        if (!stk.isVOLPlused(7, 6.0/7.0)) {
//        	log.info("Stock VOL is not plused for the past 7 days, return false");
//        	return false;
//        }
        
        
        double pct = (cur_pri - yt_cls_pri) / yt_cls_pri;
        
        log.info("check stock:" + stk.getID() + " reached daban pct:" + dbban_pct + " >= actual pct:" + pct + "=" + (pct >= dbban_pct));
        if (pct >= dbban_pct && pct < 0.095)
        {
        	
            long b1_num = stk.getB1_num();
            long s1_num = stk.getS1_num();
        	
            log.info("b1_num:" + b1_num + " / s1_num:" + s1_num + " < min_pressure_ratio:" + min_pressure_ratio + "?" + (b1_num * 1.0 / s1_num < min_pressure_ratio));
            //we prefer there are 5 times biggher buy qty comparing to sell qty.
            if (b1_num * 1.0 / s1_num < min_pressure_ratio) {
            	log.info("b1_num:" + b1_num + " / s1_num:" + s1_num + " < min_pressure_ratio:" + min_pressure_ratio + " do not buy.");
            	return false;
            }
            
           log.info("Stock:" + stk.getID() + " cur_pri:" + stk.getCur_pri() + " ytClsPri:" + stk.getYtClsPri() +", increase pct:" + pct
                   + " is exceeding dbban_pct:" + dbban_pct + ", return true.");
			stk.setTradedBySelector(this.selector_name);
			stk.setTradedBySelectorComment("Stock:" + stk.getID() + " cur_pri:" + stk.getCur_pri() + " ytClsPri:" + stk.getYtClsPri() +", increase pct:" + pct + " is exceeding dbban_pct:" + dbban_pct + ", return true.");
            return true;
        }
		return false;
	}
	
	@Override
	public int getBuyQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        double useableMny = 0;
        int buyMnt = 0;
        int maxMnt = 0;
        
        if (ac != null) {
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
