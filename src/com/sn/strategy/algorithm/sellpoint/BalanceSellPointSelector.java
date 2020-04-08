package com.sn.strategy.algorithm.sellpoint;

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
import com.sn.STConstants;
import com.sn.strategy.TradeStrategyImp;
import com.sn.strategy.algorithm.ISellPointSelector;
import com.sn.strategy.algorithm.buypoint.DefaultBuyPointSelector;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.sellmode.SellModeWatchDog;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trader.StockTrader;

public class BalanceSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(BalanceSellPointSelector.class);

    
    private boolean sim_mode;
    private String selector_name = "BalanceSellPointSelector";
    private String selector_comment = "";
    
    
    public BalanceSellPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock2 stk, ICashAccount ac) {
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
     	StockBuySellEntry sbs = null;
        sbs = lstTrades.get(stk.getID());
        
        if (sbs == null || !sbs.is_buy_point)
        {
            log.info("Stock:" + stk.getID() + " did not trade yet or sold already, return false from BlanaceSellPointSelector.");
            return false;
        }
        else {
            boolean cleanup_stock_inhand = SellModeWatchDog.isStockInStopTradeMode(stk);
            if (cleanup_stock_inhand)
            {
                log.info("Stock:" + stk.getID() + " switched to stop trade mode(not good for trade), sell up stock in hand, return true");
                stk.setTradedBySelector(this.selector_name);
                stk.setTradedBySelectorComment("Stock:" + stk.getID() + " is in stop trade mode");
                return true;
            }
            else {
                Timestamp t0 = sbs.dl_dt;
                Timestamp t1 = stk.getDl_dt();
                
                long hour = t1.getHours();
                long minutes = t1.getMinutes();
                
                long millisec = t1.getTime() - t0.getTime();
                long mins = millisec / (1000*60);
                
                log.info("Stock:" + stk.getID() + " bought " + mins + " minutes before");
                
                int mins_max = ParamManager.getIntParam("MAX_MINUTES_ALLOWED_TO_KEEP_BALANCE", "TRADING", stk.getID());
                
                if (hour == 13 && minutes == 0)
                {
                    log.info("Market just restarted at 13:00, refresh the timestame for last trade instead of trading.");
                    sbs.dl_dt = stk.getDl_dt();
                    return false;
                }
                else if (mins > mins_max)
                {
                    log.info("Stock:" + stk.getID() + " bought " + mins + " minutes agao, sold it out");
                    stk.setTradedBySelector(this.selector_name);
                    stk.setTradedBySelectorComment("Stock:" + stk.getID() + " bought:" + mins + " minutes ago.");
                    return true;
                }
                
                
                log.info("Hour:" + hour + ", Minute:" + minutes);
                
                int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING", stk.getID());
                int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING", stk.getID());
                
                if (hour >= hour_for_balance && minutes >= mins_for_balance)
                {
                    log.info("Reaching " + hour_for_balance + ":" + mins_for_balance
                             + ", Stock:" + stk.getID() + " bought " + mins + " minutes agao, sell it out");
                    stk.setTradedBySelector(this.selector_name);
                    stk.setTradedBySelectorComment("Stock:" + stk.getID() + " keep balance time:" + hour_for_balance + ":" + mins_for_balance);
                    return true;
                }
            }
        }
        return false;
    }
	
	@Override
	public int getSellQty(Stock2 s, ICashAccount ac) {
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(s.getID());
	    return (sbs == null ? 0 : sbs.quantity);
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
