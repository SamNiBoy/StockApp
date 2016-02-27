package com.sn.stock;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.work.fetcher.FetchStockData;

public class Stock2 implements Comparable<Stock2>{

    private class StockData{
        //Save all history data
        String stkid;
        List<Double> td_opn_pri_lst = new ArrayList<Double>();
        List<Double> yt_cls_pri_lst = new ArrayList<Double>();
        List<Double> td_hst_pri_lst = new ArrayList<Double>();
        List<Double> td_lst_pri_lst = new ArrayList<Double>();
        List<String> dt_lst = new ArrayList<String>();
        
        //Save today's data
        List<Integer> ft_id_lst = new ArrayList<Integer>();
        List<Double> cur_pri_lst = new ArrayList<Double>();
        List<Double> b1_bst_pri_lst = new ArrayList<Double>();
        List<Double> s1_bst_pri_lst = new ArrayList<Double>();
        List<Integer> dl_stk_num_lst = new ArrayList<Integer>();
        List<Double> dl_mny_num_lst = new ArrayList<Double>();
        List<Integer> b1_num_lst = new ArrayList<Integer>();
        List<Double> b1_pri_lst = new ArrayList<Double>();
        List<Integer> b2_num_lst = new ArrayList<Integer>();
        List<Double> b2_pri_lst = new ArrayList<Double>();
        List<Integer> b3_num_lst = new ArrayList<Integer>();
        List<Double> b3_pri_lst = new ArrayList<Double>();
        List<Integer> b4_num_lst = new ArrayList<Integer>();
        List<Double> b4_pri_lst = new ArrayList<Double>();
        List<Integer> b5_num_lst = new ArrayList<Integer>();
        List<Double> b5_pri_lst = new ArrayList<Double>();
        List<Integer> s1_num_lst = new ArrayList<Integer>();
        List<Double> s1_pri_lst = new ArrayList<Double>();
        List<Integer> s2_num_lst = new ArrayList<Integer>();
        List<Double> s2_pri_lst = new ArrayList<Double>();
        List<Integer> s3_num_lst = new ArrayList<Integer>();
        List<Double> s3_pri_lst = new ArrayList<Double>();
        List<Integer> s4_num_lst = new ArrayList<Integer>();
        List<Double> s4_pri_lst = new ArrayList<Double>();
        List<Integer> s5_num_lst = new ArrayList<Integer>();
        List<Double> s5_pri_lst = new ArrayList<Double>();
        List<Timestamp> dl_dt_lst = new ArrayList<Timestamp>();
        
        StockData(String stkId) {
            stkid = stkId;
            Connection con = DBManager.getConnection();
            try {
                Statement stm = con.createStatement();
                String sql = "select * from stkDayPri where id ='" + stkId + "' order by dt";
                
                log.info(sql);
                ResultSet rs = stm.executeQuery(sql);
                while(rs.next()) {
                    td_opn_pri_lst.add(rs.getDouble("td_opn_pri"));
                    yt_cls_pri_lst.add(rs.getDouble("yt_cls_pri"));
                    td_hst_pri_lst.add(rs.getDouble("td_hst_pri"));
                    td_lst_pri_lst.add(rs.getDouble("td_lst_pri"));
                    dt_lst.add(rs.getString("dt"));
                }
                rs.close();
                stm.close();
                con.close();
                LoadData();
            }
            catch(SQLException e) {
                e.printStackTrace();
            }
        }
        
        boolean LoadData() {
            int lst_ft_id = 0;
            if (!ft_id_lst.isEmpty()) {
                lst_ft_id = ft_id_lst.get(ft_id_lst.size() - 1);
            }
            Connection con = DBManager.getConnection();
            try {
                Statement stm = con.createStatement();
                String sql = "select * from stkDat2 where ft_id > " + lst_ft_id +
                " and id = '" + stkid +
                "' and to_char(dl_dt,'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') and rownum < 5 order by dl_dt";
                
                log.info(sql);
                ResultSet rs = stm.executeQuery(sql);
                while(rs.next()) {
                    ft_id_lst.add(rs.getInt("ft_id"));
                    cur_pri_lst.add(rs.getDouble("cur_pri"));
                    b1_bst_pri_lst.add(rs.getDouble("b1_bst_pri"));
                    s1_bst_pri_lst.add(rs.getDouble("s1_bst_pri"));
                    dl_stk_num_lst.add(rs.getInt("dl_stk_num"));
                    dl_mny_num_lst.add(rs.getDouble("dl_mny_num"));
                    b1_num_lst.add(rs.getInt("b1_num"));
                    b1_pri_lst.add(rs.getDouble("b1_pri"));
                    b2_num_lst.add(rs.getInt("b2_num"));
                    b2_pri_lst.add(rs.getDouble("b2_pri"));
                    b3_num_lst.add(rs.getInt("b3_num"));
                    b3_pri_lst.add(rs.getDouble("b3_pri"));
                    b4_num_lst.add(rs.getInt("b4_num"));
                    b4_pri_lst.add(rs.getDouble("b4_pri"));
                    b5_num_lst.add(rs.getInt("b5_num"));
                    b5_pri_lst.add(rs.getDouble("b5_pri"));
                    s1_num_lst.add(rs.getInt("s1_num"));
                    s1_pri_lst.add(rs.getDouble("s1_pri"));
                    s2_num_lst.add(rs.getInt("s2_num"));
                    s2_pri_lst.add(rs.getDouble("s2_pri"));
                    s3_num_lst.add(rs.getInt("s3_num"));
                    s3_pri_lst.add(rs.getDouble("s3_pri"));
                    s4_num_lst.add(rs.getInt("s4_num"));
                    s4_pri_lst.add(rs.getDouble("s4_pri"));
                    s5_num_lst.add(rs.getInt("s5_num"));
                    s5_pri_lst.add(rs.getDouble("s5_pri"));
                    dl_dt_lst.add(rs.getTimestamp("dl_dt"));
                }
                rs.close();
                stm.close();
                con.close();
                //PrintStockData();
            }
            catch(SQLException e) {
                e.printStackTrace();
            }
            return true;
        }
        
        
        public boolean injectRawData(StockRawData rsd) {
            if(rsd != null) {
                //ft_id_lst.add(rs.getInt("ft_id"));
                cur_pri_lst.add(rsd.cur_pri);
                b1_bst_pri_lst.add(rsd.b1_bst_pri);
                s1_bst_pri_lst.add(rsd.s1_bst_pri);
                dl_stk_num_lst.add(rsd.dl_stk_num);
                dl_mny_num_lst.add(rsd.dl_mny_num);
                b1_num_lst.add(rsd.b1_num);
                b1_pri_lst.add(rsd.b1_pri);
                b2_num_lst.add(rsd.b2_num);
                b2_pri_lst.add(rsd.b2_pri);
                b3_num_lst.add(rsd.b3_num);
                b3_pri_lst.add(rsd.b3_pri);
                b4_num_lst.add(rsd.b4_num);
                b4_pri_lst.add(rsd.b4_pri);
                b5_num_lst.add(rsd.b5_num);
                b5_pri_lst.add(rsd.b5_pri);
                s1_num_lst.add(rsd.s1_num);
                s1_pri_lst.add(rsd.s1_pri);
                s2_num_lst.add(rsd.s2_num);
                s2_pri_lst.add(rsd.s2_pri);
                s3_num_lst.add(rsd.s3_num);
                s3_pri_lst.add(rsd.s3_pri);
                s4_num_lst.add(rsd.s4_num);
                s4_pri_lst.add(rsd.s4_pri);
                s5_num_lst.add(rsd.s5_num);
                s5_pri_lst.add(rsd.s5_pri);
                dl_dt_lst.add(Timestamp.valueOf(rsd.dl_dt + " " + rsd.dl_tm));
            }
            else {
                return false;
            }
            return true;
        }
        
        void PrintStockData() {
            log.info("Total get " + dt_lst.size() + " days data.");
            for (int i = 0; i < dt_lst.size(); i++) {
                log.info("Date:" + dt_lst.get(i) +
                         " top:" + td_opn_pri_lst.get(i) +
                         " ycp:" + yt_cls_pri_lst.get(i) +
                         " thp:" + td_hst_pri_lst.get(i) +
                         " tlp:" + td_lst_pri_lst.get(i) + "\n");
            }
            log.info("Total get " + dl_dt_lst.size() + " records for today.");
            for (int i = 0; i < dl_dt_lst.size(); i++) {
                log.info(" ft_id: " + ft_id_lst.get(i) +
                         " cur_pri: " + cur_pri_lst.get(i) +
                         " b1_bst: " + b1_bst_pri_lst.get(i) +
                         " s1_bst: " + s1_bst_pri_lst.get(i) +
                         " dl_stk_num: " + dl_stk_num_lst.get(i) +
                         " dl_mny_num: " + dl_mny_num_lst.get(i) +
                         " b1_num: " + b1_num_lst.get(i) +
                         " b1_pri: " + b1_pri_lst.get(i) +
                         " b2_num: " + b2_num_lst.get(i) +
                         " b2_pri: " + b2_pri_lst.get(i) +
                         " b3_num: " + b3_num_lst.get(i) +
                         " b3_pri: " + b3_pri_lst.get(i) +
                         " b4_num: " + b4_num_lst.get(i) +
                         " b4_pri: " + b4_pri_lst.get(i) +
                         " b5_num: " + b5_num_lst.get(i) +
                         " b5_pri: " + b5_pri_lst.get(i) +
                         " s1_num: " + s1_num_lst.get(i) +
                         " s1_pri: " + s1_pri_lst.get(i) +
                         " s2_num: " + s2_num_lst.get(i) +
                         " s2_pri: " + s2_pri_lst.get(i) +
                         " s3_num: " + s3_num_lst.get(i) +
                         " s3_pri: " + s3_pri_lst.get(i) +
                         " s4_num: " + s4_num_lst.get(i) +
                         " s4_pri: " + s4_pri_lst.get(i) +
                         " s5_num: " + s5_num_lst.get(i) +
                         " s5_pri: " + s5_pri_lst.get(i) +
                         " dl_dt: " + dl_dt_lst.get(i) + "\n");
            }
        }
    }
    
    static Logger log = Logger.getLogger(Stock.class);
    /**
     * @param args
     */
    String id;
    String name;
    boolean gz_flg;
    StockData sd;
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Connection con = DBManager.getConnection();
        try {
            Statement stm = con.createStatement();
            String sql = "select id, name, gz_flg from stk where gz_flg = 1";
            
            ResultSet rs = stm.executeQuery(sql);
            List<Stock2> sl = new LinkedList<Stock2>();
            while(rs.next()) {
                Stock2 s = new Stock2(rs.getString("id"), rs.getString("name"), 1);
                sl.add(s);
            }
            for (int i = 0; i < sl.size(); i++) {
                Stock2 s = sl.get(i);
                s.printStockInfo();
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Stock2(String ids, String nm, long gzflg)
    {
        id = ids;
        name = nm;
        gz_flg = gzflg > 0;
        sd = new StockData(id);
    }

    @Override
    public int compareTo(Stock2 arg0) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public boolean injectData(StockRawData rsd) {
        return sd.injectRawData(rsd);
    }
    
    public boolean saveData(StockRawData rsd, Connection con) {
        
        if (rsd == null || con == null) {
            return false;
        }
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
            "'" + rsd.id + "',"
                + rsd.td_opn_pri + ","
                + rsd.yt_cls_pri + ","
                + rsd.cur_pri + ","
                + rsd.td_hst_pri + ","
                + rsd.td_lst_pri + ","
                + rsd.b1_bst_pri + ","
                + rsd.s1_bst_pri + ","
                + rsd.dl_stk_num + ","
                + rsd.dl_mny_num + ","
                + rsd.b1_num + ","
                + rsd.b1_pri + ","
                + rsd.b2_num + ","
                + rsd.b2_pri + ","
                + rsd.b3_num + ","
                + rsd.b3_pri + ","
                + rsd.b4_num + ","
                + rsd.b4_pri + ","
                + rsd.b5_num + ","
                + rsd.b5_pri + ","
                + rsd.s1_num + ","
                + rsd.s1_pri + ","
                + rsd.s2_num + ","
                + rsd.s2_pri + ","
                + rsd.s3_num + ","
                + rsd.s3_pri + ","
                + rsd.s4_num + ","
                + rsd.s4_pri + ","
                + rsd.s5_num + ", "
                + rsd.s5_pri + ","
            + "to_date('" + rsd.dl_dt.toString() +" " + rsd.dl_tm +"', 'yyyy-mm-dd hh24:mi:ss')" + ", '"
            + rsd.dl_tm + "'," +"sysdate)";
        
        log.info(sql);
        try {
            Statement stm = con.createStatement();
            stm.execute(sql);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    public void printStockInfo() {
        log.info("========================================\n");
        log.info("Stock " + id + " data information:\n");
        log.info("========================================\n");
        log.info("ID\t|Name\t|GZ_FLG\t|");
        log.info(id + "\t|" + name + "\t|" + ((gz_flg)? "TRUE" : "FALSE") + "\t|\n");
        sd.PrintStockData();
    }
}
