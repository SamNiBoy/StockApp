package com.sn.work.output;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
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

    public GzStock(long id, long dbn, String stk)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;
        stockID = stk;
    }

    public void run()
    {        // //////////////////Menu
        String msg = "";
        Connection con = DBManager.getConnection();
        String sql = "update stk set gz_flg = 1 where ID = '" + stockID + "'";
        try {
            Statement stm = con.createStatement();
            stm.executeUpdate(sql);

                msg += "Stock:" + stockID + " get gzed!\n";
            stm.close();
            con.close();
            log.info("gz msg:" + msg + " for opt 5");
            res = msg;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getWorkResult()
    {
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
        return "ShutDownPC";
    }

    public boolean isCycleWork()
    {
        return false;
    }
}
