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
import com.sn.sim.strategy.imp.STConstants;
import com.sn.sim.strategy.selector.ISellPointSelector;
import com.sn.sim.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trader.StockTrader;
import com.sn.work.task.SellModeWatchDog;

public class QtySellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(QtySellPointSelector.class);

	private double BASE_TRADE_THRESH = 0.02;
	//Map<String, Boolean> preSellMode = new HashMap<String, Boolean>();
    private StockBuySellEntry sbs = null;
    
    private boolean sim_mode;
    
    
    public QtySellPointSelector(boolean sm)
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
		double tradeThresh = BASE_TRADE_THRESH;
        
		
        Map<String, StockBuySellEntry> lstTrades = StockTrader.getLstTradeForStocks();
        sbs = lstTrades.get(stk.getID());

        Timestamp t1 = stk.getDl_dt();
        
        long hour = t1.getHours();
        long minutes = t1.getMinutes();
        
        log.info("Hour:" + hour + ", Minute:" + minutes);
        if ((hour * 100 + minutes) >= (STConstants.HOUR_TO_KEEP_BALANCE * 100 + STConstants.MINUTE_TO_KEEP_BALANCE))
        {
            if (sbs == null || (sbs != null && !sbs.is_buy_point))
            {
                log.info("Close to market shutdown time, no need to break balance");
                return false;
            }
        }
        
        double pct = (stk.getCur_pri() - stk.getYtClsPri()) / stk.getYtClsPri();
        
        if (Math.abs(pct) >= STConstants.STOP_BREAK_BALANCE_IF_CURPRI_REACHED_PCT)
        {
           log.info("Stock:" + stk.getID() + " cur_pri:" + stk.getCur_pri() + " ytClsPri:" + stk.getYtClsPri() +", increase pct:" + pct
                   + " is exceeding " + (-STConstants.STOP_BREAK_BALANCE_IF_CURPRI_REACHED_PCT) + " stop trading");
            return false;
        }
        
        boolean csd = SellModeWatchDog.isStockInSellMode(stk);
        
        if (csd == true && (sbs == null || !sbs.is_buy_point))
        {
            log.info("Stock:" + stk.getID() + " is in sell mode and in balance/sold, no need to break balance.");
            return false;
        }
		

		if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {

			double marketDegree = StockMarket.getDegree();
			
			tradeThresh = getSellThreshValueByDegree(marketDegree, stk);
			
			double maxPct = (maxPri - minPri) / yt_cls_pri;
			double curPct = (cur_pri - minPri) / yt_cls_pri;
			
			boolean con1 = maxPct > tradeThresh && curPct > maxPct * 9.0 / 10.0;
			boolean con2 = stk.isLstQtyPlused();
			
			log.info("Check Sell:" + stk.getDl_dt() + " stock:" + stk.getID() + "yt_cls_pri:" + yt_cls_pri + " maxPri:" + maxPri + " minPri:"
					+ minPri + " maxPct:" + maxPct + " curPct:" + curPct + " curPri:" + cur_pri + " tradeThresh:" + tradeThresh);
			log.info("price is reaching top margin:" + con1 + " isLstQtyPlused is:" + con2);
			if (con1 && con2) {
				return true;
			}
			
			/*Boolean psd = preSellMode.get(stk.getID());
			Boolean csd = SellModeWatchDog.isStockInSellMode(stk);
			
			preSellMode.put(stk.getID(), csd);*/

			//sell mode means not good for trade, BalanceSelector should do reverse trade clean up stock in hand.
			/*if (csd == true && (psd == null || psd != csd)) {
				log.info("Stock " + stk.getID() + " is in sell mode, at sell point, return true.");
				return true;
			}*/
		} else {
			log.info("isGoodSellPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
		}
		return false;
	}
	
    public double getSellThreshValueByDegree(double Degree, Stock2 stk) {
    	
    	double baseThresh = BASE_TRADE_THRESH;
    	
    	Timestamp tm = stk.getDl_dt();
        String deadline = null;
        if (tm == null) {
        	deadline = "sysdate()";
        }
        else {
            log.info("getBuyThreshValueByDegree: stk.getDl_dt().toString():" +  stk.getDl_dt().toString());
        	deadline = "str_to_date('" + stk.getDl_dt().toString() + "', '%Y-%m-%d %H:%i:%s')";
        }
        
    	try {
    		Connection con = DBManager.getConnection();
    		Statement stm = con.createStatement();
    		String sql = "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev "
    				   + "  from stkdat2 "
    				   + " where id ='" + stk.getID() + "'"
    				   + "   and left(dl_dt, 10) = left(" + deadline + ", 10)";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			double dev = rs.getDouble("dev");
    			log.info("dev calculated for stock:" + stk.getID() + " is:" + dev);
    			if (dev >= 0.01 && dev <= 0.04) {
    				baseThresh = 0.01 * (dev - 0.01) / (0.04 - 0.01) + BASE_TRADE_THRESH;
    			}
    		}
    		else {
    			baseThresh = BASE_TRADE_THRESH;
    		}
    		rs.close();
    		stm.close();
    		con.close();
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
    	

    	double ratio = 1;
    	
    	if (Degree < 0) {
    		if (Degree >= -10) {
    			ratio = 1;
    		}
    		else if (Degree < -10 && Degree >= -20) {
    			ratio = 0.5;
    	    }
    	    else if (Degree < -20) {
    	    	ratio = 0.4;
    	    }
    	}
    	else {
    		if (Degree <= 10) {
    			ratio = 1;
    		}
    		else if (Degree > 10 && Degree <= 20){
    			ratio = 1.3;
    		}
    		else {
    			ratio = 1.5;
    		}
    	}
    	log.info("Calculate sell thresh value with Degree:" + Degree + ", baseThresh:" + baseThresh + " ratio:" + ratio + " final thresh value:" + ratio * baseThresh);
    	return ratio * baseThresh;
    }

	@Override
	public int getSellQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        int sellMnt = 0;
        
        if (ac != null) {
            int sellableAmt = (int) (ac.getMaxMnyForTrade() / s.getCur_pri());
            sellMnt =  sellableAmt - sellableAmt % 100;
            log.info("getSellQty, sellableAmt:" + sellableAmt + " sellMnt:" + sellMnt);
        }
        else {
        	if (s.getCur_pri() <= 10) {
        		sellMnt = 200;
        	}
        	else {
        		sellMnt = 100;
        	}
        	log.info("getSellQty, cur_pri:" + s.getCur_pri() + " sellMnt:" + sellMnt);
        }
		return sellMnt;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
