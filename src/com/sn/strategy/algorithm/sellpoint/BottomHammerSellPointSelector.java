package com.sn.strategy.algorithm.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import com.sn.task.suggest.SuggestStock;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trader.StockTrader;

public class BottomHammerSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(BottomHammerSellPointSelector.class);

    private String selector_name = "BottomHammerSellPointSelector";
    private String selector_comment = "";
    
    private boolean sim_mode;
    
    
    public BottomHammerSellPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock2 stk, ICashAccount ac) {

		Double maxPri = stk.getMaxCurPri();
		Double minPri = stk.getMinCurPri();
		Double yt_cls_pri = stk.getYtClsPri();
		Double cur_pri = stk.getCur_pri();
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
        
        if (sbs == null || !sbs.is_buy_point) {
        	log.info("only sell stock which was bought before.");
        	return false;
        }
        else if ((cur_pri - sbs.price) / yt_cls_pri < - 0.03) {
        	log.info("Cut cost as we lost price.");
            stk.setTradedBySelector(this.selector_name);
            stk.setTradedBySelectorComment("Cut cost as we bought with price:" + sbs.price + " and now price is:" + cur_pri + " which is " + (cur_pri - sbs.price) / yt_cls_pri + " lost");
            return true;
        }
        
        Timestamp t1 = stk.getDl_dt();
        Timestamp t0 = sbs.dl_dt;
        
        log.info("Check if stock:" + stk.getID() + " was bought in past:" + t0.toString() + " at time:" + t1.toString());
        
        int mins_max = ParamManager.getIntParam("MAX_MINUTES_ALLOWED_TO_KEEP_BALANCE", "TRADING", stk.getID());
        int days_to_sell = mins_max / (60*24);
        Connection con = DBManager.getConnection();
        try {
        	Statement stm = con.createStatement();
        	String sql = "select count(distinct left(dl_dt, 10)) days from stkdat2 "
        			+ "    where id = '" + stk.getID()
        			+ "' and left(dl_dt, 10) >= '" + t0.toString().substring(0, 10)
        			+ "' and left(dl_dt, 10) <= '" + t1.toString().substring(0, 10) + "'";
        	
        	log.info(sql);
        	ResultSet rs = stm.executeQuery(sql);
        	
        	rs.next();
        	int days = rs.getInt("days");
        	
        	rs.close();
        	stm.close();
        	
        	log.info("stock bought for " + days + " days, days_to_sell:" + days_to_sell);
        	if (days < days_to_sell) {
        		log.info("can not sell stock which is bought less than " + days_to_sell + " days, actual days:" + days);
        		return false;
        	}
        }
        catch(Exception e) {
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
        
		double tradeThresh = 0;
        double margin_pct = ParamManager.getFloatParam("MARGIN_PCT_TO_TRADE_THRESH", "TRADING", stk.getID());
		if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {

			// If we bought before with lower price, use it as minPri.
			if (sbs != null && sbs.is_buy_point && sbs.price < minPri) {
				log.info("stock:" + sbs.id + " bought with price:" + sbs.price + " which is lower than:" + minPri + ", use it as minPri.");
				minPri = sbs.price;
			}
			
			tradeThresh = ParamManager.getFloatParam("SELL_BASE_TRADE_THRESH", "TRADING", stk.getID());
			
			double maxPct = (maxPri - minPri) / yt_cls_pri;
			double curPct = (cur_pri - minPri) / yt_cls_pri;
			
			boolean con1 = maxPct > tradeThresh && curPct > maxPct * (1 - margin_pct);
			boolean con2 = stk.isLstQtyPlused(2);
			boolean priceTurnedAround = stk.priceDownAfterSharpedUp(2);
			
			log.info("Check Sell:" + stk.getDl_dt() + " stock:" + stk.getID() + "yt_cls_pri:" + yt_cls_pri + " maxPri:" + maxPri + " minPri:"
					+ minPri + " maxPct:" + maxPct + " curPct:" + curPct + " curPri:" + cur_pri + " tradeThresh:" + tradeThresh + " marginPct:" + (1-margin_pct));
			log.info("price is reaching top margin:" + con1 + " priceTurnedAround is:" + priceTurnedAround + " isLstQtyPlused:" + con2);
			if (con1 && con2 && priceTurnedAround) {
                stk.setTradedBySelector(this.selector_name);
                stk.setTradedBySelectorComment("Price range:[" + minPri + ", " + maxPri + "] /" + yt_cls_pri + " > tradeThresh:" + tradeThresh + " and in margin pct:" + (1 - margin_pct) + " also priceTurnedAround:" + priceTurnedAround);
				return true;
			}
		}
        
        long hour = t1.getHours();
        long minutes = t1.getMinutes();
        
        log.info("Hour:" + hour + ", Minute:" + minutes);
        
        int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING", stk.getID());
        int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING", stk.getID());
        
        if (hour >= hour_for_balance && minutes >= mins_for_balance)
        {
            log.info("Reaching " + hour_for_balance + ":" + mins_for_balance
                     + ", Stock:" + stk.getID() + ", sell it out");
            stk.setTradedBySelector(this.selector_name);
            stk.setTradedBySelectorComment("Stock:" + stk.getID() + " keep balance time:" + hour_for_balance + ":" + mins_for_balance);
            return true;
        }
        
        return false;
	}
	
    public double getSellThreshValueByDegree(double Degree, Stock2 stk) {
    	
    	double baseThresh = ParamManager.getFloatParam("SELL_BASE_TRADE_THRESH", "TRADING", stk.getID());
    	
    	return baseThresh;
    }

	@Override
	public int getSellQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
		int sellMnt = 0;
        int sellableAmnt = TradeStrategyImp.getSellableMntForStockOnDate(s.getID(), s.getDl_dt());
        
	    if (sellableAmnt > 0)
	    {
	   	    sellMnt = sellableAmnt;
	    }
	    log.info("stock:" + s.getID() + ", calculated sellMnt:" + sellMnt);
		return sellMnt;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
