package com.sn.db;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.sn.work.output.TopTenWst;

public class DBManager {

    static Logger log = Logger.getLogger(DBManager.class);
    public static final String drive = "oracle.jdbc.driver.OracleDriver";
    /**
     * ���ӵ�ַ�����������ṩ������ס jdbc:oracle:thin:@localhost:1521:ORCL localhost ��ip��ַ��
     */
    private static final String url = "jdbc:oracle:thin:@localhost:1521:ORCL122";
    /**
     * �û� ����
     */
    private static final String DBUSER = "hpcapp";
    private static final String password = "default_16";
    private static final String AppDir = "D:/tomcat7/webapps/LaMai";

    static ComboPooledDataSource  ds = null;
    static Connection conn = null;


    static {
        initLog4j();
        initDataSource();
    }
    // TODO Auto-generated method stub

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Connection con = getConnection();

    }

    static public Connection getConnection0() {
        if (conn == null) {
            log.info("Getting db connection...");
            try {
                Class.forName(drive);
                log.info("connecting db using:" + url + "\n Usr/pwd:" + DBUSER
                        + "/" + password);
                conn = DriverManager.getConnection(url, DBUSER, password);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return conn;
    }

    static public Connection getConnection() {
        log.info("Getting db connection from pool...");
        Connection conn = null;

        try {
            conn = ds.getConnection();
            //log.info("problem connection number:" + ds.getNumUnclosedOrphanedConnections());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("Can not get db connection!");
        }
        return conn;
    }

    static void initLog4j() {
        PropertyConfigurator.configure(AppDir
                + "/WEB-INF/conf/log4j.properties");
    }

    static void initDataSource()
    {
        log.info("connecting db using:" + url + "\n Usr/pwd:" + DBUSER
                + "/" + password);
        ds = new ComboPooledDataSource();

        try {
            ds.setDriverClass(drive);
        } catch (PropertyVetoException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("Can not load driver class:" + drive);
        }
        ds.setJdbcUrl(url);
        ds.setUser(DBUSER);
        ds.setPassword(password);
        ds.setMaxPoolSize(200);
        ds.setMinPoolSize(50);
        ds.setInitialPoolSize(50);
        ds.setMaxStatements(100);
    }
    
    static public ResultSet executeSelect(String sql) {
        Connection con = getConnection();
        ResultSet rs = null;
        log.info("Try executing:" + sql);
        try {
            Statement stm = con.createStatement();
            rs = stm.executeQuery(sql);
            stm.close();
            con.close();
            stm = null;
            con = null;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return rs;
    }
    static public boolean executeUpdate(String sql) {
        Connection con = getConnection();
        boolean result = false;
        log.info("Try executing:" + sql);
        try {
            Statement stm = con.createStatement();
            result = stm.execute(sql);
            stm.close();
            con.close();
            stm = null;
            con = null;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}
