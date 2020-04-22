package com.sn.task.ga;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sn.db.DBManager;
import com.sn.mail.StockObserverable;
import com.sn.simulation.SimTrader;
import com.sn.strategy.algorithm.ga.Algorithm;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.fetcher.StockDataConsumer;
import com.sn.task.fetcher.StockDataFetcher;
import com.sn.mail.StockObserver;
import com.sn.task.WorkManager;
import com.sn.task.IWork;

public class StockParamSearch implements Job {
        
        static Logger log = Logger.getLogger(StockParamSearch.class);
        /**
         * @param args
         */
        public static void main(String[] args) {
            // TODO Auto-generated method stub
            StockParamSearch fsd = new StockParamSearch();
        }

        public StockParamSearch()
        {
        }

        /*
         * var hq_str_sh601318=
         * "�й�ƽ��,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
         * ;
         */

        public void execute(JobExecutionContext context)
                throws JobExecutionException
        {
            // TODO Auto-generated method stub
            
            log.info("StockParamSearch starts...");
            
                try {
                    log.info("Start GAParamSearch stocks...");
                    Algorithm ag = new Algorithm();
                    synchronized(Algorithm.class) {
                        ag.run();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                log.info("StockParamSearch ends...");
        }

    }
