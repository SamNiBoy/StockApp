package com.sn.work.fetcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.RawStockData;
import com.sn.stock.Stock2;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class GzStockDataFetcher implements IWork {

    static Connection con = DBManager.getConnection();
    /* Initial delay before executing work.
     */
    static long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    static long delayBeforNxtStart = 5;

    static TimeUnit tu = TimeUnit.MILLISECONDS;
    
    static int maxLstNum = 50;
    
    static public String resMsg = "Initial msg for work GzStockDataFetcher.";
    
    static GzStockDataFetcher self = null;
    static GzStockDataConsumer cnsmr = null;
    
    static Logger log = Logger.getLogger(GzStockDataFetcher.class);
    
    public static String getResMsg() {
        return resMsg;
    }

    public static void setResMsg(String resMsg) {
        GzStockDataFetcher.resMsg = resMsg;
    }

    static public boolean start() {
        self = new GzStockDataFetcher(0, Stock2.StockData.SECONDS_PER_FETCH * 1000);
        cnsmr = new GzStockDataConsumer(0, 0);
        if (WorkManager.submitWork(self)) {
            log.info("Newly created GzStockDataFetcher and started!");
            //WorkManager.submitWork(cnsmr);
            return true;
        }
        log.info("can not submit GzStockDataFetcher!");
        return false;
    }
    
    static public boolean stop() {
        if (WorkManager.cancelWork(self.getWorkName())) {
            log.info("GzStockDataFetcher is cancelled successfully.");
            return true;
        }
        log.info("GzStockDataFetcher can not be cancelled!, this is unexpected");
        return false;
    }
    /**
     * @param args
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws InterruptedException {
        // TODO Auto-generated method stub
    	GzStockDataFetcher fsd = new GzStockDataFetcher(0,4000);
    	cnsmr = new GzStockDataConsumer(0, 0);
        WorkManager.submitWork(fsd);
        //GzStockDataFetcher.start();
    	//fsd.run();
        WorkManager.waitUntilWorkIsDone("GzStockDataFetcher");
    }

    public GzStockDataFetcher(long id, long dbn)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    private String getFetchLst()
    {
        Statement stm = null;
        ResultSet rs = null;
        String sql = "select distinct stk.area, stk.id from stk, usrStk where stk.id = usrStk.id and usrStk.gz_flg = 1";

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
        log.info("GzStockDataFetcher started!!!");
        
        if (WorkManager.canSubmitWork(cnsmr.getWorkName())) {
        	WorkManager.submitWork(cnsmr);
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
                
                    //log.info(str);
                    srd = RawStockData.createStockData(str);
                    cnsmr.getDq().put(srd);
                    log.info("GzStockDataFetcher put stock data to queue:" + srd.id + " size is:" + cnsmr.getDq().size());
                }
                br.close();
                if (failCnt > 0)
                {
                    log.info("Stock data is same " + failCnt + " times, breaking loop from GzStockDataFetcher...");
                    failCnt = 0;
                    //Thread.currentThread().sleep(60*1000);
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
