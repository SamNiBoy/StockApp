package com.sn.strategy.algorithm.ga;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.sn.STConstants;
import com.sn.cashAcnt.CashAcntManger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.simulation.SimStockDriver;
import com.sn.strategy.ITradeStrategy;
import com.sn.strategy.TradeStrategyGenerator;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.stock.RawStockData;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.trader.StockTrader;
import com.sn.task.WorkManager;
import com.sn.task.IWork;

public class GAWorker implements IWork {

    /*
     * Initial delay before executing work.
     */
    static private long initDelay = 0;

    /*
     * Seconds delay befor executing next work.
     */
    static private long delayBeforNxtStart = 5;

    static private TimeUnit tu = TimeUnit.MILLISECONDS;

    public String resMsg = "Initial msg for work GAWorker.";
    
    private String workName = "GAWorker";
    
    private CountDownLatch threadsCountDown = null;
    
    private Generation gen = null;
    private SimStockDriver ssd = null;
    private ITradeStrategy strategy = null;
    private StockTrader st = StockTrader.getSimTrader();
    private String stkid = "";
    private int MAX_LOOP= 5;
    private int TOPN = 5;
    private int GEN_SIZE = 20;
    
    static Logger log = Logger.getLogger(GAWorker.class);

    
    public void setThreadsCountDown(CountDownLatch tc) {
        threadsCountDown = tc;
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

        GAWorker sw = new GAWorker("ABC","002228", TradeStrategyGenerator.generatorStrategy(true));
        sw.run();
    }

    public GAWorker(String nm, String stk, ITradeStrategy stg) {
    	workName = nm;
    	stkid = stk;
    	strategy = stg;
    }

    public void run() {
        try {
            log.info("GAWorker about to run...");
            
            log.info("Start search best param for:" + stkid);
            
            gen = new Generation(stkid, TOPN, GEN_SIZE);
            
            gen.initGeneration();
            
            int i = 0;
            int start_frm = 0;
            
            resetParamData(stkid);
            
            while(i < MAX_LOOP)
            {
                i++;
                
                if (i == 1)
                {
                    start_frm = 0;
                }
                else
                {
                    start_frm = TOPN;
                }
                for (int j= start_frm; j<gen.getSize(); j++)
                {
                    ParamManager.setStockParamMap(gen.getStk(), gen.getSTKParamMap(j).pm);
                    
                    ssd = new SimStockDriver();
                    resetTradeData(stkid);
                    simTradeOnStock(gen.getStk(), i);
                    gen.calculateScore(j);
                }
                
                gen.keepTopN();
                gen.SaveBestScoreSoFar(i);

                if (i < MAX_LOOP)
                {
                    gen.MutateOnTopN(i, MAX_LOOP);
                    gen.CrossoverOnTopN(i, MAX_LOOP);
                }
            }
            
            gen.SaveStockBestParam();
            
            log.info("Finished search best param for stk:" + stkid);
            
            log.info("threadsCountDown about to countdown:" + threadsCountDown.getCount());
            
            threadsCountDown.countDown();
            
            log.info(workName + " about to finish, there are " + threadsCountDown.getCount() + " colleagues workers running.");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.info("GAWorker run exception as below:");
            log.error(e.getMessage(),e); 
            log.info("Exception happened, countdown so other threads can continue.");
            threadsCountDown.countDown();
        }
    }
    
    private static void resetParamData(String stkid) {
        String sql;
        Connection con = null;
        try {
            con = DBManager.getConnection();
            Statement stm = con.createStatement();
            
            stm = con.createStatement();
            sql = "delete from stockParam where stock = '" + stkid + "'";
            log.info(sql);
            stm.execute(sql);
            stm.close();
            
            stm = con.createStatement();
            sql = "delete from stockParamSearch where stock = '" + stkid + "'";
            log.info(sql);
            stm.execute(sql);
            stm.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
        finally {
            try {
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    private static void resetTradeData(String stkid) {
        String sql;
        Connection con = null;
        try {
            con = DBManager.getConnection();
            Statement stm = con.createStatement();
            sql = "delete from tradedtl where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", stkid) + stkid + "%'";
            log.info(sql);
            stm.execute(sql);
            stm.close();
            
            stm = con.createStatement();
            sql = "delete from tradehdr where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", stkid) + stkid + "%'";
            log.info(sql);
            stm.execute(sql);
            stm.close();
            
            stm = con.createStatement();
            sql = "delete from CashAcnt where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", stkid) + stkid + "%'";
            log.info(sql);
            stm.execute(sql);
            stm.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
        finally {
            try {
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public void simTradeOnStock(String stkid, int genid) {
        // SimStockDriver.addStkToSim("000727");
        String start_dt = "";
        String end_dt = "";
        Connection con = null;
        Statement stm = null;
        ResultSet rs = null;
        String sql = "";
        
        try {
            con = DBManager.getConnection();
            stm = con.createStatement();
            //We train param with one day before sim day so we can evaluate how good the result is for future data.
            sql = "select left(max(dl_dt) - interval 2 day, 10) sd, left(max(dl_dt) - interval 1 day, 10) ed from stkdat2 where id = '" + stkid + "'";
            log.info(sql);
            rs = stm.executeQuery(sql);
            rs.next();
            start_dt = rs.getString("sd");
            end_dt = rs.getString("ed");
            
            rs.close();
            
            log.info("got start_dt:" + start_dt + " end_dt:" + end_dt);
        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        ssd.removeStkToSim();
        ssd.addStkToSim(stkid);
        //ssd.setStartEndSimDt("2016-03-02", "2016-04-02");
        ssd.setStartEndSimDt(start_dt, end_dt);
        
        ssd.loadStocks();

        if (!ssd.initData()) {
            log.info("can not init SimStockDriver...");
            return;
        }

        Map<String, Timestamp> lst_stmp = new HashMap<String, Timestamp>();
        Timestamp lststp = null;
        Timestamp curstp = null;
        int StepCnt = 0;
        log.info("Now start simulate trading...");
        
        st.setStrategy(strategy);
        
        log.info("Simulate trading with Strategy:" + strategy.getTradeStrategyName() + "\n\n");
        while (ssd.step()) {
            log.info(workName + " running at genid " + genid + " with simulate step:" + (++StepCnt));
            for (Object stock : ssd.simstocks.keySet()) {
                Stock2 s = (Stock2) ssd.simstocks.get((String)stock);

                lststp = lst_stmp.get(s.getID());
                curstp = s.getDl_dt();
                
                //stock may not trade today, if so skip.
                if (curstp == null)
                	continue;
                
                log.info("simulate step:" + StepCnt + " for stock:" + s.getID() + " at time:" + curstp.toString());
                
                if (((lststp != null && curstp.after(lststp)) || lststp == null) && st.performTrade(s)) {
                    //strategy.reportTradeStat();
                }
                else if (lststp != null && !curstp.after(lststp)) {
                    log.info("skip trading same record for:" + s.getID() + " at:" + lststp.toString());
                }
                
                lst_stmp.put(s.getID(), curstp);
            }
            //ssd.startOver();
        }
        ssd.finishStep();
        log.info("Now end simulate trading.");
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
