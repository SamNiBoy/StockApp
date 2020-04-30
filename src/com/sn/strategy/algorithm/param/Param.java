package com.sn.strategy.algorithm.param;

import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import com.sn.db.DBManager;

public class Param implements Cloneable{
    
    static Logger log = Logger.getLogger(Param.class);
    
    @Override
    protected Object clone() throws CloneNotSupportedException {
        log.info("Cloning Param...");
        Object obj=super.clone();
        return obj;
    }
    
    public enum TYPE {
        INT,
        FLOAT,
        STR1,
        STR2
    }
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        
        Param p1 = new Param("MIN_SHAKING_PCT", "SUGGESTER", 0.01, 0.01, 0.07, 0.01, Param.TYPE.FLOAT); 
        
        p1.generateARandomValue();
        p1.generateNxtValue();
        
        Param p2 = new Param("BUY_SELL_MAX_DIFF_CNT", "TRADING", 1, 1, 5, 1, Param.TYPE.INT); 
        
        p2.generateARandomValue();
        p2.generateNxtValue();
        
        Param s1 = new Param("SUGGESTED_BY_FOR_USER", "TRADING", "ABC", null, null, null, Param.TYPE.STR1); 
        
        s1.generateARandomValue();
        s1.generateNxtValue();
        
        Param s2 = new Param("SUGGESTED_BY_FOR_USER", "TRADING", "ABC", null, null, null, Param.TYPE.STR1); 
        s2.generateARandomValue();
        s2.generateNxtValue();

    }
    public String name;
    public String cat;
    public Object val;
    public TYPE typ;
    
    public Object max;
    public Object min;
    public Object step;
    
    Param (String nm, String ct, Object v, Object mn, Object mx, Object stp, TYPE tp)
    {
        name = nm;
        cat = ct;
        val = v;
        step = stp;
        typ = tp;
        max = mx;
        min = mn;
        /*log.info("Consturct param:");
        log.info("Name:" + name);
        log.info("cat:" + cat);
        log.info("val:" + val);
        log.info("Step:" + step);
        log.info("Typ:" + typ);
        log.info("min:" + min);
        log.info("max:" + max);*/
    }
    
    public Object generateARandomValue() {
        
        Object rtv = null;
        if (typ == TYPE.INT)
        {
            
            int mi = (Integer) min;
            int mx = (Integer) max;
            
            int rt = 0;
            rt = (int)Math.round(mi + Math.random() * (mx - mi));
            rtv = rt;
        }
        else if (typ == TYPE.FLOAT)
        {
            
            double mi = (Double) min;
            double mx = (Double) max;
            
            double rt = 0;
            rt = mi + Math.random() * (mx - mi);
            rtv = rt;
        }
        else {
            log.info("String type parameter does not support randome value.");
        }
        
        val = rtv;
        
        log.info("generateARandomValue:" + rtv);
        return rtv;
    }
    
    public Object generateNxtValue() {
        
        Object rtv = null;
        if (typ == TYPE.INT)
        {
            int rt = (Integer)val + (Integer)step;
            
            if (rt > (Integer) max)
            {
                rt = (Integer)max;
            }
            rtv = rt;
        }
        else if (typ == TYPE.FLOAT)
        {
            double rt = (Double)val + (Double)step;
            
            if (rt > (Double) max)
            {
                rt = (Double)max;
            }
            rtv = rt;
        }
        else {
            log.info("String type parameter does not support next value.");
        }
        
        val = rtv;
        
        log.info("generateNxtValue:" + rtv);
        return rtv;
    }
    
    public void print() {
        log.info("Print param:");
        log.info("Name:" + name);
        log.info("cat:" + cat);
        log.info("val:" + val);
        log.info("Typ:" + typ);
        
        if (step != null)
        {
            log.info("Step:" + step);
            log.info("min:" + min);
            log.info("max:" + max);
        }
    }
}
