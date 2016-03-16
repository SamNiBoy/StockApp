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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.RawStockData;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class StockDataFetcher implements IWork {

    Connection con = DBManager.getConnection();
    /* Initial delay before executing work.
     */
    long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 5;

    TimeUnit tu = TimeUnit.MILLISECONDS;
    
    int maxLstNum = 50;
    
    static public String resMsg = "Initial msg for work StockDataFetcher.";
    
    static StockDataFetcher self = null;
    RawStockDataConsumer cnsmr = null;
    
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
        if (self == null) {
            RawStockDataConsumer gsdc = new RawStockDataConsumer(0, 0);
            self = new StockDataFetcher(0, 35000, gsdc);
            if (WorkManager.submitWork(self)) {
                resMsg = "Newly created StockDataFetcher and started!";
                return true;
            }
        }
        else if (WorkManager.canSubmitWork(self.getWorkName())) {
            if (WorkManager.submitWork(self)) {
                resMsg = "Resubmitted StockDataFetcher and started!";
                return true;
            }
        }
        resMsg = "Work StockDataFetcher is started, can not start again!";
        return false;
    }
    
    static public boolean stop() {
        if (self == null) {
            resMsg = "StockDataFetcher is null, how did you stop it?";
            return true;
        }
        else if (WorkManager.canSubmitWork(self.getWorkName())) {
            resMsg = "StockDataFetcher is stopped, but can submit again.";
            return true;
        }
        else if (WorkManager.cancelWork(self.getWorkName())) {
            resMsg = "StockDataFetcher is cancelled successfully.";
            return true;
        }
        resMsg = "StockDataFetcher can not be cancelled!, this is unexpected";
        return false;
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        start();
    }

    public StockDataFetcher(long id, long dbn, RawStockDataConsumer sdcr)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;
        cnsmr = sdcr;
    }

    private String getFetchLst()
    {
        Statement stm = null;
        ResultSet rs = null;
        String sql = "select area, id from stk";

        String stkLst = "";
        
        int i = 0;

        try{
            stm = con.createStatement();
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                i++;
                stkLst += stkLst.length() > 0 ? "," : "";
                stkLst += rs.getString("area") + rs.getString("id");
                if (i %  maxLstNum == 0)
                {
                    stkLst += "#";
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

        return stkLst;
    }

    private String lstStkDat = "";
    private int failCnt = 0;

    public void run()
    {
        // TODO Auto-generated method stub
        String str;

        if (WorkManager.canSubmitWork(cnsmr.getWorkName())) {
            WorkManager.submitWork(cnsmr);
            log.info("StockDataFetcher successfully lunched consumer...");
        }
        else {
            log.info("StockDataFetcher consumer looks already running, skip resubmit...");
        }
        
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
                
                    log.info(str);
                    srd = RawStockData.createStockData(str);
                    
                    if (srd == null) {
                        log.info("can not create rawdata for " + str + " continue...");
                        continue;
                    }
                    log.info("StockDataFetcher put rawdata to queue with size:" + cnsmr.getDq().getDatque().size());
                    cnsmr.getDq().getDatque().put(srd);
                }
                br.close();
                if (failCnt == 1) {
                    log.info("Stock data is same 1 times, cancel current loop...");
                    break;
                }
                if (failCnt > 1)
                {
                    failCnt = 0;
                    log.info("Stock data is same 2 times, Sleeping 1 minute...");
                    Thread.currentThread().sleep(60*1000);
                    //WorkManager.cancelWork(this.getWorkName());
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
        log.info("Now fetcher exit!!!");
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
