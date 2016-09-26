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
import com.sn.stock.StockMarket;
import com.sn.trade.strategy.imp.STConstants;
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
        String sql = "select s.id, s.name, u.sell_mode_flg, u.trade_mode_id "
        		+ "     from stk s, usrStk u "
        		+ "    where s.id = u.id "
        		+ "      and u.gz_flg = 1 "
        		+ "      and u.openID ='" + frmUsr + "' "
        	    + "      and u.suggested_by in ('" + frmUsr + "','" + STConstants.SUGGESTED_BY_FOR_SYSTEM_GRANTED + "','" + STConstants.SUGGESTED_BY_FOR_SYSTEM_READ_FOR_TRADE + "')"
        	    + "     order by s.id ";
        String content = "";
        Map<String, String> Stocks = new HashMap<String, String> ();
        Map<String, Integer> sellmodes = new HashMap<String, Integer> ();
        Map<String, Integer> trademodeids = new HashMap<String, Integer> ();
        DecimalFormat df = new DecimalFormat("##.###");
        String ids = "", id = "";

        try {
        	Connection con = DBManager.getConnection();
            stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            
            while (rs.next()) {
                Stocks.put(rs.getString("id"), rs.getString("name"));
                sellmodes.put(rs.getString("id"), rs.getInt("sell_mode_flg"));
                trademodeids.put(rs.getString("id"), rs.getInt("trade_mode_id"));
                if (ids.length() > 0) {
                	ids += ",";
                }
                ids += "'" + rs.getString("id") + "'";
                id = "'" + rs.getString("id") + "'";
            }
            rs.close();
            
            double dev = 0;
            double cur_pri = 0;

            int daysCnt = StockMarket.getNumDaysAhead(id, STConstants.DEV_CALCULATE_DAYS);
            
            try {
                sql = "select id, avg(dev) dev from ("
         		   + "select id, stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, to_char(dl_dt, 'yyyy-mm-dd') atDay "
        		   + "  from stkdat2 "
        		   + " where id in (" + ids + ")"
        		   + "   and yt_cls_pri > 0 "
        		   + "   and to_char(dl_dt, 'yyyy-mm-dd') >= to_char(sysdate - " + daysCnt + ", 'yyyy-mm-dd')"
        		   + " group by id, to_char(dl_dt, 'yyyy-mm-dd'))"
        		   + " group by id order by id ";
                
                log.info(sql);
                
                rs = stm.executeQuery(sql);
                String stock = "";
                while (rs.next()) {
                	stock = rs.getString("id");
                	content += stock + ":" + Stocks.get(stock) + "\n";
                	String sellMode = (sellmodes.get(stock) == 1) ? "Yes" : "No";
                	dev = rs.getDouble("dev");
                    Double cur_pri1 = StockMarket.getStocks().get(stock).getCur_pri();
                    Integer trade_mode_id = trademodeids.get(stock);
                    
                    if (cur_pri1 != null) {
                        cur_pri = cur_pri1;
                    }
                    else {
                    	log.info("cur_pri for stock:" + stock + " is null, use -1.");
                    	cur_pri = -1;
                    }
                    content += "价:" + df.format(cur_pri) + " stddev:" + df.format(dev) + " sellMode: " + sellMode + "TradeModeId:" + trade_mode_id + "\n";
                }
                rs.close();
            } catch(SQLException e0) {
                log.info("No price infor for stocks:" + ids + " continue...");
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
