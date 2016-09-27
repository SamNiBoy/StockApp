package com.sn.trade.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.imp.TradeStrategyImp;

public class StddevStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(StddevStockSelector.class);

    double minStddev = STConstants.MIN_STEDEV_VALUE;
    double maxStddev = STConstants.MAX_STEDEV_VALUE;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
    	boolean isgood = false;
    	double dev1 = -1, dev2 = -1, dev3 = -1;
    	int daysCnt = StockMarket.getNumDaysAhead(s.getID(), STConstants.DEV_CALCULATE_DAYS);
    	if (s.isStockDivided()) {
    		log.info("Can not suggest stock:" + s.getID() + " as it was divided!");
    		return false;
    	}
    	try {
    		Connection con = DBManager.getConnection();
    		Statement stm = con.createStatement();
    		String sql = "select avg(dev) dev from ("
    				   + "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, to_char(dl_dt, 'yyyy-mm-dd') atDay "
    				   + "  from stkdat2 "
    				   + " where id ='" + s.getID() + "'"
    				   + "   and to_char(dl_dt, 'yyyy-mm-dd') >= to_char(sysdate - " + daysCnt + ", 'yyyy-mm-dd')"
    				   + "   and not exists (select 'x' from usrStk where id ='" + s.getID() + "' and sell_mode_flg = 1)"
    				   + " group by to_char(dl_dt, 'yyyy-mm-dd'))";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			 dev1 = rs.getDouble("dev");
    			if (dev1 >= minStddev && dev1 <= maxStddev) {
    				isgood = true;
    			}
    		}
    		rs.close();
    		stm.close();
    		if (isgood) {
    			isgood = false;
    			daysCnt = StockMarket.getNumDaysAhead(s.getID(), STConstants.DEV_CALCULATE_DAYS * 2);
    	       	sql = "select avg(dev) dev from ("
 		       		   + "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, to_char(dl_dt, 'yyyy-mm-dd') atDay "
 		       		   + "  from stkdat2 "
 		       		   + " where id ='" + s.getID() + "'"
 		       		   + "   and to_char(dl_dt, 'yyyy-mm-dd') >= to_char(sysdate - " + daysCnt + ", 'yyyy-mm-dd')"
 		       		   + "   and not exists (select 'x' from usrStk where id ='" + s.getID() + "' and sell_mode_flg = 1)"
 		       		   + " group by to_char(dl_dt, 'yyyy-mm-dd'))";
 		        log.info(sql);
 		        stm = con.createStatement();
 		        rs = stm.executeQuery(sql);
 		        if (rs.next()) {
 		            dev2 = rs.getDouble("dev");
 		            log.info("dev2:" + dev2 + "< dev1:" + dev1 + "? " + (dev2 < dev1));
 		          	if (dev2 < dev1) {
 		          		isgood = true;
 		       	    }
 		        }
 		        rs.close();
 		        stm.close();
    		}
    		
    		if (isgood) {
    			isgood = false;
    			daysCnt = StockMarket.getNumDaysAhead(s.getID(), STConstants.DEV_CALCULATE_DAYS * 3);
    	       	sql = "select avg(dev) dev from ("
 		       		   + "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, to_char(dl_dt, 'yyyy-mm-dd') atDay "
 		       		   + "  from stkdat2 "
 		       		   + " where id ='" + s.getID() + "'"
 		       		   + "   and to_char(dl_dt, 'yyyy-mm-dd') >= to_char(sysdate - " + daysCnt + ", 'yyyy-mm-dd')"
 		       		   + "   and not exists (select 'x' from usrStk where id ='" + s.getID() + "' and sell_mode_flg = 1)"
 		       		   + " group by to_char(dl_dt, 'yyyy-mm-dd'))";
 		        log.info(sql);
 		        stm = con.createStatement();
 		        rs = stm.executeQuery(sql);
 		        if (rs.next()) {
 		            dev3 = rs.getDouble("dev");
 		            log.info("dev3:" + dev3 + "< dev2:" + dev2 + "? " + (dev3 < dev2));
 		          	if (dev3 < dev2) {
 		          		isgood = true;
 		       	    }
 		        }
 		        rs.close();
 		        stm.close();
    		}
    		con.close();
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
    	log.info("dev1:" + dev1 + ", minStddev:" + minStddev + ", maxStddev:" + maxStddev + " return " + (isgood ? " true":"false"));
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
			minStddev += 0.002;
			if (minStddev >= maxStddev) {
				minStddev = maxStddev;
			}
		}
		else {
			minStddev -= 0.001;
			if (minStddev <= STConstants.ALLOWABLE_MIN_STEDEV_VALUE) {
				log.info("minStddev is lower than " + STConstants.ALLOWABLE_MIN_STEDEV_VALUE + ", use it.");
				minStddev = STConstants.ALLOWABLE_MIN_STEDEV_VALUE;
			}
			log.info("Now maxStddve:" + maxStddev + ", minStddev:" + minStddev);
		}
		return true;
	}
    @Override
    public Integer getTradeModeId() {
        // TODO Auto-generated method stub
        return STConstants.TRADE_MODE_ID_QTYTRADE;
    }
	@Override
	public boolean shouldStockExitTrade(String stkId) {
		// TODO Auto-generated method stub
		Connection con = DBManager.getConnection();
		Statement stm = null;
		ResultSet rs = null;
		boolean should_exit = false;
		int daysCnt = StockMarket.getNumDaysAhead(stkId, STConstants.DEV_CALCULATE_DAYS);
		
		try {
		String sql = "select avg(dev) dev from ("
				   + "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, to_char(dl_dt, 'yyyy-mm-dd') atSuggestStockDay "
				   + "  from stkdat2 "
				   + " where id ='" + stkId + "'"
				   + "   and to_char(dl_dt, 'yyyy-mm-dd') >= to_char(sysdate - " + daysCnt + ", 'yyyy-mm-dd')"
				   + " group by to_char(dl_dt, 'yyyy-mm-dd'))";
		    log.info(sql);
		    stm = con.createStatement();
		    rs = stm.executeQuery(sql);
		    if (rs.next()) {
		    	 double dev = rs.getDouble("dev");
		    	 log.info("Stock: " + stkId + "'s " + STConstants.DEV_CALCULATE_DAYS + " dev is:" + dev + ", MIN_DEV_BEFORE_EXIT_TRADE:" + STConstants.MIN_DEV_BEFORE_EXIT_TRADE);
		    	if (dev < STConstants.MIN_DEV_BEFORE_EXIT_TRADE) {
		    		log.info("Stock:" + stkId + " dev value:"+ dev + " reached min threshold value " +STConstants.MIN_DEV_BEFORE_EXIT_TRADE + ", should exit trade.");
		    		should_exit = true;
		    	}
		    }
		
		    rs.close();
		    stm.close();
	        con.close();
		}
		catch (Exception e) {
			log.error(e.getMessage());
		}
		return should_exit;
	}
}
