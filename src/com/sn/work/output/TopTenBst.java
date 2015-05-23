package com.sn.work.output;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.work.itf.IWork;

public class TopTenBst implements IWork {

    /* Initial delay before executing work.
     */
    long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 5;
    
    TimeUnit tu = TimeUnit.MILLISECONDS;
    /* Result calcualted by this worker.
     */
    static String res = "Getting top 10 best is schedulled, try again later.";
    
    static Logger log = Logger.getLogger(DBManager.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }
    
    public TopTenBst(long id, long dbn)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;        
    }
    
    public void run()
    {
        // ///////////Menu 1///////////////
        String msg = "";
        Connection con = DBManager.getConnection();
        String sql = "select stk.area || df.id id, df.cur_pri_df, stk.name, std.cur_pri "
                + "  from curpri_df_vw df, stk, (select id, cur_pri"
                + "                                from stkDat "
                + "                               where not exists(select 'x' "
                + "                                                  from stkDat sd2 "
                + "                                                 where sd2.id = stkDat.id "
                + "                                                   and sd2.ft_id > stkDat.ft_id)) std "
                + " where df.id = stk.id "
                + "   and df.id = std.id "
                + "   and not exists (select 'x' from curpri_df_vw dfv where dfv.id = df.id and dfv.ft_id > df.ft_id) "
                + "  order by df.cur_pri_df desc ";
        
        sql = "select distinct stkdat.id, stk.name, stkdat.cur_pri - avt.avp pd from stkdat, "
            + "(select id, avg(cur_pri) avp from stkdat where stkdat.cur_pri > 0 group by id) avt, "
            + "stk "
            + "where stkdat.id = avt.id "
            + "and stkdat.cur_pri > avt.avp "
            + "and stkdat.id = stk.id "
            + "and not exists(select 'x' from stkdat sd1 where sd1.id = stkdat.id and sd1.ft_id > stkdat.ft_id having(count(sd1.ft_id)) > 0) "
            + "order by stkdat.cur_pri - avt.avp";
        try {
            Statement stm = con.createStatement();
            log.info("before gettop10Bst...");
            ResultSet rs = stm.executeQuery(sql);
            log.info("after gettop10Bst...");

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
            log.info("calculating top 10 bst:" + msg + " for opt 1");
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
        return "TopTenBst";
    }

    public boolean isCycleWork()
    {
        return false;
    }
    
//  // //////////////////Menu 3/////////////////////////////////////////////
//  msg = "";
//  sql = "select stk.area || df.id id, df.cur_pri_df2, stk.name"
//          + "  from curpri_df2_vw df, stk "
//          + " where df.id = stk.id "
//          + "   and not exists (select 'x' from curpri_df2_vw dfv where dfv.id = df.id and dfv.ft_id > df.ft_id) "
//          + "  order by df.cur_pri_df2 desc ";
//  try {
//      Statement stm = con.createStatement();
//      ResultSet rs = stm.executeQuery(sql);
//
//      String id = "";
//      for (int i = 0; i < 10 && rs.next(); i++) {
//          if (id.equals(rs.getString("id"))) {
//              continue;
//          } else {
//              id = rs.getString("id");
//          }
//          msg += (i + 1) + ": " + rs.getString("id") + " "
//                  + rs.getString("name") + "\n";
//          msg += "Current Price Diff2: " + rs.getString("cur_pri_df2")
//                  + "\n";
//      }
//      stm.close();
//      log.info("putting msg:" + msg + " for opt 3");
//      msgForMenu.put("3", msg);
//
//  } catch (Exception e) {
//      e.printStackTrace();
//  }
//
//  // //////////////////Menu 4/////////////////////////////////////////////
//  msg = "";
//  sql = "select stk.area || df.id id, df.cur_pri_df2, stk.name"
//          + "  from curpri_df2_vw df, stk "
//          + " where df.id = stk.id "
//          + "   and not exists (select 'x' from curpri_df2_vw dfv where dfv.id = df.id and dfv.ft_id > df.ft_id) "
//          + "  order by df.cur_pri_df2 ";
//  try {
//      Statement stm = con.createStatement();
//      ResultSet rs = stm.executeQuery(sql);
//
//      String id = "";
//      for (int i = 0; i < 10 && rs.next(); i++) {
//          if (id.equals(rs.getString("id"))) {
//              continue;
//          } else {
//              id = rs.getString("id");
//          }
//          msg += (i + 1) + ": " + rs.getString("id") + " "
//                  + rs.getString("name") + "\n";
//          msg += "Current Price Diff2: " + rs.getString("cur_pri_df2")
//                  + "\n";
//      }
//      stm.close();
//      log.info("putting msg:" + msg + " for opt 4");
//      msgForMenu.put("4", msg);
//
//  } catch (Exception e) {
//      e.printStackTrace();
//  }
}
