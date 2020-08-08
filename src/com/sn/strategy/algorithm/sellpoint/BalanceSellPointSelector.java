package com.sn.strategy.algorithm.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
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

public class BalanceSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(BalanceSellPointSelector.class);

    
    private boolean sim_mode;
    private String selector_name = "BalanceSellPointSelector";
    private String selector_comment = "";
    
	private String lst_dte_for_sim = "";
    
    
    public BalanceSellPointSelector(boolean sm)
    {
        sim_mode = sm;
        
    	Connection con = DBManager.getConnection();
    	
    	if (sim_mode) {
    	    try {
    	    	
    	    	Statement stm = null;
    	    	ResultSet rs = null;
    	    	
    	        String sql = "select left(max(dl_dt), 10) lst_dte from stkdat2_sim s2 where s2.id = '000001'";
    	    	log.info(sql);
    	    	
    	    	stm = con.createStatement();
    	    	rs = stm.executeQuery(sql);
    	    	
    	    	if (rs.next()) {
    	    		lst_dte_for_sim = rs.getString("lst_dte");
    	    		log.info("setup lst_dte_for_sim:" + lst_dte_for_sim);
    	    	}
    	    	rs.close();
    	    	stm.close();
    	    }
    	    catch (Exception e) {
    	    	log.error(e.getMessage(), e);
    	    }
    	    finally {
    	    	try {
		    		con.close();
		    	} catch (SQLException e) {
		    		// TODO Auto-generated catch block
		    		log.error(e.getMessage(), e);
		    	}
    	    }
    	}
    }
    
	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock2 stk, ICashAccount ac) {
        int sellableAmnt = TradeStrategyImp.getSellableMntForStockOnDate(stk.getID(), stk.getDl_dt());
        
        if (sellableAmnt <= 0)
        {
            log.info("Stock:" + stk.getID() + " did not trade yet or sold already, return false from BlanaceSellPointSelector.");
            return false;
        }
        else {
//            boolean cleanup_stock_inhand = SellModeWatchDog.isStockInStopTradeMode(stk);
//            if (cleanup_stock_inhand)
//            {
//                log.info("Stock:" + stk.getID() + " switched to stop trade mode(not good for trade), sell up stock in hand, return true");
//                stk.setTradedBySelector(this.selector_name);
//                stk.setTradedBySelectorComment("Stock:" + stk.getID() + " is in stop trade mode");
//                return true;
//            }
             {
                Timestamp t1 = stk.getDl_dt();
                		
                long hour = t1.getHours();
                long minutes = t1.getMinutes();
                
                log.info("Hour:" + hour + ", Minute:" + minutes);
                
                int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING", stk.getID());
                int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING", stk.getID());
                
                if (hour >= hour_for_balance && minutes >= mins_for_balance)
                {
                    log.info("Reaching " + hour_for_balance + ":" + mins_for_balance
                             + ", Stock:" + stk.getID() + " sellableAmnt: " + sellableAmnt + ", sell it out");
                    stk.setTradedBySelector(this.selector_name);
                    stk.setTradedBySelectorComment("Stock:" + stk.getID() + " keep balance time:" + hour_for_balance + ":" + mins_for_balance);
                    return true;
                }
                
                if (lst_dte_for_sim.length() > 0 && lst_dte_for_sim.equals(stk.getDl_dt().toString().substring(0, 10))) {
                	log.info("reached last simulation date:" + lst_dte_for_sim);
                    stk.setTradedBySelector(this.selector_name);
                    stk.setTradedBySelectorComment("Stock:" + stk.getID() + " keep balance at date:" + lst_dte_for_sim);
                    return true;
                }
            }
        }
        return false;
    }
	
	@Override
	public int getSellQty(Stock2 s, ICashAccount ac) {
	    return TradeStrategyImp.getSellableMntForStockOnDate(s.getID(), s.getDl_dt());
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
