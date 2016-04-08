package com.sn.sim.strategy.selector.buypoint;

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

public class QtyBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(QtyBuyPointSelector.class);
	
	private double tradeThresh = 0.03;

	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {

		if ((ac != null && !ac.hasStockInHand(stk)) || ac == null) {
			Double maxPri = stk.getMaxCurPri();
			Double minPri = stk.getMinCurPri();
			Double yt_cls_pri = stk.getYtClsPri();
			Double cur_pri = stk.getCur_pri();

			if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {

				double marketDegree = StockMarket.getDegree();
				
				tradeThresh = getBuyThreshValueByDegree(marketDegree, stk);
				
				double maxPct = (maxPri - minPri) / yt_cls_pri;
				double curPct =(cur_pri - minPri) / yt_cls_pri;

				boolean qtyPlused = stk.isLstQtyPlused();
				
				log.info("maxPct:" + maxPct + ", tradeThresh:" + tradeThresh + ", curPct:" + curPct + ", isQtyPlused:" + qtyPlused);
				
				if (maxPct >= tradeThresh && curPct < maxPct * 1.0 / 10.0 && stk.isLstQtyPlused()) {
					log.info("isGoodBuyPoint true says Check Buy:" + stk.getDl_dt() + " stock:" + stk.getID()
							+ " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " curPri:" + cur_pri);
					return true;
				} else if (stk.isStoppingJumpWater() && !StockMarket.isJumpWater(50, 0.7, 0.8)) {
					log.info("Stock cur price is stopping dumping, isGoodBuyPoint return true.");
					//for testing purpose, still return false;
					return true;
				}
				else {
					log.info("isGoodBuyPoint false Check Buy:" + stk.getDl_dt() + " stock:" + stk.getID()
							+ " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " curPri:" + cur_pri + " tradeThresh:" + tradeThresh);
				}
			} else {
				log.info("isGoodBuyPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
			}
		} else {
			// has stock in hand;
			Double lstBuy = ac.getLstBuyPri(stk);
			Double cur_pri = stk.getCur_pri();
			Double yt_cls_pri = stk.getYtClsPri();
			if (lstBuy != null && cur_pri != null && yt_cls_pri != null) {
				if ((lstBuy - cur_pri) / yt_cls_pri > tradeThresh && stk.isLstQtyPlused()) {
					log.info("isGoodBuyPoint Buy true:" + stk.getDl_dt() + " stock:" + stk.getID() + " lstBuyPri:"
							+ lstBuy + " curPri:" + cur_pri + " yt_cls_pri:" + yt_cls_pri);
					return true;
				}
				log.info("isGoodBuyPoint Buy false:" + stk.getDl_dt() + " stock:" + stk.getID() + " lstBuyPri:" + lstBuy
						+ " curPri:" + cur_pri + " yt_cls_pri:" + yt_cls_pri);
			} else {
				log.info("isGoodBuyPoint Buy false: fields is null");
			}
		}
		return false;
	}
	
    public double getBuyThreshValueByDegree(double Degree, Stock2 stk) {
    	
    	double baseThresh = 0.03;
    	
    	try {
    		Connection con = DBManager.getConnection();
    		Statement stm = con.createStatement();
    		String sql = "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev "
    				   + "  from stkdat2 "
    				   + " where id ='" + stk.getID() + "'"
    				   + "   and to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd')";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			double dev = rs.getDouble("dev");
    			log.info("dev calculated for stock:" + stk.getID() + " is:" + dev);
    			if (dev >= 0.01 && dev <= 0.04) {
    				baseThresh = 0.01 * (dev - 0.01) / (0.04 - 0.01) + 0.03;
    			}
    		}
    		else {
    			baseThresh = 0.03;
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
    			ratio = 1.2;
    	    }
    	    else if (Degree < -20) {
    	    	ratio = 1.3;
    	    }
    	}
    	else {
    		if (Degree < 10) {
    			ratio = 1;
    		}
    		else {
    			ratio = 1.1;
    		}
    	}
    	log.info("Calculate buy thresh value with Degree:" + Degree + ", baseThresh:" + baseThresh + " ratio:" + ratio + " final thresh value:" + ratio * baseThresh);

    	return ratio * baseThresh;
    }

	@Override
	public int getBuyQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        double useableMny = 0;
        int buyMnt = 0;
        int maxMnt = 0;
        
        if (ac != null) {
            useableMny = ac.getMaxAvaMny();
            maxMnt = (int)(useableMny/s.getCur_pri()) / 100 * 100;
            
            if (maxMnt >= 400) {
            	buyMnt = maxMnt / 2;
            	buyMnt = buyMnt - buyMnt % 100;
            }
            else {
            	buyMnt = maxMnt;
            }
            log.info("getBuyQty, useableMny:" + useableMny + " buyMnt:" + buyMnt + " maxMnt:" + maxMnt);
        }
        else {
        	if (s.getCur_pri() <= 10) {
        		buyMnt = 200;
        	}
        	else {
        		buyMnt = 100;
        	}
        	log.info("getBuyQty, cur_pri:" + s.getCur_pri() + " buyMnt:" + buyMnt);
        }
		return buyMnt;
	}
    

}
