package com.sn.task.fetcher;

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
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sn.db.DBManager;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.stock.RawStockData;

public class StockDataConsumer implements Job {

    static private int MAX_QUEUE_SIZE = 10000;
    
    static private ArrayBlockingQueue<RawStockData> dataqueue = new ArrayBlockingQueue<RawStockData>(MAX_QUEUE_SIZE, false);
    
    static Connection con = DBManager.getConnection();
    
    static int maxLstNum = 50;
    
    static Logger log = Logger.getLogger(StockDataConsumer.class);
    /**
     * @param args
     * @throws JobExecutionException 
     */
    public static void main(String[] args) throws JobExecutionException {
        // TODO Auto-generated method stub
        StockDataConsumer fsd = new StockDataConsumer();
        fsd.execute(null);
    }

    public StockDataConsumer()
    {
    }

    public static ArrayBlockingQueue<RawStockData> getDq() {
        return dataqueue;
    }

    public void setDq(ArrayBlockingQueue<RawStockData> dq) {
        this.dataqueue = dq;
    }

    public void execute(JobExecutionContext context)
            throws JobExecutionException {
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
                    //StockMarket.calIndex(null);
                    StockMarket.setCur_stats_ts(ts);
                    cnt = 0;
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
