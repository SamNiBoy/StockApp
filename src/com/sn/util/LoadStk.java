package com.sn.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;

public class LoadStk {

    static Logger log = Logger.getLogger(LoadStk.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        DBManager dbm;
        Connection con = DBManager.getConnection();
        Statement stm = null;
        String sql;

        try {
            con.setAutoCommit(false);
            stm = con.createStatement();

           // FileReader fr = new FileReader(".\\scripts\\stockcodes.txt");
           // BufferedReader br = new BufferedReader(fr);

//            String s = br.readLine();
//            while (s != null && false) {
//                log.info(s);
//                sql = CreateStk(s);
//                stm.execute(sql);
//                s = br.readLine();
//            }
            LoadRest(stm, con);
            con.commit();
            //br.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                con.rollback();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        finally {
            try {
                stm.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    static int LoadRest(Statement stm, Connection con)
    {
        int loadedCnt = 0;
        
        int i;
        String pf, id, str, stkSql;
        
        for (i =0; i< 9999; i++)
        {
            pf = "000000" + String.valueOf(i);
            id = "sz" + pf.substring(pf.length() - 6);
            stkSql = "http://hq.sinajs.cn/list=" + id;
            
            log.info("Trying " + stkSql);
            try{
                URL url = new URL(stkSql);
                InputStream is = url.openStream();
                InputStreamReader isr = new InputStreamReader(is, "GBK");
                BufferedReader br = new BufferedReader(isr);
                str = br.readLine();
                int idx = str.indexOf(",");
                if (str != null && str.length() > 0 && idx >= 0) {
                    log.info("got new stock:" + str);
                    String area = str.substring(11, 13);
                    String stkID = str.substring(13, 19);
                    
                    String stkName = str.substring(21, idx);
                    
                    if (stkID.equals("002759"))
                    {
                        log.info("Whats wrong here!");
                    }
                    
                    String sql = "insert into stk (id, area, name, py, bu, avgpri1, avgpri2, avgpri3, add_dt, mod_dt) values('"
                            + stkID + "', '"
                            + area + "', '"
                            + stkName + "', '"
                            + "py" + "', '"
                            + "NoDef" + "', null, null, null, sysdate(), sysdate())";
                    log.info(sql);
                    stm.executeUpdate(sql);
                    loadedCnt++;
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        
        try {
            con.commit();
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        for (i =0; i< 9999; i++)
        {
            pf = "000000" + String.valueOf(i);
            id = "sz3" + pf.substring(pf.length() - 5);
            stkSql = "http://hq.sinajs.cn/list=" + id;
            
            log.info("Trying " + stkSql);
            try{
                URL url = new URL(stkSql);
                InputStream is = url.openStream();
                InputStreamReader isr = new InputStreamReader(is, "GBK");
                BufferedReader br = new BufferedReader(isr);
                str = br.readLine();
                int idx = str.indexOf(",");
                if (str != null && str.length() > 0 && idx >= 0) {
                    log.info("got new stock:" + str);
                    String area = str.substring(11, 13);
                    String stkID = str.substring(13, 19);
                    
                    String stkName = str.substring(21, idx);
                    
                    String sql = "insert into stk (id, area, name, py, bu, avgpri1, avgpri2, avgpri3, add_dt, mod_dt) values('"
                            + stkID + "', '"
                            + area + "', '"
                            + stkName + "', '"
                            + "py" + "', '"
                            + "NoDef" + "', null, null, null, sysdate(), sysdate())";
                    log.info(sql);
                    stm.executeUpdate(sql);
                    loadedCnt++;
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        
        try {
            con.commit();
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        for (i =0; i< 9999; i++)
        {
            pf = "000000" + String.valueOf(i);
            id = "sh6" + pf.substring(pf.length() - 5);
            stkSql = "http://hq.sinajs.cn/list=" + id;
            
            log.info("Trying " + stkSql);
            try{
                URL url = new URL(stkSql);
                InputStream is = url.openStream();
                InputStreamReader isr = new InputStreamReader(is, "GBK");
                BufferedReader br = new BufferedReader(isr);
                str = br.readLine();
                int idx = str.indexOf(",");
                if (str != null && str.length() > 0 && idx >= 0) {
                    log.info("got new stock:" + str);
                    String area = str.substring(11, 13);
                    String stkID = str.substring(13, 19);
                    
                    String stkName = str.substring(21, idx);
                    
                    String sql = "insert into stk (id, area, name, py, bu, avgpri1, avgpri2, avgpri3, add_dt, mod_dt) values('"
                            + stkID + "', '"
                            + area + "', '"
                            + stkName + "', '"
                            + "py" + "', '"
                            + "NoDef" + "', null, null, null, sysdate(), sysdate())";
                    log.info(sql);
                    stm.executeUpdate(sql);
                    loadedCnt++;
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        
        try {
            con.commit();
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        log.info("Total loaded new stocks:" + loadedCnt);
        return loadedCnt;
    }
    static String CreateStk(String stk)
    {
        if (stk.length() <= 0)
            return "";

        String values[] = stk.split(",");

        String sql = "insert into stk (id, area, name, py, bu, avgpri1, avgpri2, avgpri3, add_dt, mod_dt) values('"
                + values[1] + "', '"
                + values[0] + "', '"
                + values[2] + "', '"
                + "py" + "', '"
                + values[3] + "')";

        log.info(sql);
        return sql;
    }

}
