package com.sn.trade.strategy.selector.sellpoint;

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
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.work.task.SellModeWatchDog;

public class QtySellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(QtySellPointSelector.class);

	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock stk, ICashAccount ac) {

		Double maxPri = stk.getMaxCurPri();
		Double minPri = stk.getMinCurPri();
		Double yt_cls_pri = stk.getYtClsPri();
		Double cur_pri = stk.getCur_pri();
		double tradeThresh = STConstants.BASE_TRADE_THRESH;
		

		if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {

			double marketDegree = StockMarket.getDegree();
			
			tradeThresh = getSellThreshValueByDegree(marketDegree, stk, ac);
			
			double maxPct = (maxPri - minPri) / yt_cls_pri;
			double curPct = (cur_pri - minPri) / yt_cls_pri;
			
			boolean con1 = maxPct > tradeThresh && curPct > maxPct * 8.0 / 10.0;
			boolean con2 = stk.isLstQtyPlused();
			boolean con3 = stk.isLstPriTurnaround(false);
			
			log.info("Check Sell:" + stk.getDl_dt() + " stock:" + stk.getID() + "yt_cls_pri:" + yt_cls_pri + " maxPri:" + maxPri + " minPri:"
					+ minPri + " maxPct:" + maxPct + " curPct:" + curPct + " curPri:" + cur_pri + " tradeThresh:" + tradeThresh);
			log.info("con1 is:" + con1 + " con2 is:" + con2 + " con3 is:" +con3);
			if (con1 && con2 && con3) {
				return true;
			}
			
			Boolean psd = StockMarket.getStockSellMode(stk.getID());
			Boolean csd = SellModeWatchDog.isStockInSellMode(stk);
			
			StockMarket.putStockSellMode(stk.getID(), csd);

			//If we switched to sell mode, make sure sell once.
			if ((csd != null && psd != null) && (csd == true && psd == false)) {
				log.info("Stock " + stk.getID() + " is in sell mode, at sell point, return true.");
				return true;
			}
			log.info("common bad sell point.");
		} else {
			log.info("isGoodSellPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
		}
		return false;
	}
	
    public double getSellThreshValueByDegree(double Degree, Stock stk, ICashAccount ac) {
    	
    	double baseThresh = STConstants.BASE_TRADE_THRESH;
    	
    	if (ac.hasStockInHandBeforeDays(stk, 0) && SellModeWatchDog.isStockInSellMode(stk) && stk.getTrade_mode_id() == STConstants.TRADE_MODE_ID_AVGPRI) {
    	    log.info("Stock:" + stk.getID() + " is in sell mode, and is trade mode AVGPRI, set sell baseThresh to 0.0");
    	    return 0.0;
    	}

    	Timestamp tm = stk.getDl_dt();
        String deadline = null;
        if (tm == null) {
        	deadline = "sysdate";
        }
        else {
        	deadline = "to_date('" + tm.toLocaleString() + "', 'yyyy-mm-dd HH24:MI:SS')";
        }
        
    	try {
    		Connection con = DBManager.getConnection();
    		Statement stm = con.createStatement();
    		String sql = "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev "
    				   + "  from stkdat2 "
    				   + " where id ='" + stk.getID() + "'"
    				   + "   and to_char(dl_dt, 'yyyy-mm-dd') = to_char(" + deadline + ", 'yyyy-mm-dd')";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			double dev = rs.getDouble("dev");
    			log.info("dev calculated for stock:" + stk.getID() + " is:" + dev);
    			if (dev >= 0.01 && dev <= 0.04) {
    				baseThresh = STConstants.BASE_TRADE_THRESH * (dev - 0.01) / (0.04 - 0.01) + STConstants.BASE_TRADE_THRESH;
    			}
    		}
    		else {
    			baseThresh = STConstants.BASE_TRADE_THRESH;
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
    			ratio = 0.9;
    	    }
    	    else if (Degree < -20) {
    	    	ratio = 0.8;
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
	public int getSellQty(Stock s, ICashAccount ac) {
		// TODO Auto-generated method stub
        int sellMnt = 0;
        
        if (ac != null) {
            String dt = s.getDl_dt().toString().substring(0, 10);
            int sellableAmt = ac.getSellableAmt(s.getID(), dt);
            
            if (sellableAmt >= 400) {
            	sellMnt = sellableAmt / 2;
            	sellMnt = sellMnt - sellMnt % 100;
            }
            else {
            	sellMnt = sellableAmt;
            }
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

    @Override
    public boolean matchTradeModeId(Stock s) {
        // TODO Auto-generated method stub
        Integer trade_mode_id = s.getTrade_mode_id();
        boolean sell_mode_flg = SellModeWatchDog.isStockInSellMode(s);
        
        log.info("trade_mode_id for stock:" + s.getID() + " is:" + trade_mode_id + "sell_mode_flg:" + sell_mode_flg
                + " expected:" + STConstants.TRADE_MODE_ID_QTYTRADE
                + " or:" + STConstants.TRADE_MODE_ID_MANUAL
                + " or:" + STConstants.TRADE_MODE_ID_AVGPRI);
        if (trade_mode_id != null && (trade_mode_id == STConstants.TRADE_MODE_ID_QTYTRADE ||
                trade_mode_id == STConstants.TRADE_MODE_ID_MANUAL ||
                trade_mode_id == STConstants.TRADE_MODE_ID_AVGPRI)) {
            log.info("trade mode matched, continue");
            return true;
        }
        else if (sell_mode_flg) {
            log.info("even trade mode not match, but in sell mode, return true");
            return true;
        }
        log.info("trade mode does not matched, continue");
        return false;
    }
}
