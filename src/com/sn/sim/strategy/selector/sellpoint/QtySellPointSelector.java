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
			
			tradeThresh = getSellThreshValueByDegree(marketDegree, stk);
			
			double maxPct = (maxPri - minPri) / yt_cls_pri;
			double curPct = (cur_pri - minPri) / yt_cls_pri;
			
			boolean con1 = maxPct > tradeThresh && curPct > maxPct * 9.0 / 10.0;
			boolean con2 = stk.isLstQtyPlused();
			
			log.info("Check Sell:" + stk.getDl_dt() + " stock:" + stk.getID() + "yt_cls_pri:" + yt_cls_pri + " maxPri:" + maxPri + " minPri:"
					+ minPri + " maxPct:" + maxPct + " curPct:" + curPct + " curPri:" + cur_pri + " tradeThresh:" + tradeThresh);
			log.info("con1 is:" + con1 + " con2 is:" + con2);
			if (con1 && con2) {
				return true;
			}
			// If current stock is 70% decrease, and market has 70% decrease for 80% stocks in last 50 times
			else if (stk.isJumpWater(100, 0.7) && StockMarket.isJumpWater(50, 0.7, 0.8)) {
				log.info("Stock " + stk.getID() + " cur price is jump water, a good sell point, return true.");
				//for testing purpose, still return false;
				return true;
			}
			log.info("common bad sell point.");
		} else {
			log.info("isGoodSellPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
		}
		return false;
	}
	
    public double getSellThreshValueByDegree(double Degree, Stock2 stk) {
    	
    	double baseThresh = 0.02;
    	
    	try {
    		Connection con = DBManager.getConnection();
    		Statement stm = con.createStatement();
    		String sql = "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev "
    				   + "  from stkdat2 "
    				   + " where id ='" + stk.getID() + "'"
    				   + "   and dl_dt >= sysdate - 1/24"
    				   + "   and to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd')";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			double dev = rs.getDouble("dev");
    			if (dev >= 0.01 && dev <= 0.04) {
    				baseThresh = 0.01 * (dev - 0.01) / (0.04 - 0.01) + 0.02;
    			}
    		}
    		else {
    			baseThresh = 0.02;
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
    			ratio = 1.1;
    		}
    		else {
    			ratio = 1.2;
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
}
