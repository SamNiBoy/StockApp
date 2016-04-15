package com.sn.work.fetcher;

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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.stock.RawStockData;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class StockDataConsumer implements IWork {

    static private int MAX_QUEUE_SIZE = 10000;
    
    static private ArrayBlockingQueue<RawStockData> dataqueue = new ArrayBlockingQueue<RawStockData>(MAX_QUEUE_SIZE, false);
    
    static Connection con = DBManager.getConnection();
    /* Initial delay before executing work.
     */
    long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 5;

    TimeUnit tu = TimeUnit.MILLISECONDS;
    
    static int maxLstNum = 50;
    
    static Logger log = Logger.getLogger(StockDataConsumer.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        StockDataConsumer fsd = new StockDataConsumer(1, 3);
        fsd.run();
    }

    public StockDataConsumer(long id, long dbn)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    public ArrayBlockingQueue<RawStockData> getDq() {
        return dataqueue;
    }

    public void setDq(ArrayBlockingQueue<RawStockData> dq) {
        this.dataqueue = dq;
    }

    public void run()
    {
        log.info("Now about to run StockConsumer's run...");
        ConcurrentHashMap<String, Stock2> ss = StockMarket
        .getStocks();
        int cnt = 0;
        try {
            while (true) {
                RawStockData srd = dataqueue.take();
                log.info("take return stock:" + srd.id);
                Stock2 s = ss.get(srd.id);
                if (s != null) {
                    cnt++;
                    log.info("About to consume RawData from RawStockDataConsume, queue size:" + dataqueue.size());
                    s.saveData(srd, con);
                }
                if ((cnt >= ss.size() && s != null) || dataqueue.isEmpty()) {
                	log.info("after fetch:" + cnt + " rows, and calIndex at:" + s.getDl_dt());
                    Timestamp ts = s.getDl_dt();
                    StockMarket.calIndex(ts);
                    cnt = 0;
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public String getWorkResult()
    {
        return "";
    }

    public String getWorkName()
    {
        return "StockDataConsumer";
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
        return false;
    }

}
