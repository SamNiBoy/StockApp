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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.stock.RawStockData;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class RawStockDataConsumer implements IWork {

    private int MAX_QUEUE_SIZE = 10000;
    public RawStockDataQueue dq = new RawStockDataQueue(MAX_QUEUE_SIZE);
    static Connection con = DBManager.getConnection();
    /* Initial delay before executing work.
     */
    long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 5;

    TimeUnit tu = TimeUnit.MILLISECONDS;
    
    static int maxLstNum = 50;
    
    static Logger log = Logger.getLogger(RawStockDataConsumer.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        RawStockDataConsumer fsd = new RawStockDataConsumer(1, 3);
        fsd.run();
    }

    public RawStockDataConsumer(long id, long dbn)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    public RawStockDataQueue getDq() {
        return dq;
    }

    public void setDq(RawStockDataQueue dq) {
        this.dq = dq;
    }

    public void run()
    {
        ConcurrentHashMap<String, Stock2> ss = StockMarket
        .getStocks();
        ArrayBlockingQueue<RawStockData> dd = dq.getDatque();
        int cnt = 0;
        try {
            while (true) {
                RawStockData srd = dd.take();
                Stock2 s = ss.get(srd.id);
                if (s != null) {
                    cnt++;
                    log.info("About to consume RawData from RawStockDataConsume, queue size:" + dd.size());
                    s.saveData(srd, con);
                }
                if (dd.isEmpty()) {
                    log.info("Now run ExactDatForstkDat2 RawStockDataConsume.");
                    ExactDatForstkDat2();
                    con.commit();
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    static void ExactDatForstkDat2()
    {
        Statement stm = null;
        String sql = "insert into stkDat2 " +
                     "select  ft_id," +
                             "id," +
                             "td_opn_pri," +
                             "yt_cls_pri," +
                             "cur_pri," +
                             "td_hst_pri," +
                             "td_lst_pri," +
                             "b1_bst_pri," +
                             "s1_bst_pri," +
                             "dl_stk_num," +
                             "dl_mny_num," +
                             "b1_num," +
                             "b1_pri," +
                             "b2_num," +
                             "b2_pri," +
                             "b3_num," +
                             "b3_pri," +
                             "b4_num," +
                             "b4_pri," +
                             "b5_num," +
                             "b5_pri," +
                             "s1_num," +
                             "s1_pri," +
                             "s2_num," +
                             "s2_pri," +
                             "s3_num," +
                             "s3_pri," +
                             "s4_num," +
                             "s4_pri," +
                             "s5_num," +
                             "s5_pri," +
                             "dl_dt" +
                       " from stkdat s1 " +
                       "where not exists (select 'x' from stkDat2 s2 where s2.ft_id = s1.ft_id) " +
                       "  and not exists (select 'x' from stkDat s3 where s3.id = s1.id and s3.dl_dt = s1.dl_dt and s3.ft_id < s1.ft_id) " +
                       "  and s1.cur_pri > 0";
        log.info(sql);
        try{
            int cnt = 0;
            stm = con.createStatement();
            cnt = stm.executeUpdate(sql);
            stm.close();
        }
        catch(Exception e)
        {
            log.error("FetchStockData errored:" + e.getMessage());
            e.printStackTrace();
        }
        log.info("ExactDatForstkDat2 from RawStockDataConsume finished");
    }


    public String getWorkResult()
    {
        return "";
    }

    public String getWorkName()
    {
        return "RawStockDataConsumer";
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
