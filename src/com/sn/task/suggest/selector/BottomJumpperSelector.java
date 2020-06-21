package com.sn.task.suggest.selector;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.json.JSONArray;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.strategy.TradeStrategyImp;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.IStockSelector;
import com.sn.task.suggest.SuggestStock;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

/*
 * This selector used to find stock which has jump up today when price is overall at bottom.
 */
public class BottomJumpperSelector implements IStockSelector {

    static Logger log = Logger.getLogger(BottomJumpperSelector.class);
    
    double topPct = 0.1;
    int minDays = 10;
    String on_dte = "";
    String suggest_by = "BottomJumpperSelector";
    
    public BottomJumpperSelector (String s) {
    	on_dte = s;
    }
    
    public void setMinValues(double minpct, int mindys) {
    	topPct = minpct;
    	minDays = mindys;
    }
    
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	
    	boolean isGoodStock = false;
    	String sql = "";
    	Connection con = DBManager.getConnection();
    	Statement stm = null;
    	ResultSet rs = null;
    	double max_pst_cur_pri = 0.0;
    	double td_lst_pri = 0.0;
    	double pct = 0.0;
    	long max_td_ft_id = 0;
    	
    	
    	try {
			sql = "select max(cur_pri) max_pst_cur_pri, (max(yt_cls_pri) - min(yt_cls_pri)) / min(yt_cls_pri) pct, max(ft_id) max_ft_id "
			    + "  from stkdat2 "
			    + " where id = '" + s.getID() + "' "
			    + "   and left(dl_dt, 10) < left(str_to_date('" + on_dte + "', '%Y-%m-%d'), 10)"
			    + "   and left(dl_dt + interval " + minDays + " day, 10) > left(str_to_date('" + on_dte + "', '%Y-%m-%d'), 10)";
			
			log.info(sql);
			
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			
			if (rs.next() && rs.getDouble("max_pst_cur_pri") > 0) {
				max_pst_cur_pri = rs.getDouble("max_pst_cur_pri");
				pct = rs.getDouble("pct");
				max_td_ft_id = rs.getLong("max_ft_id");
			}
			
			rs.close();
			stm.close();
			
			if (max_td_ft_id > 0) {
			    sql = "select td_lst_pri from stkdat2 "
			        + " where id = '" + s.getID() + "'"
			        + "   and ft_id = " + max_td_ft_id;
			    
			    log.info(sql);
			    
				stm = con.createStatement();
			    rs = stm.executeQuery(sql);
			    
			    rs.next();
			    td_lst_pri = rs.getDouble("td_lst_pri");
			    
			    rs.close();
			    stm.close();
			}
			else {
				log.info("No data stock:" + s.getID() + "on date:" + on_dte);
				return false;
			}
			
			log.info("Got min/max price for stock:" + s.getID() + " max_pst_cur_pri: [" + max_pst_cur_pri + "], td_lst_pri:" + td_lst_pri + ", pct:" + pct);
            
            if(pct <= topPct && td_lst_pri > max_pst_cur_pri) {
                    s.setSuggestedBy(this.suggest_by);
                    s.setSuggestedComment("bottom pct:" + pct + " today lowest price :" + td_lst_pri + " jumpped over passed max price:" + max_pst_cur_pri);
                    s.setSuggestedscore(topPct);
            	    isGoodStock = true;
            }
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    	}
    	finally {
    		try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getMessage(), e);
			}
    	}
        
    	log.info("for stock:" + s.getID() + ",BottomJumpperSelector calculated isGoodStock:" + isGoodStock);
    	
        return isGoodStock;
    }
    
    
	@Override
	public boolean isORCriteria() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean isMandatoryCriteria() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		if (harder) {
			topPct -= 0.02;
			minDays++;
		}
		else {
			topPct += 0.02;
			minDays--;
		}
		
		if (minDays < 5) {
			minDays = 5;
		}
		
		if (topPct > 0.2) {
			log.info("topPct reached 0.2, use 0.2");
			topPct = 0.2;
		}
		else if (topPct < 0.1) {
			log.info("topPct reached 0.1, use 0.1");
			topPct = 0.1;
		}
		log.info("BottomJumpperSelector is mandatory criteria, adjusted to topPct:" + topPct + ", minDays:" + minDays + " with harder:" + harder);
		return true;
	}
}
