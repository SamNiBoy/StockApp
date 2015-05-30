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

public class ShutDownPC implements com.sn.work.itf.IWork {

    /* Initial delay before executing work.
     */
    long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 5;

    TimeUnit tu = TimeUnit.MILLISECONDS;
    /* Result calcualted by this worker.
     */
    static String res = "PC is shuting Down.";

    static Logger log = Logger.getLogger(ShutDownPC.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    public ShutDownPC(long id, long dbn)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    public void run()
    {
        log.info("PC shutdown invoked, shutdow works first.");
        WorkManager.shutdownWorks();

        log.info("Now PC shuting down.");
        Runtime rt = Runtime.getRuntime();
        try {
            rt.exec("shutdown -s -t 40");
        } catch (IOException e) {
            // TODO Auto-generated catch block
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
