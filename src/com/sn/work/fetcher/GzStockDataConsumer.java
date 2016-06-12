package com.sn.work.fetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.mail.reporter.GzStockBuySellPointObserverable;
import com.sn.stock.Stock;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.trade.StockTrader;
import com.sn.stock.RawStockData;
import com.sn.work.itf.IWork;

public class GzStockDataConsumer implements IWork {

	static private int MAX_QUEUE_SIZE = 1;
	static private ArrayBlockingQueue<RawStockData> dataqueue = new ArrayBlockingQueue<RawStockData>(MAX_QUEUE_SIZE, false);
    static private StockTrader st = new StockTrader(false);
    
    /*
     * Initial delay before executing work.
     */
    static long initDelay = 0;

    /*
     * Seconds delay befor executing next work.
     */
    static long delayBeforNxtStart = 5;

    static TimeUnit tu = TimeUnit.MILLISECONDS;

    static int maxLstNum = 50;
    
    static Logger log = Logger.getLogger(GzStockDataConsumer.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        GzStockDataConsumer fsd = new GzStockDataConsumer(1, 3);
        fsd.run();
    }

    public GzStockDataConsumer(long id, long dbn) {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    public ArrayBlockingQueue<RawStockData> getDq() {
        return dataqueue;
    }

    public void setDq(ArrayBlockingQueue<RawStockData> dq) {
        this.dataqueue = dq;
    }
    
    public void run() {
        ConcurrentHashMap<String, Stock> gzs = StockMarket
        .getGzstocks();
        while (true) {
        	log.info("after while, dataqueue.take()...");
        	RawStockData srd = null;
        	try {
                 srd= dataqueue.take();
        	}
        	catch(Exception e) {
        		e.printStackTrace();
        		log.info("Unexpected exception happened from GzStockDataConsumer take()");
        	}
            synchronized (srd) {
                Stock s = gzs.get(srd.id);
                
                if (s != null) {
                    log.info("Now consuming StockRawData " + srd.id + " Name" + srd.name + "dq size:" + dataqueue.size());
                    
                    s.injectData(srd);
                    
                    log.info("check stock " + s.getID() + " for buy/sell point");
                    
                    try {
                        st.performTrade(s);
                    }
                    catch (Exception e) {
                        log.error("Got error with:" + e.getMessage());
                    }
                }
                else {
                	log.info("Stock:" + srd.id + " is not in gzstock list, adding to gz list!");
                	StockMarket.addGzStocks(srd.id);
                }

            	log.info("After GzStockDataConsumer consume the srd:" + srd.id + " call notify() to wakeup GzStockDataFetcher.");
                srd.notify();
            }
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
