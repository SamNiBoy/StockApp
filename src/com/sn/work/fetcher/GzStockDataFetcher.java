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

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.RawStockData;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;
import com.sn.work.monitor.MonitorGzStockData;
import com.sn.work.monitor.MonitorStockData;

public class GzStockDataFetcher implements IWork {

    Connection con = DBManager.getConnection();
    /* Initial delay before executing work.
     */
    long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 5;

    TimeUnit tu = TimeUnit.MILLISECONDS;
    
    int maxLstNum = 50;
    
    static public String resMsg = "Initial msg for work GzStockDataFetcher.";
    
    static GzStockDataFetcher self = null;
    GzRawStockDataConsumer cnsmr = null;
    MonitorGzStockData monitor = null;
    
    static Logger log = Logger.getLogger(GzStockDataFetcher.class);
    
    public static String getResMsg() {
        return resMsg;
    }

    public static void setResMsg(String resMsg) {
        GzStockDataFetcher.resMsg = resMsg;
    }

    static public boolean start() {
        if (self == null) {
            GzRawStockDataConsumer gsdc = new GzRawStockDataConsumer(0, 0);
            self = new GzStockDataFetcher(0, 5000, gsdc);
            if (WorkManager.submitWork(self)) {
                resMsg = "Newly created GzStockDataFetcher and started!";
                return true;
            }
        }
        else if (WorkManager.canSubmitWork(self.getWorkName())) {
            if (WorkManager.submitWork(self)) {
                resMsg = "Resubmitted GzStockDataFetcher and started!";
                return true;
            }
        }
        resMsg = "Work GzStockDataFetcher is started, can not start again!";
        return false;
    }
    
    static public boolean stop() {
        if (self == null) {
            resMsg = "GzStockDataFetcher is null, how did you stop it?";
            return true;
        }
        else if (WorkManager.canSubmitWork(self.getWorkName())) {
            resMsg = "GzStockDataFetcher is stopped, but can submit again.";
            return true;
        }
        else if (WorkManager.cancelWork(self.getWorkName())) {
            resMsg = "GzStockDataFetcher is cancelled successfully.";
            return true;
        }
        resMsg = "GzStockDataFetcher can not be cancelled!, this is unexpected";
        return false;
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        GzRawStockDataConsumer gsdc = new GzRawStockDataConsumer(0, 0);
        GzStockDataFetcher fsd = new GzStockDataFetcher(0, 10, gsdc);
        log.info("Main exit");
        WorkManager.submitWork(fsd);
    }

    public GzStockDataFetcher(long id, long dbn, GzRawStockDataConsumer sdcr)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;
        cnsmr = sdcr;
    }

    private String getFetchLst()
    {
        Statement stm = null;
        ResultSet rs = null;
        String sql = "select distinct stk.area, stk.id from stk, usrStk where stk.id = usrStk.id and usrStk.gz_flg = 1";

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
            log.info("GzStockDataFetcher successfully lunched consumer...");
        }
        else {
            log.info("GzStockDataFetcher consumer looks already running, skip resubmit...");
        }
        
        MonitorGzStockData mgsd = new MonitorGzStockData(cnsmr.refreshedStocks);
        if (WorkManager.canSubmitWork(mgsd.getWorkName())) {
            WorkManager.submitWork(mgsd);
            log.info("GzStockDataFetcher successfully lunched MonitorGzStockData...");
        }
        else {
            log.info("GzStockDataFetcher MonitorGzStockData looks already running, skip resubmit...");
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
                    cnsmr.getRdq().getDatque().put(srd);
                }
                br.close();
                if (failCnt > 0)
                {
                    failCnt = 0;
                    log.info("Stock data is same 50 times, sleep 1 minute GzStockDataFetcher...");
                    Thread.currentThread().sleep(60*1000);
                    //WorkManager.cancelWork(this.getWorkName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("GzStockDataFetcher Now exit!!!");
    }

    public String getWorkResult()
    {
        return "";
    }

    public String getWorkName()
    {
        return "GzStockDataFetcher";
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
