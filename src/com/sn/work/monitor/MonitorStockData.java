package com.sn.work.monitor;

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
import com.sn.sim.SimTrader;
import com.sn.mail.reporter.StockObserverable;
import com.sn.mail.reporter.StockObserver;
import com.sn.work.WorkManager;
import com.sn.work.fetcher.FetchStockData;
import com.sn.work.fetcher.StockDataFetcher;
import com.sn.work.itf.IWork;

public class MonitorStockData implements IWork {

    static Connection con = DBManager.getConnection();
    /* Initial delay before executing work.
     */
    long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 5;

    TimeUnit tu = TimeUnit.MILLISECONDS;
    
    static int maxLstNum = 50;
    
    static String res = "MonitorStockData is scheduled, try again later.";
    
    static Logger log = Logger.getLogger(MonitorStockData.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        MonitorStockData fsd = new MonitorStockData(1, 3);
        fsd.run();
    }

    public MonitorStockData(long id, long dbn)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    /*
     * var hq_str_sh601318=
     * "�й�ƽ��,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
     * ;
     */

    public void run()
    {
        // TODO Auto-generated method stub
            StockDataFetcher.lock.lock();
            try {
                log.info("Waiting before start mointor stocks...");
                StockDataFetcher.finishedOneRoundFetch.await();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                StockDataFetcher.lock.unlock();
            }

            StockObserverable spo = new StockObserverable();
            spo.update();
            
            if (spo.hasSentMail()) {
                res = "Already sent mail to your mailbox!";
            }
            else {
                res = "No mail sent because no significent stock price change!";
            }
    }

    public String getWorkResult()
    {
        return res;
    }

    public String getWorkName()
    {
        return "MonitorStockData";
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
