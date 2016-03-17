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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.sim.SimTrader;
import com.sn.sim.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.sim.strategy.selector.buypoint.QtyBuyPointSelector;
import com.sn.sim.strategy.selector.sellpoint.DefaultSellPointSelector;
import com.sn.sim.strategy.selector.sellpoint.QtySellPointSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.mail.reporter.GzStockBuySellPointObserverable;
import com.sn.mail.reporter.StockObserverable;
import com.sn.mail.reporter.StockObserver;
import com.sn.work.WorkManager;
import com.sn.work.fetcher.FetchStockData;
import com.sn.work.itf.IWork;

public class MonitorGzStockData implements IWork {

    /* Initial delay before executing work.
     */
    long initDelay = 0;
    
    private ArrayBlockingQueue<Stock2> queueToMonitor = null;
    private List<StockBuySellEntry> stockTomail = new ArrayList<StockBuySellEntry>();
    GzStockBuySellPointObserverable gsbsob = new GzStockBuySellPointObserverable(stockTomail);

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 5;

    TimeUnit tu = TimeUnit.MILLISECONDS;
    
    static String res = "MonitorGzStockData is scheduled, try again later.";
    
    static Logger log = Logger.getLogger(MonitorGzStockData.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        //MonitorGzStockData fsd = new MonitorGzStockData();
        //fsd.run();
    }

    public MonitorGzStockData(ArrayBlockingQueue<Stock2> qtm)
    {
        initDelay = 0;
        delayBeforNxtStart = 0;
        queueToMonitor = qtm;        
    }

    public void run()
    {
        // TODO Auto-generated method stub
        Stock2 s;
        while(true) {
        try {
            s = queueToMonitor.poll();
            log.info("check stock " + s.getId() + " for buy/sell point");
            QtySellPointSelector sps = new QtySellPointSelector();
            QtyBuyPointSelector dbs = new QtyBuyPointSelector();
            if (sps.isGoodSellPoint(s, null)) {
                stockTomail.add(new StockBuySellEntry(s.getId(), s.getName(), s.getSd().getCur_pri_lst().get(s.getSd().getCur_pri_lst().size() - 1), false, s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1).toLocaleString()));
            }
            else if(dbs.isGoodBuyPoint(s, null)) {
                stockTomail.add(new StockBuySellEntry(s.getId(), s.getName(),s.getSd().getCur_pri_lst().get(s.getSd().getCur_pri_lst().size() - 1), true, s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1).toLocaleString()));
            }
            if (!stockTomail.isEmpty()) {
               log.info("Now sending buy/sell stock information for " + stockTomail.size());
               gsbsob.setData(stockTomail);
               gsbsob.update();
               stockTomail.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
    }

    public String getWorkResult()
    {
        return res;
    }

    public String getWorkName()
    {
        return "MonitorGzStockData";
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
