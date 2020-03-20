package com.sn.sim.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.imp.TradeStrategyImp;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class DealMountStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(DealMountStockSelector.class);
    int days = 7;
	long minAvgStkNum = 20000000;
	double minAvgMnyNum = 300000000;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	boolean isgood = false;
        long avgStkNum = 0;
        double avgMnyNum = 0.0;
 		Connection con = DBManager.getConnection();
   		Statement stm = null;
    	try {
     		stm = con.createStatement();
    		String sql = "select avg(max_mny_num) avg_mny_num, avg(max_stk_num) avg_stk_num from ("
    				   + "select max(dl_mny_num) max_mny_num, max(dl_stk_num) max_stk_num, left(dl_dt, 10) atDay "
    				   + "  from stkdat2 "
    				   + " where id ='" + s.getID() + "'"
    				   + "   and left(dl_dt, 10) >= left(sysdate() - interval " + days + " day, 10)"
    				   + " group by left(dl_dt, 10)) t";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		if (rs.next()) {
    			 avgStkNum = rs.getLong("avg_stk_num");
    			 avgMnyNum = rs.getDouble("avg_mny_num");
    			if (avgStkNum > minAvgStkNum && avgMnyNum > minAvgMnyNum) {
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
                log.info(e.getMessage());
            }
        }
    	log.info("avgStkNum:" + avgStkNum + ", minAvgStkNum:" + minAvgStkNum + ", avgMnyNum:" + avgMnyNum + ", minAvgMnyNum:" + minAvgMnyNum + " return " + (isgood ? " true":"false"));
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
