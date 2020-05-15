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
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class DaBanStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(DaBanStockSelector.class);
    int days = 7;
	String on_dte = "";
	double minPct = 0.09;
    private String suggest_by = "DaBanStockSelector";

	public DaBanStockSelector(String od) {
		on_dte = od;
	}
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	boolean isgood = false;
        double pct = 0.0;
        double cur_pri = 0;
        double td_opn_pri = 0;
        double yt_cls_pri = 0;
        double dl_mny_num =0;
 		Connection con = DBManager.getConnection();
   		Statement stm = null;
    	try {
     		stm = con.createStatement();
    		String sql = "select cur_pri, yt_cls_pri, td_opn_pri, (cur_pri - yt_cls_pri) / yt_cls_pri pct, id, dl_mny_num"
    				   + "  from stkdat2 "
    				   + " where id ='" + s.getID() + "'"
    				   + "   and ft_id = (select max(ft_id) from stkdat2 s2 where s2.id = '" + s.getID() + "' and left(s2.dl_dt, 10) = '" + on_dte + "')";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			cur_pri = rs.getDouble("cur_pri");
    			td_opn_pri = rs.getDouble("td_opn_pri");
    			yt_cls_pri = rs.getDouble("yt_cls_pri");
    			dl_mny_num = rs.getDouble("dl_mny_num");
    			
    			pct = rs.getDouble("pct");
    			
    			log.info("suggest stock:" + s.getID() + "\n td_opn_pri > yt_cls_pri: " + td_opn_pri + " > " + yt_cls_pri + " ? " + (td_opn_pri > yt_cls_pri));
    			log.info("pct > minPct:" + pct + " > " + minPct + " ? " + (pct > minPct));
    			
    			if (td_opn_pri > yt_cls_pri && pct > minPct) {
    	            s.setSuggestedBy(this.suggest_by);
    	            s.setSuggestedComment("td_opn_pri > yt_cls_pri: " + td_opn_pri + " > " + yt_cls_pri + " ? " + (td_opn_pri > yt_cls_pri) + " and pct > minPct:" + pct + " > " + minPct + " ? " + (pct > minPct));
    				return true;
    			}
    		}
            rs.close();
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                log.info(e.getMessage());
            }
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
		return true;
	}
}
