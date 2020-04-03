package com.sn.strategy.algorithm.param;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;

public class ParamManager {

    static Logger log = Logger.getLogger(ParamManager.class);
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ParamManager.getIntParam("BUY_SELL_MAX_DIFF_CNT", "TRADING");
        ParamManager.getFloatParam("COMMISSION_RATE", "VENDOR");
        ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING");
        ParamManager.getStr2Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING");

    }
    
    public static Integer getIntParam(String name, String cat)
    {
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
                log.info("Got param int value:" + rs.getInt("intval") + " for name:" + name + ", catagory:" + cat);
                rtval = rs.getInt("intval");
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
    
    public static Float getFloatParam(String name, String cat)
    {
        ResultSet rs = null;
        Statement st = null;
        Connection con = DBManager.getConnection();
        String sql = "select fltval from param where name = '" + name + "' and cat = '" + cat + "'";
        
        Float rtval =  null;
        
        try {
            st = con.createStatement();
            rs = st.executeQuery(sql);
            if (rs.next())
            {
                log.info("Got param float value:" + rs.getFloat("fltval") + " for name:" + name + ", catagory:" + cat);
                rtval = rs.getFloat("fltval");
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
    public static String getStr1Param(String name, String cat)
    {
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
    public static String getStr2Param(String name, String cat)
    {
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
