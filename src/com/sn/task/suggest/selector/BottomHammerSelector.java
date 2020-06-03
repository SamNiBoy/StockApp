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
 * This selector used to find stock which has price on the bottom with minDays,
 * and has a long bottom thin foot line.
 */
public class BottomHammerSelector implements IStockSelector {

    static Logger log = Logger.getLogger(BottomHammerSelector.class);
    
    double topPct = 0.8;
    int minDays = 15;
    String on_dte = "";
    String suggest_by = "BottomHammerSelector";
    
    public BottomHammerSelector (String s) {
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
    	double td_opn_pri = 0.0;
    	double td_hst_pri = 0.0;
    	double td_lst_pri = 0.0;
    	double td_cls_pri = 0.0;
    	double min_cur_pri = 0.0;
    	double max_cur_pri = 0.0;
    	long max_td_ft_id = 0;
    	
    	
    	try {
			sql = "select max(yt_cls_pri) lst_cls_pri, max(td_opn_pri) td_opn_pri, max(cur_pri) max_cur_pri, min(cur_pri) min_cur_pri, max(ft_id) max_ft_id, left(dl_dt, 10) dte "
			    + "  from stkdat2 "
			    + " where id = '" + s.getID() + "' "
			    + "   and left(dl_dt, 10) = left(str_to_date('" + on_dte + "', '%Y-%m-%d'), 10)"
			    + " group by left(dl_dt, 10)";
			
			log.info(sql);
			
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			
			if (rs.next() && rs.getDouble("td_opn_pri") > 0) {
				td_opn_pri = rs.getDouble("td_opn_pri");
				td_hst_pri = rs.getDouble("max_cur_pri");
				td_lst_pri = rs.getDouble("min_cur_pri");
				max_td_ft_id = rs.getLong("max_ft_id");
			}
			
			rs.close();
			stm.close();
			
			if (max_td_ft_id > 0) {
			    sql = "select cur_pri td_cls_pri from stkdat2 "
			        + " where id = '" + s.getID() + "'"
			        + "   and ft_id = " + max_td_ft_id;
			    
			    log.info(sql);
			    
				stm = con.createStatement();
			    rs = stm.executeQuery(sql);
			    
			    rs.next();
			    td_cls_pri = rs.getDouble("td_cls_pri");
			    
			    rs.close();
			    stm.close();
			}
			else {
				log.info("No data stock:" + s.getID() + "on date:" + on_dte);
				return false;
			}
			
			log.info("Got min/max price for stock:" + s.getID() + " td_hst_pri/td_lst_pri: [" + td_hst_pri + "," + td_lst_pri + "]" + " td_opn_pri/td_cls_pri: [" + td_opn_pri + "," + td_cls_pri + "], shaking pct:" + (td_hst_pri - td_lst_pri) / td_cls_pri);
            
            if(td_cls_pri > td_opn_pri && td_opn_pri >= (td_lst_pri + topPct * (td_hst_pri - td_lst_pri)) && (td_hst_pri - td_lst_pri) / td_cls_pri > 0.04) {
            	
    			stm = con.createStatement();
    			
    			sql = "select min(cur_pri) min_cur_pri, max(cur_pri) max_cur_pri "
    				    + "  from stkdat2 "
    				    + " where id = '" + s.getID() + "' "
    				    + "   and left(dl_dt, 10) >= left(str_to_date('" + on_dte + "', '%Y-%m-%d') - interval " + minDays + " day, 10)"
    				    + "   and left(dl_dt, 10) <= left(str_to_date('" + on_dte + "', '%Y-%m-%d'), 10)";
    			
    			log.info(sql);
    			
    			rs = stm.executeQuery(sql);
    			
    			rs.next();
    			min_cur_pri = rs.getDouble("min_cur_pri");
    			max_cur_pri = rs.getDouble("max_cur_pri");
    			
    			rs.close();
    			stm.close();
    			
    			log.info("td_lst_pri" + td_lst_pri + " close to min_cur_pri:" + min_cur_pri + " for past:" + minDays + " days, max_cur_pri:" + max_cur_pri);
            	
            	if (Math.abs((td_lst_pri - min_cur_pri) / td_cls_pri) < 0.01 && (max_cur_pri - min_cur_pri) / td_cls_pri > 0.3)
            	{
                    s.setSuggestedBy(this.suggest_by);
                    s.setSuggestedComment("td_cls_pri:" + td_cls_pri + " is higher than td_opn_pri:" + td_opn_pri + " and formed a bottom hammer shape.");
                    s.setSuggestedscore(topPct);
            	    isGoodStock = true;
            	}
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
        
    	log.info("for stock:" + s.getID() + ", calculated isGoodStock:" + isGoodStock);
    	
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
		if (harder) {
			topPct += 0.02;
			minDays++;
		}
		else {
			topPct -= 0.02;
			minDays--;
		}
		
		if (minDays < 7) {
			minDays = 7;
		}
		
		if (topPct <= 0.7) {
			log.info("topPct reached 0.7, use 0.7");
			topPct = 0.7;
		}
		else if (topPct > 1.0) {
			log.info("topPct reached 1, use 0.9:" + 0.9);
			topPct = 0.9;
		}
		log.info("BottomHammerSelector is mandatory criteria, adjusted to topPct:" + topPct + ", minDays:" + minDays + " with harder:" + harder);
		return true;
	}
}
