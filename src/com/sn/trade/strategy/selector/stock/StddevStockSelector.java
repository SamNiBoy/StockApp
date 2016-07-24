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
    	double dev = -1;
    	
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
    				   + "   and to_char(dl_dt, 'yyyy-mm-dd') >= to_char(sysdate - " + STConstants.DEV_CALCULATE_DAYS + ", 'yyyy-mm-dd')"
    				   + "   and not exists (select 'x' from usrStk where id ='" + s.getID() + "' and sell_mode_flg = 1)"
    				   + " group by to_char(dl_dt, 'yyyy-mm-dd'))";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			 dev = rs.getDouble("dev");
    			if (dev >= minStddev && dev <= maxStddev) {
    				isgood = true;
    			}
    		}
    		rs.close();
    		stm.close();
    		con.close();
    	}
    	catch(Exception e) {
    		e.printStackTrace();
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
}
