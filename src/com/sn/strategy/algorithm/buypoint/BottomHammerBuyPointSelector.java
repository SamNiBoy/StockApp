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
import com.sn.task.suggest.selector.BottomHammerSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trader.StockTrader;

public class BottomHammerBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(BottomHammerBuyPointSelector.class);
	
    private boolean sim_mode;
    private String selector_name = "BottomHammerBuyPointSelector";
    private String selector_comment = "";
    
    
    public BottomHammerBuyPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {
        
        Timestamp t1 = stk.getDl_dt();
        
        long hour = t1.getHours();
        long minutes = t1.getMinutes();
        
        if (hour != 14 || minutes < 55) {
        	log.info("we only start to buy after 14:55, return false");
        	return false;
        }
        
		Double yt_cls_pri = stk.getYtClsPri();
		Double cur_pri = stk.getCur_pri();
		
		if ((yt_cls_pri - cur_pri) / yt_cls_pri > 0.01) {
			log.info("skip buy as we got price lost today...");
			return false;
		}
		
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
        
        if (sbs != null && sbs.is_buy_point) {
        	log.info("we only buy once for stock:" + stk.getID()+ " it already bought at:" + sbs.dl_dt.toString() + " with qty:" + sbs.quantity);
        	return false;
        }
        
        BottomHammerSelector bhs = new BottomHammerSelector(t1.toString().substring(0, 10));
        bhs.setMinValues(0.8, 7);
        
        if (bhs.isTargetStock(stk, ac)) {
        	log.info("but stock:" + stk.getID() + " as it matched bottom hammer shape.");
			stk.setTradedBySelector(this.selector_name);
			stk.setTradedBySelectorComment("but stock:" + stk.getID() + " as it matched bottom hammer shape at time:" + stk.getDl_dt().toString());
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
