package com.sn.work.task;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.work.itf.IWork;
import com.sn.work.fetcher.FetchStockData;

public class usrStock implements IWork {

    static Connection con = DBManager.getConnection();
    /*
     * Initial delay before executing work.
     */
    long initDelay = 0;
    String frmUsr = "";
    String stkID = "";
    static String resContent = "Initial usrStock result is not set.";

    public String bell = "this is notify use.";

    /*
     * Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 60000;

    TimeUnit tu = TimeUnit.MILLISECONDS;

    static Logger log = Logger.getLogger(usrStock.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        usrStock fsd = new usrStock(1, 0, "abc", "601318");
        fsd.run();
    }

    public usrStock(long id, long dbn, String fu, String sid) {
        initDelay = id;
        delayBeforNxtStart = dbn;
        frmUsr = fu;
        stkID = sid;
    }

    /*
     * var hq_str_sh601318=
     * "�й�ƽ��,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
     * ;
     */

    public void run() {
        // TODO Auto-generated method stub
        try {
            log.info("Now start usrStock work...");

            Statement mainStm = con.createStatement();
            ResultSet mainRs;
            DecimalFormat df = new DecimalFormat("##.##");
            log.info("Check if stock "+stkID + " exist!");
            String mainSql = "select id from stk where id = '" + stkID + "'";

            mainRs = mainStm.executeQuery(mainSql);
            if (!mainRs.next()) {
                resContent = "Stock:" + stkID + " does not exists!";
                return;
            }
            log.info("after "+mainSql);
            String sql = "select sum(case when cur_pri >= (td_opn_pri + 0.01) then 1 else 0 end) incNum,"
                    + "                    sum(case when cur_pri >= (td_opn_pri + 0.01) and td_opn_pri > 0 then (cur_pri - td_opn_pri)/ td_opn_pri else 0 end) /"
                    + "                       decode(sum(case when cur_pri >= (td_opn_pri + 0.01) then 1 else 0 end), 0, 1, sum(case when cur_pri >= (td_opn_pri + 0.01) then 1 else 0 end)) avgIncPct,"
                    + "       sum(case when cur_pri <= (td_opn_pri - 0.01) then 1 else 0 end) dscNum,"
                    + "                    sum(case when cur_pri <= (td_opn_pri - 0.01) and td_opn_pri > 0 then (cur_pri - td_opn_pri)/td_opn_pri else 0 end) /"
                    + "                       decode(sum(case when cur_pri <= (td_opn_pri - 0.01) then 1 else 0 end), 0, 1, sum(case when cur_pri <= (td_opn_pri - 0.01) then 1 else 0 end)) avgDscPct,"
                    + "       sum(case when abs(cur_pri - td_opn_pri) < 0.01 then 1 else 0 end) equNum"
                    + "  from stkdat2 "
                    + " where to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') "
                    + "   and ft_id in (select max(ft_id) from stkdat2 group by ID)";
            log.info("usrStock:" + sql);
            mainRs = mainStm.executeQuery(sql);
            mainRs.next();
            resContent = "IncCnt:[" + mainRs.getString("incNum") + ", "
                    + df.format(mainRs.getDouble("avgIncPct")) + "]\n"
                    + "DscCnt:[" + mainRs.getString("dscNum") + ", "
                    + df.format(mainRs.getDouble("avgDscPct")) + "]\n"
                    + "equNum:[" + mainRs.getString("equNum") + "]\n";

            mainRs.close();
            sql = "select (sd.cur_pri - sd.td_opn_pri) / sd.td_opn_pri stkIncPct," +
            		"      sd.cur_pri," +
            		"      to_char(sd.dl_dt, 'yyyy-mm-dd HH24:MI:SS') tm, " +
            		"      s.name "
                    + " from stkdat2 sd, stk s "
                    + "where sd.id = '" + stkID + "'"
                    + "  and sd.id = s.id "
                    + "  and sd.ft_id = (select max(ft_id) from stkdat2 where id ='" + stkID + "')"
                    + "  and to_char(sd.dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') ";
            log.info("usrStock:" + sql);
            mainRs = mainStm.executeQuery(sql);
            if (mainRs.next()) {
                resContent += "Stock:[" + stkID + mainRs.getString("name") + ", incPct:"
                        + df.format(mainRs.getDouble("stkIncPct"))
                        + "\nCurPri:" + df.format(mainRs.getDouble("cur_pri"))
                        + "\nTime:" + mainRs.getString("tm") + "]";
            } else {
                resContent += "no infor for stock:" + stkID;
            }
            log.info("ustStock:" + resContent);
        } catch (Exception e) {
            log.error("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getWorkResult() {
        return resContent;
    }

    public String getWorkName() {
        return "usrStock";
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

    public boolean isCycleWork() {
        return delayBeforNxtStart > 0;
    }

}
