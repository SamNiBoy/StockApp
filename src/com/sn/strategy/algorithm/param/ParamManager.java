package com.sn.strategy.algorithm.param;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;

public class ParamManager {

    static Logger log = Logger.getLogger(ParamManager.class);
    
    private static Map<String, Integer>cacheIntParams = new ConcurrentHashMap<String, Integer>();
    private static Map<String, Double>cacheFloatParams = new ConcurrentHashMap<String, Double>();
    private static Map<String, String>cacheStr1Params = new ConcurrentHashMap<String, String>();
    private static Map<String, String>cacheStr2Params = new ConcurrentHashMap<String, String>();
    
    
    private static Map<String, ParamMap> stock_param = new ConcurrentHashMap<String, ParamMap>();
    
    public static void setStockParamMap(String stkid, ParamMap pm)
    {
        stock_param.put(stkid, pm);
    }
    
    public static void refreshAllParams()
    {
        log.info("Refresh out all cached parameters.");
        cacheIntParams.clear();   
        cacheFloatParams.clear();
        cacheStr1Params.clear();
        cacheStr2Params.clear();
    }
    
    public static void printAllParams()
    {
        log.info("Now dump all cached params:");
        for(String PK : cacheIntParams.keySet())
        {
            log.info(PK + "==>" + cacheIntParams.get(PK));
        }
        for(String PK : cacheFloatParams.keySet())
        {
            log.info(PK + "==>" + cacheFloatParams.get(PK));
        }
        for(String PK : cacheStr1Params.keySet())
        {
            log.info(PK + "==>" + cacheStr1Params.get(PK));
        }
        for(String PK : cacheStr2Params.keySet())
        {
            log.info(PK + "==>" + cacheStr2Params.get(PK));
        }
        log.info("Dumpping params completed!");
    }
    
    public static boolean overrideParam(String name, String cat, Object val, boolean isStr1) 
    {
        String PK = name + "@" + cat;
        String sql_to_run = "";
        
        if (val instanceof Integer)
        {
            Integer oldVal = cacheIntParams.get(PK);
            if (oldVal != null)
            {
                log.info("Overriding old integer param:" + oldVal + " with new value:" + val);
                cacheIntParams.put(PK, (Integer)val);
            }
            else {
                log.info("Overriding put new int value:" + val + " into cache.");
                cacheIntParams.put(PK, (Integer)val);
            }
            sql_to_run = "update param set intval = " + val + " where name = '" + name + "' and cat = '" + cat + "'";
        }
        else if (val instanceof Float || val instanceof Double)
        {
            Double oldVal = cacheFloatParams.get(PK);
            double newVal = (double)val;
            if (oldVal != null)
            {
                log.info("Overriding old float param:" + oldVal + " with new value:" + val);
                cacheFloatParams.put(PK, (double)newVal);
            }
            else {
                log.info("Overriding put new float value:" + val + " into cache.");
                cacheFloatParams.put(PK, (double)newVal);
            }
            sql_to_run = "update param set fltval = " + val + " where name = '" + name + "' and cat = '" + cat + "'";
        }
        else if (val instanceof String)
        {
            if (isStr1)
            {
                String oldVal = cacheStr1Params.get(PK);
                if (oldVal != null)
                {
                    log.info("Overriding old Str1 param:" + oldVal + " with new value:" + val);
                    cacheStr1Params.put(PK, (String)val);
                }
                else {
                    log.info("Overriding put new Str1 value:" + val + " into cache.");
                    cacheStr1Params.put(PK, (String)val);
                }
                sql_to_run = "update param set str1 = '" + val + "' where name = '" + name + "' and cat = '" + cat + "'";
            }
            else {
                String oldVal = cacheStr2Params.get(PK);
                if (oldVal != null)
                {
                    log.info("Overriding old Str2 param:" + oldVal + " with new value:" + val);
                    cacheStr2Params.put(PK, (String)val);
                }
                else {
                    log.info("Overriding put new Str2 value:" + val + " into cache.");
                    cacheStr2Params.put(PK, (String)val);
                }
                sql_to_run = "update param set str2 = '" + val + "' where name = '" + name + "' and cat = '" + cat + "'";
            }
        }
        
        if (sql_to_run.length() > 0)
        {
            Statement st = null;
            Connection con = DBManager.getConnection();
            
            try {
                log.info("update db param with sql:" + sql_to_run);
                st = con.createStatement();
                st.execute(sql_to_run);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.info("DB Exception:" + e.getMessage());
            }
            finally {
                try {
                     st.close();
                     con.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    log.info("Close DB Connection Exception:" + e.getMessage());
                }
            }
        }
        return true;
    }
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ParamManager.getIntParam("BUY_SELL_MAX_DIFF_CNT", "TRADING", null);
        ParamManager.getFloatParam("COMMISSION_RATE", "VENDOR", null);
        ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
        ParamManager.getStr2Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
        printAllParams();
        ParamManager.overrideParam("BUY_SELL_MAX_DIFF_CNT", "TRADING", 3, false);
        ParamManager.overrideParam("COMMISSION_RATE", "VENDOR", 0.0014, false);
        ParamManager.overrideParam("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", "SYSTEM_SUGGESTER", true);
        ParamManager.overrideParam("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", "SYSTEM_GRANTED_TRADER", false);
        printAllParams();
    }
    
    public static Integer getIntParam(String name, String cat, String stkid)
    {
        
        
        if (stkid != null)
        {
            String pk = name + "@" + cat;
            ParamMap pm = stock_param.get(stkid);
            
            if (pm != null)
            {
                Map<String, Param> p = pm.getKV();
                
                if (p.get(pk) != null)
                {
                    Integer v = (Integer)p.get(pk).val;
                    if (v != null)
                    {
                        log.info("get int param from stockParam:" + v);
                        return v;
                    }
                }
            }
        }
        String PK = name + "@" + cat;
        Integer val = cacheIntParams.get(PK);
        
        if (val != null)
        {
            log.info("get int param from cache:" + val + " for name:" + name + ", cat:" + cat);
            return val;
        }
        ResultSet rs = null;
        Statement st = null;
        Connection con = DBManager.getConnection();
        String sql = "select intval from param where name = '" + name + "' and cat = '" + cat + "'";
        
        Integer rtval =  null;
        
        try {
            st = con.createStatement();
            rs = st.executeQuery(sql);
            if (rs.next())
            {
                log.info("Got param int value:" + rs.getInt("intval") + " from db for name:" + name + ", catagory:" + cat);
                rtval = rs.getInt("intval");
                
                cacheIntParams.put(PK, rtval);
                
                rs.close();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            try {
                 st.close();
                 con.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return rtval;
    }
    
    public static double getFloatParam(String name, String cat, String stkid)
    {
        if (stkid != null)
        {
            String pk = name + "@" + cat;
            ParamMap pm = stock_param.get(stkid);
            
            if (pm != null)
            {
                Map<String, Param> p = pm.getKV();
                
                if (p.get(pk) != null)
                {
                    Double v = (double)p.get(pk).val;
                    if (v != null)
                    {
                        log.info("get float param from stockParam:" + v);
                        return v;
                    }
                }
            }
        }
        String PK = name + "@" + cat;
        Double val = cacheFloatParams.get(PK);
        
        if (val != null)
        {
            log.info("get float param from cache:" + val + " for name:" + name + ", cat:" + cat);
            return val;
        }
        
        ResultSet rs = null;
        Statement st = null;
        Connection con = DBManager.getConnection();
        String sql = "select fltval from param where name = '" + name + "' and cat = '" + cat + "'";
        
        Double rtval =  null;
        
        try {
            st = con.createStatement();
            rs = st.executeQuery(sql);
            if (rs.next())
            {
                log.info("Got param float value:" + rs.getFloat("fltval") + " for name:" + name + ", catagory:" + cat);
                rtval = (Double)rs.getDouble("fltval");
                
                cacheFloatParams.put(PK, rtval);
                
                rs.close();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            try {
                 st.close();
                 con.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return rtval;
    }
    public static String getStr1Param(String name, String cat, String stkid)
    {
        if (stkid != null)
        {
            String pk = name + "@" + cat;
            ParamMap pm = stock_param.get(stkid);
            
            if (pm != null)
            {
                Map<String, Param> p = pm.getKV();
                
                if (p.get(pk) != null)
                {
                    String v = (String)p.get(pk).val;
                    
                    if (v != null)
                    {
                        log.info("get str1 param from stockParam:" + v);
                        return v;
                    }
                }
            }
        }
        String PK = name + "@" + cat;
        String val = cacheStr1Params.get(PK);
        
        if (val != null)
        {
            log.info("get Str1 param from cache:" + val + " for name:" + name + ", cat:" + cat);
            return val;
        }
        
        ResultSet rs = null;
        Statement st = null;
        Connection con = DBManager.getConnection();
        String sql = "select str1 from param where name = '" + name + "' and cat = '" + cat + "'";
        
        String rtval =  null;
        
        try {
            st = con.createStatement();
            rs = st.executeQuery(sql);
            if (rs.next())
            {
                log.info("Got param str1 value:" + rs.getString("str1") + " for name:" + name + ", catagory:" + cat);
                rtval = rs.getString("str1");
                
                cacheStr1Params.put(PK, rtval);
                
                rs.close();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            try {
                 st.close();
                 con.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return rtval;
    }
    public static String getStr2Param(String name, String cat, String stkid)
    {
        if (stkid != null)
        {
            String pk = name + "@" + cat;
            ParamMap pm = stock_param.get(stkid);
            
            if (pm != null)
            {
                Map<String, Param> p = pm.getKV();
                if (p.get(pk) != null)
                {
                    String v = (String)p.get(pk).val;
                    if (v != null)
                    {
                        log.info("get str2 param from stockParam:" + v);
                        return v;
                    }
                }
            }
        }
        
        String PK = name + "@" + cat;
        String val = cacheStr2Params.get(PK);
        
        if (val != null)
        {
            log.info("get Str2 param from cache:" + val + " for name:" + name + ", cat:" + cat);
            return val;
        }
        
        ResultSet rs = null;
        Statement st = null;
        Connection con = DBManager.getConnection();
        String sql = "select str2 from param where name = '" + name + "' and cat = '" + cat + "'";
        
        String rtval =  null;
        
        try {
            st = con.createStatement();
            rs = st.executeQuery(sql);
            if (rs.next())
            {
                log.info("Got param str2 value:" + rs.getString("str2") + " for name:" + name + ", catagory:" + cat);
                rtval = rs.getString("str2");
                
                cacheStr2Params.put(PK, rtval);
                
                rs.close();
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            try {
                 st.close();
                 con.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return rtval;
    }

}
