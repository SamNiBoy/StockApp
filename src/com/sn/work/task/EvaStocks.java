package com.sn.work.task;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.work.itf.IWork;
import com.sn.work.fetcher.FetchStockData;

public class EvaStocks implements IWork {

    static Connection con = null;
    /*
     * Initial delay before executing work.
     */
    long initDelay = 0;

    boolean onTimeRun = false;

    /*
     * Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 60000;

    TimeUnit tu = TimeUnit.MILLISECONDS;

    public static ArrayList<Stock> stkLst = new ArrayList<Stock>();
    public static Map<String, Double> stkMaxs = new ConcurrentHashMap<String, Double>();

    static Logger log = Logger.getLogger(EvaStocks.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        EvaStocks fsd = new EvaStocks(1, 3, true);
        fsd.run();
    }

    public EvaStocks(long id, long dbn, boolean otr) {
        initDelay = id;
        delayBeforNxtStart = dbn;
        onTimeRun = otr;
    }

    /*
     * var hq_str_sh601318=
     * "�й�ƽ��,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
     * ;
     */

    public void run() {
        // TODO Auto-generated method stub

        while (true) {

            if (!onTimeRun) {
                synchronized (FetchStockData.bellForWork) {
                    try {
                        log.info("Waiting before start evaluating stocks...");
                        FetchStockData.bellForWork.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        log.error("EvaStocks Can not wait on bellForWork:"
                                + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
            con = DBManager.getConnection();

            try {} catch (Exception e) {
                log.error("Error: " + e.getMessage());
                e.printStackTrace();
            }
            
            if (onTimeRun)
            {
                onTimeRun = false;
                break;
            }
        }
        // getBst10();
    }

    private static void calMaxs() {}


    public synchronized String getBst10() {
        String msg = "Top 10 as follows:\n";
        log.info("EvaStocks getBst10, got:" + stkLst.size() + " stocks!");
        // for (int i = 0; i < stkLst.size(); i++)
        // {
        // log.info(stkLst.get(i).dsc());
        // }
        for (int i = 0; i < 10 && i < stkLst.size(); i++) {
            msg += (i + 1) + ":" + stkLst.get(i).dsc();
        }
        return msg;
    }


    public String getWorkResult() {
        return "";
    }

    public String getWorkName() {
        return "EvaStocks";
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
