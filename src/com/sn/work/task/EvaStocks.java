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

    static Connection con = DBManager.getConnection();
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

            try {
                log.info("Now start evaluating stocks...");
                Statement mainStm = con.createStatement();
                ResultSet mainRs;
                String mainSql = "select id from stk where id in (select id from stkddf having(count(*)) > 1 group by id) order by id";

                log.info(mainSql);
                mainRs = mainStm.executeQuery(mainSql);
                log.info("returned:" + mainRs.getRow() + " rows.");
                synchronized (stkLst) {
                    stkLst.clear();
                    while (mainRs.next()) {
                        String id = mainRs.getString("id");
                        Stock stk = new Stock(id, 5, 1);
                        stkLst.add(stk);
                    }
                    mainStm.close();
                    calMaxs();
                }
            } catch (Exception e) {
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

    private static void calMaxs() {
        double incCntMax = 0;
        double dscCntMax = 0;
        double ctnIncMax = 0;
        double ctnDscMax = 0;
        double incPctMax = 0;
        double qtyRatioMax = 0;
        for (int i = 0; i < stkLst.size(); i++) {
            Stock s = stkLst.get(i);
            if (s.map.get("incCnt") > incCntMax) {
                incCntMax = s.map.get("incCnt");
                log.info("Calculated incCntMax:" + incCntMax);
                stkMaxs.put("incCntMax", incCntMax);
            }
            if (s.map.get("dscCnt") > dscCntMax) {
                dscCntMax = s.map.get("dscCnt");
                log.info("Calculated dscCntMax:" + dscCntMax);
                stkMaxs.put("dscCntMax", dscCntMax);
            }
            if (s.map.get("ctnInc") > ctnIncMax) {
                ctnIncMax = s.map.get("ctnInc");
                log.info("Calculated ctnIncMax:" + ctnIncMax);
                stkMaxs.put("ctnIncMax", ctnIncMax);
            }
            if (s.map.get("ctnDsc") > ctnDscMax) {
                ctnDscMax = s.map.get("ctnDsc");
                log.info("Calculated ctnDscMax:" + ctnDscMax);
                stkMaxs.put("ctnDscMax", ctnDscMax);
            }
            if (s.map.get("incPct") > incPctMax) {
                incPctMax = s.map.get("incPct");
                log.info("Calculated incPctMax:" + incPctMax);
                stkMaxs.put("incPctMax", incPctMax);
            }
            if (s.map.get("qtyRatio") > qtyRatioMax) {
                qtyRatioMax = s.map.get("qtyRatio");
                log.info("Calculated qtyRatioMax:" + qtyRatioMax);
                stkMaxs.put("qtyRatioMax", qtyRatioMax);
            }
        }
    }

    class SortBst implements Comparator<Stock> {

        @Override
        public int compare(Stock arg0, Stock arg1) {
            // TODO Auto-generated method stub
            Stock s0 = arg0;
            Stock s1 = arg1;
            double incCnt = 0, dscCnt = 0, ctnInc = 0, ctnDsc = 0, incPct = 0, qtyRatio = 0;
            double incCnt1 = 0, dscCnt1 = 0, ctnInc1 = 0, ctnDsc1 = 0, incPct1 = 0, qtyRatio1 = 0;
            double wgtIncCnt = 0.1, wgtDscCnt = 0.1, wgtCtnInc = 0.2, wgtCtnDsc = 0.1, wgtIncPct = 0.2, wgtQtyRatio = 0.3;
            incCnt = s0.map.get("incCnt") / stkMaxs.get("incCntMax");
            dscCnt = s0.map.get("dscCnt") / stkMaxs.get("dscCntMax");
            ctnInc = s0.map.get("ctnInc") / stkMaxs.get("ctnIncMax");
            ctnDsc = s0.map.get("ctnDsc") / stkMaxs.get("ctnDscMax");
            incPct = s0.map.get("incPct") / stkMaxs.get("incPctMax");
            qtyRatio = s0.map.get("qtyRatio") / stkMaxs.get("qtyRatioMax");

            incCnt1 = s1.map.get("incCnt") / stkMaxs.get("incCntMax");
            dscCnt1 = s1.map.get("dscCnt") / stkMaxs.get("dscCntMax");
            ctnInc1 = s1.map.get("ctnInc") / stkMaxs.get("ctnIncMax");
            ctnDsc1 = s1.map.get("ctnDsc") / stkMaxs.get("ctnDscMax");
            incPct1 = s1.map.get("incPct") / stkMaxs.get("incPctMax");
            qtyRatio1 = s1.map.get("qtyRatio") / stkMaxs.get("qtyRatioMax");

            // log.info("incCnt:" + incCnt + " wgtIncCnt:" + wgtIncCnt +
            // " incCnt*wgtIncCnt:" +(incCnt*wgtIncCnt));
            // log.info("dscCnt:" + dscCnt + " wgtDscCnt:" + wgtDscCnt +
            // " dscCnt*wgtDscCnt:" +(dscCnt*wgtDscCnt));
            // log.info("ctnInc:" + ctnInc + " wgtCtnInc:" + wgtCtnInc +
            // " ctnInc*wgtCtnInc:" +(incCnt*wgtCtnInc));
            // log.info("ctnDsc:" + ctnDsc + " wgtCtnDsc:" + wgtCtnDsc +
            // " ctnDsc*wgtCtnDsc:" +(ctnDsc*wgtCtnDsc));
            // log.info("incPct:" + incPct + " wgtIncPct:" + wgtIncPct +
            // " incPct*wgtIncPct:" +(incPct*wgtIncPct));
            // log.info("qtyRatio:" + qtyRatio + " wgtQtyRatio:" + wgtQtyRatio +
            // " qtyRatio*wgtQtyRatio:" +(qtyRatio*wgtQtyRatio));
            //            
            // log.info("incCnt1:" + incCnt1 + " wgtIncCnt:" + wgtIncCnt +
            // " incCnt1*wgtIncCnt:" +(incCnt1*wgtIncCnt));
            // log.info("dscCnt1:" + dscCnt1 + " wgtDscCnt:" + wgtDscCnt +
            // " dscCnt1*wgtDscCnt:" +(dscCnt1*wgtDscCnt));
            // log.info("ctnInc1:" + ctnInc1 + " wgtCtnInc:" + wgtCtnInc +
            // " ctnInc1*wgtCtnInc:" +(incCnt1*wgtCtnInc));
            // log.info("ctnDsc1:" + ctnDsc1 + " wgtCtnDsc:" + wgtCtnDsc +
            // " ctnDsc1*wgtCtnDsc:" +(ctnDsc1*wgtCtnDsc));
            // log.info("incPct1:" + incPct1 + " wgtIncPct:" + wgtIncPct +
            // " incPct1*wgtIncPct:" +(incPct1*wgtIncPct));
            // log.info("qtyRatio1:" + qtyRatio1 + " wgtQtyRatio:" + wgtQtyRatio
            // + " qtyRatio1*wgtQtyRatio:" +(qtyRatio1*wgtQtyRatio));

            if ((incCnt * wgtIncCnt + dscCnt * wgtDscCnt + ctnInc * wgtCtnInc
                    + ctnDsc * wgtCtnDsc + incPct * wgtIncPct + qtyRatio
                    * wgtQtyRatio) > (incCnt1 * wgtIncCnt + dscCnt1 * wgtDscCnt
                    + ctnInc1 * wgtCtnInc + ctnDsc1 * wgtCtnDsc + incPct1
                    * wgtIncPct + qtyRatio1 * wgtQtyRatio)) {
                log.info("s0 ID:" + s0.getID() + " < s1 ID:" + s1.getID());
                return -1;
            } else {
                log.info("s0 ID:" + s0.getID() + " > s1 ID:" + s1.getID());
                return 1;
            }
        }
    }

    class SortWst implements Comparator<Stock> {

        @Override
        public int compare(Stock arg0, Stock arg1) {
            // TODO Auto-generated method stub
            Stock s0 = arg0;
            Stock s1 = arg1;
            double incCnt = 0, dscCnt = 0, ctnInc = 0, ctnDsc = 0, incPct = 0, qtyRatio = 0;
            double incCnt1 = 0, dscCnt1 = 0, ctnInc1 = 0, ctnDsc1 = 0, incPct1 = 0, qtyRatio1 = 0;
            double wgtIncCnt = 0, wgtDscCnt = 0, wgtCtnInc = 0.5, wgtCtnDsc = 0, wgtIncPct = 0, wgtQtyRatio = 0.5;
            incCnt = s0.map.get("incCnt") / stkMaxs.get("incCntMax");
            dscCnt = s0.map.get("dscCnt") / stkMaxs.get("dscCntMax");
            ctnInc = s0.map.get("ctnInc") / stkMaxs.get("ctnIncMax");
            ctnDsc = s0.map.get("ctnDsc") / stkMaxs.get("ctnDscMax");
            incPct = s0.map.get("incPct") / stkMaxs.get("incPctMax");
            qtyRatio = s0.map.get("qtyRatio") / stkMaxs.get("qtyRatioMax");

            incCnt1 = s1.map.get("incCnt") / stkMaxs.get("incCntMax");
            dscCnt1 = s1.map.get("dscCnt") / stkMaxs.get("dscCntMax");
            ctnInc1 = s1.map.get("ctnInc") / stkMaxs.get("ctnIncMax");
            ctnDsc1 = s1.map.get("ctnDsc") / stkMaxs.get("ctnDscMax");
            incPct1 = s1.map.get("incPct") / stkMaxs.get("incPctMax");
            qtyRatio1 = s1.map.get("qtyRatio") / stkMaxs.get("qtyRatioMax");

            log.info("incCnt:" + incCnt + " wgtIncCnt:" + wgtIncCnt
                    + " incCnt*wgtIncCnt:" + (incCnt * wgtIncCnt));
            log.info("dscCnt:" + dscCnt + " wgtDscCnt:" + wgtDscCnt
                    + " dscCnt*wgtDscCnt:" + (dscCnt * wgtDscCnt));
            log.info("ctnInc:" + ctnInc + " wgtCtnInc:" + wgtCtnInc
                    + " ctnInc*wgtCtnInc:" + (incCnt * wgtCtnInc));
            log.info("ctnDsc:" + ctnDsc + " wgtCtnDsc:" + wgtCtnDsc
                    + " ctnDsc*wgtCtnDsc:" + (ctnDsc * wgtCtnDsc));
            log.info("incPct:" + incPct + " wgtIncPct:" + wgtIncPct
                    + " incPct*wgtIncPct:" + (incPct * wgtIncPct));
            log.info("qtyRatio:" + qtyRatio + " wgtQtyRatio:" + wgtQtyRatio
                    + " qtyRatio*wgtQtyRatio:" + (qtyRatio * wgtQtyRatio));

            log.info("incCnt1:" + incCnt1 + " wgtIncCnt:" + wgtIncCnt
                    + " incCnt1*wgtIncCnt:" + (incCnt1 * wgtIncCnt));
            log.info("dscCnt1:" + dscCnt1 + " wgtDscCnt:" + wgtDscCnt
                    + " dscCnt1*wgtDscCnt:" + (dscCnt1 * wgtDscCnt));
            log.info("ctnInc1:" + ctnInc1 + " wgtCtnInc:" + wgtCtnInc
                    + " ctnInc1*wgtCtnInc:" + (incCnt1 * wgtCtnInc));
            log.info("ctnDsc1:" + ctnDsc1 + " wgtCtnDsc:" + wgtCtnDsc
                    + " ctnDsc1*wgtCtnDsc:" + (ctnDsc1 * wgtCtnDsc));
            log.info("incPct1:" + incPct1 + " wgtIncPct:" + wgtIncPct
                    + " incPct1*wgtIncPct:" + (incPct1 * wgtIncPct));
            log.info("qtyRatio1:" + qtyRatio1 + " wgtQtyRatio:" + wgtQtyRatio
                    + " qtyRatio1*wgtQtyRatio:" + (qtyRatio1 * wgtQtyRatio));

            if ((incCnt * wgtIncCnt + dscCnt * wgtDscCnt + ctnInc * wgtCtnInc
                    + ctnDsc * wgtCtnDsc + incPct * wgtIncPct + qtyRatio
                    * wgtQtyRatio) > (incCnt1 * wgtIncCnt + dscCnt1 * wgtDscCnt
                    + ctnInc1 * wgtCtnInc + ctnDsc1 * wgtCtnDsc + incPct1
                    * wgtIncPct + qtyRatio1 * wgtQtyRatio)) {
                log.info("s0 ID:" + s0.getID() + " < s1 ID:" + s1.getID());
                return -1;
            } else {
                log.info("s0 ID:" + s0.getID() + " > s1 ID:" + s1.getID());
                return 1;
            }
        }
    }

    public synchronized String getBst10() {
        String msg = "Top 10 as follows:\n";
        Collections.sort(stkLst, new SortBst());
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

    public synchronized String getWst10() {
        String msg = "";
        Collections.sort(stkLst, new SortWst());
        for (int i = 0; i < 10; i++) {
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
        return false;
    }
}
