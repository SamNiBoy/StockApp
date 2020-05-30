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
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trader.StockTrader;

public class BalanceBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(BalanceBuyPointSelector.class);
	
    private String selector_name = "BalanceBuyPointSelector";
    private String selector_comment = "";

	private boolean sim_mode = false;
    
	public BalanceBuyPointSelector(boolean sm)
	{
	    sim_mode = sm;
	}
    
	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {
	    int buyableAmnt = TradeStrategyImp.getBuyableMntForStockOnDate(stk.getID(), stk.getDl_dt());
        
	    if (buyableAmnt <= 0)
	    {
	        log.info("Stock:" + stk.getID() + " did not trade yet or brought already, return false from BlanaceBuyPointSelector.");
            return false;
	    }
	    else {
	        boolean cleanup_stock_inhand = SellModeWatchDog.isStockInStopTradeMode(stk);
            
	        if (cleanup_stock_inhand)
	        {
     	        log.info("Stock:" + stk.getID() + " switched to sell_mode(not good for trade), buy back stock in hand, return true");
                stk.setTradedBySelector(this.selector_name);
                stk.setTradedBySelectorComment("Stock:" + stk.getID() + " is in stop trade mode");
                return true;
	        }
	        else {
	            Timestamp t1 = stk.getDl_dt();
                
                long hour = t1.getHours();
                long minutes = t1.getMinutes();
                
                int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING", stk.getID());
                int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING", stk.getID());
                
                if (hour >= hour_for_balance && minutes >= mins_for_balance)
                {
                    log.info("Hour:" + hour + ", Minute:" + minutes);
                    log.info("Reaching " + hour_for_balance + ":" + mins_for_balance
                             + ", Stock:" + stk.getID() + " sold buyableAmnt:" + buyableAmnt + " minutes agao, buy it back");
                    stk.setTradedBySelector(this.selector_name);
                    stk.setTradedBySelectorComment("Stock:" + stk.getID() + " keep balance time:" + hour_for_balance + ":" + mins_for_balance);
                    return true;
                }
	        }
	    }
        return false;
	}
	
	@Override
	public int getBuyQty(Stock2 s, ICashAccount ac) {
	    return TradeStrategyImp.getBuyableMntForStockOnDate(s.getID(), s.getDl_dt());
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
    

}
