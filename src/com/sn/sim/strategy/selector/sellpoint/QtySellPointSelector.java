package com.sn.sim.strategy.selector.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;

public class QtySellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(QtySellPointSelector.class);

	private double tradeThresh = 0.02;
	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock2 stk, ICashAccount ac) {

		Double maxPri = stk.getMaxCurPri();
		Double minPri = stk.getMinCurPri();
		Double yt_cls_pri = stk.getYtClsPri();
		Double cur_pri = stk.getCur_pri();

		if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {

			double marketDegree = StockMarket.getDegree();
			
			tradeThresh = getSellThreshValueByDegree(marketDegree);
			
			double maxFlt = (maxPri - minPri) / yt_cls_pri;
			if (maxFlt > tradeThresh && (cur_pri - minPri) / yt_cls_pri > maxFlt * 9.0 / 10.0) {
				log.info("Check Sell:" + stk.getDl_dt() + " stock:" + stk.getID() + " maxPri:" + maxPri + " minPri:"
						+ minPri + " maxFlg:" + maxFlt + " curPri:" + cur_pri + " tradeThresh:" + tradeThresh);
				return true;
			}
			// If cur price is 90% decrease with 100 time(about 10 minutes) then it means jump water.
			else if (stk.isJumpWater(100, 0.9)) {
				log.info("Stock " + stk.getID() + " cur price is jump water, a good sell point, return true.");
				//for testing purpose, still return false;
				return false;
			}
			log.info("common bad sell point.");
		} else {
			log.info("isGoodSellPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
		}
		return false;
	}
	
    public double getSellThreshValueByDegree(double Degree) {
    	double val = 0.02;
    	if (Degree < 0) {
    		if (Degree >= -10) {
    			val = 0.02;
    		}
    		else if (Degree < -10 && Degree >= -20) {
    	    	val = 0.015;
    	    }
    	    else if (Degree < -20) {
    	    	val = 0.01;
    	    }
    	}
    	else {
    		if (Degree <= 10) {
    			val = 0.02;
    		}
    		else if (Degree > 10 && Degree <= 20){
    			val = 0.02;
    		}
    		else {
    			val = 0.03;
    		}
    	}
    	log.info("Degree is:" + Degree + " sell threshValue is:" + val);
    	return val;
    }
}
