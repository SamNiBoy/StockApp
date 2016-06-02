package com.sn.trade.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
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
//    int MIN_INC_STOP_CNT = 3;
    static int MAX_STOCK_NUM = 50;
    
    static Double MAX_THRESH_VALUE = 0.01;
    static int    MAX_DAYS_VALUE = 7;
    
    static int MINUTES_FOR_EXPIRE = 5;
    
    
    static private Map<String, Double> topStocks = new HashMap<String, Double>();
    
    static private LocalDateTime lst_time = null;
	public TopGainStockSelector() {
	}
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	synchronized (topStocks) {
    		
    		boolean time_expired = false;
            LocalDateTime lt = LocalDateTime.now();
            
            log.info("Now time is:" + lt.toString() + ", lst_time is:" + (lst_time == null ? "null" : lst_time.toString()));
            if (lst_time == null || lt.minusMinutes(MINUTES_FOR_EXPIRE).isAfter(lst_time)) {
            	lst_time = lt;
            	time_expired = true;
            }

            log.info("time_expired is:" + time_expired);
    	    if (time_expired) {
    	    	topStocks.clear();
    	    }
    	    
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
    }
    
    private boolean buildtopStocks() {
        Connection con = DBManager.getConnection();

        int cnt = 0;

        try {
            Statement stm = con.createStatement();
//            String sql = " select id, " +
//            		     "        sum(case when (td_cls_pri - yt_cls_pri)/yt_cls_pri >=0.09 then 1 else 0 end) incStopCnt" +
//            		     "   from stkdlyinfo " +
//            		     "  where yt_cls_pri > 0 " +
//            		     "    and (td_cls_pri - td_opn_pri) / yt_cls_pri >= 0.06 " +
//            		     "    and dt >= to_char(sysdate - 14, 'yyyy-mm-dd') " +
//            		     "  group by id " +
//            		     "  having sum(case when (td_cls_pri - yt_cls_pri)/yt_cls_pri >=0.09 then 1 else 0 end) >=  " + MIN_INC_STOP_CNT +
//            		     "  order by incStopCnt desc";
            String sql = "select (tp.cur_pri - tp.td_opn_pri) / tp.yt_cls_pri detpri, tp.id from stkdat2 tp, stkdlyinfo yp "
                         + " where tp.td_opn_pri > yp.td_opn_pri "
                         + " and tp.id = yp.id "
                         + " and to_char(tp.dl_dt-1,'yyyy-mm-dd') = yp.dt "
                         + " and tp.cur_pri > tp.td_opn_pri "
                         + " and (tp.cur_pri - tp.td_opn_pri) / tp.yt_cls_pri < " + MAX_THRESH_VALUE
                         + " and tp.ft_id = (select max(ft_id) from stkdat2 t2 where tp.id = t2.id) "
                         + " and tp.cur_pri > (select max(td_hst_pri) from stkdlyinfo t3 where t3.id = tp.id and t3.dt >= to_char(tp.dl_dt - " + MAX_DAYS_VALUE + ",'yyyy-mm-dd') and t3.dt < to_char(tp.dl_dt,'yyyy-mm-dd')) "
                         + " and not exists (select 'x' from stkdlyinfo t4 where tp.id = t4.id and t4.dt >= to_char(sysdate - " + MAX_DAYS_VALUE + ", 'yyyy-mm-dd') and (t4.td_cls_pri - t4.td_opn_pri) / t4.yt_cls_pri > 0.03) "
                         + " order by detpri desc ";
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);
            while (rs.next() && cnt < MAX_STOCK_NUM) {
                log.info("Build topStocks: " + rs.getString("id") + " detpri:" + rs.getDouble("detpri"));
                topStocks.put(rs.getString("id"), rs.getDouble("detpri"));
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
		return false;
	}
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		log.info("try " + (harder ? " harder" : " loose") + " MAX_THRESH_VALUE:" + MAX_THRESH_VALUE + " MAX_DAYS_VALUE:" + MAX_DAYS_VALUE);
		if (harder) {
			MAX_DAYS_VALUE ++;
		    MAX_STOCK_NUM--;
		    if (MAX_DAYS_VALUE > 14) {
		    	MAX_DAYS_VALUE = 14;
		    }
		    if (MAX_STOCK_NUM < 20) {
		        MAX_STOCK_NUM = 20;
		    }
		    MAX_THRESH_VALUE -= 0.01;
		    if (MAX_THRESH_VALUE < 0.01) {
		    	MAX_THRESH_VALUE = 0.01;
		    }
		}
		else {
			MAX_THRESH_VALUE += 0.01;
			MAX_DAYS_VALUE--;
			if (MAX_THRESH_VALUE > 0.03) {
				MAX_THRESH_VALUE = 0.03;
			}
			if (MAX_DAYS_VALUE < 3) {
				MAX_DAYS_VALUE = 3;
			}
		}
	    log.info("After adjust, try " + (harder ? " harder" : " loose") + " MAX_THRESH_VALUE:" + MAX_THRESH_VALUE + " MAX_DAYS_VALUE:" + MAX_DAYS_VALUE);

		return false;
	}
}
