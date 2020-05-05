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

public class ClosePriceUpSelector implements IStockSelector {

    static Logger log = Logger.getLogger(ClosePriceUpSelector.class);
    
    private String start_dte = "", end_dte = "";
    
    public ClosePriceUpSelector (String s, String e) {
    	start_dte = s;
    	end_dte = e;
    }
    
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	
    	//first of all, let's make sure the trend of the stock is going up.
    	
    	if (!(SuggestStock.calculateStockTrend(s.getID()) > 0))
    	{
    		log.info("skip stock:" + s.getID() + "/" + s.getName() + " as trend is not going up.");
    		return false;
    	}
    	Connection con = DBManager.getConnection();
    	long max_ft_id = 0;
    	boolean isGoodStock = false;
    	
    	try {
    		Statement stm = con.createStatement();
    		String sql = "select max(ft_id) max_ft_id from stkdat2 s2 where s2.id = '" + s.getID() + "' and left(s2.dl_dt, 10) >= '" + start_dte + "' and left(s2.dl_dt, 10) < '" + end_dte + "'";
    		
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			max_ft_id = rs.getInt("max_ft_id");
    		}
    		
    		rs.close();
    		stm.close();
    		
    		sql = "select left(dl_dt, 10) dl_dt, cur_pri, td_opn_pri, yt_cls_pri from stkdat2 s where s.id = '" + s.getID() + "' and cur_pri > yt_cls_pri and cur_pri > td_opn_pri and ft_id = " + max_ft_id;
    		
    		log.info(sql);
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			String dl_dt = rs.getString("dl_dt");
    			double cur_pri = rs.getDouble("cur_pri");
    			double td_opn_pri = rs.getDouble("td_opn_pri");
    			double yt_cls_pri = rs.getDouble("yt_cls_pri");
    			log.info("Stock last price:" + cur_pri + " at date:" + dl_dt + " is higher than td_opn_pri:" + td_opn_pri + " and yt_cls_pri:" + yt_cls_pri + " return true.");
    			isGoodStock = true;
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
		log.info("ClosePriceUpSelector is mandatory criteria, no adjustment with harder:" + harder);
		return true;
	}
}
