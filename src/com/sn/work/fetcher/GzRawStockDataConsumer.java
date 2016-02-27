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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.stock.StockRawData;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class GzRawStockDataConsumer implements IWork {

    public RawStockDataQueue dq = new RawStockDataQueue(5000);
    /*
     * Initial delay before executing work.
     */
    long initDelay = 0;

    /*
     * Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 5;

    TimeUnit tu = TimeUnit.MILLISECONDS;

    static int maxLstNum = 50;

    static Logger log = Logger.getLogger(GzRawStockDataConsumer.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        GzRawStockDataConsumer fsd = new GzRawStockDataConsumer(1, 3);
        fsd.run();
    }

    public GzRawStockDataConsumer(long id, long dbn) {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    public void run() {
        ArrayBlockingQueue<StockRawData> dd = dq.getDatque();
        try {
            while (true) {
                StockRawData srd = dd.take();
                ConcurrentHashMap<String, Stock2> gzs = StockMarket
                        .getGzstocks();
                Stock2 s = gzs.get(srd.id);
                if (s != null) {
                    s.injectData(srd);
                }
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getWorkResult() {
        return "";
    }

    public String getWorkName() {
        return "GzRawStockDataConsumer";
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
        return false;
    }

}
