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

public class TopNMnyBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(TopNMnyBuyPointSelector.class);
	
    private boolean sim_mode;
    private String selector_name = "TopNMnyBuyPointSelector";
    private String selector_comment = "";
    static private ArrayList<String> topNstks = new ArrayList<String>();
    static private ArrayList<Double> topNPcts = new ArrayList<Double>();
    static private ArrayList<Double> topNMnys = new ArrayList<Double>();
    static private String timestamp_for_topN = "";
    
    public TopNMnyBuyPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {
		
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
        
        if (sbs != null && sbs.is_buy_point) {
        	log.info("TopNMnyBuyPointSelector return false for stock:" + stk.getID() + " because it's already bought success.");
        	return false;
        }
        
        Timestamp t0 = stk.getDl_dt();
        
        long hour = t0.getHours();
        long minutes = t0.getMinutes();
        
        if (!(hour == 9 && minutes == 30)) {
        	log.info("only buy at 09:30");
        	return false;
        }
        
        if (isStockInMnyTopN(stk)) {
			stk.setTradedBySelector(this.selector_name);
			stk.setTradedBySelectorComment(this.selector_comment);
            return true;
        }
        return false;
	}
	
    public boolean isStockInMnyTopN(Stock2 stk) {
		int TopN = 10;
		boolean isGoodToBuy = false;
		int idx = 0;
		double pct = 0.0;
		
		if (timestamp_for_topN.equals("") || !timestamp_for_topN.equals(stk.getDl_dt().toString().substring(0,16))) {
			
			timestamp_for_topN = stk.getDl_dt().toString().substring(0,16);
			
    	    try {
		        Connection con = DBManager.getConnection();
		        Statement stm = con.createStatement();
		        String sql =  "select distinct left(s1.dl_dt, 16) mytimestamp, s1.cur_pri, s1.dl_mny_num, s1.dl_stk_num, (s1.cur_pri - s1.yt_cls_pri)/s1.yt_cls_pri  pct, s2.name, s2.id from stkdat2 s1 join stk s2 on s1.id = s2.id " + 
		    			" where left(s1.dl_dt, 16) = '" + stk.getDl_dt().toString().substring(0,16) + "'" +
		    			" order by dl_mny_num desc";
		        log.info(sql);
		        ResultSet rs = stm.executeQuery(sql);
		        
		        while (rs.next()) {
		        	idx++;
		        	
		        	if (idx > TopN) {
		        		break;
		        	}
		        	String id = rs.getString("id");
		        	pct = rs.getDouble("pct");
		        	double dl_mny_num = rs.getDouble("dl_mny_num");
		        	double price = rs.getDouble("cur_pri");
		        	
		        	log.info("Build TopN list, stock:" + id + " on top " + idx + ", at price:" + price + " with pct:" + pct + ", dl_mny_num:" + dl_mny_num);
		        	
		        	topNstks.add(id);
		        	topNPcts.add(pct);
		        	topNMnys.add(dl_mny_num);
		        }
		        rs.close();
		        stm.close();
		        con.close();
	        }
	        catch(Exception e) {
	        	e.printStackTrace();
	        }
		}
		
		idx = 0;
		for(String stkid : topNstks) {
        	if (!stkid.equals(stk.getID())) {
        		idx++;
        		continue;
        	}
        	else {
        		
        		pct = topNPcts.get(idx);
    	    	log.info("Check stock:" + stkid + " on top " + TopN + " list, buy at price:" + stk.getCur_pri() + " with pct:" + pct);
    	    	selector_comment = "Stock:" + stkid + " on top: " + idx + " which is on TopN:" + TopN + " list, buy at price:" + stk.getCur_pri() + " with pct:" + pct;
        		if (pct > 0 && pct <= 0.03) {
        			isGoodToBuy = true;
        			break;
        		}
        	}
		}
    	return isGoodToBuy;
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
