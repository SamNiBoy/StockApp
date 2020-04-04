package com.sn.wechat.action;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.STConstants;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.trader.StockTrader;
import com.sn.task.IWork;
import com.sn.task.WorkManager;

public class GzStock implements IWork {

    /* Initial delay before executing work.
     */
    long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 0;
    
    String stockID;

    String frmUsr;
    TimeUnit tu = TimeUnit.MILLISECONDS;
    /* Result calcualted by this worker.
     */
    static String res = "Now gz stock started.";

    static Logger log = Logger.getLogger(GzStock.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    public GzStock(long id, long dbn, String usr, String stk)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;
        stockID = stk;
        frmUsr = usr;
    }

    public void run()
    {        // //////////////////Menu
        String msg = "";
        Connection con = DBManager.getConnection();
        Statement stm = null;
        String sql = "select gz_flg, suggested_by from usrStk where id = '" + stockID + "' and openID = '" + frmUsr + "'";
        try {
            
            String system_suggest_role = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING");
            stm = con.createStatement();
            ResultSet rs = null;
            rs = stm.executeQuery(sql);

            if (rs.next()) {
            	long gz_flg = rs.getLong("gz_flg");
            	String suggested_by = rs.getString("suggested_by");
            	rs.close();
            	stm.close();
                if (gz_flg == 1 && suggested_by.equals(system_suggest_role))
                {
            	    sql = "update usrStk set suggested_by = '" + frmUsr + "', mod_dt = sysdate() where id = '" + stockID + "' and openID = '" + frmUsr + "'";
                }
                else {
            	    sql = "update usrStk set gz_flg = 1 - gz_flg, suggested_by = '" + frmUsr + "', mod_dt = sysdate() where id = '" + stockID + "' and openID = '" + frmUsr + "'";
                }
            	stm = con.createStatement();
            	log.info(sql);
            	stm.execute(sql);
            	if (gz_flg == 1 && !suggested_by.equals(system_suggest_role)) {
            	    msg = "成功取消关注:" + stockID;
            	    StockMarket.removeGzStocks(stockID);
            	}
            	else {
            		msg = "成功关注:" + stockID;
            		StockMarket.addGzStocks(stockID);
            	}
            }
            else {
            	rs.close();
            	stm.close();
            	stm = con.createStatement();
            	sql = "select 'x' from stk where id = '" + stockID + "'";
            	log.info(sql);
            	rs = stm.executeQuery(sql);
            	if (!rs.next()) {
            		msg = "不存在该股票代码:" + stockID;
            		rs.close();
            	}
            	else {
            		rs.close();
            	    sql = "insert into usrStk values ('" + frmUsr + "','" + stockID + "',1,0,'" + frmUsr + "', sysdate())";
            	    log.info(sql);
            	    stm.execute(sql);
        		    msg = "成功添加关注:" + stockID;
        		    StockMarket.addGzStocks(stockID);
            	}
            }
            res = msg;
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage() + " with error: " + e.getErrorCode());
            }
        }
    }

    public String getWorkResult()
    {
    	try{
    	    WorkManager.waitUntilWorkIsDone(this.getWorkName());
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
        return res;
    }

    public long getInitDelay()
    {
        return initDelay;
    }

    public long getDelayBeforeNxt()
    {
        return delayBeforNxtStart;
    }

    public TimeUnit getTimeUnit()
    {
        return tu;
    }
    public String getWorkName()
    {
        return "GzStock";
    }

    public boolean isCycleWork()
    {
        return false;
    }
}
