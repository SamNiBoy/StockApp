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

public class StddevStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(StddevStockSelector.class);
    int days = 7;
	double maxStddev = 0.1;
	double minStddev = 0.023;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	boolean isgood = false;
    	double dev = -1;
   		Connection con = DBManager.getConnection();
   		Statement stm = null;
    	try {
     		stm = con.createStatement();
    		String sql = "select avg(dev) dev from ("
    				   + "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, left(dl_dt, 10) atDay "
    				   + "  from stkdat2 "
    				   + " where id ='" + s.getID() + "'"
    				   + "   and left(dl_dt, 10) >= left(sysdate() - interval " + days + " day , 10)"
    				   + " group by left(dl_dt, 10)) t";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			 dev = rs.getDouble("dev");
    			if (dev >= minStddev && dev <= maxStddev) {
    				isgood = true;
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
               log.error(e.getMessage() + " with error code:" + e.getErrorCode()); 
            }
        }
    	log.info("dev:" + dev + ", minStddev:" + minStddev + ", maxStddev:" + maxStddev + " return " + (isgood ? " true":"false"));
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
			if (minStddev <= 0.01) {
				log.info("minStddev is lower than 0.01, use 0.01.");
				minStddev = 0.01;
			}
			log.info("Now maxStddve:" + maxStddev + ", minStddev:" + minStddev);
		}
		return true;
	}
}
