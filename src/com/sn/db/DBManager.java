package com.sn.db;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.sn.work.output.TopTenWst;

public class DBManager {

    static Logger log = Logger.getLogger(DBManager.class);
    public static final String drive = "oracle.jdbc.driver.OracleDriver";
    /**
     * 连接地址，各个厂商提供单独记住 jdbc:oracle:thin:@localhost:1521:ORCL localhost 是ip地址。
     */
    private static final String url1 = "jdbc:oracle:thin:@192.168.0.100:1521:SO";
    private static final String url2 = "jdbc:oracle:thin:@192.168.0.59:1521:ORCL";
    private static final String url3 = "jdbc:oracle:thin:@localhost:1521:MAINT";
    /**
     * 用户 密码
     */
    private static final String DBUSER = "sam";
    private static final String password = "sam";
    private static final String AppDir1 = "D:/mfc/stockapp";
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
            log.info("problem connection number:" + ds.getNumUnclosedOrphanedConnections());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("Can not get db connection!");
        }
        return conn;
    }

    static void initLog4j() {
        PropertyConfigurator.configure(AppDir2
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
        ds.setJdbcUrl(url2);
        ds.setUser(DBUSER);
        ds.setPassword(password);
        ds.setMaxPoolSize(50);
        ds.setMinPoolSize(20);
        ds.setInitialPoolSize(20);
        ds.setMaxStatements(180);
    }
}
