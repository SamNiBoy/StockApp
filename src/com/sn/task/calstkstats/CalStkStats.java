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
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import com.sn.db.DBManager;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.IWork;
import com.sn.task.JobScheduler;
import com.sn.task.WorkManager;
import com.sn.task.ga.StockParamSearch;

public class CalStkStats implements Job {

    static Connection con = null;
    static Logger log = Logger.getLogger(CalStkStats.class);

    /**
     * @param args
     * @throws JobExecutionException 
     */
    public static void main(String[] args) throws JobExecutionException {
        // TODO Auto-generated method stub
        CalStkStats fsd = new CalStkStats();
        fsd.execute(null);
    }

    public CalStkStats() {
    }
    
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        // TODO Auto-generated method stub
        log.info("Running CalStkStats task begin");
        StockMarket.calStats();
        log.info("Running CalStkStats task end");
    }
}
