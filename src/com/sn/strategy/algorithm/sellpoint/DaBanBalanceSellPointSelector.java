package com.sn.strategy.algorithm.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
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

public class DaBanBalanceSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(DaBanBalanceSellPointSelector.class);

    
    private boolean sim_mode;
    private String selector_name = "DaBanBalanceSellPointSelector";
    private String selector_comment = "";
    
    
    public DaBanBalanceSellPointSelector(boolean sm)
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
            log.info("Stock:" + stk.getID() + " did not trade yet or sold already, return false from DaBanBalanceSellPointSelector.");
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
                Timestamp t0 = stk.getDl_dt();
                Timestamp t1 = sbs.dl_dt;
                
                log.info("Check if stock:" + stk.getID() + " was bought in past:" + t1.toString() + " at time:" + t0.toString());
                
                long millisec = t0.getTime() - t1.getTime();
                long hrs = millisec / (1000*60*60);
                
                if (hrs <= 12) {
                	log.info("can not sell stock which is bought at same day, return false.");
                	return false;
                }
                
                double cur_pri = stk.getCur_pri();
                double yt_cls_pri = stk.getYtClsPri();
                
                //If we have price increased to about 0.1 pct limit, make sure s1_num is always 0, otherwise, it is breaking the limit.
                if ((cur_pri - yt_cls_pri) / yt_cls_pri > 0.09) {
                	List<Integer>s1_num_lst = stk.getS1_num_lst();
                	if (s1_num_lst.size() > 2) {
                		long pre_s1_num = s1_num_lst.get(s1_num_lst.size() - 2);
                		long lst_s1_num = s1_num_lst.get(s1_num_lst.size() - 1);
                		
                		log.info("check if stock is breaking price top limit 10% by looking at pre_s1_num:" + pre_s1_num + " = 0 and lst_s1_num:" + lst_s1_num + " > 0 ?");
                		
                		if (pre_s1_num == 0 && lst_s1_num > 0) {
                            stk.setTradedBySelector(this.selector_name);
                            stk.setTradedBySelectorComment("Stock:" + stk.getID() + " upmost price limit is breaking, sell stock.");
                			return true;
                		}
                	}
                }
                
                long hour = t0.getHours();
                long minutes = t0.getMinutes();
                
                log.info("Hour:" + hour + ", Minute:" + minutes);
                
                int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING", stk.getID());
                int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING", stk.getID());
                
                if (hour >= hour_for_balance && minutes >= mins_for_balance)
                {
                    log.info("Reaching " + hour_for_balance + ":" + mins_for_balance
                             + ", Stock:" + stk.getID() + " bought at " + t1.toString() + ", now sell it out");
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
