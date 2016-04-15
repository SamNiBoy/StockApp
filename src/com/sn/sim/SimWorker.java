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
import com.sn.stock.StockMarket;
import com.sn.trader.StockTrader;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class SimWorker implements IWork {

    /*
     * Initial delay before executing work.
     */
    static private long initDelay = 0;

    /*
     * Seconds delay befor executing next work.
     */
    static private long delayBeforNxtStart = 5;

    static private TimeUnit tu = TimeUnit.MILLISECONDS;

    private List<ITradeStrategy> strategies = new ArrayList<ITradeStrategy>();

    private ITradeStrategy strategy = null;

    private List<String> stkToSim = new ArrayList<String>();

    private StockTrader st = new StockTrader(true);
    
    SimStockDriver ssd = new SimStockDriver();

    public String resMsg = "Initial msg for work SimWorker.";
    
    private String workName = "SimWorker";
    
    static volatile boolean marketDegree_refreshed = false;
    
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
        sw.stkToSim.add("002388");
        sw.startSim();
    }

    private boolean reportTradeStat() {
        log.info("reportBuySellStat...");

        boolean rs = strategy.reportTradeStat();

        return rs;
    }

    public void startSim() throws Exception {
        // SimStockDriver.addStkToSim("000727");
    	String start_dt = "";
    	String end_dt = "";
    	Connection con = null;
    	Statement stm = null;
    	ResultSet rs = null;
    	String sql = "";
    	CashAcntManger.ACNT_PREFIX = "SimAcnt";
    	try {
    		con = DBManager.getConnection();
    		stm = con.createStatement();
    		sql = "select to_char(sysdate - 5, 'yyyy-mm-dd') sd, to_char(sysdate, 'yyyy-mm-dd') ed from dual ";
    		log.info(sql);
    		rs = stm.executeQuery(sql);
    		rs.next();
    		start_dt = rs.getString("sd");
    		end_dt = rs.getString("ed");
    		log.info("got start_dt:" + start_dt + " end_dt:" + end_dt);
    	}
    	finally {
    		rs.close();
    		stm.close();
    		con.close();
    	}
        for (String stk : stkToSim) {
            ssd.removeStkToSim();
            ssd.addStkToSim(stk);
            //ssd.setStartEndSimDt("2016-03-02", "2016-04-02");
            ssd.setStartEndSimDt(start_dt, end_dt);
            
            ssd.loadStocks();

            if (!ssd.initData()) {
                log.info("can not init SimStockDriver...");
                return;
            }

//            String AcntForStk = CashAcntManger.ACNT_PREFIX + stk;
//            CashAcntManger
//                    .crtAcnt(AcntForStk, CashAcntManger.DFT_INIT_MNY, 0.0, 0.0, CashAcntManger.DFT_SPLIT, CashAcntManger.DFT_MAX_USE_PCT, false);
//            ICashAccount acnt = CashAcntManger.loadAcnt(AcntForStk);

            for (ITradeStrategy cs : strategies) {
                strategy = cs;
                int StepCnt = 0;
                log.info("Now start simulate trading...");
                while (ssd.step()) {
                    log.info("Simulate step:" + (++StepCnt));
                    for (String stock : ssd.simstocks.keySet()) {
                        Stock2 s = ssd.simstocks.get(stock);
                        st.setStrategy(strategy);
                        if (st.performTrade(s)) {
                            reportTradeStat();
                        }
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
