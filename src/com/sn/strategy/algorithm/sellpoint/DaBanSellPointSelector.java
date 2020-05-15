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

public class DaBanSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(DaBanSellPointSelector.class);

    private String selector_name = "DaBanSellPointSelector";
    private String selector_comment = "";
    
    private boolean sim_mode;
    
    
    public DaBanSellPointSelector(boolean sm)
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
		Double opn_pri = stk.getOpen_pri();
        
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
        
        if (sbs == null || !sbs.is_buy_point) {
        	log.info("DaBanSellPointSelector only sell stock which was bought yesterday.");
        	return false;
        }
        
        Timestamp t0 = stk.getDl_dt();
        Timestamp t1 = sbs.dl_dt;
        
        log.info("Check if stock:" + stk.getID() + " was bought in past:" + t1.toString() + " at time:" + t0.toString());
        
        long millisec = t0.getTime() - t1.getTime();
        long hrs = millisec / (1000*60*60);
        
        if (hrs <= 12) {
        	log.info("can not sell stock which is bought at same day, return false.");
        	return false;
        }
        
//		if (opn_pri != null && yt_cls_pri != null && opn_pri < yt_cls_pri) {
//			double lost_pct = (opn_pri - yt_cls_pri) / yt_cls_pri;
//			if (lost_pct < -0.02) {
//			    log.info("today open price is at least -0.02 pct lower than yt_cls_pri, sell it out");
//                stk.setTradedBySelector(this.selector_name);
//                stk.setTradedBySelectorComment("today open price:" + opn_pri + " is lower than yt_cls_pri:" + yt_cls_pri + ", sell it out");
//                return true;
//			}
//			else {
//				log.info("lost pct is:" + lost_pct + " use normal logic to sell.");
//			}
//		}
		
        double marketDegree = StockMarket.getDegree(stk.getDl_dt());

		double tradeThresh = 0;
        double margin_pct = ParamManager.getFloatParam("MARGIN_PCT_TO_TRADE_THRESH", "TRADING", stk.getID());
		if (yt_cls_pri != null && cur_pri != null) {
			

			// If we bought before with lower price, use it as minPri.
			if (sbs != null && sbs.is_buy_point && sbs.price < minPri) {
				log.info("stock:" + sbs.id + " bought with price:" + sbs.price + " which is lower than:" + minPri + ", use it as minPri.");
				minPri = sbs.price;
			}
			
			tradeThresh = getSellThreshValueByDegree(marketDegree, stk);
			
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
		return false;
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
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(s.getID());
	    if (sbs != null && sbs.is_buy_point)
	    {
	    	sellMnt = sbs.quantity;
	    	log.info("stock:" + s.getID() + " bought qty:" + sbs.quantity + " already, sell same out");
	    }
	    else {
	    	//DaBan only sell what had bought, so give 0 if no bought.
	    	sellMnt = 0;
	    }
		return sellMnt;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
