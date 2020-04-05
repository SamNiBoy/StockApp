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

public class QtyBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(QtyBuyPointSelector.class);
	
    private StockBuySellEntry sbs = null;
    
    private boolean sim_mode;
    private String selector_name = "QtyBuyPointSelector";
    private String selector_comment = "";
    
    
    public QtyBuyPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {
        
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        sbs = lstTrades.get(stk.getID());

        Timestamp t1 = stk.getDl_dt();
        
        long hour = t1.getHours();
        long minutes = t1.getMinutes();
        
        
        int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING");
        int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING");
        
        if ((hour * 100 + minutes) >= (hour_for_balance * 100 + mins_for_balance))
        {
            if (sbs == null || (sbs != null && sbs.is_buy_point))
            {
                log.info("Hour:" + hour + ", Minute:" + minutes);
                log.info("Close to market shutdown time, no need to break balance");
                return false;
            }
        }
        
        double pct = (stk.getCur_pri() - stk.getYtClsPri()) / stk.getYtClsPri();
        
        double stop_trade_for_max_pct = ParamManager.getFloatParam("STOP_BREAK_BALANCE_IF_CURPRI_REACHED_PCT", "TRADING");
        if (Math.abs(pct) >= stop_trade_for_max_pct)
        {
           log.info("Stock:" + stk.getID() + " cur_pri:" + stk.getCur_pri() + " ytClsPri:" + stk.getYtClsPri() +", increase pct:" + pct
                   + " is exceeding " + stop_trade_for_max_pct + " stop trading");
            return false;
        }
        
        
        boolean csd = SellModeWatchDog.isStockInStopTradeMode(stk);
        
        if (csd == true && (sbs == null || sbs.is_buy_point))
        {
            log.info("Stock:" + stk.getID() + " is in sell mode and in balance/bought, no need to break balance.");
            return false;
        }
    

		double tradeThresh = 0;
		double margin_pct = ParamManager.getFloatParam("MARGIN_PCT_TO_TRADE_THRESH", "TRADING");
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
				
				if (maxPct >= tradeThresh && curPct < maxPct * margin_pct && qtyPlused) {
					log.info("isGoodBuyPoint true says Check Buy:" + stk.getDl_dt() + " stock:" + stk.getID()
							+ " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " curPri:" + cur_pri + " margin_pct:" + margin_pct);
                    
					stk.setTradedBySelector(this.selector_name);
					stk.setTradedBySelectorComment("Price range:[" + minPri + ", " + maxPri + "] /" + yt_cls_pri + " > tradeThresh:" + tradeThresh + " and in margin pct:" + margin_pct + " also qtyPlused:" + qtyPlused);
					return true;
				} else if (stk.isStoppingJumpWater() && !StockMarket.isGzStocksJumpWater(5, 0.01, 0.5)) {
					log.info("Stock cur price is stopping dumping, isGoodBuyPoint return true.");
					//for testing purpose, still return false;
                    stk.setTradedBySelector(this.selector_name);
                    stk.setTradedBySelectorComment("Stock price is stop jummping water");
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
                    stk.setTradedBySelector(this.selector_name);
					stk.setTradedBySelectorComment("cur_pri:" + cur_pri + " is tradeThresh:" + tradeThresh + " comparing to last buy price:" + lstBuy);
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
    	
    	double baseThresh = ParamManager.getFloatParam("BUY_BASE_TRADE_THRESH", "TRADING");
    	
    	Timestamp tm = stk.getDl_dt();
        String deadline = null;
        if (tm == null) {
        	deadline = "sysdate()";
        }
        else {
     		//log.info("getBuyThreshValueByDegree: stk.getDl_dt().toString():" +  stk.getDl_dt().toString());
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
    				baseThresh = 0.01 * (dev - 0.01) / (0.04 - 0.01) + baseThresh;
    			}
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
	public int getBuyQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        double useableMny = 0;
        int buyMnt = 0;
        int maxMnt = 0;
        
        if (ac != null) {
            useableMny = ac.getMaxMnyForTrade();
            maxMnt = (int)(useableMny/s.getCur_pri()) / 100 * 100;
            
           	buyMnt = maxMnt;
            
           	if (!sim_mode)
           	{
           	    int sellableAmt = ac.getSellableAmt(s.getID(), null);
                if (buyMnt > sellableAmt)
                {
                    log.info("Tradex sellable amount:" + sellableAmt + " less than calculated amt:" + buyMnt + " use sellabeAmt.");
                    buyMnt = sellableAmt;
                }
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

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
    

}
