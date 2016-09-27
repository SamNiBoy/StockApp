package com.sn.trade.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.trade.strategy.imp.STConstants;

public class MostPlusStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(MostPlusStockSelector.class);
    static int MIN_PRE_COUNT = 5;
    static int MIN_TODAY_COUNT = 1;
    
    static private Map<String, StockPlusCount> topStocks = new HashMap<String, StockPlusCount>();
    static private Map<String, StockPlusCount> allStocks = new HashMap<String, StockPlusCount>();
    
    static private LocalDateTime lst_time = null;
    static int MINUTES_FOR_EXPIRE = 5;
    static boolean criteria_chg = false;
    
	private class StockPlusCount implements Comparable<StockPlusCount>{
    	public String stkID;
    	public int past_count;
    	public int today_count;
    	StockPlusCount(String stk) {
    		stkID = stk;
    		past_count = today_count = 0;
    	}
		@Override
		public int compareTo(StockPlusCount other) {
			// TODO Auto-generated method stub
			return -((past_count + today_count) - (other.past_count + other.today_count));
		}
    }
	public MostPlusStockSelector() {
	}
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
    	synchronized (allStocks) {
    		
            buildAllStocks();
        
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
    
    private boolean buildAllStocks() {
        
        boolean DataChanged = false;

        if (allStocks.isEmpty()) {
            int daysCnt = StockMarket.getNumDaysAhead("000001", STConstants.DEV_CALCULATE_DAYS);
            
            try {
                Connection con = DBManager.getConnection();
                Statement stm = con.createStatement();
                String sql = "select cur_pri, yt_cls_pri, id, to_char(dl_dt,'yyyy-mm-dd') dayStr, to_char(sysdate, 'yyyy-mm-dd') todayStr "
                		     + " from stkdat2 s"
                		     + " where dl_dt >= sysdate - " + daysCnt
                             + " order by id, ft_id ";
                log.info(sql);
                ResultSet rs = stm.executeQuery(sql);
                double maxPri = 0, minPri = 0, curpri = 0, yt_cls_pri = 0;
                String stkId = "", pre_stkId = "";
                String dayStr = "", todayStr = "", pre_dayStr = "";
                StockPlusCount spc = null;
                
                while (rs.next()) {
                	stkId = rs.getString("id");
                	curpri = rs.getDouble("cur_pri");
                	yt_cls_pri = rs.getDouble("yt_cls_pri");
                	dayStr = rs.getString("dayStr");
                	todayStr = rs.getString("todayStr");
                	
                	if (!stkId.equals(pre_stkId)) {
                		if (spc != null && pre_stkId.length() > 0) {
                			log.info("Create Stock:" + spc.stkID + ", past_count:" + spc.past_count + ", today_count:" + spc.today_count);
                			allStocks.put(pre_stkId, spc);
                		}
                	    spc = new StockPlusCount(stkId);
                	    pre_stkId = stkId;
                	}
                	
                	if (!pre_dayStr.equals(dayStr)) {
                		maxPri = minPri = 0;
                		pre_dayStr = dayStr;
                	}
                	
                	if (maxPri < curpri) {
                		maxPri = curpri;
                	}
                	
                	if (minPri > curpri || minPri == 0) {
                		minPri = curpri;
                	}
                	
                	if ((maxPri - minPri) / yt_cls_pri * 1.0 > STConstants.BASE_TRADE_THRESH) {
                		if (dayStr.equals(todayStr)) {
                			spc.today_count++;
                		}
                		else {
                		    spc.past_count++;
                		}
                		log.info("Added counts for Stock:" + spc.stkID + ", past_count:" + spc.past_count + ", today_count:" + spc.today_count);
                		maxPri = minPri = 0;
                	}
                }
                
        		if (spc != null && pre_stkId.length() > 0) {
        			log.info("Last Create Stock:" + spc.stkID + ", past_count:" + spc.past_count + ", today_count:" + spc.today_count);
        			allStocks.put(pre_stkId, spc);
        		}
        		
                rs.close();
                stm.close();
                con.close();
                
                if (!allStocks.isEmpty()) {
                    DataChanged = true;
                }
            }
            catch (Exception e) {
                log.info("allStocks exception:" + e.getMessage());
            }
        }
        else {
        	
    		boolean time_expired = false;
            LocalDateTime lt = LocalDateTime.now();
            
            log.info("Now time is:" + lt.toString() + ", lst_time is:" + (lst_time == null ? "null" : lst_time.toString()));
            if (lst_time == null || lt.minusMinutes(MINUTES_FOR_EXPIRE).isAfter(lst_time)) {
            	lst_time = lt;
            	time_expired = true;
            }

            log.info("time_expired is:" + time_expired);
    	    if (time_expired) {
                try {
                    Connection con = DBManager.getConnection();
                    Statement stm = con.createStatement();
                    String sql = "select cur_pri, yt_cls_pri, id, to_char(dl_dt,'yyyy-mm-dd') dayStr, to_char(sysdate, 'yyyy-mm-dd') todayStr "
                    		     + " from stkdat2 s"
                    		     + " where to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') "
                                 + " order by id, ft_id ";
                    log.info(sql);
                    ResultSet rs = stm.executeQuery(sql);
                    double maxPri = 0, minPri = 0, curpri = 0, yt_cls_pri = 0;
                    String stkId = "", pre_stkId = "";
                    StockPlusCount spc = null;
                    
                    while (rs.next()) {
                    	stkId = rs.getString("id");
                    	curpri = rs.getDouble("cur_pri");
                    	yt_cls_pri = rs.getDouble("yt_cls_pri");
                    	
                    	if (!stkId.equals(pre_stkId)) {
                    		
                    		if (spc != null && pre_stkId.length() > 0) {
                    			log.info("Update Stock:" + spc.stkID + ", past_count:" + spc.past_count + ", today_count:" + spc.today_count);
                    			allStocks.put(pre_stkId, spc);
                    			DataChanged = true;
                    		}
                    		spc = allStocks.get(stkId);
                    		if (spc == null) {
                    			log.info("this should never happen:" + stkId);
                    			continue;
                    		}
                    		spc.today_count = 0;
                    	    pre_stkId = stkId;
                    	}
                    	
                    	if (maxPri < curpri) {
                    		maxPri = curpri;
                    	}
                    	
                    	if (minPri > curpri || minPri == 0) {
                    		minPri = curpri;
                    	}
                    	
                    	if ((maxPri - minPri) / yt_cls_pri * 1.0 > STConstants.BASE_TRADE_THRESH) {
                    	    spc.today_count++;
                    	    log.info("Increase today_count for Stock:" + spc.stkID + ", past_count:" + spc.past_count + ", today_count:" + spc.today_count);
                    		maxPri = minPri = 0;
                    	}
                    }
                    rs.close();
                    stm.close();
                    con.close();
                }
                catch (Exception e) {
                    log.info("2 allStocks exception:" + e.getMessage());
                }
    	    }
        }
        
        if (DataChanged || criteria_chg) {
            List<StockPlusCount> lst = new ArrayList<StockPlusCount>(allStocks.values());
            Collections.sort(lst);
            StockPlusCount spc = null;
            
            for (int j = 0; j < lst.size(); j++) {
            	spc = lst.get(j);
            	log.info("Print No." + (j+1) + " Stock:" + spc.stkID + ", past_count:" +spc.past_count + ", today_count:" + spc.today_count);
            }
            
            topStocks.clear();
            for (int i = 0; i < lst.size(); i++) {
            	spc = lst.get(i);
            	if (spc.past_count >= MIN_PRE_COUNT && spc.today_count >= MIN_TODAY_COUNT) {
            	    log.info("Adding to topStocks, Stock:" + spc.stkID + ", past_count:" +spc.past_count + ", today_count:" + spc.today_count);
            	    topStocks.put(lst.get(i).stkID, lst.get(i));
            	}
            }
            criteria_chg = false;
        }
        
        log.info("Total loaded: " + topStocks.size() + " stocks.");
        if (topStocks.size() > 0) {
            return true;
        }
        else {
            return false;
        }
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
		log.info("try " + (harder ? " harder" : " loose") + " MIN_PRE_COUNT:" + MIN_PRE_COUNT + ", MIN_TODAY_COUNT:" + MIN_TODAY_COUNT);
		if (harder) {
		    MIN_PRE_COUNT++;
		    if (MIN_PRE_COUNT > 5) {
		    	MIN_PRE_COUNT = 5;
		    }
		    
		    MIN_TODAY_COUNT++;
		    if (MIN_TODAY_COUNT > 2) {
		    	MIN_TODAY_COUNT = 2;
		    }
		}
		else {
			
		    MIN_PRE_COUNT--;
		    if (MIN_PRE_COUNT < 2) {
		    	MIN_PRE_COUNT = 2;
		    }
		    
		    MIN_TODAY_COUNT--;
		    if (MIN_TODAY_COUNT < 0) {
		    	MIN_TODAY_COUNT = 0;
		    }
		}
		
		criteria_chg = true;
		log.info("After try " + (harder ? " harder" : " loose") + " MIN_PRE_COUNT:" + MIN_PRE_COUNT + ", MIN_TODAY_COUNT:" + MIN_TODAY_COUNT);

		return false;
	}
    @Override
    public Integer getTradeModeId() {
        // TODO Auto-generated method stub
        return STConstants.TRADE_MODE_ID_QTYTRADE;
    }
	@Override
	public boolean shouldStockExitTrade(String stkId) {
		// TODO Auto-generated method stub
		StockPlusCount spc = null;
		spc = allStocks.get(stkId);
		
		if (spc != null) {
			log.info("shouldStockExitTrade for:" + stkId + " return today_count:" + spc.today_count + " past_count:" + spc.past_count + " <= 0 " + (spc.today_count + spc.past_count <= 0));
			if (spc.today_count + spc.past_count <= 0) {
				log.info("stock:" + stkId + " no trade chance, should exit trade.");
				return true;
			}
		}
		return false;
	}
}
