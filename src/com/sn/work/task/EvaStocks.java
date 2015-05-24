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
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.work.itf.IWork;

public class EvaStocks implements IWork {

    static Connection con = DBManager.getConnection();
    /*
     * Initial delay before executing work.
     */
    long initDelay = 0;

    /*
     * Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 60000;

    TimeUnit tu = TimeUnit.MILLISECONDS;
    
    static ArrayList<Stock> stkLst = new ArrayList<Stock>();

    static Logger log = Logger.getLogger(EvaStocks.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        EvaStocks fsd = new EvaStocks(1, 3);
        fsd.run();
    }

    public EvaStocks(long id, long dbn) {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    /*
     * var hq_str_sh601318=
     * "中国平安,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
     * ;
     */

    private String lstStkDat = "";

    public synchronized void run() {
        // TODO Auto-generated method stub

        try {
            Statement mainStm = con.createStatement();
            ResultSet mainRs;
            String mainSql = "select id from stk order by id";

            mainRs = mainStm.executeQuery(mainSql);
            stkLst.clear();
            while (mainRs.next()) {
                String id = mainRs.getString("id");
                Stock stk = new Stock(id, 5, 1);
                stkLst.add(stk);
            }
            mainStm.close();
        } catch (Exception e) {
            log.error("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    class SortBst implements Comparator{

        @Override
        public int compare(Object arg0, Object arg1) {
            // TODO Auto-generated method stub
            Stock s0 = (Stock)arg0;
            Stock s1 = (Stock)arg0;
            int incCnt = 0, dscCnt = 0, ctnInc = 0, ctnDsc = 0, incPct = 0, qtyRatio = 0;
            if (s0.map.get("incCnt") > s1.map.get("incCnt"))
            {
                incCnt = 1;
            }
            else {
                incCnt = -1;
            }
            if (s0.map.get("dscCnt") > s1.map.get("dscCnt"))
            {
                dscCnt = -1;
            }
            else
            {
                dscCnt = 1;
            }
            if (s0.map.get("ctnInc") > s1.map.get("ctnInc"))
            {
                ctnInc = 1;
            }
            else {
                ctnInc = -1;
            }
            if (s0.map.get("ctnDsc") > s1.map.get("ctnDsc"))
            {
                ctnDsc = -1;
            }
            else {
                ctnDsc = 1;
            }
            if (s0.map.get("incPct") > s1.map.get("incPct"))
            {
                incPct = 1;
            }
            else {
                incPct = -1;
            }
            if (s0.map.get("qtyRatio") > s1.map.get("qtyRatio"))
            {
                qtyRatio = 1;
            }
            else {
                qtyRatio = -1;
            }
            
            if ((incCnt + dscCnt + ctnInc + ctnDsc + incPct + qtyRatio) > 0)
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
    }
    
    class SortWst implements Comparator{

        @Override
        public int compare(Object arg0, Object arg1) {
            // TODO Auto-generated method stub
            Stock s0 = (Stock)arg0;
            Stock s1 = (Stock)arg0;
            int incCnt = 0, dscCnt = 0, ctnInc = 0, ctnDsc = 0, incPct = 0, qtyRatio = 0;
            if (s0.map.get("incCnt") > s1.map.get("incCnt"))
            {
                incCnt = 1;
            }
            else {
                incCnt = -1;
            }
            if (s0.map.get("dscCnt") > s1.map.get("dscCnt"))
            {
                dscCnt = -1;
            }
            else
            {
                dscCnt = 1;
            }
            if (s0.map.get("ctnInc") > s1.map.get("ctnInc"))
            {
                ctnInc = 1;
            }
            else {
                ctnInc = -1;
            }
            if (s0.map.get("ctnDsc") > s1.map.get("ctnDsc"))
            {
                ctnDsc = -1;
            }
            else {
                ctnDsc = 1;
            }
            if (s0.map.get("incPct") > s1.map.get("incPct"))
            {
                incPct = 1;
            }
            else {
                incPct = -1;
            }
            if (s0.map.get("qtyRatio") > s1.map.get("qtyRatio"))
            {
                qtyRatio = 1;
            }
            else {
                qtyRatio = -1;
            }
            
            if ((incCnt + dscCnt + ctnInc + ctnDsc + incPct + qtyRatio) < 0)
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
    }
    
    public synchronized String getBst10()
    {
        String msg = "";
        Collections.sort(stkLst, new SortBst());
        for (int i=0; i< 10; i++)
        {
            msg += stkLst.get(i).dsc();
        }
        return msg;
    }
    
    public synchronized String getWst10()
    {
        String msg = "";
        Collections.sort(stkLst, new SortWst());
        for (int i=0; i< 10; i++)
        {
            msg += stkLst.get(i).dsc();
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
        return true;
    }

}
