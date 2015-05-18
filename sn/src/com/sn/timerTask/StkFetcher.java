package com.sn.timerTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Timer;
import java.util.TimerTask;

import com.sn.db.DBManager;
import com.sn.reporter.WeChatReporter;

public class StkFetcher extends TimerTask {

    private String lstStkDat[] = new String[100];
    @Override
    public void run() {
        // TODO Auto-generated method stub
        String str;
        Connection con = DBManager.getConnection();
        int cnt = 0;
        boolean hasDiff = false;
        
        try {
            con.setAutoCommit(false);
            Statement stm = con.createStatement();
            String fs = getFetchSql(), cs;
            URL url = new URL(fs);
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            while ((str = br.readLine()) != null) {
                if (cnt == 100 && !hasDiff)
                {
                    System.out.println("Stock data is 100 times same, skip fetching...");
                    break;
                }
                if (!hasDiff && str.equals(lstStkDat[cnt]))
                {
                    cnt++;
                    System.out.println(cnt + " Stock data is same, skip fetching...");
                    continue;
                }
                else
                {
                    hasDiff = true;
                    if (cnt < 100)
                    {
                        lstStkDat[cnt++] = str;                        
                    }
                }
                System.out.println(str);
                cs = createStockData(str);
                
                if (cs != null)
                {
                    stm.execute(cs);
                }
            }
            br.close();
            stm.close();
            con.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                con.rollback();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }
    
    static String getFetchSql()
    {
        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        String sql = "select area, id from stk";
        
        String stkSql = "http://hq.sinajs.cn/list=";
        String stkLst = "";
        
        try{
            stm = con.createStatement();
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                stkLst += stkLst.length() > 0 ? "," : "";
                stkLst += rs.getString("area") + rs.getString("id");
            }
        }
        catch(Exception e)
        {
            try {
                stm.close();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        System.out.println(stkSql + stkLst);
        
        return stkSql + stkLst;
    }
    /*
     * var hq_str_sh601318=
     * "中国平安,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
     * ;
     */
    static String createStockData(String stkDat) {
        String dts[] = stkDat.split(",");
        
        if (dts.length < 32)
        {
            System.out.println("Exception stkDat(Less than 32 columns):" + stkDat);
            return null;
        }
        for (int i = 0; i < dts.length; i++) {
            System.out.println(i + ":" + dts[i]);
        }
        
        String area = dts[0].substring(11, 13);
        String stkID = dts[0].substring(13, 19);
        String stkName = dts[0].substring(21);
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
        
        return sql;

    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        Timer timer = new Timer();  
        timer.schedule(new StkFetcher(), 5000, 1000 * 60); 
    }

}
