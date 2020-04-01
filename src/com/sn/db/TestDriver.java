package com.sn.db;

import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.sn.task.WorkManager;

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

    //public static final String drive = "oracle.jdbc.driver.OracleDriver";
    public static final String drive = "org.gjt.mm.mysql.Driver";
    /**
     * ���ӵ�ַ�����������ṩ������ס jdbc:oracle:thin:@localhost:1521:ORCL localhost ��ip��ַ��
     */
    private static final String url = "jdbc:mysql://111.229.27.150/stockApp?autoReconnect=true&failOverReadOnly=false";
    public static final String url2 = "jdbc:oracle:thin:@localhost:1521:ORCL";
    /**
     * �û� ����
     */
    public static final String DBUSER = "root";
    public static final String password = "mysql,16";

    static Logger log = Logger.getLogger(TestDriver.class);
    public static void testDB() throws Exception {
        // TODO Auto-generated method stub
        Connection conn = null;// ��ʾ��ݿ�����
        Statement stmt = null;// ��ʾ��ݿ�ĸ���
        ResultSet result = null;// ��ѯ��ݿ�
        Class.forName(drive);// ʹ��class�������س���
        conn = DriverManager.getConnection(url, DBUSER, password); // ������ݿ�
        // Statement�ӿ�Ҫͨ��connection�ӿ�������ʵ�����
        stmt = conn.createStatement();
        // ִ��SQL�������ѯ��ݿ�
        result = stmt.executeQuery("SELECT 'abc' name FROM dual");
        while (result.next()) {// �ж���û����һ��
            String name = result.getString(1);
            log.info("name=" + name + ";");
        }

        // log.info(conn);
        result.close();// ��ݿ��ȿ����
        stmt.close();
        conn.close();// �ر���ݿ�

    }

    static void testReadingURL() {
        String str;
        try {
            URL url = new URL("http://hq.sinajs.cn/list=sz000031");
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            while ((str = br.readLine()) != null) {
                log.info(str);
                createStockData(str);
            }
            br.close();
        } catch (IOException e) {
            log.info(e);
        }
    }

    /*
     * var hq_str_sh601318=
     * "�й�ƽ��,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
     * ;
     */
    static void createStockData(String stkDat) {
        String dts[] = stkDat.split(",");
        for (int i = 0; i < dts.length; i++) {
            log.info(i + ":" + dts[i]);
        }
        String area = dts[0].substring(11, 13);
        String stkID = dts[0].substring(13, 19);
        String stkName = dts[0].substring(21, 25);
        log.info("area:" + area + " stkID:" + stkID + " stkName:" + stkName);
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
        String sql = "insert into stkDat2 (ft_id,"
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
                               + " select case when max(ft_id) is null then 0 else max(ft_id) end + 1," +
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
                               + "str_to_date('" + dl_dt.toString() +" " + dl_tm +"', '%Y-%m-%d %T')" + ", '"
                               + dl_tm + "'," +"sysdate() from stkDat2";
        log.info("sql:" + sql);

        Connection conn = null;// ��ʾ��ݿ�����
        Statement stmt = null;// ��ʾ��ݿ�ĸ���
        ResultSet result = null;// ��ѯ��ݿ�
        try {
        Class.forName(drive);// ʹ��class�������س���
        conn = DriverManager.getConnection(url, DBUSER, password); // ������ݿ�
        // Statement�ӿ�Ҫͨ��connection�ӿ�������ʵ�����
        stmt = conn.createStatement();
        stmt.execute(sql);
        stmt.close();
        conn.close();// �ر���ݿ�
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
