package com.sn.work.output;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.StockMarket;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class CalFetchStat implements com.sn.work.itf.IWork {

    /* Initial delay before executing work.
     */
    long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 5;

    TimeUnit tu = TimeUnit.MILLISECONDS;
    /* Result calcualted by this worker.
     */
    static String res = "Calculating fetch status is scheduled, try again later.";
    /**
     * @param args
     */
    static Logger log = Logger.getLogger(CalFetchStat.class);

    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    public CalFetchStat(long id, long dbn)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    public void run()
    {        // //////////////////Menu
        // 5///////////////////////////////////////////////////
        String msg = "";
        Connection con = DBManager.getConnection();
        String sql = "select count(*) totCnt, count(*)/count(distinct id) cntPerStk "
                + "  from stkDat";
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);

            if (rs.next()){
                msg += "总共收集:" + rs.getLong("totCnt") + "条记录.\n"
                      +"平均每股收集:" + rs.getLong("cntPerStk") + "次.\n";
            }
            rs.close();
            stm.close();
            con.close();
            log.info("calculate fetch stat msg:" + msg + " for opt 5");
        } catch (Exception e) {
            e.printStackTrace();
            msg = "无数据.\n";
        }
        msg += StockMarket.getShortDesc();
        res = msg;
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
        return "CalFetchStat";
    }

    public boolean isCycleWork()
    {
        return false;
    }
}
