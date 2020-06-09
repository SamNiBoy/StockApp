package com.sn.task.suggest.selector;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.strategy.TradeStrategyImp;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.IStockSelector;
import com.sn.task.suggest.SuggestStock;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class KShapeFilterSelector implements IStockSelector {

    /* This selector will remove stocks with price on highest but K shape goes worse.
     */
    static Logger log = Logger.getLogger(KShapeFilterSelector.class);
    private String on_dte = "";
    
    private String suggest_by = "KShapeFilterSelector";

    public KShapeFilterSelector (String s) {
    	on_dte = s;
    }
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
        
        boolean isgood = true;
        Connection con = DBManager.getConnection();
        
        double hst_pri = 0;
        double lst_pri = 0;
        
        try {
        	Statement stm = null;
            stm = con.createStatement();            
            String sql = "select max(cur_pri) hst_pri,"
                       + " min(cur_pri) lst_pri, "
                       + " max(ft_id) max_ft_id, "
                       + " left(dl_dt, 10) dte"
                       + "  from stkdat2 "
                       + " where id ='" + s.getID() + "'"
                       + "   and left(dl_dt, 10) <= '" + on_dte + "'"
                       + "  group by left(dl_dt, 10) order by dte desc";
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) {
                 long max_ft_id = rs.getLong("max_ft_id");
                 String dte = rs.getString("dte");
                 
                 log.info("got max_ft_id:" + max_ft_id + " at date:" + dte);
                 if (max_ft_id > 0 && rs.next()) {
                		hst_pri = rs.getDouble("hst_pri");
                        lst_pri = rs.getDouble("lst_pri");
                        dte = rs.getString("dte");
                        
                        log.info("Stock:" + s.getID() + " K shape check, 5 days hst_pri:" + hst_pri + ", lst_pri:" + lst_pri + " at date:" + dte);
                        sql = "select td_opn_pri, cur_pri, td_hst_pri, td_lst_pri, yt_cls_pri from stkdat2 s where s.id = '" + s.getID() + "' and s.ft_id = " + max_ft_id;
                        
                        log.info(sql);
                        
                        Statement stm2 = con.createStatement();
                        ResultSet rs2 = stm2.executeQuery(sql);
                        
                        if (rs2.next()) {
                   	     double td_opn_pri = rs2.getDouble("td_opn_pri");
                   	     double cur_pri = rs2.getDouble("cur_pri");
                   	     double td_hst_pri = rs2.getDouble("td_hst_pri");
                   	     double td_lst_pri = rs2.getDouble("td_lst_pri");
                   	     double yt_cls_pri = rs2.getDouble("yt_cls_pri");
                   	     
                   	     log.info("Stock:" + s.getID() + " K shape check, td_hst_pri:" + td_hst_pri + ", td_lst_pri:" + td_lst_pri + ", td_opn_pri:" + td_opn_pri + ", yt_cls_pri:" + yt_cls_pri + ", cur_pri:" + cur_pri);
                   	     //log.info("Stock:" + s.getID() + " is td_hst_pri on 5 days top?" + (Math.abs((td_hst_pri - hst_pri) / yt_cls_pri) < 0.01));
                   	    
                   	    	 double td_pct = (cur_pri - td_opn_pri) / yt_cls_pri;
                   	    	 double body_pct = (cur_pri - td_opn_pri) / (td_hst_pri - td_lst_pri);
                   	    	 boolean notontop = (td_hst_pri - cur_pri) / yt_cls_pri > 0.01;
                   	    	 boolean td_lst_pri_lower_than_yt_hst_pri = td_lst_pri < hst_pri;
                   	    	 
                   	    	log.info("Stock:" + s.getID() + " is today price lost(<0):" + td_pct + ", body pct(<0.3):" + body_pct + ", notontop:" + notontop + ", td_lst_pri_lower_than_yt_hst_pri:" + td_lst_pri_lower_than_yt_hst_pri);
                   	    	 if (td_pct <= 0 || body_pct < 0.3 || notontop) {
                   	    		isgood = false;
                   	    	 }
                   	    }
            	        stm2.close();
            	        rs2.close();
                 }
                 else {
                	 isgood = false;
                 }
            }
            else {
                log.info("Null value found, not good to suggest");
                isgood = false;
            }
            rs.close();
            stm.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
               log.error(e.getMessage() + " with error code:" + e.getErrorCode()); 
            }
        }
        
        if (isgood)
        {
            log.info("KShapeFilterSelector found stock:" + s.getID() + ", name:" + s.getName() + " passed K shape check.");
            s.setSuggestedBy(this.suggest_by);
            s.setSuggestedComment("KShapeFilterSelector found stock:" + s.getID() + ", name:" + s.getName() + " passed K shape check.");
            isgood = true;
        }
        return isgood;
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
        }
        else {
        }
        
        log.info("KShapeFilterSelector has no criteria adjust for harder:" + harder);
		return false;
	}
}
