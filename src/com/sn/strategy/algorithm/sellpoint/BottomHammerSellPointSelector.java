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

        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
        
        if (sbs == null || !sbs.is_buy_point) {
        	log.info("only sell stock which was bought before.");
        	return false;
        }
        
        Timestamp t1 = stk.getDl_dt();
        Timestamp t0 = sbs.dl_dt;
        
        log.info("Check if stock:" + stk.getID() + " was bought in past:" + t0.toString() + " at time:" + t1.toString());
        
        long millisec = t1.getTime() - t0.getTime();
        long hrs = millisec / (1000*60*60);
        
        if ((stk.getCur_pri() - sbs.price) / stk.getYtClsPri() > 0.2) {
        	log.info("got enough profit, sell it out.");
            stk.setTradedBySelector(this.selector_name);
            stk.setTradedBySelectorComment("stock:" + stk.getID() + " bought " + hrs + " ago, with price:" + sbs.price + ", now price:" + stk.getCur_pri() + ", enough profit, sell it out.");
            return true;
        }
        
        if (hrs <= 72) {
        	log.info("can not sell stock which is bought less than 72 hours, return false.");
        	return false;
        }

	    log.info("Check Sell:" + stk.getDl_dt() + " stock:" + stk.getID() + " to sell after hours:" + hrs);
        stk.setTradedBySelector(this.selector_name);
        stk.setTradedBySelectorComment("stock:" + stk.getID() + " bought " + hrs + " ago, sell it out now.");
		return true;
	}
	
    public double getSellThreshValueByDegree(double Degree, Stock2 stk) {
    	
    	double baseThresh = ParamManager.getFloatParam("SELL_BASE_TRADE_THRESH", "TRADING", stk.getID());
    	
//    	Timestamp tm = stk.getDl_dt();
//        String deadline = null;
//        if (tm == null) {
//        	deadline = "sysdate()";
//        }
//        else {
//        	deadline = "str_to_date('" + stk.getDl_dt().toString() + "', '%Y-%m-%d %H:%i:%s') - interval 1 day ";
//        }
//        
//    	try {
//    		Connection con = DBManager.getConnection();
//    		Statement stm = con.createStatement();
//    		String sql = "select (max(td_hst_pri) - min(td_lst_pri)) / max(yt_cls_pri) / 2.0 yt_shk_hlf_pct "
//    				   + "  from stkdat2 "
//    				   + " where id ='" + stk.getID() + "'"
//    				   + "   and left(dl_dt, 10) = left(" + deadline + ", 10)";
//    		log.info(sql);
//    		ResultSet rs = stm.executeQuery(sql);
//    		if (rs.next()) {
//    			double yt_shk_hlf_pct = rs.getDouble("yt_shk_hlf_pct");
//    			log.info("yt_shk_hlf_pct calculated for stock:" + stk.getID() + " is:" + yt_shk_hlf_pct + " vs baseThresh:" + baseThresh);
//    			if (yt_shk_hlf_pct > baseThresh) {
//    				baseThresh = yt_shk_hlf_pct;
//    			}
//    		}
//    		rs.close();
//    		stm.close();
//    		con.close();
//    	}
//    	catch(Exception e) {
//    		e.printStackTrace();
//    	}
//    	
//    	log.info("Calculate buy thresh value with Degree:" + Degree + ", final baseThresh:" + baseThresh);
    	
    	return baseThresh;
    }

	@Override
	public int getSellQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        int sellMnt = 0;
        if (ac != null) {
            int sellableAmt = (int) (ac.getMaxMnyForTrade() / s.getCur_pri());
            sellMnt =  sellableAmt - sellableAmt % 100;
            
            if (!sim_mode)
            {
                  int tradeLocal = ParamManager.getIntParam("TRADING_AT_LOCAL", "TRADING", null);
                  if (tradeLocal == 1)
                  {
                      log.info("Trade at local sellable amount is zero, user max mny per trade to get the qty:" + sellMnt);
                  }
                  else
                  {
                       sellableAmt = ac.getSellableAmt(s.getID(), null);
                       if (sellMnt > sellableAmt)
                       {
                            log.info("Tradex sellable amount:" + sellableAmt + " less than calculated amt:" + sellMnt + " use sellabeAmt.");
                            sellMnt = sellableAmt;
                       }
                  }
             }
             log.info("getSellQty, sellableAmt:" + sellableAmt + " sellMnt:" + sellMnt);
        }
        
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(s.getID());
	    if (sbs != null && sbs.is_buy_point)
	    {
	    	if (sellMnt > sbs.quantity)
	    	{
	    		sellMnt = sbs.quantity;
	    	}
	    	else if (sellMnt + 100 >= sbs.quantity)
	    	{
	    		sellMnt = sbs.quantity;
	    	}
	    	log.info("stock:" + s.getID() + " bought qty:" + sbs.quantity + " already, sell " + sellMnt + " out");
	    }
		return sellMnt;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
