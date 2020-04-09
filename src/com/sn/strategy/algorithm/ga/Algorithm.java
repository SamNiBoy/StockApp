package com.sn.strategy.algorithm.ga;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.simulation.SimStockDriver;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.strategy.ITradeStrategy;
import com.sn.strategy.TradeStrategyGenerator;
import com.sn.strategy.algorithm.param.Param;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.strategy.algorithm.param.ParamMap;
import com.sn.trader.StockTrader;

import sun.util.logging.resources.logging;


class STKParamMap implements Cloneable {
    static Logger log = Logger.getLogger(STKParamMap.class);
    public ParamMap pm = new ParamMap();
    public double score = 0.0;
    
    @Override
    protected Object clone() throws CloneNotSupportedException {
        log.info("cloning STKParamMap...");
        Object obj=super.clone();
        ((STKParamMap)obj).pm= (ParamMap)pm.clone();
        return obj;
    }
    
    public void calcualteScore(String stk) {
        log.info("Start calculate score for stk:" + stk);
        log.info("With param:");
        pm.printParamMap();
        
        //this.score = Math.random();
        //log.info("got a random value as score:" + this.score);
        Connection con = DBManager.getConnection();
        Statement stm = null;
        
        try {
            String sql = "select count(*) cnt,"
                    + "      case when sum(used_mny) is null then 0 else sum(used_mny) end tot_used_mny,"
                    + "      case when sum(used_mny * used_mny_hrs) / sum(used_mny) is null then 0 else sum(used_mny * used_mny_hrs) / sum(used_mny) end tot_used_mny_hrs,"
                    + "      case when sum(pft_mny) is null then 0 else sum(pft_mny) end tot_pft_mny,"
                    + "      case when sum(pft_mny) - sum(commission_mny) is null then 0 else sum(pft_mny) - sum(commission_mny) end net_pft_mny,"
                    + "      case when sum(in_hand_stk_mny) is null then 0 else sum(in_hand_stk_mny) end tot_in_hand_stk_mny,"
                    + "      case when sum(amount_mny) is null then 0 else sum(amount_mny) end tot_amount_mny,"
                    + "      case when sum(commission_mny) is null then 0 else sum(commission_mny) end tot_commission_mny,"
                    + "      case when sum(used_mny) is null then 0 else (sum(pft_mny) - sum(commission_mny)) / (case when sum(used_mny) <= 0 then 1 else sum(used_mny) end) end profit_pct"
                    + "  from cashacnt ca"
                    + "  join (select sum(in_hand_qty * in_hand_stk_price) in_hand_stk_mny,"
                    + "               sum(total_amount) amount_mny,"
                    + "               sum(commission_mny) commission_mny,"
                    + "               acntid"
                    + "          from tradehdr "
                    + "         where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", stk) + stk + "%'"
                    + "         group by acntid) th  "
                    + "    on ca.acntid = th.acntid ";
            log.info(sql);
            stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            
            rs.next();
            if (rs.getInt("cnt")>0) {
                this.score = rs.getDouble("net_pft_mny");
                log.info("got score:" + score + " as net profit mny for stock" + stk);
            }
            else {
                log.info("No net profit got for stok" + stk);
                this.score = 0;
            }
            rs.close();
            stm.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
          try {
            con.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
        }
        log.info("After calculate score for stk:" + stk);
    }
}

class STKParamMapComparator implements Comparator<STKParamMap> {
    public int compare(STKParamMap a, STKParamMap b) {
        if(a.score < b.score){
            return 1;
        }else if(a.score == b.score){
            return 0;
        }else{
            return -1;
        }
    }
}

class Generation {
    static Logger log = Logger.getLogger(Generation.class);
    private int size = 20;
    private int topN = 10;
    private Vector<STKParamMap> entities = new Vector<STKParamMap>(size);
    private String stk = "";
    
    public STKParamMap getSTKParamMap(int i)
    {
        return entities.get(i);
    }
    
    public Generation(String sid, int Topn, int sz)
    {
        stk = sid;
        topN = Topn;
        size = sz;
    }
    public String getStk() {
        return stk;
    }
    public void setStk(String sid) {
        stk = sid;
    }
    public boolean initGeneration() {
        for (int i = 0; i<size; i++)
        {
            STKParamMap spm = new STKParamMap();
            spm.pm.initParams();
            log.info("add STKParamMap " + i + " at initGeneration.");
            entities.add(i, spm);
        }
        return true;
    }
    public int getSize() {
        return size;
    }
    public void calculateScore(int kid)
    {
        entities.get(kid).calcualteScore(stk);
    }
    public void keepTopN() throws CloneNotSupportedException {
        Comparator<STKParamMap> cmp =new STKParamMapComparator();
        Collections.sort(entities, cmp);
        printGen();
        
        for (int j = topN; j<entities.size(); j++) {
            entities.set(j, (STKParamMap)entities.get(j - topN).clone());
        }
        log.info("After keep TopN:" + topN);
        printGen();
    }
    
    public void MutateOnTopN(int i, int max) {
        
        for (int j = topN; j<entities.size(); j++) {
            entities.get(j).score = 0;
            entities.get(j).pm.mutate((max -i) * 1.0 /max);
        }
        log.info("After mutate");
        printGen();
    }
    
    public void CrossoverOnTopN(int i, int max) {
        
        double r = Math.random();
        
        if (r < (max - i) * 1.0 / max)
        {
            for (int j = topN; j<entities.size() - 1; j++) {
                entities.get(j).pm.crossover(entities.get(j+1).pm);
            }
        }
        log.info("After crossover:" + topN);
        printGen();
    }
    
    public void SaveStockBestParam() {
        log.info("Save best param for stock:" + this.stk + " with score:" + entities.get(0).score);
        String sql = "";
        Connection con = DBManager.getConnection();
        Statement stm = null;
        try {
            //fist one is the best.
            STKParamMap spm = entities.get(0);
            ParamMap pm = spm.pm;
            Map<String, Param> kv = pm.getKV();
            
            for (String name : kv.keySet())
            {
                String PK[] = name.split("@");
                
                Param p = kv.get(name);
                
                if (p.typ == Param.TYPE.INT)
                {
                    sql = "insert into stockParam value('" + stk + "','" + PK[0] + "','" + PK[1] + "'," + (Integer)p.val + ", null, null, null, 'GA Algorithm', sysdate(), sysdate())";
                }
                else if (p.typ == Param.TYPE.FLOAT)
                {
                    sql = "insert into stockParam value('" + stk + "','" + PK[0] + "','" + PK[1] + "',null," + (Double)p.val + ", null, null, 'GA Algorithm', sysdate(), sysdate())";
                }
                else if (p.typ == Param.TYPE.STR1)
                {
                    sql = "insert into stockParam value('" + stk + "','" + PK[0] + "','" + PK[1] + "',null,null,'" + (String)p.val + "', null, 'GA Algorithm', sysdate(), sysdate())";
                }
                else if (p.typ == Param.TYPE.STR2)
                {
                    sql = "insert into stockParam value('" + stk + "','" + PK[0] + "','" + PK[1] + "',null,null,null,'" + (String)p.val + "', 'GA Algorithm', sysdate(), sysdate())";
                }
                log.info(sql);
                stm = con.createStatement();
                stm.execute(sql);
                stm.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
          try {
            con.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
        }
        log.info("After keep TopN:" + topN);
    }
    
    public void SaveBestScoreSoFar(int i) {
        String sql = "";
        String msg = "";
        Connection con = DBManager.getConnection();
        Statement stm = null;
        try {
            //fist one is the best.
            STKParamMap spm = entities.get(0);
            ParamMap pm = spm.pm;
            Map<String, Param> kv = pm.getKV();
            
            log.info("Save best score for stock:" + this.stk + " so fare with score:" + spm.score);
            
            for (String PK : kv.keySet())
            {
                
                if (msg.length() > 0)
                {
                    msg += ":";
                }
                Param p = kv.get(PK);
                if (p.typ == Param.TYPE.INT)
                {
                    msg += PK + "=>" + (Integer) p.val;
                }
                else if (p.typ == Param.TYPE.FLOAT)
                {
                    msg += PK + "=>" + (Double) p.val;
                }
                else if (p.typ == Param.TYPE.STR1)
                {
                    msg += PK + "=>" + (String) p.val;
                }
                else if (p.typ == Param.TYPE.STR2)
                {
                    msg += PK + "=>" + (String) p.val;
                }
            }
            
            sql = "insert into stockParamSearch values ('" + stk + "'," + i + "," + spm.score + ", '" + msg + "', sysdate())";
            
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
          try {
            con.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
        }
        log.info("After SaveBestScoreSoFar");
    }
    
    public void printGen() {
        for(int i=0; i<entities.size(); i++)
        {
            log.info("Gen:" + i + " with score:" + entities.get(i).score);
            entities.get(i).pm.printParamMap();
        }
    }
}
public class Algorithm {
    
    static Logger log = Logger.getLogger(Algorithm.class);
    private Generation gen = null;
    private SimStockDriver ssd = null;
    private ITradeStrategy strategy = null;
    private StockTrader st = StockTrader.getSimTrader();
    private int MAX_LOOP= 5;
    private int TOPN = 5;
    private int GEN_SIZE = 20;
    
    public Algorithm()
    {
    }
    public static void main(String[] args) throws CloneNotSupportedException {
        // TODO Auto-generated method stub
        Algorithm ag = new Algorithm();
        ag.run();
    }
    
    public void run() throws CloneNotSupportedException {
        

        //get gzed stock, for each do:
        
        log.info("Now start search best param for gz stock");
        ConcurrentHashMap<String, Stock2> gzstocks = StockMarket.getGzstocks();
        
        for (String stk : gzstocks.keySet())
        {
            log.info("Start search best param for:" + stk);
            gen = new Generation(stk, TOPN, GEN_SIZE);
            
            gen.initGeneration();
            
            int i = 0;
            int start_frm = 0;
            
            resetParamData(stk);
            
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
                    resetTradeData(stk);
                    simTradeOnStock(gen.getStk());
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
            log.info("Finished search best param for stk:" + stk);
        }
        log.info("After GA searched all good param for stocks, load these pareams into ParamManager for trading.");
        ParamManager.loadStockParam();
        ParamManager.printAllParams();
        log.info("Now GA Algorithm task completed!");
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
            sql = "select left(max(dl_dt) - interval 1  day, 10) sd, left(max(dl_dt), 10) ed from stkdat2 where id = '" + stkid + "'";
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
        
        strategy = TradeStrategyGenerator.generatorStrategy(true);
        
        strategy.resetStrategyStatus();
        
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
