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
import com.sn.util.StockDataProcess;
import com.sn.util.VOLPRICEHISTRO;

public class QtyBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(QtyBuyPointSelector.class);
	
    private boolean sim_mode;
    private String selector_name = "QtyBuyPointSelector";
    private String selector_comment = "";
    private double maxrt = 0;
    private String pre_dte = "";
    
    public QtyBuyPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {
        
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        Double maxPri = 0.0;//stk.getMaxCurPri();
        Double minPri = 0.0;//stk.getMinCurPri();
        Double yt_cls_pri = stk.getYtClsPri();
        Double cur_pri = stk.getCur_pri();
        //Double td_hst_pri = stk.getMaxTd_hst_pri();
        Double avg_pri = stk.getDl_mny_num() / stk.getDl_stk_num();
        
        
        if (avg_pri > cur_pri) {
        	maxPri = avg_pri;
        	minPri = cur_pri;
        }
        else {
        	maxPri = cur_pri;
        	minPri = avg_pri;
        }
        
        log.info("cur_pri:" + cur_pri + ", maxPri:" + maxPri + ", minPri:" + minPri);
        
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
        Timestamp t1 = stk.getDl_dt();
        
        long hour = t1.getHours();
        long minutes = t1.getMinutes();
        
        int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING", stk.getID());
        int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING", stk.getID());
        
        if ((hour * 100 + minutes) >= (hour_for_balance * 100 + mins_for_balance))
        {
            if (sbs == null || (sbs != null && sbs.is_buy_point))
            {
                log.info("Hour:" + hour + ", Minute:" + minutes);
                log.info("Close to market shutdown time, no need to break balance");
                return false;
            }
        }
        
        double pct = (cur_pri - yt_cls_pri) / yt_cls_pri;
        
        double stop_trade_for_max_pct = ParamManager.getFloatParam("STOP_BREAK_BALANCE_IF_CURPRI_REACHED_PCT", "TRADING", stk.getID());
        
        log.info("check stock:" + stk.getID() + " reached no trading margin:" + stop_trade_for_max_pct + " with actual pct:" + pct);
        if (Math.abs(pct) >= stop_trade_for_max_pct && (sbs == null || sbs.is_buy_point))
        {
           log.info("Stock:" + stk.getID() + " cur_pri:" + stk.getCur_pri() + " ytClsPri:" + stk.getYtClsPri() +", increase pct:" + pct
                   + " is exceeding " + stop_trade_for_max_pct + " stop trading to increase unbalance.");
            return false;
        }
        
        
        boolean csd = SellModeWatchDog.isStockInStopTradeMode(stk);
        
        if (csd == true && (sbs == null || sbs.is_buy_point))
        {
            log.info("Stock:" + stk.getID() + " is in sell mode and in balance/bought, no need to break balance.");
            return false;
        }

		double tradeThresh = 0;
		double margin_pct = ParamManager.getFloatParam("MARGIN_PCT_TO_TRADE_THRESH", "TRADING", stk.getID());

     	tradeThresh = ParamManager.getFloatParam("BUY_BASE_TRADE_THRESH", "TRADING", stk.getID());
         
		if ((ac != null && !ac.hasStockInHand(stk)) || ac == null) {
            
				if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {
				
				if (sbs != null && sbs.price > cur_pri) {
					log.info("stock:" + sbs.id + " previous trade with price:" + sbs.price + " which is higher than:" + maxPri + ", use it as maxPri.");
					maxPri = sbs.price;
				}
				else if (sbs != null){
					log.info("previous trade with pri:" + sbs.price + ", skip trade again.");
					return false;
				}
				
				double maxPct = (maxPri - minPri) / yt_cls_pri;
				double curPct =(cur_pri - minPri) / yt_cls_pri;
				
				boolean priceTurnedAround = stk.priceUpAfterSharpedDown(2);
				boolean con2 = stk.isLstQtyPlused(2);
				
				
				log.info("maxPct:" + maxPct + ", tradeThresh:" + tradeThresh + ", curPct:" + curPct + ", priceTurnedAround:" + priceTurnedAround + ", isLstQtyPlused:" + con2);
				
				if (maxPct >= tradeThresh && curPct < maxPct * margin_pct && priceTurnedAround) {
					log.info("isGoodBuyPoint true says Check Buy:" + stk.getDl_dt() + " stock:" + stk.getID()
							+ " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " curPri:" + cur_pri + " margin_pct:" + margin_pct);
					    stk.setTradedBySelector(this.selector_name);
					    stk.setTradedBySelectorComment("Price range:[" + minPri + ", " + maxPri + "] /" + yt_cls_pri + " > tradeThresh:" + tradeThresh + " and in margin pct:" + margin_pct + " also priceTurnedAround:" + priceTurnedAround);
					    return true;
				}
			} else {
				log.info("isGoodBuyPoint says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
			}
		}
		return false;
	}
	
    public double getMaxB1S1RationForPastDays(int days_to_check, Stock2 stk) {
    	
    	//double baseThresh = ParamManager.getFloatParam("BUY_BASE_TRADE_THRESH", "TRADING", stk.getID());
    	
        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        
        int i = 0;
        try {
            stm = con.createStatement();
            String sql = "select max(b1_num / s1_num) max_rt, left(s1.dl_dt, 10) dte from stkdat2 s1 "
            		+ "  where left(s1.dl_dt, 10) < '" + stk.getDl_dt().toString().substring(0, 10) + "'"
            		+ "    and s1.id = '" + stk.getID() + "'"
            		+ "  group by left(s1.dl_dt, 1) "
            		+ "  order by dte";
            log.info(sql);
            rs = stm.executeQuery(sql);
            
            double currt = 0;
            while (rs.next() && i < days_to_check) {
            	i++;
                log.info("get ratio:" + rs.getDouble("max_rt") + " at date:" + rs.getString("dte"));
                currt = rs.getDouble("max_rt");
                if (currt > maxrt) {
                	maxrt = currt;
                }
            }
            rs.close();
            stm.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally {
            try {
                con.close();
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
        }
        
        if (i == days_to_check) {
        	log.info("got max ration:" + maxrt + " for past " + days_to_check + " days");
        	pre_dte = stk.getDl_dt().toString().substring(0, 10);
        }
        else if (maxrt < 20000){
        	log.info("only has:" + i + " days data to check, use default 10000.");
        	maxrt = 20000;
        }
    	return maxrt;
    }
    
    public double getAvgYtClsPri(Stock2 stk, double cur_pri, int days) {
		double avgytclspri = 0;
    	try {
		    Connection con = DBManager.getConnection();
		    Statement stm = con.createStatement();
		    String sql = "select avg(yt_cls_pri) avgpri, (max(yt_cls_pri) - min(yt_cls_pri)) / min(yt_cls_pri) detpri "
		    		   + " from (select yt_cls_pri from (select distinct yt_cls_pri, left(dl_dt, 10) dte from stkdat2 s"
		    		   + " where id = '" + stk.getID() + "'"
		    		   + "   and left(dl_dt, 10) <= '" + stk.getDl_dt().toString().substring(0, 10) + "' order by dte desc) t limit " + days + ") t2";
		    log.info(sql);
		    ResultSet rs = stm.executeQuery(sql);
		    if (rs.next()) {
		    	avgytclspri = rs.getDouble("avgpri");
		    	double detpri = rs.getDouble("detpri");
		    	
		    	log.info("avgytclspri calculated for stock:" + stk.getID() + " is:" + avgytclspri + " vs cur_pri:" + cur_pri + ", cur_pri < avgytclspri? " + (cur_pri < avgytclspri) + ", detpripct:" + detpri);
		    }
		    rs.close();
		    stm.close();
		    con.close();
	    }
	    catch(Exception e) {
	    	e.printStackTrace();
	    }
		return avgytclspri;
    }

	@Override
	public int getBuyQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        double useableMny = 0;
        int buyMnt = 0;
        int maxMnt = 0;
        
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(s.getID());
	    if (sbs != null && !sbs.is_buy_point)
	    {
	    	buyMnt = sbs.quantity;
	    	log.info("stock:" + s.getID() + " with qty:" + sbs.quantity + " already, buy same back");
	    }
	    else if (ac != null) {
	    	int cnt = TradeStrategyImp.getBuySellCount(s.getID(), s.getDl_dt().toString().substring(0, 10), true);
	    	
	    	cnt++;
	    	
            useableMny = ac.getMaxMnyForTrade();
            maxMnt = (int)(useableMny * cnt/s.getCur_pri()) / 100 * 100;
            
           	buyMnt = maxMnt;
            log.info("getBuyQty, useableMny:" + useableMny + " buyMnt:" + buyMnt + " maxMnt:" + maxMnt);
        }
		return buyMnt;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
    

}
