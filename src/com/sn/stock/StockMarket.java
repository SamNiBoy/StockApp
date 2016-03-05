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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.work.fetcher.FetchStockData;

public class StockMarket{

    static Logger log = Logger.getLogger(StockMarket.class);
    
    static private ConcurrentHashMap<String, Stock2> stocks = null;
    static private ConcurrentHashMap<String, Stock2> gzstocks = null;
    static private ConcurrentHashMap<String, Stock2> recomstocks = null;
    
    private static int StkNum = 0;
    private static int TotInc = 0;
    private static int TotDec = 0;
    private static int TotEql = 0;
    private static double AvgIncPct = 0.0;
    private static double AvgDecPct = 0.0;
    private static double totIncDlMny = 0.0;
    private static double totDecDlMny = 0.0;
    private static double totEqlDlMny = 0.0;
    private static double Degree = 0.0;
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
            for (String s : stocks.keySet()) {
                Stock2 stk = stocks.get(s);
                stk.printStockInfo();
            }
    }
    
    static public boolean loadStocks() {

        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        
        stocks = new ConcurrentHashMap<String, Stock2>();
        Stock2 s = null;
        int Total = 0, cnt = 0;
        try {
            stm = con.createStatement();
            String sql = "select id, name, gz_flg from stk order by id";
            rs = stm.executeQuery(sql);
            
            String id, name;
            
            while (rs.next()) {
                id = rs.getString("id");
                name = rs.getString("name");
                s = new Stock2(id, name, rs.getLong("gz_flg"));
                stocks.put(id, s);
                cnt++;
                log.info("LoadStocks completed:" + cnt * 1.0 / 2811);
            }
            rs.close();
            stm.close();
            con.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        log.info("StockMarket loadStock successed!");
        return true;
    }
    
    static public boolean loadGzStocks() {

        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        
        gzstocks = new ConcurrentHashMap<String, Stock2>();
        Stock2 s = null;
        int Total = 0, cnt = 0;
        try {
            stm = con.createStatement();
            String sql = "select id, name, gz_flg from stk where gz_flg = 1 order by id";
            rs = stm.executeQuery(sql);
            
            String id, name;
            
            while (rs.next()) {
                id = rs.getString("id");
                name = rs.getString("name");
                s = new Stock2(id, name, rs.getLong("gz_flg"));
                gzstocks.put(id, s);
                cnt++;
                log.info("LoadStocks completed:" + cnt * 1.0 / 2811);
            }
            rs.close();
            stm.close();
            con.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        log.info("StockMarket loadStock successed!");
        return true;
    }
    
    
    public static ConcurrentHashMap<String, Stock2> getStocks() {
        synchronized (StockMarket.class) {
            if (stocks == null) {
                loadStocks();
            }
            return stocks;
        }
    }

    public static void setStocks(ConcurrentHashMap<String, Stock2> stocks) {
        StockMarket.stocks = stocks;
    }

    public static ConcurrentHashMap<String, Stock2> getGzstocks() {
        if (gzstocks == null) {
            loadGzStocks();
        }
        return gzstocks;
    }

    public static void setGzstocks(ConcurrentHashMap<String, Stock2> gzstocks) {
        StockMarket.gzstocks = gzstocks;
    }

    public static ConcurrentHashMap<String, Stock2> getRecomstocks() {
        return recomstocks;
    }

    public static void setRecomstocks(ConcurrentHashMap<String, Stock2> recomstocks) {
        StockMarket.recomstocks = recomstocks;
    }

    public static int getStkNum() {
        return StkNum;
    }

    public static void setStkNum(int stkNum) {
        StkNum = stkNum;
    }

    public static int getTotInc() {
        return TotInc;
    }

    public static void setTotInc(int totInc) {
        TotInc = totInc;
    }

    public static int getTotDec() {
        return TotDec;
    }

    public static void setTotDec(int totDec) {
        TotDec = totDec;
    }

    public static int getTotEql() {
        return TotEql;
    }

    public static void setTotEql(int totEql) {
        TotEql = totEql;
    }

    public static double getAvgIncPct() {
        return AvgIncPct;
    }

    public static void setAvgIncPct(double avgIncPct) {
        AvgIncPct = avgIncPct;
    }

    public static double getAvgDecPct() {
        return AvgDecPct;
    }

    public static void setAvgDecPct(double avgDecPct) {
        AvgDecPct = avgDecPct;
    }

    public static double getTotIncDlMny() {
        return totIncDlMny;
    }

    public static void setTotIncDlMny(double totIncDlMny) {
        StockMarket.totIncDlMny = totIncDlMny;
    }

    public static double getTotDecDlMny() {
        return totDecDlMny;
    }

    public static void setTotDecDlMny(double totDecDlMny) {
        StockMarket.totDecDlMny = totDecDlMny;
    }

    public static double getTotEqlDlMny() {
        return totEqlDlMny;
    }

    public static void setTotEqlDlMny(double totEqlDlMny) {
        StockMarket.totEqlDlMny = totEqlDlMny;
    }

    public static double getDegree() {
        return Degree;
    }

    public static void setDegree(double degree) {
        Degree = degree;
    }

    static public boolean calIndex(Timestamp tm) {

        Connection con = DBManager.getConnection();
        Statement stm = null;

        int catagory = -2;
        try {
            stm = con.createStatement();
            String sql = "select count(case when cur_pri > td_opn_pri then 1 else 0 end) IncNum, " +
                         "       count(case when cur_pri < td_opn_pri then 1 else 0 end) DecNum, " +
                         "       count(case when cur_pri = td_opn_pri then 1 else 0 end) EqlNum, " +
                         " avg((cur_pri - td_opn_pri)/td_opn_pri) avgPct," +
                         " sum(dl_mny_num) totDlMny," +
                         " case when cur_pri > td_opn_pri then 1 " +
                         "               when cur_pri < td_opn_pri then -1 " +
                         "               when cur_pri = td_opn_pri then 0 end catagory " +
                         " from stkdat2 " +
                         " where td_opn_pri > 0 " +
                         "   and dl_dt <= to_date('" + tm.toLocaleString() + "', 'yyyy-mm-dd HH24:MI:SS')" +
                         "   and not exists (select 'x' from stkdat2 skd where skd.id = stkdat2.id and ft_id > stkdat2.ft_id)" +
                         " group by case when cur_pri > td_opn_pri then 1 " +
                         "               when cur_pri < td_opn_pri then -1 " +
                         "               when cur_pri = td_opn_pri then 0 end" +
                         " order by catagory asc";
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);

            while (rs.next()) {
                catagory = rs.getInt("catagory");
                if (catagory == -1)
                {
                    TotDec = rs.getInt("DecNum");
                    AvgDecPct = rs.getDouble("avgPct");
                    totDecDlMny = rs.getDouble("totDlMny");
                }
                else if (catagory == 0)
                {
                    TotEql = rs.getInt("EqlNum");
                    totEqlDlMny = rs.getDouble("totDlMny");
                }
                else if (catagory == 1)
                {
                    TotInc = rs.getInt("IncNum");
                    AvgIncPct = rs.getDouble("avgPct");
                    totIncDlMny = rs.getDouble("totDlMny");
                }
            }
            rs.close();
            stm.close();
            con.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        StkNum = TotDec + TotInc + TotEql;
        Degree = (TotInc * AvgIncPct + TotDec * AvgDecPct) * 100.0 / (TotInc * 0.1 + TotDec * 0.1);
        
        return true;
    }
    
    static public String getShortDesc() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        return "Deg:" + nf.format(Degree) + " StkNum/Inc/Dec/Eql:(" + StkNum + "/" + TotInc + "/" + TotDec + "/" + TotEql + ")";
    }
    
    static public String getLongDsc() {
        
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        String index = "<table border = 1>" +
        "<tr>" +
        "<th> Stock Count</th> " +
        "<th> Total+ </th> " +
        "<th> AvgPct+ </th> " +
        "<th> TotDlMny+ </th> " +
        "<th> Total-</th> " +
        "<th> AvgPct-</th> " + 
        "<th> TotDlMny- </th> " +
        "<th> Total= </th> " +
        "<th> TotDlMny= </th> " +
        "<th> Degree </th> </tr> ";
        index += "<tr> <td>" + StkNum + "</td>" +
        "<td> " + TotInc + "</td>" +
         "<td> " + nf.format(AvgIncPct*100) + "%</td>" +
         "<td> " + nf.format(totIncDlMny/100000000) + "亿</td>" +
         "<td> " + TotDec + "</td>" +
         "<td> " + nf.format(AvgDecPct*100) + "%</td>" +
         "<td> " + nf.format(totDecDlMny/100000000) + "亿</td>" +
         "<td> " + TotEql + "</td>" +
         "<td> " + nf.format(totEqlDlMny/100000000) + "亿</td>" +
         "<td> " + nf.format((TotInc * AvgIncPct + TotDec * AvgDecPct) * 100.0 / (TotInc * 0.1 + TotDec * 0.1)) + " C</tr></table>";
        return index;
    }
    static public boolean isMarketTooCold(Timestamp tm) {
        if (Degree == 0.0) {
            calIndex(tm);
        }
        return Degree < -20;
    }
    
    static public boolean hasMoreIncStock() {
        return TotInc > TotDec;
    }
    
    static double getMnyRatioIncDec() {
        if (totDecDlMny == 0) {
            //not possible ratio.
            return 10000;
        }
        return totIncDlMny / totDecDlMny;
    }
}
