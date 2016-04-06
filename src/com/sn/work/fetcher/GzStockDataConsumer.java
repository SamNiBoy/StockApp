package com.sn.work.fetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.mail.reporter.GzStockBuySellPointObserverable;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.trader.StockTrader;
import com.sn.stock.RawStockData;
import com.sn.work.itf.IWork;

public class GzStockDataConsumer implements IWork {

	static private int MAX_QUEUE_SIZE = 10000;
	static private ArrayBlockingQueue<RawStockData> dataqueue = new ArrayBlockingQueue<RawStockData>(MAX_QUEUE_SIZE, false);
    static private List<StockBuySellEntry> stockTomail = new ArrayList<StockBuySellEntry>();
    static private GzStockBuySellPointObserverable gsbsob = new GzStockBuySellPointObserverable(stockTomail);
    
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
        ConcurrentHashMap<String, Stock2> gzs = StockMarket
        .getGzstocks();
        StockBuySellEntry sbse = null;
        while (true) {
        	
        	RawStockData srd = null;
        	try {
                 srd= dataqueue.take();
        	}
        	catch(Exception e) {
        		e.printStackTrace();
        		log.info("Unexpected exception happened from GzStockDataConsumer take()");
        	}
            
            Stock2 s = gzs.get(srd.id);
           
            log.info("Now consuming StockRawData " + srd.id + " Name" + srd.name + "dq size:" + dataqueue.size());
            
            s.injectData(srd);
            
            log.info("check stock " + s.getID() + " for buy/sell point");
            
            if (StockTrader.shouldBuyStock(s)) {
            	sbse = new StockBuySellEntry(s.getID(),
            			                     s.getName(),
            			                     s.getSd().getCur_pri_lst().get(s.getSd().getCur_pri_lst().size() - 1),
            			                     true,
            			                     s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1));
            	if (StockTrader.tradeStock(sbse)) {
                    stockTomail.add(sbse);
            	}
            }
            else if(StockTrader.shouldSellStock(s)) {
            	sbse = new StockBuySellEntry(s.getID(),
            			                     s.getName(),
            			                     s.getSd().getCur_pri_lst().get(s.getSd().getCur_pri_lst().size() - 1),
            			                     false,
            			                     s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1));
            	if (StockTrader.tradeStock(sbse)) {
                    stockTomail.add(sbse);
            	}
            }
            if (!stockTomail.isEmpty()) {
               log.info("Now sending buy/sell stock information for " + stockTomail.size());
               gsbsob.setData(stockTomail);
               gsbsob.update();
               stockTomail.clear();
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
