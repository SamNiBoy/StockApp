package com.sn.work;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.db.DBManager;

public class TopTenWst implements IWork {

    long initDelay = 0;
    long delayBeforNxtStart = 5;
    static String res = "";

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

    public void run() { // //////////////////Menu
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
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);

            for (int i = 0; i < 10 && rs.next(); i++) {
                msg += (i + 1) + ": " + rs.getString("id") + " "
                        + rs.getString("name") + "\n";
                msg += "CP: " + rs.getString("cur_pri") + "\n";
                msg += "CPD: " + rs.getString("cur_pri_df") + "\n";
            }
            stm.close();
            System.out.println("calculating top 10 bst:" + msg + " for opt 1");
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
