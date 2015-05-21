package com.sn.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.sn.work.TopTenWst;

public class DBManager {

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

    static {
        initLog4j();
    }
    // TODO Auto-generated method stub
    static Connection conn = null;
    
    static Logger log = Logger.getLogger(DBManager.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Connection con = getConnection();

    }

    static public Connection getConnection() {
        if (conn == null) {
            log.info("Getting db connection...");
            try {
                Class.forName(drive);
                log.info("connecting db using:" + url3 + "\n Usr/pwd:" + DBUSER + "/" + password);
                conn = DriverManager.getConnection(url3, DBUSER, password);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return conn;
    }
    
    static void initLog4j()
    {
        PropertyConfigurator.configure(AppDir1 + "/sn/WEB-INF/conf/log4j.properties"); 
    }
}
