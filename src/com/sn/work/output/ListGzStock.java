package com.sn.work.output;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.sim.strategy.imp.STConstants;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class ListGzStock implements IWork {

    Logger log = Logger.getLogger(ListGzStock.class);

    long initDelay = 0;
    long delayBeforNxtStart = 5;
    static String res = "开始收集关注股票信息...";
    String frmUsr;

    TimeUnit tu = TimeUnit.MILLISECONDS;

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ListGzStock lgs = new ListGzStock(0, 0, "osCWfs-ZVQZfrjRK0ml-eEpzeop0");
        lgs.run();
    }

    public ListGzStock(long id, long dbn, String usr) {
        initDelay = id;
        delayBeforNxtStart = dbn;
        frmUsr = usr;
    }

    public void run() {
        log.info("This is from logger4j infor msg...");
        String msg = "";
        try {
            msg = getGzStockInfo();
            if (msg.length() <= 0) {
                msg = "目前没有关注股票，请发送股票代码进行关注.";
            }
            log.info("list gzed stocks:" + msg);

        } catch (Exception e) {
            e.printStackTrace();
            msg = "Exception happend when list gzStock!";
        }
        res = msg;
    }

    private String getGzStockInfo()
    {
        Statement stm = null;
        String sql = "select s.id, s.name, u.sell_mode_flg "
        		+ "     from stk s, usrStk u "
        		+ "    where s.id = u.id "
        		+ "      and u.gz_flg = 1 "
        		+ "      and u.openID ='" + frmUsr + "' "
        	    + "      and u.suggested_by in ('" + frmUsr + "','" + STConstants.SUGGESTED_BY_FOR_SYSTEMGRANTED + "')";
        String content = "";
        Map<String, String> Stocks = new HashMap<String, String> ();
        Map<String, Integer> sellmodes = new HashMap<String, Integer> ();
        DecimalFormat df2 = new DecimalFormat("##.##");
        DecimalFormat df3 = new DecimalFormat("##.###");

        try {
        	Connection con = DBManager.getConnection();
            stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            
            while (rs.next()) {
                Stocks.put(rs.getString("id"), rs.getString("name"));
                sellmodes.put(rs.getString("id"), rs.getInt("sell_mode_flg"));
            }
            rs.close();
            
            for (String stock : Stocks.keySet()) {
                double dev = 0;
                double cur_pri = 0;

                
                String sellMode = (sellmodes.get(stock) == 1) ? "是" : "否";

                try {
                    sql = "select avg(dev) dev from ("
         				   + "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, left(dl_dt, 10) atDay "
        				   + "  from stkdat2 "
        				   + " where id ='" + stock + "'"
        				   + "   and yt_cls_pri > 0 "
        				   + "   and left(dl_dt, 10) >= left(sysdate() - interval 7 day, 10)"
        				   + " group by left(dl_dt, 10)) t";
                    log.info(sql);
                    rs = stm.executeQuery(sql);
                    if (rs.next()) {
                    	dev = rs.getDouble("dev");
                    }
                    Stock2 stk = StockMarket.getStocks().get(stock);
                    Double cur_pri1 = stk.getCur_pri();
                    if (cur_pri1 != null) {
                        cur_pri = cur_pri1;
                    }
                    else {
                    	log.info("cur_pri for stock:" + stock + " is null, use -1.");
                    	cur_pri = -1;
                    }
                    
                    Integer dl_stk_num = stk.getDl_stk_num();
                    Double dl_mny_num = stk.getDl_mny_num();
                    
                    Double hst_pri = stk.getMaxTd_hst_pri();
                    Double lst_pri = stk.getMinTd_lst_pri();
                    Double b1_pri = stk.getB1_pri();
                    Double b2_pri = stk.getB2_pri();
                    Double b3_pri = stk.getB3_pri();
                    Double b4_pri = stk.getB4_pri();
                    Double b5_pri = stk.getB5_pri();
                    Integer b1_num = stk.getB1_num();
                    Integer b2_num = stk.getB2_num();
                    Integer b3_num = stk.getB3_num();
                    Integer b4_num = stk.getB4_num();
                    Integer b5_num = stk.getB5_num();
                    Double s1_pri = stk.getS1_pri();
                    Double s2_pri = stk.getS2_pri();
                    Double s3_pri = stk.getS3_pri();
                    Double s4_pri = stk.getS4_pri();
                    Double s5_pri = stk.getS5_pri();
                    Integer s1_num = stk.getS1_num();
                    Integer s2_num = stk.getS2_num();
                    Integer s3_num = stk.getS3_num();
                    Integer s4_num = stk.getS4_num();
                    Integer s5_num = stk.getS5_num();
                    Double opn_pri = stk.getOpen_pri();
                    Double yt_cls_pri = stk.getYtClsPri();
                    Double dlt_pri = cur_pri - yt_cls_pri;
                    Double pct = dlt_pri / yt_cls_pri * 100;
                    
                    content += "[" + stock + "]:" + Stocks.get(stock) + " 昨收" + df2.format(yt_cls_pri) + "\n";
                    content += "现价:" + df2.format(cur_pri) + "    今开:" + df2.format(opn_pri) + "\n";
                    content += "涨跌:" + df2.format(dlt_pri) + "    涨幅:" + df2.format(pct) + "%\n";
                    content += "最高:" + df2.format(hst_pri) + "    最低:" + df2.format(lst_pri) + "\n";
                    content += "成交:" + df2.format(dl_stk_num / 1000000.0) + "万手" + "  金额:" + df2.format(dl_mny_num / 10000000) + "千万\n";
                    content += "买五:" + df2.format(b5_pri) + "   手:" + b5_num / 100 + "\n";
                    content += "买四:" + df2.format(b4_pri) + "   手:" + b4_num / 100 + "\n";
                    content += "买三:" + df2.format(b3_pri) + "   手:" + b3_num / 100 + "\n";
                    content += "买二:" + df2.format(b2_pri) + "   手:" + b2_num / 100 + "\n";
                    content += "买一:" + df2.format(b1_pri) + "   手:" + b1_num / 100 + "\n";
                    content += "--------------------\n";
                    content += "卖一:" + df2.format(s1_pri) + "   手:" + s1_num / 100 + "\n";
                    content += "卖二:" + df2.format(s2_pri) + "   手:" + s2_num / 100 + "\n";
                    content += "卖三:" + df2.format(s3_pri) + "   手:" + s3_num / 100 + "\n";
                    content += "卖四:" + df2.format(s4_pri) + "   手:" + s4_num / 100 + "\n";
                    content += "卖五:" + df2.format(s5_pri) + "   手:" + s5_num / 100 + "\n";
                    content += "统计:" + " 七天dev:" + df3.format(dev) + " sellMode: " + sellMode + "\n\n";
                    rs.close();
                } catch(SQLException e0) {
                    log.info("No price infor for stock:" + stock + " continue...");
                    continue;
                }
            }
            stm.close();
            con.close();
        } catch (SQLException e1) {
        
            e1.printStackTrace();
        }
        return content;
    }

    public String getWorkResult() {
    	try{
    	    WorkManager.waitUntilWorkIsDone(this.getWorkName());
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
        return res;
    }

    public long getInitDelay() {
        return initDelay;
    }

    public long getDelayBeforeNxt() {
        return delayBeforNxtStart;
    }

    public TimeUnit getTimeUnit() {
        return tu;
    }

    public String getWorkName() {
        return "ListGzStock";
    }

    public boolean isCycleWork() {
        return false;
    }
}
