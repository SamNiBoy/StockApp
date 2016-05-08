package com.sn.work.output;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.StockMarket;
import com.sn.trade.StockTrader;
import com.sn.work.itf.IWork;
import com.sn.work.WorkManager;

public class GzStock implements com.sn.work.itf.IWork {

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
        String sql = "select gz_flg from usrStk where id = '" + stockID + "' and openID = '" + frmUsr + "'";
        try {
            Statement stm = con.createStatement();
            ResultSet rs = null;
            rs = stm.executeQuery(sql);

            if (rs.next()) {
            	long gz_flg = rs.getLong("gz_flg");
            	rs.close();
            	stm.close();
            	sql = "update usrStk set gz_flg = 1 - gz_flg, suggested_by = '" + frmUsr + "' where id = '" + stockID + "' and openID = '" + frmUsr + "'";
            	stm = con.createStatement();
            	log.info(sql);
            	stm.execute(sql);
            	if (gz_flg == 1) {
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
            	    sql = "insert into usrStk values ('" + frmUsr + "','" + stockID + "',1,0,'" + frmUsr + "', sysdate)";
            	    log.info(sql);
            	    stm.execute(sql);
        		    msg = "成功添加关注:" + stockID;
        		    StockMarket.addGzStocks(stockID);
            	}
            }
            con.commit();
            stm.close();
            con.close();
            res = msg;
        } catch (Exception e) {
            e.printStackTrace();
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
