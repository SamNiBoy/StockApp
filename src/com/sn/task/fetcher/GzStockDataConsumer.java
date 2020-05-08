package com.sn.task.fetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sn.mail.GzStockBuySellPointObserverable;
import com.sn.strategy.ITradeStrategy;
import com.sn.strategy.TradeStrategyGenerator;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.trader.StockTrader;
import com.sn.stock.RawStockData;
import com.sn.task.IWork;

@DisallowConcurrentExecution
public class GzStockDataConsumer implements Job {

	static private int MAX_QUEUE_SIZE = 1;
	static private ArrayBlockingQueue<RawStockData> dataqueue = new ArrayBlockingQueue<RawStockData>(MAX_QUEUE_SIZE, false);
    static private StockTrader st = null;
    static private ITradeStrategy strategy = null;

    static int maxLstNum = 50;
    static int trade_at_local = ParamManager.getIntParam("TRADING_AT_LOCAL", "TRADING", null);
    static int trade_at_local_with_sim_mode = ParamManager.getIntParam("TRADING_AT_LOCAL_WITH_SIM", "TRADING", null);
    
    static Logger log = Logger.getLogger(GzStockDataConsumer.class);

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        // TODO Auto-generated method stub
        GzStockDataConsumer fsd = new GzStockDataConsumer();
        fsd.execute(null);
    }

    public GzStockDataConsumer() throws Exception {
    }

    public static ArrayBlockingQueue<RawStockData> getDq() {
        return dataqueue;
    }

    public static void setDq(ArrayBlockingQueue<RawStockData> dq) {
        dataqueue = dq;
    }
    
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
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
                
                if (strategy == null)
                {
                    strategy = TradeStrategyGenerator.generatorStrategy(trade_at_local_with_sim_mode == 1 ? true : false);
                }
                if (st == null)
                {
                	if (trade_at_local_with_sim_mode == 0) {
                		if (trade_at_local == 0) {
                	        st = StockTrader.getTradexTrader();
                		}
                		else {
                			st = StockTrader.getGFTrader();
                		}
                	}
                	else {
                		st = StockTrader.getSimTrader();
                	}
                }
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
}
