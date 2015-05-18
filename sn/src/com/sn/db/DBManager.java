package com.sn.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBManager {

    public static final String drive = "oracle.jdbc.driver.OracleDriver";
    /**
     * 连接地址，各个厂商提供单独记住 jdbc:oracle:thin:@localhost:1521:ORCL localhost 是ip地址。
     */
    public static final String url1 = "jdbc:oracle:thin:@192.168.0.100:1521:SO";
    public static final String url2 = "jdbc:oracle:thin:@192.168.0.52:1521:ORCL";
    /**
     * 用户 密码
     */
    public static final String DBUSER = "sam";
    public static final String password = "sam";

    // TODO Auto-generated method stub
    static Connection conn = null;

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    static public Connection getConnection() {
        if (conn == null) {
            System.out.println("Getting db connection...");
            try {
                Class.forName(drive);
                conn = DriverManager.getConnection(url1, DBUSER, password);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return conn;
    }

}
