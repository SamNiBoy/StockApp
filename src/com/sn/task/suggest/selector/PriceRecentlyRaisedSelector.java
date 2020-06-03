package com.sn.task.suggest.selector;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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
    private ArrayList<String> topNStocks = new ArrayList<String>();
    private ArrayList<Double> topNPcts = new ArrayList<Double>();
    int topN = 100;
    double pct = 0.5;
    
    public PriceRecentlyRaisedSelector (String s) {
    	on_dte = s;

    	Connection con = DBManager.getConnection();
    	double stkpct = 0.0;
    	
    	log.info("Build array list for topn stocks...");
    	try {
    		Statement stm = con.createStatement();
    		String sql = "select stk.id, stk.name, (s1.cur_pri - s2.cur_pri) / s2.yt_cls_pri pct " + 
    				"from stkdat2 s1 " + 
    				"join (select id, max(ft_id) mx_ft_id, min(ft_id) mn_ft_id from stkdat2 where left(stkdat2.dl_dt, 10) <= '" + on_dte + "' group by id) t2 " + 
    				"on s1.id = t2.id " + 
    				"and s1.ft_id = t2.mx_ft_id " + 
    				"join stkdat2 s2 " + 
    				"on s2.id = t2.id " + 
    				"and s2.ft_id = t2.mn_ft_id " + 
    				"join stk on s1.id = stk.id " +
    				" order by pct desc";
    		
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		int idx = 0;
    		while (rs.next()) {
    			idx++;
    			
    			if (idx > topN) {
    				break;
    			}
    			String stkid = rs.getString("id");
    			stkpct = rs.getDouble("pct");
    			
    			log.info("Stock:" + stkid + " is on top:" + idx + " with pct:" + stkpct);
    			topNStocks.add(stkid);
    			topNPcts.add(stkpct);
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
    
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	
    	boolean isGoodStock = false;
    	double stkpct = 0.0;
    	int idx = 0;
    	boolean found = false;
    	
    	for(String stkid : topNStocks) {
    		idx++;
    		if (s.getID().equals(stkid)) {
    			stkpct = topNPcts.get(idx - 1);
    			found = true;
    			break;
    		}
    	}
    	
    	if (found) {
    		
    	    log.info("Stock raised pct" + stkpct + " is in top:" + idx);
    	    
    	    if (stkpct >= pct) {
                s.setSuggestedBy(this.suggest_by);
                s.setSuggestedComment("Stock raised pct " + stkpct + " is in top:" + idx);
    	        isGoodStock = true;
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
			pct += 0.1;
		}
		else {
			pct -= 0.1;
		}
		
		if (pct < 0.3) {
			pct = 0.3;
		}
		log.info("try harder:" + harder + ", pct:" + pct + ", topN:" + topN);
		return true;
	}
}
