package com.sn.work.output;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.work.itf.IWork;

public class TopTenWst implements IWork {

    Logger log = Logger.getLogger(TopTenWst.class);

    long initDelay = 0;
    long delayBeforNxtStart = 5;
    static String res = "Getting top 10 worse is schedulled, try again later.";

    TimeUnit tu = TimeUnit.MILLISECONDS;
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    public TopTenWst(long id, long dbn) {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    public void run() {
        log.info("This is from logger4j infor msg...");
        // //////////////////Menu
        // 2///////////////////////////////////////////////////
        String msg = "", sql;
        Connection con = DBManager.getConnection();
        sql = "select stk.area || df.id id, df.cur_pri_df, stk.name, std.cur_pri "
                + "  from curpri_df_vw df, stk, (select id, cur_pri"
                + "                                from stkDat "
                + "                               where not exists(select 'x' "
                + "                                                  from stkDat sd2 "
                + "                                                 where sd2.id = stkDat.id "
                + "                                                   and sd2.ft_id > stkDat.ft_id)) std "
                + " where df.id = stk.id "
                + "   and df.id = std.id "
                + "   and not exists (select 'x' from curpri_df_vw dfv where dfv.id = df.id and dfv.ft_id > df.ft_id) "
                + "  order by df.cur_pri_df ";

        sql = "select distinct stkdat.id, stk.name, stkdat.cur_pri - avt.avp pd from stkdat, "
                + "(select id, avg(cur_pri) avp from stkdat where stkdat.cur_pri > 0 group by id) avt, "
                + "stk "
                + "where stkdat.id = avt.id "
                + "and stkdat.cur_pri < avt.avp "
                + "and stkdat.id = stk.id "
                + "and not exists(select 'x' from stkdat sd1 where sd1.id = stkdat.id and sd1.ft_id > stkdat.ft_id having(count(sd1.ft_id)) > 0) "
                + "order by stkdat.cur_pri - avt.avp";

        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);

            for (int i = 0; i < 10 && rs.next(); i++) {
                msg += (i + 1) + ": " + rs.getString("id") + " "
                        + rs.getString("name") + "\n";
            }
            rs.close();
            stm.close();
            con.close();
            if (msg.length() <= 0)
            {
                msg = "No enough stock data available, use option 3 fetch first.";
            }
            log.info("calculating top 10 bst:" + msg + " for opt 2");
            res = msg;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getWorkResult()
    {
        return res;
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

    public String getWorkName()
    {
        return "TopTenWst";
    }

    public boolean isCycleWork()
    {
        return false;
    }
}
