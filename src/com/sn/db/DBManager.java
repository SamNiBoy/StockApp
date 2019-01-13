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
    private static final String url1 = "jdbc:oracle:thin:@localhost:1523:ORCL12";
    private static final String url2 = "jdbc:oracle:thin:@192.168.0.103:1523:ORCL12";
    private static final String url3 = "jdbc:oracle:thin:@localhost:1523:MAINT";
    /**
     * �û� ����
     */
    private static final String DBUSER = "stockapp";
    private static final String password = "stockapp";
    private static final String AppDir1 = "D:/tomcat7/webapps/StockApp";
    private static final String AppDir2 = "E:/mfc/stockapp";

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
                log.info("connecting db using:" + url2 + "\n Usr/pwd:" + DBUSER
                        + "/" + password);
                conn = DriverManager.getConnection(url2, DBUSER, password);
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
        PropertyConfigurator.configure(AppDir1
                + "/WEB-INF/conf/log4j.properties");
    }

    static void initDataSource()
    {
        log.info("connecting db using:" + url3 + "\n Usr/pwd:" + DBUSER
                + "/" + password);
        ds = new ComboPooledDataSource();

        try {
            ds.setDriverClass(drive);
        } catch (PropertyVetoException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("Can not load driver class:" + drive);
        }
        ds.setJdbcUrl(url1);
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
