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

    public class StockData{
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
        
        
        public String getStkid() {
            return stkid;
        }

        public void setStkid(String stkid) {
            this.stkid = stkid;
        }

        public List<Double> getTd_opn_pri_lst() {
            return td_opn_pri_lst;
        }

        public void setTd_opn_pri_lst(List<Double> tdOpnPriLst) {
            td_opn_pri_lst = tdOpnPriLst;
        }

        public List<Double> getYt_cls_pri_lst() {
            return yt_cls_pri_lst;
        }

        public void setYt_cls_pri_lst(List<Double> ytClsPriLst) {
            yt_cls_pri_lst = ytClsPriLst;
        }

        public List<Double> getTd_hst_pri_lst() {
            return td_hst_pri_lst;
        }

        public void setTd_hst_pri_lst(List<Double> tdHstPriLst) {
            td_hst_pri_lst = tdHstPriLst;
        }

        public List<Double> getTd_lst_pri_lst() {
            return td_lst_pri_lst;
        }

        public void setTd_lst_pri_lst(List<Double> tdLstPriLst) {
            td_lst_pri_lst = tdLstPriLst;
        }

        public List<String> getDt_lst() {
            return dt_lst;
        }

        public void setDt_lst(List<String> dtLst) {
            dt_lst = dtLst;
        }

        public List<Integer> getFt_id_lst() {
            return ft_id_lst;
        }

        public void setFt_id_lst(List<Integer> ftIdLst) {
            ft_id_lst = ftIdLst;
        }

        public List<Double> getCur_pri_lst() {
            return cur_pri_lst;
        }

        public void setCur_pri_lst(List<Double> curPriLst) {
            cur_pri_lst = curPriLst;
        }

        public List<Double> getB1_bst_pri_lst() {
            return b1_bst_pri_lst;
        }

        public void setB1_bst_pri_lst(List<Double> b1BstPriLst) {
            b1_bst_pri_lst = b1BstPriLst;
        }

        public List<Double> getS1_bst_pri_lst() {
            return s1_bst_pri_lst;
        }

        public void setS1_bst_pri_lst(List<Double> s1BstPriLst) {
            s1_bst_pri_lst = s1BstPriLst;
        }

        public List<Integer> getDl_stk_num_lst() {
            return dl_stk_num_lst;
        }

        public void setDl_stk_num_lst(List<Integer> dlStkNumLst) {
            dl_stk_num_lst = dlStkNumLst;
        }

        public List<Double> getDl_mny_num_lst() {
            return dl_mny_num_lst;
        }

        public void setDl_mny_num_lst(List<Double> dlMnyNumLst) {
            dl_mny_num_lst = dlMnyNumLst;
        }

        public List<Integer> getB1_num_lst() {
            return b1_num_lst;
        }

        public void setB1_num_lst(List<Integer> b1NumLst) {
            b1_num_lst = b1NumLst;
        }

        public List<Double> getB1_pri_lst() {
            return b1_pri_lst;
        }

        public void setB1_pri_lst(List<Double> b1PriLst) {
            b1_pri_lst = b1PriLst;
        }

        public List<Integer> getB2_num_lst() {
            return b2_num_lst;
        }

        public void setB2_num_lst(List<Integer> b2NumLst) {
            b2_num_lst = b2NumLst;
        }

        public List<Double> getB2_pri_lst() {
            return b2_pri_lst;
        }

        public void setB2_pri_lst(List<Double> b2PriLst) {
            b2_pri_lst = b2PriLst;
        }

        public List<Integer> getB3_num_lst() {
            return b3_num_lst;
        }

        public void setB3_num_lst(List<Integer> b3NumLst) {
            b3_num_lst = b3NumLst;
        }

        public List<Double> getB3_pri_lst() {
            return b3_pri_lst;
        }

        public void setB3_pri_lst(List<Double> b3PriLst) {
            b3_pri_lst = b3PriLst;
        }

        public List<Integer> getB4_num_lst() {
            return b4_num_lst;
        }

        public void setB4_num_lst(List<Integer> b4NumLst) {
            b4_num_lst = b4NumLst;
        }

        public List<Double> getB4_pri_lst() {
            return b4_pri_lst;
        }

        public void setB4_pri_lst(List<Double> b4PriLst) {
            b4_pri_lst = b4PriLst;
        }

        public List<Integer> getB5_num_lst() {
            return b5_num_lst;
        }

        public void setB5_num_lst(List<Integer> b5NumLst) {
            b5_num_lst = b5NumLst;
        }

        public List<Double> getB5_pri_lst() {
            return b5_pri_lst;
        }

        public void setB5_pri_lst(List<Double> b5PriLst) {
            b5_pri_lst = b5PriLst;
        }

        public List<Integer> getS1_num_lst() {
            return s1_num_lst;
        }

        public void setS1_num_lst(List<Integer> s1NumLst) {
            s1_num_lst = s1NumLst;
        }

        public List<Double> getS1_pri_lst() {
            return s1_pri_lst;
        }

        public void setS1_pri_lst(List<Double> s1PriLst) {
            s1_pri_lst = s1PriLst;
        }

        public List<Integer> getS2_num_lst() {
            return s2_num_lst;
        }

        public void setS2_num_lst(List<Integer> s2NumLst) {
            s2_num_lst = s2NumLst;
        }

        public List<Double> getS2_pri_lst() {
            return s2_pri_lst;
        }

        public void setS2_pri_lst(List<Double> s2PriLst) {
            s2_pri_lst = s2PriLst;
        }

        public List<Integer> getS3_num_lst() {
            return s3_num_lst;
        }

        public void setS3_num_lst(List<Integer> s3NumLst) {
            s3_num_lst = s3NumLst;
        }

        public List<Double> getS3_pri_lst() {
            return s3_pri_lst;
        }

        public void setS3_pri_lst(List<Double> s3PriLst) {
            s3_pri_lst = s3PriLst;
        }

        public List<Integer> getS4_num_lst() {
            return s4_num_lst;
        }

        public void setS4_num_lst(List<Integer> s4NumLst) {
            s4_num_lst = s4NumLst;
        }

        public List<Double> getS4_pri_lst() {
            return s4_pri_lst;
        }

        public void setS4_pri_lst(List<Double> s4PriLst) {
            s4_pri_lst = s4PriLst;
        }

        public List<Integer> getS5_num_lst() {
            return s5_num_lst;
        }

        public void setS5_num_lst(List<Integer> s5NumLst) {
            s5_num_lst = s5NumLst;
        }

        public List<Double> getS5_pri_lst() {
            return s5_pri_lst;
        }

        public void setS5_pri_lst(List<Double> s5PriLst) {
            s5_pri_lst = s5PriLst;
        }

        public List<Timestamp> getDl_dt_lst() {
            return dl_dt_lst;
        }

        public void setDl_dt_lst(List<Timestamp> dlDtLst) {
            dl_dt_lst = dlDtLst;
        }

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
                "' and to_char(dl_dt,'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') order by dl_dt";
                
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
        
        
        public boolean injectRawData(RawStockData rsd) {
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
    
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isGz_flg() {
        return gz_flg;
    }

    public void setGz_flg(boolean gzFlg) {
        gz_flg = gzFlg;
    }

    public StockData getSd() {
        return sd;
    }

    public void setSd(StockData sd) {
        this.sd = sd;
    }

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
    
    public boolean injectData(RawStockData rsd) {
        return sd.injectRawData(rsd);
    }
    
    public boolean saveData(RawStockData rsd, Connection con) {
        
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
            stm.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        
        log.info("saveData ran success.");
        
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
