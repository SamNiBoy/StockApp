package com.sn.sim.strategy.selector.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trader.StockTrader;
import com.sn.work.task.SellModeWatchDog;

public class BalanceSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(BalanceSellPointSelector.class);

	private double MAX_MINUTES_ALLOWED= 30;
	private StockBuySellEntry sbs = null;
    
    private boolean sim_mode;
    
    
    public BalanceSellPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock2 stk, ICashAccount ac) {
        Map<String, StockBuySellEntry> lstTrades = StockTrader.getLstTradeForStocks();
        sbs = lstTrades.get(stk.getID());
        
        if (sbs == null || !sbs.is_buy_point)
        {
            log.info("Stock:" + stk.getID() + " did not trade yet or sold already, return false from BlanaceSellPointSelector.");
            return false;
        }
        else {
            Timestamp t0 = sbs.dl_dt;
            Timestamp t1 = stk.getDl_dt();
            
            long millisec = t1.getTime() - t0.getTime();
            long mins = millisec / (1000*60);
            
            log.info("Stock:" + stk.getID() + " bought " + mins + " minutes before");
            
            if (mins > MAX_MINUTES_ALLOWED)
            {
                log.info("Stock:" + stk.getID() + " bought " + mins + " minutes agao, sold it out");
                return true;
            }
        }
        return false;
    }
	
	@Override
	public int getSellQty(Stock2 s, ICashAccount ac) {
	    return (sbs == null ? 0 : sbs.quantity);
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
