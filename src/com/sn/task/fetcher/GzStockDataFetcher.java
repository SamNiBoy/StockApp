package com.sn.task.fetcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sn.db.DBManager;
import com.sn.stock.RawStockData;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.WorkManager;
import com.sn.task.IWork;

@DisallowConcurrentExecution
public class GzStockDataFetcher implements Job {

    static int maxLstNum = 50;
    
    static Logger log = Logger.getLogger(GzStockDataFetcher.class);
    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        // TODO Auto-generated method stub
        GzStockDataFetcher fsd = new GzStockDataFetcher();
        //cnsmr = new GzStockDataConsumer();
        //WorkManager.submitWork(fsd);
        //WorkManager.submitWork(cnsmr);
        //GzStockDataFetcher.start();
        //fsd.run();
        //WorkManager.waitUntilWorkIsDone("GzStockDataFetcher");
        /*DayOfWeek week = LocalDateTime.of(2020, 03, 21, 0, 0).getDayOfWeek();
        
        if(week.equals(DayOfWeek.SATURDAY))
        System.out.println(week);*/
    }

    public GzStockDataFetcher()
    {
    }

    private static String getFetchLst()
    {
        Connection con = null;
        Statement stm = null;
        ResultSet rs = null;

        StringBuilder stkLst = new StringBuilder();
        
        int i = 0;

        try{
        	con = DBManager.getConnection();
            stm = con.createStatement();
            rs = stm.executeQuery(StockMarket.GZ_STOCK_SELECT);
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
                con.close();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        log.info(stkLst);

        return stkLst.toString();
    }

    private static String lstStkDat = "";
    private static int failCnt = 0;

    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        // TODO Auto-generated method stub
        String str;
        log.info("GzStockDataFetcher started!!!");
        
        try {
            String fs [] = getFetchLst().split("#"), cs;
            RawStockData srd = null;
            boolean first_start_flg = false; 
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
                        if (lstStkDat.length() <= 0)
                        {
                            first_start_flg = true;
                        }
                        failCnt = 0;
                    }
                    if (i == j && i == 0)
                    {
                        /* Make sure lstStkDat store the last value of first stock*/
                        lstStkDat = str;
                    }
                    
                    j++;
                    
                    if (first_start_flg)
                    {
                        //skip very first record to avoid alwasy fetching when start program during non-business time.
                        br.close();
                        return;
                    }
                
                    //log.info(str);
                    srd = RawStockData.createStockData(str);

                    if (srd.td_opn_pri <= 0) {
                        log.info("market not open yet. td_opn_pri <= 0 for gzstock:" + srd.id + " can not trade based on it, continue");
                        continue;
                    }
                    GzStockDataConsumer.getDq().put(srd);
                    
                    log.info("GzStockDataFetcher put stock data to queue:" + srd.id + " size is:" + GzStockDataConsumer.getDq().size());
                    
                    synchronized (srd) {
                        log.info("now wait GzStockDataConsumer consume the srd:" + srd.id);
                        srd.wait();
                    }
                    log.info("returned from wait GzStockDataConsumer consume the srd:" + srd.id);
                }
                br.close();
                if (failCnt > 0)
                {
                    log.info("Stock data is same or first time " + failCnt + " times, breaking loop from GzStockDataFetcher...");
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
}
