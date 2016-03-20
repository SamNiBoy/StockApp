package com.sn.work.output;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
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
        ListGzStock lgs = new ListGzStock(0, 0, "abc");
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
        String sql = "select s.id, s.name from stk s, usrStk u where s.id = u.id and u.gz_flg = 1 and u.openID ='" + frmUsr + "'";
        String content = "";
        Map<String, String> Stocks = new HashMap<String, String> ();

        try {
        	Connection con = DBManager.getConnection();
            stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            
            while (rs.next()) {
                Stocks.put(rs.getString("id"), rs.getString("name"));
            }
            rs.close();
            
            long incPriCnt = 0, eqlPriCnt = 0;
            long desPriCnt = 0;
            long detQty = 0, qtyCnt = 0;
            for (String stock : Stocks.keySet()) {
                double pre_cur_pri = 0, cur_pri = 0;
                double pre_qty = 0, cur_qty = 0;
                incPriCnt = eqlPriCnt = desPriCnt = 0;
                detQty = qtyCnt = 0;

                content += stock + ":" + Stocks.get(stock) + " 信息:\n";

                try {
                    sql = "select cur_pri, dl_stk_num from stkdat2 where id ='"
                            + stock
                            + "' and to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') order by ft_id";
                    log.info(sql);
                    rs = stm.executeQuery(sql);
                    
                    while (rs.next()) {
                        cur_pri = rs.getDouble("cur_pri");
                        cur_qty = rs.getDouble("dl_stk_num");
                        if (pre_cur_pri != 0) {
                            if (cur_pri > pre_cur_pri) {
                                incPriCnt++;
                            } else if (cur_pri == pre_cur_pri) {
                                eqlPriCnt++;
                            } else {
                                desPriCnt++;
                            }
                        }
                        pre_cur_pri = cur_pri;
                    
                        if (pre_qty != 0) {
                            if (cur_qty > pre_qty) {
                                detQty += cur_qty - pre_qty;
                            }
                            qtyCnt++;
                        }
                        pre_qty = cur_qty;
                    }
                    rs.close();

                    if (qtyCnt > 0) {
                        detQty = detQty / qtyCnt;
                    }
                    
                    if (incPriCnt + eqlPriCnt + desPriCnt > 0) {
                        double d0 = eqlPriCnt * 1.0
                                / (incPriCnt + desPriCnt + eqlPriCnt);
                        double d1 = Math.abs(incPriCnt - desPriCnt) * 1.0
                                / Math.abs(Math.max(1,Math.max(incPriCnt, desPriCnt)));
                        log.info("eqlRt:" + d0 + "\ndifRt:" + d1);
                            content +=" 涨:" + incPriCnt + " 跌:" + desPriCnt
                                    + " 平:" + eqlPriCnt + " 幅:" + detQty + "价:" + cur_pri + "\n";
                    }
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
        return "TopTenWst";
    }

    public boolean isCycleWork() {
        return false;
    }
}
