package com.sn.task.calstkstats;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.IWork;
import com.sn.task.WorkManager;
import com.sn.task.ga.StockParamSearch;

public class CalStkStats implements IWork {

    static Connection con = null;
    /*
     * Initial delay before executing work.
     */
    long initDelay = 0;

    /*
     * Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 60000;

    TimeUnit tu = TimeUnit.MILLISECONDS;

    static Logger log = Logger.getLogger(CalStkStats.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        CalStkStats fsd = new CalStkStats(1, 3);
        fsd.run();
    }

    public CalStkStats(long id, long dbn) {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    static public boolean start() {
        int fetch_per_seconds = ParamManager.getIntParam("FETCH_EVERY_SECONDS", "TRADING", null);

        IWork self = new CalStkStats(0,  1 * fetch_per_seconds * 1000);
        if (WorkManager.submitWork(self)) {
            log.info("开始CalStkStats task!");
            return true;
        }
        return false;
    }
    
    public void run() {
        // TODO Auto-generated method stub
        log.info("Running CalStkStats task begin");
        StockMarket.calStats();
        log.info("Running CalStkStats task end");
    }

    public String getWorkResult() {
        return "";
    }

    public String getWorkName() {
        return "CalStkStats";
    }

    public long getInitDelay() {
        return initDelay;
    }

    public long getDelayBeforeNxt() {
        return delayBeforNxtStart;
    }

    public TimeUnit getTimeUnit() {
        return tu;
    }

    public boolean isCycleWork() {
        return true;
    }

}
