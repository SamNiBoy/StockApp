package com.sn.trade.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock2;

public class TopGainStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(TopGainStockSelector.class);
    int MIN_INC_STOP_CNT = 3;
    int MAX_STOCK_NUM = 50;
    
    static private Map<String, Integer> topStocks = new HashMap<String, Integer>();
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
        if (topStocks.isEmpty()) {
            buildtopStocks();
        }
        
        if (!topStocks.isEmpty() && topStocks.containsKey(s.getID())) {
            log.info("Stock:" + s.getID() + " is selected as good stock from " + topStocks.size() + " stocks.");
            return true;
        }
        else {
            log.info("Stock:" + s.getID() + " is NOT selected as good stock from " + topStocks.size() + " stocks.");
            return false;
        }
    }
    
    private boolean buildtopStocks() {
        Connection con = DBManager.getConnection();

        int cnt = 0;

        try {
            Statement stm = con.createStatement();
            String sql = " select id, " +
            		     "        sum(case when (td_cls_pri - yt_cls_pri)/yt_cls_pri >=0.09 then 1 else 0 end) incStopCnt" +
            		     "   from stkdlyinfo " +
            		     "  where yt_cls_pri > 0 " +
            		     "    and dt >= to_char(sysdate - 14, 'yyyy-mm-dd') " +
            		     "  group by id " +
            		     "  having sum(case when (td_cls_pri - yt_cls_pri)/yt_cls_pri >=0.09 then 1 else 0 end) >=  " + MIN_INC_STOP_CNT +
            		     "  order by incStopCnt desc";
            
            ResultSet rs = stm.executeQuery(sql);
            while (rs.next() && cnt < MAX_STOCK_NUM) {
                log.info("Build topStocks: " + rs.getString("id") + " incStopCnt:" + rs.getInt("incStopCnt"));
                topStocks.put(rs.getString("id"), rs.getInt("incStopCnt"));
                cnt++;
            }
            rs.close();
            stm.close();
            con.close();
        }
        catch (Exception e) {
            log.info("buildtopStocks exception:" + e.getMessage());
        }
        log.info("Total loaded: " + cnt + " stocks.");
        if (cnt > 0) {
            return true;
        }
        else {
            return false;
        }
    }
	@Override
	public boolean isORCriteria() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean isMandatoryCriteria() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		log.info("try " + (harder ? " harder" : " loose") + " MIN_INC_STOP_CNT:" + MIN_INC_STOP_CNT + " MAX_STOCK_NUM:" + MAX_STOCK_NUM);
		if (harder) {
		    MIN_INC_STOP_CNT ++;
		    MAX_STOCK_NUM--;
		    if (MIN_INC_STOP_CNT > 14) {
		        MIN_INC_STOP_CNT = 14;
		    }
		    if (MAX_STOCK_NUM < 20) {
		        MAX_STOCK_NUM = 20;
		    }
		}
	    log.info("After adjust, try " + (harder ? " harder" : " loose") + " MIN_INC_STOP_CNT:" + MIN_INC_STOP_CNT + " MAX_STOCK_NUM:" + MAX_STOCK_NUM);

		return false;
	}
}
