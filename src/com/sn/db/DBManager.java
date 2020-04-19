package com.sn.db;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.sn.wechat.action.TopTenWst;

public class DBManager {

    static Logger log = Logger.getLogger(DBManager.class);
    //public static final String drive = "oracle.jdbc.driver.OracleDriver";
    public static final String drive = "org.gjt.mm.mysql.Driver";
    
    public static final String drive4 = "com.mysql.cj.jdbc.Driver";
    /**
     * ���ӵ�ַ�����������ṩ������ס jdbc:oracle:thin:@localhost:1521:ORCL localhost ��ip��ַ��
     */
    private static final String url1 = "jdbc:oracle:thin:@localhost:1523:ORCL12";
    private static final String url2 = "jdbc:oracle:thin:@192.168.0.103:1523:ORCL12";
    private static final String url3 = "jdbc:oracle:thin:@localhost:1523:MAINT";
    /**
     * �û� ����
     */
    private static final String url4 = "jdbc:mysql://111.229.27.150/stockApp?autoReconnect=true&failOverReadOnly=false";
    private static final String url = "jdbc:mysql://localhost/stockApp?autoReconnect=true&failOverReadOnly=false";

    /**
     * �û� ����
     */
    private static final String DBUSER = "root";
    private static final String password = "mysql,16";
    
    private static final String AppDir1 = "D:/tomcat9034/webapps/StockApp";
    private static final String AppDir = "/usr/share/tomcat/webapps/StockApp";
    private static final String AppDir2 = "E:/mfc/stockapp";

    static ComboPooledDataSource  ds = null;
    static Connection conn = null;

    // TODO Auto-generated method stub

    static {
    	DBManager.initLog4j();
    	DBManager.initDataSource();
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Connection con = getConnection();
        log.info("got db connection success: "+ con);

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

    public static void initLog4j() {
        PropertyConfigurator.configure(AppDir1
                + "/WEB-INF/conf/log4j.properties");
    }

    public static void initDataSource()
    {
        log.info("connecting db using:" + url + "\n Usr/pwd:" + DBUSER
                + "/" + password);
        log.info("Driver:" + drive);
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
        Statement stm = null;
        ResultSet rs = null;
        log.info("Try executing:" + sql);
        Timestamp start = Timestamp.valueOf(LocalDateTime.now());
        try {
            stm = con.createStatement();
            rs = stm.executeQuery(sql);
            
            int rowcnt = 0;
            
            if (rs.last()) {
                rowcnt = rs.getRow();
            }
            rs.first();
            
            Timestamp end = Timestamp.valueOf(LocalDateTime.now());
            log.info("Query returned:" + rowcnt + " rows, in " + (end.getTime() - start.getTime())/1000.0 + " seconds.");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.info("Excepton:" + e.getErrorCode());
            }
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
