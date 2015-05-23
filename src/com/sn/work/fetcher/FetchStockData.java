package com.sn.work.fetcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;


import com.sn.db.DBManager;
import com.sn.work.itf.IWork;

public class FetchStockData implements IWork {

    static Connection con = DBManager.getConnection();
    /* Initial delay before executing work.
     */
    long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 5;
    
    TimeUnit tu = TimeUnit.MILLISECONDS;
    
    static Logger log = Logger.getLogger(FetchStockData.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        FetchStockData fsd = new FetchStockData(1, 3);
        fsd.run();
    }
    
    public FetchStockData(long id, long dbn)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;        
    }
    
    static String getFetchSql()
    {
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
            e.printStackTrace();
        }
        finally {
            try {
                rs.close();
                stm.close();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }            
        }
        log.info(stkSql + stkLst);
        
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
            log.info("Exception stkDat(Less than 32 columns):" + stkDat);
            return null;
        }
        
        String area = dts[0].substring(11, 13);
        String stkID = dts[0].substring(13, 19);
        String stkName = dts[0].substring(21);
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
                                   + s1_pri + ","
                                   + s2_num + ","
                                   + s2_pri + ","
                                   + s3_num + ","
                                   + s3_pri + ","
                                   + s4_num + ","
                                   + s4_pri + ","
                                   + s5_num + ", "
                                   + s5_pri + ","
                               + "to_date('" + dl_dt.toString() +" " + dl_tm +"', 'yyyy-mm-dd hh24:mi:ss')" + ", '"
                               + dl_tm + "'," +"sysdate)";
        log.info("sql:" + sql);
        
        return sql;

    }

    private String lstStkDat = "";
    
    public void run()
    {
        // TODO Auto-generated method stub
        String str;
        boolean cancel_work = false;
        
        try {
            con.setAutoCommit(false);
            Statement stm = con.createStatement();
            String fs = getFetchSql(), cs;
            URL url = new URL(fs);
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            while ((str = br.readLine()) != null) {
                if (str.equals(lstStkDat))
                {
                    cancel_work = true;
                    break;
                }
                
                if (lstStkDat.equals(""))
                {
                    lstStkDat = str;
                }
                log.info(str);
                cs = createStockData(str);
                
                if (cs != null)
                {
                    stm.executeUpdate(cs);
                }
            }
            br.close();
            stm.close();
            
            ExactDatForstkDat2();
            
            con.commit();
            
            if (cancel_work)
            {
                log.info("Stock data is same, cancel current work...");
                WorkManager.cancelWork(this.getWorkName());
            }
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
    
    static void ExactDatForstkDat2()
    {
        Statement stm = null;
        String sql = "insert into stkDat2 " +
                     "select  ft_id," +
                             "id," +
                             "td_opn_pri," +
                             "yt_cls_pri," +
                             "cur_pri," +
                             "td_hst_pri," +
                             "td_lst_pri," +
                             "b1_bst_pri," +
                             "s1_bst_pri," +
                             "dl_stk_num," +
                             "dl_mny_num," +
                             "b1_num," +
                             "b1_pri," +
                             "b2_num," +
                             "b2_pri," +
                             "b3_num," +
                             "b3_pri," +
                             "b4_num," +
                             "b4_pri," +
                             "b5_num," +
                             "b5_pri," +
                             "s1_num," +
                             "s1_pri," +
                             "s2_num," +
                             "s2_pri," +
                             "s3_num," +
                             "s3_pri," +
                             "s4_num," +
                             "s4_pri," +
                             "s5_num," +
                             "s5_pri," +
                             "dl_dt" +
                       " from stkdat s1 " +
                       "where not exists (select 'x' from stkDat2 s2 where s2.ft_id = s1.ft_id) " +
                       "  and not exists (select 'x' from stkDat s3 where s3.id = s1.id and s3.dl_dt = s1.dl_dt and s3.ft_id < s1.ft_id) " +
                       "  and s1.cur_pri > 0";
        log.info(sql);
        try{
            int cnt = 0;
            stm = con.createStatement();
            cnt = stm.executeUpdate(sql);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally {
            try {
                stm.close();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }            
        }
    }
    
    
    public String getWorkResult()
    {
        return "";
    }
    
    public String getWorkName()
    {
        return "FetchStockData";
    }
    
    public long getInitDelay()
    {
        return initDelay;
    }
    
    public long getDelayBeforeNxt()
    {
        return delayBeforNxtStart;
    }

    public TimeUnit getTimeUnit()
    {
        return tu;
    }
    
    public boolean isCycleWork()
    {
        return true;
    }

}
