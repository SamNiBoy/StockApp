package com.sn.strategy.algorithm.param;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import com.sn.db.DBManager;

public class ParamMap implements Cloneable{
    
    static Logger log = Logger.getLogger(Param.class);
    //Connection con = DBManager.getConnection();
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        log.info("Cloning ParamMap...");
        Object obj=super.clone();
        ((ParamMap)obj).kv = new ConcurrentHashMap<String, Param>();
        for(String k : kv.keySet())
        {
            Param c = kv.get(k);
            Param cl = (Param)c.clone();
            ((ParamMap)obj).kv.put(k, cl);
        }
        return obj;
    }
    
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ParamMap pm = new ParamMap();
        
        pm.initParams();
        
        pm.printParamMap();
        
        pm.mutate(1);
        
        log.info("After mutate:\n");
        pm.printParamMap();
        
        log.info("Now below is for pm2:\n");
        ParamMap pm2 = new ParamMap();
        
        pm2.initParams();
        
        pm2.crossover(pm);
        
        pm2.printParamMap();
        
    }
    
    private Map<String, Param> kv = new ConcurrentHashMap<String, Param>();
    
    /*
     * insert into param values('VOLUME_PLUS_PCT', 'TRADING',null,0.5, '', '', 'Define the last delta trading volume is this above the pct of delta volumes in the queue then it means volume plused.', sysdate(),sysdate());
       insert into param values('BUY_SELL_MAX_DIFF_CNT', 'TRADING', 3,null, '', '', 'Max extra times between buy and sell for same stock.', sysdate(),sysdate());
       insert into param values('MAX_MINUTES_ALLOWED_TO_KEEP_BALANCE', 'TRADING', 30,null, '', '', 'How many minutes in maximum we need to buy/sell stock back for keep balance.', sysdate(),sysdate());
       insert into param values('STOP_BREAK_BALANCE_IF_CURPRI_REACHED_PCT', 'TRADING',null, 0.06, '', '', 'If delta price go above this percentage, stop trading for breaking balance.', sysdate(),sysdate());
       insert into param values('BUY_BASE_TRADE_THRESH', 'TRADING',null, 0.03, '', '', 'QtyBuyPointSelector: Stock min/max price must be bigger than this threshold value for trading.', sysdate(),sysdate());
       insert into param values('SELL_BASE_TRADE_THRESH', 'TRADING',null, 0.03, '', '', 'QtySellPointSelector: Stock min/max price must be bigger than this threshold value for trading.', sysdate(),sysdate());
       insert into param values('MARGIN_PCT_TO_TRADE_THRESH', 'TRADING',null, 0.01, '', '', 'How close to the margin of BASE_TRADE_THRESHOLD value.', sysdate(),sysdate());
     */
    public void initParams() {
        Param p1 = new Param("VOLUME_PLUS_PCT", "TRADING", 0.5, 0.1, 0.8, 0.1, Param.TYPE.FLOAT);
        Param p2 = new Param("BUY_SELL_MAX_DIFF_CNT", "TRADING", 3, 1, 5, 1, Param.TYPE.INT);
        Param p3 = new Param("MAX_MINUTES_ALLOWED_TO_KEEP_BALANCE", "TRADING", 30, 10, 120, 10, Param.TYPE.INT);
        Param p4 = new Param("STOP_BREAK_BALANCE_IF_CURPRI_REACHED_PCT", "TRADING", 0.05, 0.03, 0.08, 0.01, Param.TYPE.FLOAT);
        Param p5 = new Param("BUY_BASE_TRADE_THRESH", "TRADING", 0.03, 0.01, 0.08, 0.01, Param.TYPE.FLOAT);
        Param p6 = new Param("SELL_BASE_TRADE_THRESH", "TRADING", 0.03, 0.01, 0.08, 0.01, Param.TYPE.FLOAT);
        Param p7 = new Param("MARGIN_PCT_TO_TRADE_THRESH", "TRADING", 0.01, 0.001, 0.02, 0.002, Param.TYPE.FLOAT);
        
        kv.put("VOLUME_PLUS_PCT@TRADING", p1);
        kv.put("BUY_SELL_MAX_DIFF_CNT@TRADING", p2);
        kv.put("MAX_MINUTES_ALLOWED_TO_KEEP_BALANCE@TRADING", p3);
        kv.put("STOP_BREAK_BALANCE_IF_CURPRI_REACHED_PCT@TRADING", p4);
        kv.put("BUY_BASE_TRADE_THRESH@TRADING", p5);
        kv.put("SELL_BASE_TRADE_THRESH@TRADING", p6);
        kv.put("MARGIN_PCT_TO_TRADE_THRESH@TRADING", p7);
        
        randomize();
    }
    
    public void randomize() {
        for (String k : kv.keySet())
        {
            log.info("Randomize param for key:" + k);
            kv.get(k).generateARandomValue();
        }
    }
    
    public void nextStep() {
        for (String k : kv.keySet())
        {
            log.info("param for key:" + k);
            kv.get(k).generateNxtValue();
        }
    }
    public void printParamMap() {
        log.info("Print paramMap:");
        String msg = "";
        for (String k : kv.keySet())
        {
            msg += kv.get(k).val + "|";
            
            //kv.get(k).print();
        }
        log.info(msg);
    }
    
    public boolean mutate(double pct)
    {
        boolean mutated = false;
        for (String k : kv.keySet())
        {
            double rd = Math.random();
            if (rd < pct)
            {
                Param p = kv.get(k);
                p.generateARandomValue();
                mutated = true;
            }
        }
        return mutated;
    }
    
    public Map<String, Param> getKV() {
        return kv;
    }

    public void crossover (ParamMap p)
    {
        int sz = kv.size();
        long pos = Math.round((sz - 2) * Math.random()) + 2;
        log.info("crossover at position:" + pos + " with size:" + sz);
        
        int i = 0;
        for (String k : kv.keySet())
        {
            i++;
            if (i < pos)
            {
                continue;
            }
            Param t = kv.get(k);
            Param t2 = p.getKV().get(k);
            
            log.info("Exchange param: " + k);
            kv.put(k, t2);
            p.getKV().put(k, t);
        }
    }
    
}
