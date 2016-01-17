package com.sn.work.output;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.work.itf.IWork;

public class ListGzStock implements IWork {

    Logger log = Logger.getLogger(ListGzStock.class);

    long initDelay = 0;
    long delayBeforNxtStart = 5;
    static String res = "List gz stocks...";

    TimeUnit tu = TimeUnit.MILLISECONDS;
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ListGzStock lgs = new ListGzStock(0,0);
        lgs.run();
    }

    public ListGzStock(long id, long dbn) {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    public void run() {
        log.info("This is from logger4j infor msg...");
        // //////////////////Menu
        // 2///////////////////////////////////////////////////
        String msg = "", sql;
        Connection con = DBManager.getConnection();
        sql = "select stk.ID, stk.name "
                + "  from stk "
                + " where gz_flg = 1 ";

        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);

            for (int i = 0; rs.next(); i++) {
                msg += (i + 1) + ": " + rs.getString("id") + "\n";
                        //+ rs.getString("name") + "\n";
            }
            rs.close();
            stm.close();
            con.close();
            if (msg.length() <= 0)
            {
                msg = "No stock gzed!";
            }
            log.info("list gzed stocks:" + msg);
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
        return "TopTenWst";
    }

    public boolean isCycleWork()
    {
        return false;
    }
}
