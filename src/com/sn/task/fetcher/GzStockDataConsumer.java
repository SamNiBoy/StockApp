package com.sn.task.fetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.mail.GzStockBuySellPointObserverable;
import com.sn.strategy.ITradeStrategy;
import com.sn.strategy.TradeStrategyGenerator;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.trader.StockTrader;
import com.sn.stock.RawStockData;
import com.sn.task.IWork;

public class GzStockDataConsumer implements IWork {

	static private int MAX_QUEUE_SIZE = 1;
	static private ArrayBlockingQueue<RawStockData> dataqueue = new ArrayBlockingQueue<RawStockData>(MAX_QUEUE_SIZE, false);
    static private StockTrader st = StockTrader.getTradexTrader();
    private ITradeStrategy strategy = null;
    
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
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        // TODO Auto-generated method stub
        GzStockDataConsumer fsd = new GzStockDataConsumer(1, 3);
        fsd.run();
    }

    public GzStockDataConsumer(long id, long dbn) throws Exception {
        initDelay = id;
        delayBeforNxtStart = dbn;
        strategy = TradeStrategyGenerator.generatorStrategy(false);
    }

    public ArrayBlockingQueue<RawStockData> getDq() {
        return dataqueue;
    }

    public void setDq(ArrayBlockingQueue<RawStockData> dq) {
        this.dataqueue = dq;
    }
    
    public void run() {
        ConcurrentHashMap<String, Stock2> gzs = StockMarket
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
            
            Stock2 s = gzs.get(srd.id);
           
            if (s != null) {
                log.info("Now consuming StockRawData " + srd.id + " Name" + srd.name + "dq size:" + dataqueue.size());
                
                s.injectData(srd);
                
                log.info("check stock " + s.getID() + " for buy/sell point");
                
                log.info("Now start trading with stragtegy:" + strategy.getTradeStrategyName() + " on stock:" + s.getID() + ", name:" + s.getName() + "\n\n");
                st.setStrategy(strategy);
                st.performTrade(s);
                
            }
            else {
            	log.info("Stock:" + srd.id + " is not in gzstock list!");
            }
            synchronized (srd) {
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
