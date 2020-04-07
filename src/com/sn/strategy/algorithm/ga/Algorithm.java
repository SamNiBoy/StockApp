package com.sn.strategy.algorithm.ga;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.simulation.SimStockDriver;
import com.sn.stock.Stock2;
import com.sn.strategy.ITradeStrategy;
import com.sn.strategy.TradeStrategyGenerator;
import com.sn.strategy.algorithm.param.Param;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.strategy.algorithm.param.ParamMap;
import com.sn.trader.StockTrader;


class STKParamMap {
    public ParamMap pm = new ParamMap();
    public double score = 0.0;
}
class Generation {
    private static int size = 10;
    private STKParamMap [] generation = new STKParamMap[size];
    private String stk = "";
    
    public Generation(String sid)
    {
        stk = sid;
    }
    public String getStk() {
        return stk;
    }
    public void setStk(String sid) {
        stk = sid;
    }
    public boolean initGeneration() {
        for (int i = 0; i<generation.length; i++)
        {
            STKParamMap spm = new STKParamMap();
            spm.pm.initParams();
            generation[i]  = spm;
        }
        return true;
    }
    public int getSize() {
        return size;
    }
    
    public double calcualteProfit() {
        return 0;
    }
}
public class Algorithm {
    
    static Logger log = Logger.getLogger(Algorithm.class);
    private int size = 10;
    private Generation gen = null;
    private SimStockDriver ssd = new SimStockDriver();
    private ITradeStrategy strategy = null;
    private StockTrader st = StockTrader.getSimTrader();
    private int MAX_LOOP= 100;
    
    public Algorithm()
    {
    }
    
    public void run() {
        
        //get gzed stock, for each do:
        gen.initGeneration();
        int i = 0;
        while(i < MAX_LOOP)
        {
            i++;
            for (int j=0; j<gen.getSize(); j++)
            {
                simTradeOnStock(gen.getStk());
                gen.calcualteProfit();
            }
            /*keepTopN();
            MutateOnTopN();
            CrossoverOnTopN();*/
        }
        //SaveBest();
    }
    
    
    public void simTradeOnStock(String stkid) {
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
            sql = "select left(max(dl_dt) - interval 1  day, 10) sd, left(max(dl_dt), 10) ed from stkdat2 where stkid = '" + stkid + "'";
            log.info(sql);
            rs = stm.executeQuery(sql);
            rs.next();
            start_dt = rs.getString("sd");
            end_dt = rs.getString("ed");
            
            log.info("got start_dt:" + start_dt + " end_dt:" + end_dt);
        }
        catch (Exception e)
        {
            
        }
        finally {
            try {
                rs.close();
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
        
        strategy = TradeStrategyGenerator.generatorStrategy(true);
        
        st.setStrategy(strategy);
        log.info("Simulate trading with Strategy:" + strategy.getTradeStrategyName() + "\n\n");
        while (ssd.step()) {
            log.info("Simulate step:" + (++StepCnt));
            for (Object stock : ssd.simstocks.keySet()) {
                Stock2 s = (Stock2) ssd.simstocks.get((String)stock);

                lststp = lst_stmp.get(s.getID());
                curstp = s.getDl_dt();
                
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

}
