package com.sn.work.output;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.work.itf.IWork;
import com.sn.work.task.EvaStocks;

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
    
    static EvaStocks evs = new EvaStocks(0, 0);

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
        log.info("calculating top 10 bst:" + res + " for opt 1");
        res = evs.getBst10();
        log.info("return best top 10 to res for report:" + res);
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
