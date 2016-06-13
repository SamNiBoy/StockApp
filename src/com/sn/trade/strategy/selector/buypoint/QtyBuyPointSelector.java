package com.sn.trade.strategy.selector.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
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

public class QtyBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(QtyBuyPointSelector.class);
	
	double DISALLOW_BUY_IF_LOST = -0.05;
	@Override
	public boolean isGoodBuyPoint(Stock stk, ICashAccount ac) {

		double tradeThresh = STConstants.BASE_TRADE_THRESH;
		if ((ac != null && !ac.hasStockInHand(stk)) || ac == null) {
			Double maxPri = stk.getMaxCurPri();
			Double minPri = stk.getMinCurPri();
			Double yt_cls_pri = stk.getYtClsPri();
			Double cur_pri = stk.getCur_pri();
			Double opn_pri = stk.getOpen_pri();

			if ((cur_pri - opn_pri) / yt_cls_pri <= DISALLOW_BUY_IF_LOST) {
			    log.info("DISALLOW BUY, cur_pri:" + cur_pri + ", opn_pri:" + opn_pri + ", DISALLOW_BUY_IF_LOST:" + DISALLOW_BUY_IF_LOST + ", lost:" + (cur_pri - opn_pri) / yt_cls_pri);
			    return false;
			}
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
				} else if (stk.isStoppingJumpWater() && !StockMarket.isGzStocksJumpWater(5, 0.01, 0.5)) {
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

//        Boolean psd = StockMarket.getStockSellMode(stk.getID());
//        Boolean csd = SellModeWatchDog.isStockInSellMode(stk);
//        
//        StockMarket.putStockSellMode(stk.getID(), csd);
//
//        //If we switched to non sell mode, make sure buy once.
//        if ((csd != null && psd != null) && (csd == false && psd == true)) {
//            log.info("Stock " + stk.getID() + " is switched to non sell mode, buy point return true.");
//            return true;
//        }
        
		return false;
	}
	
    public double getBuyThreshValueByDegree(double Degree, Stock stk) {
    	
    	double baseThresh = STConstants.BASE_TRADE_THRESH;
    	
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
    			ratio = 1.5;
    	    }
    	    else if (Degree < -20) {
    	    	ratio = 2;
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
	public int getBuyQty(Stock s, ICashAccount ac) {
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
