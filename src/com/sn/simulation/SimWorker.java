package com.sn.simulation;

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
import com.sn.strategy.ITradeStrategy;
import com.sn.strategy.TradeStrategyGenerator;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.stock.RawStockData;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.trader.StockTrader;
import com.sn.task.WorkManager;
import com.sn.task.IWork;

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

    private ITradeStrategy strategy = null;

    private List<String> stkToSim = new ArrayList<String>();

    private StockTrader st = StockTrader.getSimTrader();
    
    SimStockDriver ssd = new SimStockDriver();

    public String resMsg = "Initial msg for work SimWorker.";
    
    private String workName = "SimWorker";
    
    private CountDownLatch threadsCountDown = null;
    
    static volatile boolean marketDegree_refreshed = false;
    
    static Logger log = Logger.getLogger(SimWorker.class);

    
    public void setThreadsCountDown(CountDownLatch tc) {
        threadsCountDown = tc;
    }
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

        SimWorker sw = new SimWorker(0, 0, "testSimWorker", TradeStrategyGenerator.generatorStrategy(true));
        sw.stkToSim.add("300265");
        sw.startSim();
    }

    public void startSim() throws Exception {
        // SimStockDriver.addStkToSim("000727");
    	String start_dt = "";
    	String end_dt = "";
    	Connection con = null;
    	Statement stm = null;
    	ResultSet rs = null;
    	String sql = "";
    	boolean no_enough_date = false;
        
    	//int sim_days = ParamManager.getIntParam("SIM_DAYS", "SIMULATION", null);
    	int sim_shift_days = ParamManager.getIntParam("SIM_SHIFT_DAYS", "SIMULATION", null);
    	
    	try {
    		con = DBManager.getConnection();
    		sql =  "select left(dl_dt, 10) dte from stkdat2 where id = '000001' group by left(dl_dt, 10) order by dte desc";
		    
    		log.info(sql);

    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		int cnt = sim_shift_days;
    		
    		while(cnt > 1) {
    			if(!rs.next()) {
    				no_enough_date = true;
    				break;
    			}
    			cnt--;
    		}
    		
    		if (no_enough_date || !rs.next()) {
    			no_enough_date = true;
    		}
    		else {
    		    end_dt = rs.getString("dte");
    		}
    		
    		if (no_enough_date || !rs.next()) {
    			no_enough_date = true;
    		}
    		else {
    		    start_dt = rs.getString("dte");
    		}
    		
    		rs.close();
    		
    		if (no_enough_date) {
    			log.info("No enough dates for simulation.");
    			return;
    		}
            
    		/*start_dt = "2020-03-29";
    		end_dt = "2020-03-30";*/
            
    		log.info("got start_dt:" + start_dt + " end_dt:" + end_dt);
    	}
    	finally {
    		stm.close();
    		con.close();
    	}
    	
        ssd.removeStkToSim();
        for (String stk : stkToSim) {
            ssd.addStkToSim(stk);
        }
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
            log.info("Simulate step:" + (++StepCnt));
            for (Object stock : ssd.simstocks.keySet()) {
                Stock2 s = (Stock2) ssd.simstocks.get((String)stock);

                lststp = lst_stmp.get(s.getID());
                curstp = s.getDl_dt();
                
                //stock may not trade today, if so skip.
                if (curstp == null)
                	continue;
                
                log.info(workName + ": simulate step:" + StepCnt + " for stock:" + s.getID() + " at time:" + curstp.toString());
                
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

    public SimWorker(long id, long dbn, String wn, ITradeStrategy stg) throws Exception {
        initDelay = id;
        delayBeforNxtStart = dbn;
        workName = wn;
        strategy = stg;
    }

    public void run() {
        try {
            log.info("SimWorker about to run...");
           //Reset some set/map entries before/after Simulation. 
            startSim();
            
            log.info("threadsCountDown about to countdown:" + threadsCountDown.getCount());
            
            threadsCountDown.countDown();
            
            log.info(workName + " about to finish, there are " + threadsCountDown.getCount() + " colleagues workers running.");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.info("SimWorker run exception as below:");
            log.error(e.getMessage(),e); 
            log.info("Exception happened, countdown so other threads can continue.");
            threadsCountDown.countDown();
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
