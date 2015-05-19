package com.sn.work;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.db.DBManager;
import com.sn.work.itf.IWork;

public class CalFetchStat implements IWork {

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
                msg += "Total stkDat:" + rs.getLong("totCnt") + "\n"
                      +"CNT/STK:" + rs.getLong("cntPerStk") + "\n";
            }
            stm.close();
            System.out.println("calculate fetch stat msg:" + msg + " for opt 5");
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
        return "CalFetchStat";
    }

    public boolean isCycleWork()
    {
        return false;
    }
}
