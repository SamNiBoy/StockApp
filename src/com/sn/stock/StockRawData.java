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

public class StockRawData{

    static Logger log = Logger.getLogger(StockRawData.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        
    }
   
    public String area = null;
    public String id = null;
    public String name = null;
    public int  ft_id_lst = 0;
    public double cur_pri= 0;
    public double b1_bst_pri_= 0;
    public double s1_bst_pri_= 0;
    public int dl_stk_num= 0;
    public double dl_mny_num= 0;
    public int b1_num = 0;
    public double b1_pri = 0;
    public int  b2_num = 0;
    public double b2_pri = 0;
    public int  b3_num = 0;
    public double b3_pri = 0;
    public int  b4_num = 0;
    public double b4_pri = 0;
    public int  b5_num = 0;
    public double b5_pri = 0;
    public int  s1_num = 0;
    public double s1_pri = 0;
    public int  s2_num = 0;
    public double s2_pri = 0;
    public int  s3_num = 0;
    public double s3_pri = 0;
    public int  s4_num = 0;
    public double s4_pri = 0;
    public int  s5_num = 0;
    public double s5_pri = 0;
    public Date dl_dt = null;
    public double td_opn_pri = 0;
    public double yt_cls_pri = 0;
    public double td_hst_pri = 0;
    public double td_lst_pri = 0;
    public double b1_bst_pri = 0;
    public double s1_bst_pri = 0;
    public String dl_tm = null;

    static public StockRawData createStockData(String stkDat) {
        String dts[] = stkDat.split(",");

        if (dts.length < 32)
        {
            log.info("Exception stkDat(Less than 32 columns):" + stkDat);
            return null;
        }
        log.info("Parse row data for:" + stkDat);
        StockRawData srd = new StockRawData();
        srd.id = dts[0].substring(13, 19);;
        srd.area = dts[0].substring(11, 13);
        srd.name = dts[0].substring(21);
        srd.td_opn_pri = Double.valueOf(dts[1]);
        srd.yt_cls_pri = Double.valueOf(dts[2]);
        srd.cur_pri = Double.valueOf(dts[3]);
        srd.td_hst_pri = Double.valueOf(dts[4]);
        srd.td_lst_pri = Double.valueOf(dts[5]);
        srd.b1_bst_pri = Double.valueOf(dts[6]);
        srd.s1_bst_pri = Double.valueOf(dts[7]);
        srd.dl_stk_num= Integer.valueOf(dts[8]);
        srd.dl_mny_num = Double.valueOf(dts[9]);
        srd.b1_num = Integer.valueOf(dts[10]);
        srd.b1_pri = Double.valueOf(dts[11]);
        srd.b2_num = Integer.valueOf(dts[12]);
        srd.b2_pri = Double.valueOf(dts[13]);
        srd.b3_num = Integer.valueOf(dts[14]);
        srd.b3_pri = Double.valueOf(dts[15]);
        srd.b4_num = Integer.valueOf(dts[16]);
        srd.b4_pri = Double.valueOf(dts[17]);
        srd.b5_num = Integer.valueOf(dts[18]);
        srd.b5_pri = Double.valueOf(dts[19]);
        srd.s1_num = Integer.valueOf(dts[20]);
        srd.s1_pri = Double.valueOf(dts[21]);
        srd.s2_num = Integer.valueOf(dts[22]);
        srd.s2_pri = Double.valueOf(dts[23]);
        srd.s3_num = Integer.valueOf(dts[24]);
        srd.s3_pri = Double.valueOf(dts[25]);
        srd.s4_num = Integer.valueOf(dts[26]);
        srd.s4_pri = Double.valueOf(dts[27]);
        srd.s5_num = Integer.valueOf(dts[28]);
        srd.s5_pri = Double.valueOf(dts[29]);
        srd.dl_dt = Date.valueOf(dts[30]);
        srd.dl_tm = dts[31];
        
        return srd;
    }
}
