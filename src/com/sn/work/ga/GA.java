package com.sn.work.ga;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class GA implements IWork {

    static Connection con = DBManager.getConnection();
    /* Initial delay before executing work.
     */
    long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 5;

    TimeUnit tu = TimeUnit.MILLISECONDS;
    
    static int [][]  mat = null;
    static double [][] trainSet = null;
    static int stkNum = 0;
    static String [] stocks = null;
    static public String bellForWork = "This is bell for other waiting threads";

    static Logger log = Logger.getLogger(GA.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        GA fsd = new GA(1, 3);
        fsd.run();
    }

    public GA(long id, long dbn)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    static boolean initMat()
    {
        Statement stm = null;
        ResultSet rs = null;
        String sql = "select distinct id from stkdat2 where to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') and td_opn_pri > 0 order by id ";

        int i = 0;

        try{
            stm = con.createStatement();
            rs = stm.executeQuery(sql);
            
            rs.last();
            stkNum = rs.getRow();
            
            if (stkNum == 0) {
                return false;
            }
            stocks = new String[stkNum];
            mat = new int[stkNum][stkNum];
            
            rs.first();
            while (rs.next()) {
                stocks[i] = rs.getString("id");
                i++;
            }
            
            double p;
            for (i = 0; i < stkNum; i++) {
                for (int j = 0; j < stkNum; j++) {
                    p = Math.random();
                    if (p < 1/3.0) {
                        mat[i][j] = -1;
                    }
                    else if (p >= 1/3.0 && p < 2/3.0) {
                        mat[i][j] = 0;
                    }
                    else {
                        mat[i][j] = 1;
                    }
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
        finally {
            try {
                rs.close();
                stm.close();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        return true;
    }

    public void run()
    {
        // TODO Auto-generated method stub
        try {
            if (initMat() && buildTrainSet()) {
                trainMat();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean buildTrainSet()
    {
        Statement stm = null;
        ResultSet rs = null;
        String sql = "select (cur_pri - td_opn_pri) /td_opn_pri pct, id" +
                       " from stkdat2 s1 " +
                       "where td_opn_pri > 0 " +
                       "  and dl_dt >= sysdate - 5 * 1.0 / (24 * 60)" +
                       " order by id, ft_id ";
        log.info(sql);
        try{
            int rowCnt = 0;
            int sidx = 0, eidx = 0;
            stm = con.createStatement();
            rs = stm.executeQuery(sql);
            rs.last();
            rowCnt = rs.getRow();
            
            while (!rs.getString("id").equals(stocks[0])) {
                sidx++;
            }
            
            if (sidx >= rowCnt) {
                return false;
            }
            
            int restCnt = rowCnt - sidx;
            eidx = restCnt - restCnt % stkNum;
            if (eidx == 0) {
                return false;
            }
            
            assert(sidx < eidx);
            
            trainSet = new double[stkNum][(eidx - sidx) / stkNum];
            
            rs.first();
            for (int i = 0; i < stkNum; i++) {
                for (int j = sidx; j < eidx; j++) {
                    trainSet[i][j] = rs.getDouble(i * stkNum + j);
                    assert(rs.getString(i * stkNum + j).equals(stocks[i]));
                }
            }
            return true;
        }
        catch(Exception e)
        {
        	log.error("GA errored:" + e.getMessage());
            e.printStackTrace();
            return false;
        }
        finally {
            try {
                stm.close();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }
    
    static void trainMat()
    {
        
    }


    public String getWorkResult()
    {
        return "";
    }

    public String getWorkName()
    {
        return "GA";
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

    public boolean isCycleWork()
    {
        return true;
    }

}
