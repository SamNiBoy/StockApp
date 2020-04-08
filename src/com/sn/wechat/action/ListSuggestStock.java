package com.sn.wechat.action;

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
import com.sn.STConstants;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.WorkManager;
import com.sn.task.IWork;

public class ListSuggestStock implements IWork {

    Logger log = Logger.getLogger(ListSuggestStock.class);

    long initDelay = 0;
    long delayBeforNxtStart = 5;
    static String res = "开始收集推荐股票信息...";
    String frmUsr;

    TimeUnit tu = TimeUnit.MILLISECONDS;

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ListSuggestStock lgs = new ListSuggestStock(0, 0, "osCWfs-ZVQZfrjRK0ml-eEpzeop0");
        lgs.run();
    }

    public ListSuggestStock(long id, long dbn, String usr) {
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
                msg = "目前没有系统推荐股票.";
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
        String system_suggester = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
        String system_trader = ParamManager.getStr2Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
        
        String sql = "select s.id, s.name from stk s, usrStk u where s.id = u.id and u.gz_flg = 1 and u.openID ='" + frmUsr + "' and u.suggested_by in ('" + system_suggester + "','" + system_trader + "')";
        String content = "";
        Map<String, String> Stocks = new HashMap<String, String> ();
        DecimalFormat df = new DecimalFormat("##.###");

        try {
        	Connection con = DBManager.getConnection();
            stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            
            while (rs.next()) {
                Stocks.put(rs.getString("id"), rs.getString("name"));
            }
            rs.close();
            
            for (String stock : Stocks.keySet()) {
                double dev = 0;
                double cur_pri = 0;

                content += stock + ":" + Stocks.get(stock) + "\n";

                try {
                    sql = "select avg(dev) dev from ("
         				   + "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, left(dl_dt, 10) atDay "
        				   + "  from stkdat2 "
        				   + " where id ='" + stock + "'"
        				   + "   and yt_cls_pri > 0 "
        				   + "   and left(dl_dt, 10) >= left(sysdate() - interval 7 day, 10)"
        				   + " group by left(dl_dt, 10))";
                    log.info(sql);
                    rs = stm.executeQuery(sql);
                    if (rs.next()) {
                    	dev = rs.getDouble("dev");
                    }
                    Stock2 s =  (Stock2)StockMarket.getStocks().get(stock);
                    cur_pri =  s.getCur_pri();
                    content += "价:" + df.format(cur_pri) + " stddev:" + df.format(dev) + "\n";
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
        return "ListSuggestStock";
    }

    public boolean isCycleWork() {
        return false;
    }
}
