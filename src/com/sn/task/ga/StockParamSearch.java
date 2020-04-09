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
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

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

public class StockParamSearch implements IWork {

        /* Initial delay before executing work.
         */
        long initDelay = 0;

        /* Seconds delay befor executing next work.
         */
        long delayBeforNxtStart = 5;

        TimeUnit tu = TimeUnit.MILLISECONDS;
        
        static Logger log = Logger.getLogger(StockParamSearch.class);
        /**
         * @param args
         */
        public static void main(String[] args) {
            // TODO Auto-generated method stub
            StockParamSearch fsd = new StockParamSearch(1, 3);
            fsd.run();
        }

        public StockParamSearch(long id, long dbn)
        {
            initDelay = id;
            delayBeforNxtStart = dbn;
        }
        
        static public boolean start() {
            //self = new StockDataFetcher(0, Stock2.StockData.SECONDS_PER_FETCH * 1000);
            
            IWork self = new StockParamSearch(0,  24 * 60 * 1 * 1000);
            if (WorkManager.submitWork(self)) {
                log.info("开始GAParamSearch task!");
                return true;
            }
            return false;
        }

        /*
         * var hq_str_sh601318=
         * "�й�ƽ��,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
         * ;
         */

        public void run()
        {
            // TODO Auto-generated method stub
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
                finally {
                }
        }

        public String getWorkResult()
        {
            return null;
        }

        public String getWorkName()
        {
            return "GAParamSearch";
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

        public boolean isCycleWork()
        {
            return true;
        }

    }
