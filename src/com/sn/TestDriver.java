package com.sn;

import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

public class TestDriver {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        //testReadingURL();
        try {
            testDB();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

     public static final String drive = "oracle.jdbc.driver.OracleDriver";
        /**
         * 连接地址，各个厂商提供单独记住
         * jdbc:oracle:thin:@localhost:1521:ORCL localhost 是ip地址。
         */
        public static final String url = "jdbc:oracle:thin:@192.168.0.100:1521:SO";
        /**
         * 用户 密码
         */
        public static final String DBUSER="sam";
        public static final String password="sam";


        public static void testDB() throws Exception{
            // TODO Auto-generated method stub
                Connection conn = null;//表示数据库连接
                Statement stmt= null;//表示数据库的更新
                ResultSet result = null;//查询数据库
                Class.forName(drive);//使用class类来加载程序
                conn =DriverManager.getConnection(url,DBUSER,password); //连接数据库
                //Statement接口要通过connection接口来进行实例化操作
                stmt = conn.createStatement();
                //执行SQL语句来查询数据库
                result =stmt.executeQuery("SELECT 'abc' name FROM dual");
                while (result.next()) {//判断有没有下一行
                    String name =result.getString(1);
                    System.out.print("name="+name+";");
                    System.out.println();
                }

                //System.out.println(conn);
                result.close();//数据库先开后关
                stmt.close();
                conn.close();//关闭数据库

        }
     static void testReadingURL()
     {
         String str;
            try {
            URL url = new URL("http://hq.sinajs.cn/list=sh601318,sh601313");
                InputStream is = url.openStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                while ((str = br.readLine()) != null)
                {
                    System.out.println("haha");
                    System.out.println(str);
                }
                br.close();
            } catch(IOException e) {
                System.out.println(e);
            }
     }
}
