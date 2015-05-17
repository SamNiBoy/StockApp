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

        try {
            // testDB();
            testReadingURL();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static final String drive = "oracle.jdbc.driver.OracleDriver";
    /**
     * 连接地址，各个厂商提供单独记住 jdbc:oracle:thin:@localhost:1521:ORCL localhost 是ip地址。
     */
    public static final String url1 = "jdbc:oracle:thin:@192.168.0.100:1521:SO";
    public static final String url2 = "jdbc:oracle:thin:@192.168.0.52:1521:ORCL";
    /**
     * 用户 密码
     */
    public static final String DBUSER = "samni";
    public static final String password = "samni";

    public static void testDB() throws Exception {
        // TODO Auto-generated method stub
        Connection conn = null;// 表示数据库连接
        Statement stmt = null;// 表示数据库的更新
        ResultSet result = null;// 查询数据库
        Class.forName(drive);// 使用class类来加载程序
        conn = DriverManager.getConnection(url2, DBUSER, password); // 连接数据库
        // Statement接口要通过connection接口来进行实例化操作
        stmt = conn.createStatement();
        // 执行SQL语句来查询数据库
        result = stmt.executeQuery("SELECT 'abc' name FROM dual");
        while (result.next()) {// 判断有没有下一行
            String name = result.getString(1);
            System.out.print("name=" + name + ";");
            System.out.println();
        }

        // System.out.println(conn);
        result.close();// 数据库先开后关
        stmt.close();
        conn.close();// 关闭数据库

    }

    static void testReadingURL() {
        String str;
        try {
            URL url = new URL("http://hq.sinajs.cn/list=sz000031");
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            while ((str = br.readLine()) != null) {
                System.out.println(str);
                createStockData(str);
            }
            br.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /*
     * var hq_str_sh601318=
     * "中国平安,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
     * ;
     */
    static void createStockData(String stkDat) {
        String dts[] = stkDat.split(",");
        for (int i = 0; i < dts.length; i++) {
            System.out.println(i + ":" + dts[i]);
        }
        String area = dts[0].substring(11, 13);
        String stkID = dts[0].substring(13, 19);
        String stkName = dts[0].substring(21, 25);
        System.out.println("area:" + area + " stkID:" + stkID + " stkName:" + stkName);
        String py = "";
        double td_opn_pri = Double.valueOf(dts[1]);
        double yt_cls_pri = Double.valueOf(dts[2]);
        double cur_pri = Double.valueOf(dts[3]);
        double td_hst_pri = Double.valueOf(dts[4]);
        double td_lst_pri = Double.valueOf(dts[5]);
        double b1_bst_pri = Double.valueOf(dts[6]);
        double s1_bst_pri = Double.valueOf(dts[7]);
        long dl_stk= Integer.valueOf(dts[8]);
        double dl_mny = Double.valueOf(dts[9]);
        long b1_num = Integer.valueOf(dts[10]);
        double b1_pri = Double.valueOf(dts[11]);
        long b2_num = Integer.valueOf(dts[12]);
        double b2_pri = Double.valueOf(dts[13]);
        long b3_num = Integer.valueOf(dts[14]);
        double b3_pri = Double.valueOf(dts[15]);
        long b4_num = Integer.valueOf(dts[16]);
        double b4_pri = Double.valueOf(dts[17]);
        long b5_num = Integer.valueOf(dts[18]);
        double b5_pri = Double.valueOf(dts[19]);
        long s1_num = Integer.valueOf(dts[20]);
        double s1_pri = Double.valueOf(dts[21]);
        long s2_num = Integer.valueOf(dts[22]);
        double s2_pri = Double.valueOf(dts[23]);
        long s3_num = Integer.valueOf(dts[24]);
        double s3_pri = Double.valueOf(dts[25]);
        long s4_num = Integer.valueOf(dts[26]);
        double s4_pri = Double.valueOf(dts[27]);
        long s5_num = Integer.valueOf(dts[28]);
        double s5_pri = Double.valueOf(dts[29]);
        Date dl_dt = Date.valueOf(dts[30]); 
        String dl_tm = dts[31]; 
        String sql = "insert into stkDat (ft_id,"
                               + " id,"
                               + " td_opn_pri,"
                               + " yt_cls_pri,"
                               + " cur_pri,"
                               + " td_hst_pri,"
                               + " td_lst_pri,"
                               + " b1_bst_pri,"
                               + " s1_bst_pri,"
                               + " dl_stk_num,"
                               + " dl_mny_num,"
                               + " b1_num,"
                               + " b1_pri,"
                               + " b2_num,"
                               + " b2_pri,"
                               + " b3_num,"
                               + " b3_pri,"
                               + " b4_num,"
                               + " b4_pri,"
                               + " b5_num,"
                               + " b5_pri,"
                               + " s1_num,"
                               + " s1_pri,"
                               + " s2_num,"
                               + " s2_pri,"
                               + " s3_num,"
                               + " s3_pri,"
                               + " s4_num,"
                               + " s4_pri,"
                               + " s5_num,"
                               + " s5_pri,"
                               + " dl_dt,"
                               + " dl_tm,"
                               + " ft_dt)"
                               + " values (SEQ_STKDAT_PK.nextval," +
                               "'" + stkID + "',"
                                   + td_opn_pri + ","
                                   + yt_cls_pri + ","
                                   + cur_pri + ","
                                   + td_hst_pri + ","
                                   + td_lst_pri + ","
                                   + b1_bst_pri + ","
                                   + s1_bst_pri + ","
                                   + dl_stk + ","
                                   + dl_mny + ","
                                   + b1_num + ","
                                   + b1_pri + ","
                                   + b2_num + ","
                                   + b2_pri + ","
                                   + b3_num + ","
                                   + b3_pri + ","
                                   + b4_num + ","
                                   + b4_pri + ","
                                   + b5_num + ","
                                   + b5_pri + ","
                                   + s1_num + ","
                                   + s1_num + ","
                                   + s2_pri + ","
                                   + s2_pri + ","
                                   + s3_num + ","
                                   + s3_pri + ","
                                   + s4_num + ","
                                   + s4_pri + ","
                                   + s5_num + ", "
                                   + s5_pri + ","
                               + "to_date('" + dl_dt.toString() +" " + dl_tm +"', 'yyyy-mm-dd hh24:mi:ss')" + ", '"
                               + dl_tm + "'," +"sysdate)";
        System.out.println("sql:" + sql);

        Connection conn = null;// 表示数据库连接
        Statement stmt = null;// 表示数据库的更新
        ResultSet result = null;// 查询数据库
        try {
        Class.forName(drive);// 使用class类来加载程序
        conn = DriverManager.getConnection(url2, DBUSER, password); // 连接数据库
        // Statement接口要通过connection接口来进行实例化操作
        stmt = conn.createStatement();
        stmt.execute(sql);
        stmt.close();
        conn.close();// 关闭数据库
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
