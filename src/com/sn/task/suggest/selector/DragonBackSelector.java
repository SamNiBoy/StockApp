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
 * This selector used to find stock which has increased a lot but drop a lot
 * in minDay days.
 */
public class DragonBackSelector implements IStockSelector {

    static Logger log = Logger.getLogger(DragonBackSelector.class);
    
    double pct1 = 0.2;
    double pct2 = 0.8;
    String on_dte = "";
    String suggest_by = "DragonBackSelector";
    
    public DragonBackSelector (String s) {
    	on_dte = s;
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
    	double avgytclspri = 0.0;
    	double td_cls_pri = 0.0;
    	double pct = 0.0;
    	double yt_cls_pri = 0.0;
    	long max_td_ft_id = 0;
    	String dte = "";
    	double lstClsPriPct1 = 0.0;
    	double lstClsPriPct2 = 0.0;
    	
    	try {
			sql = "select max(td_opn_pri) td_opn_pri, max(yt_cls_pri) yt_cls_pri, left(dl_dt, 10) dte, max(ft_id) max_ft_id "
			    + "  from stkdat2 "
			    + " where id = '" + s.getID() + "' "
			    + "   and left(dl_dt, 10) <= left(str_to_date('" + on_dte + "', '%Y-%m-%d'), 10)"
			    + " group by left(dl_dt, 10) "
			    + "  order by dte desc";
			
			log.info(sql);
			
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			
			if (rs.next()) {
				max_td_ft_id = rs.getLong("max_ft_id");
				yt_cls_pri = rs.getDouble("yt_cls_pri");
				dte = rs.getString("dte");
			}
			if (max_td_ft_id > 0) {
			    sql = "select cur_pri td_cls_pri from stkdat2 "
			        + " where id = '" + s.getID() + "'"
			        + "   and ft_id = " + max_td_ft_id;
			    
			    log.info(sql);
			    
				Statement stm2 = con.createStatement();
			    ResultSet rs2 = stm2.executeQuery(sql);
			    
			    rs2.next();
			    td_cls_pri = rs2.getDouble("td_cls_pri");

			    rs2.close();
			    stm2.close();
			    
			    log.info("stock:" + s.getID() + ", yt_cls_pri:" + yt_cls_pri + ", td_cls_pri:"+ td_cls_pri + ", at date:" + dte);
			    if (td_cls_pri > yt_cls_pri) {
			    	int cnt = 4;
			    	double max_yt_cls_pri = yt_cls_pri;
			    	double min_yt_cls_pri = yt_cls_pri;
			    	while(cnt > 0 && rs.next()) {
			    		cnt--;
			    		yt_cls_pri = rs.getDouble("yt_cls_pri");
			    		dte = rs.getString("dte");
			    		
			    		log.info("get yt_cls_pri:" + yt_cls_pri + " at date " + dte + " for stock:" + s.getID());
			    		
			    		if (max_yt_cls_pri < yt_cls_pri) {
			    			max_yt_cls_pri = yt_cls_pri; 
			    		}
			    		if (min_yt_cls_pri > yt_cls_pri) {
			    			min_yt_cls_pri = yt_cls_pri;
			    		}
			    	}
			    	
			    	log.info("get min_yt_cls_pri:" + min_yt_cls_pri + ", max_yt_cls_pri:" + max_yt_cls_pri + " for stock:" + s.getID() + " for past " + (5 - cnt) + " days.");
			    	
			    	if (cnt == 0) {
			    		double yt_cls_pri_bak = yt_cls_pri;
				    	lstClsPriPct1 = (td_cls_pri - min_yt_cls_pri) / (max_yt_cls_pri - min_yt_cls_pri);
				    	
				    	log.info("stock:" + s.getID() + " is " + lstClsPriPct1 + " close to past 5 days yt_cls_pri.");
			    		if (lstClsPriPct1 < pct1) {
			    			int reminDays = 10;
				    		max_yt_cls_pri = yt_cls_pri;
					    	min_yt_cls_pri = yt_cls_pri;
			    			while(reminDays > 0 && rs.next()) {
			    				reminDays--;
			    				yt_cls_pri = rs.getDouble("yt_cls_pri");
			    				
					    		if (max_yt_cls_pri < yt_cls_pri) {
					    			max_yt_cls_pri = yt_cls_pri; 
					    		}
					    		if (min_yt_cls_pri > yt_cls_pri) {
					    			min_yt_cls_pri = yt_cls_pri;
					    		}
			    			}
			    			
			    			if (reminDays == 0) {
			    				lstClsPriPct2 = (yt_cls_pri_bak - min_yt_cls_pri) / (max_yt_cls_pri - min_yt_cls_pri);
			    				log.info("stock:" + s.getID() + ", reminDays:" + reminDays + ", lstClsPriPct:" + lstClsPriPct2);
			    				if (lstClsPriPct2 > pct2) {
			    					isGoodStock = true;
			    				}
			    			}
			    		}
			    	}
			    }

			}
			else {
				log.info("No data stock:" + s.getID() + "on date:" + on_dte);
			}
			
			rs.close();
			stm.close();
			
            if(isGoodStock) {
                    s.setSuggestedBy(this.suggest_by);
                    s.setSuggestedComment("td_cls_pri:" + td_cls_pri + " is close to 5 days bottom pct:" + lstClsPriPct1 + " 5 days back is high pct:" + lstClsPriPct2);
                    s.setSuggestedscore(pct1 + pct2);
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
        
    	log.info("for stock:" + s.getID() + ",DragonBackSelector calculated isGoodStock:" + isGoodStock);
    	
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
			pct1 -= 0.02;
			pct2 += 0.02;
		}
		else {
			pct1 += 0.02;
			pct2 -= 0.02;
		}
		
		log.info("DragonBackSelector is mandatory criteria, adjusted to pct1:" + pct1 + ", pct2:" + pct2 + " with harder:" + harder);
		return true;
	}
}
