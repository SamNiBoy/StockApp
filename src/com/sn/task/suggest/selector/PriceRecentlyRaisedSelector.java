package com.sn.task.suggest.selector;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.strategy.TradeStrategyImp;
import com.sn.task.IStockSelector;
import com.sn.task.suggest.SuggestStock;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class PriceRecentlyRaisedSelector implements IStockSelector {

    static Logger log = Logger.getLogger(PriceRecentlyRaisedSelector.class);
    
    private String on_dte = "";
    private String suggest_by = "PriceRecentlyRaisedSelector";
    int days_to_check = 7;
    double pct = 0.05;
    
    public PriceRecentlyRaisedSelector (String s) {
    	on_dte = s;
    }
    
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	
    	Connection con = DBManager.getConnection();
    	boolean isGoodStock = false;
    	double min_cur_pri = 0.0;
    	
    	try {
    		Statement stm = con.createStatement();
    		String sql = "select min(cur_pri) min_cur_pri from stkdat2 s2 where s2.id = '" + s.getID() + "' and left(s2.dl_dt + interval " + days_to_check +" day, 10) >= '" + on_dte + "' and left(s2.dl_dt, 10) <= '" + on_dte + "'";
    		
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			min_cur_pri = rs.getDouble("min_cur_pri");
    		}
    		
    		rs.close();
    		stm.close();
    		
    		sql = "select max(cur_pri) max_cur_pri from stkdat2 s2 where s2.id = '" + s.getID() + "' and left(s2.dl_dt + interval " + days_to_check +" day, 10) < '" + on_dte + "'";
    		
    		log.info(sql);
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			double max_cur_pri = rs.getDouble("max_cur_pri");
    			log.info("Stock min price for past " + days_to_check + " days:" + min_cur_pri + " must be raised a lot comparing to max price:" + max_cur_pri);
    			
    			if ((min_cur_pri - max_cur_pri) /max_cur_pri > pct) {
                    s.setSuggestedBy(this.suggest_by);
                    s.setSuggestedComment("Stock min price:" + min_cur_pri + " raised " + pct +" pct comparing to " + days_to_check + " max price:" + max_cur_pri);
    			    isGoodStock = true;
    			}
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
        return isGoodStock;
    }
	@Override
	public boolean isORCriteria() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isMandatoryCriteria() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		log.info("PriceRecentlyDoubledSelector is mandatory criteria, adjustment with harder:" + harder);
		if (harder) {
			pct += 0.01;
			days_to_check--;
		}
		else {
			pct -= 0.01;
			days_to_check++;
		}
		
		if (pct < 0.03) {
			pct = 0.03;
		}
		if (days_to_check < 5) {
			days_to_check = 5;
		}
		
		log.info("try harder:" + harder + ", pct:" + pct + ", days_to_check:" + days_to_check);
		return true;
	}
}
