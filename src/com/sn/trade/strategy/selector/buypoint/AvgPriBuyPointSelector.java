package com.sn.trade.strategy.selector.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.imp.TradeStrategyGenerator;
import com.sn.trade.strategy.imp.TradeStrategyImp;
import com.sn.trade.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.trade.strategy.selector.stock.AvgPriStockSelector;
import com.sn.work.task.SellModeWatchDog;

public class AvgPriBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(AvgPriBuyPointSelector.class);
	
	@Override
	public boolean isGoodBuyPoint(Stock stk, ICashAccount ac) {

	    if (!TradeStrategyImp.tradeRecord.get(stk.getID()).isEmpty()) {
	        log.info("Stock:" + stk.getID() + " already traded today, not buy again!");
	        return false;
	    }
	    
	    AvgPriStockSelector selector = new AvgPriStockSelector();
	    if (selector.isTargetStock(stk, ac)) {
	        log.info("Stock:" + stk.getID() + " matched AvgPriStockSelector and no trade so far, now bug it once!");
	        return true;
	    }
	    else {
	        log.info("Stock:" + stk.getID() + " NOT matched AvgPriStockSelector, NOT bug it!");
		    return false;
	    }
	}
	
	@Override
	public int getBuyQty(Stock s, ICashAccount ac) {
		// TODO Auto-generated method stub
        double useableMny = 0;
        int buyMnt = 0;
        int maxMnt = 0;
        
        if (ac != null) {
            useableMny = ac.getMaxAvaMny();
            maxMnt = (int)(useableMny/s.getCur_pri()) / 100 * 100;
            
            if (maxMnt >= 400) {
            	buyMnt = maxMnt / 2;
            	buyMnt = buyMnt - buyMnt % 100;
            }
            else {
            	buyMnt = maxMnt;
            }
            log.info("getBuyQty, useableMny:" + useableMny + " buyMnt:" + buyMnt + " maxMnt:" + maxMnt);
        }
        else {
        	if (s.getCur_pri() <= 10) {
        		buyMnt = 200;
        	}
        	else {
        		buyMnt = 100;
        	}
        	log.info("getBuyQty, cur_pri:" + s.getCur_pri() + " buyMnt:" + buyMnt);
        }
		return buyMnt;
	}

    @Override
    public boolean matchTradeModeId(Stock s) {
        // TODO Auto-generated method stub
        Integer trade_mode_id = s.getTrade_mode_id();
        log.info("stock:" + s.getID() + " trade mode id:" + trade_mode_id);
        if (trade_mode_id != null && trade_mode_id == STConstants.TRADE_MODE_ID_AVGPRI) {
            log.info("trade mode matched, continue");
            return true;
        }
        log.info("trade mode does not matched, continue");
        return false;
    }
    

}
