package com.sn.sim;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.CashAcntManger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.ITradeStrategy;
import com.sn.sim.strategy.imp.TradeStrategyGenerator;
import com.sn.stock.RawStockData;
import com.sn.stock.Stock2;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class SimWorker implements IWork {

    /*
     * Initial delay before executing work.
     */
    private long initDelay = 0;

    /*
     * Seconds delay befor executing next work.
     */
    private long delayBeforNxtStart = 5;

    private TimeUnit tu = TimeUnit.MILLISECONDS;

    private List<ITradeStrategy> strategies = new ArrayList<ITradeStrategy>();

    private ITradeStrategy strategy = null;

    private List<String> stkToSim = new ArrayList<String>();

    SimStockDriver ssd = new SimStockDriver();

    public String resMsg = "Initial msg for work SimWorker.";
    
    private String workName = "DefaultWorker";

    static Logger log = Logger.getLogger(SimWorker.class);

    public boolean addStksToWorker(List<String> stkLst) {
        stkToSim.addAll(stkLst);
        return true;
    }

    public String getResMsg() {
        return resMsg;
    }

    public void setResMsg(String reMsg) {
        resMsg = reMsg;
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        SimWorker sw = new SimWorker(0, 0, "testSimWorker");
        sw.stkToSim.add("600891");
        sw.startSim();
    }

    private boolean buyStock() {
        log.info("strat buyStock...");
        boolean hasBoughtStock = false;

        for (String stock : ssd.simstocks.keySet()) {
            Stock2 s = ssd.simstocks.get(stock);
            if (strategy.isGoodStockToSelect(s) && strategy.isGoodPointtoBuy(s)) {
                if (strategy.buyStock(s)) {
                    hasBoughtStock = true;
                }
            }
            if (strategy.getCashAccount().hasStockInHand(s)) {
                String dt = s.getDl_dt().toString().substring(0, 10);
                strategy.calProfit(dt, ssd.simstocks);
            }
        }
        log.info("end buyStock...");
        return hasBoughtStock;
    }

    private boolean sellStock() {
        log.info("start sellStock...");
        boolean hasSoldStock = false;

        for (String stock : ssd.simstocks.keySet()) {
            Stock2 s = ssd.simstocks.get(stock);
            if (strategy.isGoodPointtoSell(s)) {
                if (strategy.sellStock(s)) {
                    hasSoldStock = true;
                }
            }
            //we may have no qty in hand after sell.
            if (strategy.getCashAccount().hasStockInHand(s) || hasSoldStock) {
                String dt = s.getDl_dt().toString().substring(0, 10);
                strategy.calProfit(dt, ssd.simstocks);
            }
        }
        log.info("end sellStock...");
        return hasSoldStock;
    }

    private boolean reportTradeStat() {
        log.info("reportBuySellStat...");

        boolean rs = strategy.reportTradeStat();

        return rs;
    }

    public void startSim() throws Exception {
        // SimStockDriver.addStkToSim("000727");
        for (String stk : stkToSim) {
            ssd.removeStkToSim();
            ssd.addStkToSim(stk);
            ssd.setStartEndSimDt("2016-03-06", "2016-03-09");
            
            ssd.loadStocks();

            if (!ssd.initData()) {
                log.info("can not init SimStockDriver...");
                return;
            }

            String AcntForStk = "Acnt" + stk;
            CashAcntManger
                    .crtAcnt(AcntForStk, 20000.0, 0.0, 0.0, 4, 0.5, false);
            ICashAccount acnt = CashAcntManger.loadAcnt(AcntForStk);

            for (ITradeStrategy cs : strategies) {
                strategy = cs;
                strategy.setCashAccount(acnt);
                int StepCnt = 0;
                log.info("Now start simulate trading...");
                while (ssd.step()) {
                    log.info("Simulate step:" + (++StepCnt));
                    if (buyStock() || sellStock()) {
                        reportTradeStat();
                    }
                }
                strategy.reportTradeStat();
                ssd.startOver();
            }
        }
        ssd.finishStep();
        log.info("Now end simulate trading.");
    }

    public SimWorker(long id, long dbn, String wn) throws Exception {
        initDelay = id;
        delayBeforNxtStart = dbn;
        workName = wn;
        strategies.addAll(TradeStrategyGenerator.generatorStrategies());
    }

    public void run() {
        try {
            startSim();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String getWorkResult() {
        return "";
    }

    public String getWorkName() {
        return workName;
    }
    
    public String setWorkName(String wn) {
        return workName = wn;
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
