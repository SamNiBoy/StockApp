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

public class CollectionPricingStockSelector implements IStockSelector {

    /* This selector is looking at stock with a good increase during the day
     * if the b1_num > 10000 and b1_num / s1_num > 1 at 9:25;
     */
    static Logger log = Logger.getLogger(CollectionPricingStockSelector.class);
    private String on_dte = "";
    
    private double min_winpct = 0.05;
    private String suggest_by = "CollectionPricingStockSelector";

    public CollectionPricingStockSelector (String s) {
    	on_dte = s;
    }
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
        
        boolean isgood = false;
        double winpct = 0.0;
        int wincnt = 0;
        int lstcnt = 0;
        double gainPct = 0;
        double lostPct = 0;
        
        Connection con = DBManager.getConnection();
        Statement stm = null;
        try {
            stm = con.createStatement();            
            String sql = "select distinct s1.b1_num,"
                       + " s1.s1_num, "
            		   + " s1.b1_num / s1.s1_num rt, "
                       + " (s2.cur_pri - s1.cur_pri) / s2.yt_cls_pri pct,"
            		   + " left(s1.dl_dt, 10) dte "
                       + "  from stkdat2 s1 "
                       + "  join (select max(ft_id) max_ft_id, id, left(dl_dt, 10) dte from stkdat2 where id = '" + s.getID() + "' group by left(dl_dt, 10), id ) t "
                       + "    on s1.id = t.id "
                       + "   and left(s1.dl_dt, 10) = t.dte "
            		   + "  join stkdat2 s2 "
            		   + "    on s1.id = s2.id "
            		   + "   and s2.ft_id = t.max_ft_id "
                       + " where s1.id ='" + s.getID() + "'"
                       + "   and left(s1.dl_dt, 10) <= '" + on_dte + "'"
                       + "   and right(left(s1.dl_dt, 16), 5) = '09:25'"
                      // + "   and s1.s1_num >= 10000 "
                       + "   and s1.b1_num / s1.s1_num > 1"
                       + "  order by dte";
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);

            while (rs.next()) {
            	double pct = rs.getDouble("pct");
            	String dte = rs.getString("dte");
            	log.info("got pct:" + pct + " for date:" + dte);
            	if (pct > 0.01) {
            		wincnt++;
            		gainPct += pct;
            	}
            	else {
            		lstcnt++;
            		lostPct += Math.abs(pct);
            	}
            }
            
            winpct = (gainPct - lostPct) * 1.0 / (wincnt + lstcnt);
            log.info("stock:" + s.getID() + " wincnt:" + wincnt + ", lstcnt:" + lstcnt + "total gainPct:" + gainPct + ", total lostPct:" + lostPct + ", avg Gain pct:" + winpct);
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
        
        if (winpct > min_winpct && (wincnt + lstcnt) >= 4)
        {
            log.info("CollectionPricingStockSelector found stock:" + s.getID() + ", name:" + s.getName() + " good for trade, winpct:" + winpct);
            s.setSuggestedBy(this.suggest_by);
            s.setSuggestedComment("CollectionPricingStockSelector found stock:" + s.getID() + ", name:" + s.getName() + " good for trade, winpct:" + winpct);
            s.setSuggestedscore(winpct);
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
        	min_winpct+= 0.01;
        }
        else {
        	min_winpct-= 0.01;
        }
        
        if (min_winpct < 0.03)
        {
        	log.info("min_winpct can not less than 0.3, use 0.3");
        	min_winpct = 0.03;
        }
        log.info("try harder:" + harder);
        log.info("new min_winpct:" + min_winpct);
		return false;
	}
}
