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
import com.sn.task.IStockSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class PriceShakingStockSelector implements IStockSelector {

    /* This mode trying to split [lowest price, highest price] into 3 different areas,
     * lowest price is the lowest price since yesterday, highest price is highest price so far.
     * and count the number of times individual price across each different area, 
     * the higest number of times acrossed different area means best stock, which
     * is why it named 'Shaking' model.
     */
    static Logger log = Logger.getLogger(PriceShakingStockSelector.class);
    double line1_pri = 0.0;
    double line2_pri = 0.0;
    
    int to_lvl1_cnt = 0;
    int to_lvl2_cnt = 0;
    int to_lvl3_cnt = 0;
    
    int MIN_JUMP_TIMES_FOR_GOOD_STOCK = 10;
    double MIN_SHAKING_PCT = 0.06;
    
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
        
        boolean isgood = false;
        double hst_pri = 0.0;
        double lst_pri = 0.0;
        double pre_pri = 0.0;
        double cur_pri = 0.0;
        double yt_cls_pri = 0.0;
        
        double jump_area_pri = 0;
        
        int pre_area_id = 0;
        int cur_area_id = 0;
        
        int jump_area_cnt = 0;
        
        Connection con = DBManager.getConnection();
        Statement stm = null;
        try {
            stm = con.createStatement();
            String sql = "select max(cur_pri) hst_pri,"
                       + " min(cur_pri) lst_pri, "
                       + " max(yt_cls_pri) yt_cls_pri, "
                       + " min(cur_pri) + 1 / 3.0 * (max(cur_pri) - min(cur_pri)) line1_pri, "
                       + " min(cur_pri) + 2 / 3.0 * (max(cur_pri) - min(cur_pri)) line2_pri "
                       + "  from stkdat2 "
                       + " where id ='" + s.getID() + "'"
                       + "   and left(dl_dt, 10) >= left(sysdate()" + " - interval 1 day , 10)";
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next() && rs.getDouble("line1_pri") > 0) {
                 line1_pri = rs.getDouble("line1_pri");
                 line2_pri = rs.getDouble("line2_pri");
                 hst_pri = rs.getDouble("hst_pri");
                 lst_pri = rs.getDouble("lst_pri");
                 yt_cls_pri = rs.getDouble("yt_cls_pri");
            }
            else {
                log.info("Null value found, not good to suggest");
                return false;
            }
            
            rs.close();
            stm.close();
            
            log.info("PriceShakingStockSelector start with para:");
            log.info("name:" + s.getName());
            log.info("hst_pri:" + hst_pri);
            log.info("lst_pri:" + lst_pri + "\n");
            log.info("line1_pri:" + line1_pri);
            log.info("line2_pri:" + line2_pri + "\n");
            log.info("shaking percentage:" + (hst_pri - lst_pri) / yt_cls_pri);
            
            if ((hst_pri - lst_pri) / yt_cls_pri < MIN_SHAKING_PCT) {
                log.info("Price Shaking percentage: " + (hst_pri - lst_pri) / yt_cls_pri + " is less than:" + MIN_SHAKING_PCT + " not good for trade.");
                return false;
            }
            
            sql = "select cur_pri, ft_id, dl_dt"
                   + "  from stkdat2 "
                   + " where id ='" + s.getID() + "'"
                   + "   and left(dl_dt, 10) >= left(sysdate() " + "- interval 1 day , 10)"
                   + " order by ft_id";
            
            stm = con.createStatement();
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                
                 cur_pri = rs.getDouble("cur_pri");
                 Timestamp cur_tm = rs.getTimestamp("dl_dt");
                 
                 if (pre_pri == 0.0) {
                     pre_pri = cur_pri;
                     continue;
                 }
                 
                 if (pre_pri >= lst_pri && pre_pri < line1_pri)
                 {
                     pre_area_id = 1;
                 }
                 else if (pre_pri >= line1_pri && pre_pri < line2_pri)
                 {
                     pre_area_id = 2;
                 }
                 else {
                     pre_area_id = 3;
                 }
                 
                 if (cur_pri >= lst_pri && cur_pri < line1_pri)
                 {
                     cur_area_id = 1;
                 }
                 else if (cur_pri >= line1_pri && cur_pri < line2_pri)
                 {
                     cur_area_id = 2;
                 }
                 else {
                     cur_area_id = 3;
                 }
                 
                 log.info("line1_pri:" + line1_pri);
                 log.info("line2_pri:" + line2_pri + "\n");
                 
                 log.info("pre_pri:" + pre_pri);
                 log.info("cur_tm:" + cur_tm.toString());
                 log.info("cur_pri:" + cur_pri + "\n");
                 
                 log.info("pre_area_id:" + pre_area_id);
                 log.info("cur_area_id:" + cur_area_id + "\n");
                 
                 log.info("jump_area_pri:" + jump_area_pri + "\n");
                 log.info("jump_area_cnt:" + jump_area_cnt + "\n");
                 
                 if (jump_area_pri == 0)
                 {
                     if (cur_area_id != pre_area_id)
                     {
                         jump_area_cnt += Math.abs(cur_area_id - pre_area_id);
                         log.info("jump_area_cnt changed to:" + jump_area_cnt);
                         
                         jump_area_pri = cur_pri;
                     }
                 }
                 else {
                     if (Math.abs(jump_area_pri - cur_pri) >= (hst_pri - lst_pri) / 3)
                     {
                         jump_area_cnt += 1;
                         log.info("jump_area_cnt increase 1 becuase of cur_pri leave last jump_area_pri a lot.");
                         jump_area_pri = 0;
                     }
                     
                 }
                 
                 pre_pri = cur_pri;
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
               log.error(e.getMessage() + " with error code:" + e.getErrorCode()); 
            }
        }
        
        if (jump_area_cnt >= MIN_JUMP_TIMES_FOR_GOOD_STOCK)
        {
            log.info("PriceShakingStockSelector found stock:" + s.getID() + ", name:" + s.getName() + " jumped ares " + jump_area_cnt + " times, good for trade.");
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
            MIN_JUMP_TIMES_FOR_GOOD_STOCK++;
            MIN_SHAKING_PCT += 0.01;
        }
        else {
            MIN_JUMP_TIMES_FOR_GOOD_STOCK--;
        }
        
        log.info("try harder:" + harder);
        log.info("new`MIN_JUMP_TIMES_FOR_GOOD_STOCK:" + MIN_JUMP_TIMES_FOR_GOOD_STOCK);
        log.info("new MIN_SHAKING_PCT:" + MIN_SHAKING_PCT);
		return false;
	}
}
