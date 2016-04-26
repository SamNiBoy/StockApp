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
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.RawStockData;
import com.sn.stock.Stock2;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class StockDataFetcher implements IWork {

    Connection con = DBManager.getConnection();
    /* Initial delay before executing work.
     */
    static long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    static long delayBeforNxtStart = 5;

    static TimeUnit tu = TimeUnit.MILLISECONDS;
    
    static int maxLstNum = 50;
    
    static public String resMsg = "Initial msg for work StockDataFetcher.";
    
    static StockDataFetcher self = null;
    static StockDataConsumer cnsmr = null;
    
    public static ReentrantLock lock = new ReentrantLock();
    public static Condition finishedOneRoundFetch = lock.newCondition();
    
    static Logger log = Logger.getLogger(StockDataFetcher.class);
    
    public static String getResMsg() {
        return resMsg;
    }

    public static void setResMsg(String resMsg) {
        StockDataFetcher.resMsg = resMsg;
    }

    static public boolean start() {
        self = new StockDataFetcher(0, Stock2.StockData.SECONDS_PER_FETCH * 1000);
        if (WorkManager.submitWork(self)) {
            log.info("开始收集股票数据!");
            cnsmr = new StockDataConsumer(0, 0);
            WorkManager.submitWork(cnsmr);
            return true;
        }
        return false;
    }
    
    static public boolean stop() {
        if (WorkManager.cancelWork(self.getWorkName())) {
            log.info("成功取消数据收集器.");
            WorkManager.cancelWork(cnsmr.getWorkName());
            return true;
        }
        log.info("StockDataFetcher can not be cancelled!, this is unexpected");
        return false;
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        start();
    }

    public StockDataFetcher(long id, long dbn)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    private String getFetchLst()
    {
        Statement stm = null;
        ResultSet rs = null;
        String sql = "select area, id from stk";

        StringBuilder stkLst = new StringBuilder();
        
        int i = 0;

        try{
            stm = con.createStatement();
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                i++;
                stkLst.append(stkLst.length() > 0 ? "," : "");
                stkLst.append(rs.getString("area") + rs.getString("id"));
                if (i %  maxLstNum == 0)
                {
                    stkLst.append("#");
                    i = 0;
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally {
            try {
                rs.close();
                stm.close();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        log.info(stkLst);

        return stkLst.toString();
    }

    private String lstStkDat = "";
    private int failCnt = 0;

    public void run()
    {
        // TODO Auto-generated method stub
        String str;

        log.info("Now StockDataFetcher start!!!");
        try {
            String fs [] = getFetchLst().split("#"), cs;
            RawStockData srd = null;
            String stkSql = "http://hq.sinajs.cn/list=";
            for (int i = 0; i < fs.length; i++)
            {
                log.info("Fetching..." + stkSql + fs[i]);
                URL url = new URL(stkSql + fs[i]);
                InputStream is = url.openStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                int j = 0;
                while ((str = br.readLine()) != null) {
                    if (str.equals(lstStkDat))
                    {
                        failCnt++;
                        break;
                    }
                    else {
                        failCnt = 0;
                    }
                    if (i == j && i == 0)
                    {
                        /* Make sure lstStkDat store the last value of first stock*/
                        lstStkDat = str;
                    }
                    j++;
                
                    LocalDateTime lt = LocalDateTime.now();
                    int hr = lt.getHour();
                    
                    if (hr <= 8 || hr >= 15) {
                        log.info(" hour is not in market time: " + hr + ", will not save the data!");
                        continue;
                    }
                    
                    log.info(str);
                    srd = RawStockData.createStockData(str);
                    
                    if (srd == null) {
                        log.info("can not create rawdata for " + str + " continue...");
                        continue;
                    }
                    
                    if (srd.td_opn_pri <= 0) {
                        log.info("market not open yet. td_opn_pri <= 0 for gzstock:" + srd.id + " can not trade based on it, continue");
                        continue;
                    }
                    
                    log.info("StockDataFetcher put rawdata to queue with size:" + cnsmr.getDq().size());
                    cnsmr.getDq().put(srd);
                }
                br.close();
                if (failCnt > 0)
                {
                    failCnt = 0;
                    log.info("Stock data is same, break loop from StockDataFetcher...");
                    break;
                }
            }
            
            if (failCnt == 0) {
                lock.lock();
                try {
                    finishedOneRoundFetch.signalAll();
                }
                finally {
                    lock.unlock();
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("Now StockDataFetcher exit!!!");
    }

    public String getWorkResult()
    {
        return "";
    }

    public String getWorkName()
    {
        return "StockDataFetcher";
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
