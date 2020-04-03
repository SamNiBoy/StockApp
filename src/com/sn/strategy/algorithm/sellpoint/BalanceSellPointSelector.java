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
        Map<String, StockBuySellEntry> lstTrades = (sim_mode ? StockTrader.getSimTrader().getLstTradeForStocks() : StockTrader.getTradexTrader().getLstTradeForStocks());
        sbs = lstTrades.get(stk.getID());
        
        if (sbs == null || !sbs.is_buy_point)
        {
            log.info("Stock:" + stk.getID() + " did not trade yet or sold already, return false from BlanaceSellPointSelector.");
            return false;
        }
        else {
            
            boolean cleanup_stock_inhand = SellModeWatchDog.isStockInSellMode(stk);
            if (cleanup_stock_inhand)
            {
                log.info("Stock:" + stk.getID() + " switched to sell_mode(not good for trade), sell up stock in hand, return true");
                return true;
            }
            else {
                Timestamp t0 = sbs.dl_dt;
                Timestamp t1 = stk.getDl_dt();
                
                long millisec = t1.getTime() - t0.getTime();
                long mins = millisec / (1000*60);
                
                log.info("Stock:" + stk.getID() + " bought " + mins + " minutes before");
                
                int mins_max = ParamManager.getIntParam("MAX_MINUTES_ALLOWED_TO_KEEP_BALANCE", "TRADING");
                if (mins > mins_max)
                {
                    log.info("Stock:" + stk.getID() + " bought " + mins + " minutes agao, sold it out");
                    return true;
                }
                
                long hour = t1.getHours();
                long minutes = t1.getMinutes();
                
                log.info("Hour:" + hour + ", Minute:" + minutes);
                
                int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING");
                int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING");
                
                if (hour >= hour_for_balance && minutes >= mins_for_balance)
                {
                    log.info("Reaching " + hour_for_balance + ":" + mins_for_balance
                             + ", Stock:" + stk.getID() + " bought " + mins + " minutes agao, sell it out");
                    return true;
                }
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
