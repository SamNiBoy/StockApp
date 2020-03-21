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
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.work.itf.IWork;

public class CalStkDDF implements IWork {

    static Connection con = null;
    /*
     * Initial delay before executing work.
     */
    long initDelay = 0;

    /*
     * Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 60000;

    TimeUnit tu = TimeUnit.MILLISECONDS;

    static Logger log = Logger.getLogger(CalStkDDF.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        CalStkDDF fsd = new CalStkDDF(1, 3);
        fsd.run();
    }

    public CalStkDDF(long id, long dbn) {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    /*
     * var hq_str_sh601318=
     * "�й�ƽ��,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
     * ;
     */

    private String lstStkDat = "";

    public void run() {
        // TODO Auto-generated method stub
        while (true) {
            con = DBManager.getConnection();
            try {
                log.info("Now start CalStkDDF work...");

                Statement mainStm = con.createStatement();
                ResultSet mainRs;
                String mainSql = "select id from stk order by id";

                mainRs = mainStm.executeQuery(mainSql);
                while (mainRs.next()) {

                    String id = mainRs.getString("id");

                    Statement stmLstDt = con.createStatement();
                    String sqlLstDt = "select decode(max(left(dl_dt,10)), null, 'xxxx', max(left(dl_dt, 10))) lst_dl_dt from stkddf where id = '"
                            + id + "'";
                    ResultSet rsLstDt = stmLstDt.executeQuery(sqlLstDt);

                    String clause = " where t1.id ='" + id + "'";
                    rsLstDt.next();
                    String LstDt = rsLstDt.getString("lst_dl_dt");

                    stmLstDt.close();

                    log.info("Processing id:" + id + " got LstDt:" + LstDt);

                    if (!LstDt.equals("xxxx")) {
                        clause += " and t1.dl_dt > to_date('" + LstDt
                                + "', 'yyyy-mm-dd HH24:MI:SS')";
                    }

                    Statement stm = con.createStatement();
                    ResultSet rs;
                    String sql = "select t1.ft_id, t1.id, " + "cur_pri,"
                            + " dl_stk_num," + " dl_mny_num," + " b1_num,"
                            + " b1_pri," + " b2_num," + " b2_pri," + " b3_num,"
                            + " b3_pri," + " b4_num," + " b4_pri," + " b5_num,"
                            + " b5_pri," + " s1_num," + " s1_pri," + " s2_num,"
                            + " s2_pri," + " s3_num," + " s3_pri," + " s4_num,"
                            + " s4_pri," + " s5_num," + " s5_pri," + " dl_dt "
                            + " from stkdat2 t1 " + clause
                            + " order by t1.id, t1.ft_id";

                    log.info(sql);
                    rs = stm.executeQuery(sql);

                    CrtDDF(rs, 1);

                    stm.close();
                }
            } catch (Exception e) {
                log.error("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static boolean CrtDDF(ResultSet rs, int gap) {

        String sql;
        long ft_id;
        String id;
        double cur_pri;
        long dl_stk_num;
        double dl_mny_num;
        long b1_num;
        double b1_pri;
        long b2_num;
        double b2_pri;
        long b3_num;
        double b3_pri;
        long b4_num;
        double b4_pri;
        long b5_num;
        double b5_pri;
        long s1_num;
        double s1_pri;
        long s2_num;
        double s2_pri;
        long s3_num;
        double s3_pri;
        long s4_num;
        double s4_pri;
        long s5_num;
        double s5_pri;
        String dl_dt;

        Statement stm2;
        try {
            stm2 = con.createStatement();

            int i = 0;
            while (rs.next()) {
                ft_id = rs.getLong("ft_id");
                id = rs.getString("id");
                cur_pri = rs.getDouble("cur_pri");
                dl_stk_num = rs.getLong("dl_stk_num");
                dl_mny_num = rs.getDouble("dl_mny_num");
                b1_num = rs.getLong("b1_num");
                b1_pri = rs.getDouble("b1_pri");
                b2_num = rs.getLong("b2_num");
                b2_pri = rs.getDouble("b2_pri");
                b3_num = rs.getLong("b3_num");
                b3_pri = rs.getDouble("b3_pri");
                b4_num = rs.getLong("b4_num");
                b4_pri = rs.getDouble("b4_pri");
                b5_num = rs.getLong("b5_num");
                b5_pri = rs.getDouble("b5_pri");
                s1_num = rs.getLong("s1_num");
                s1_pri = rs.getDouble("s1_pri");
                s2_num = rs.getLong("s2_num");
                s2_pri = rs.getDouble("s2_pri");
                s3_num = rs.getLong("s3_num");
                s3_pri = rs.getDouble("s3_pri");
                s4_num = rs.getLong("s4_num");
                s4_pri = rs.getDouble("s4_pri");
                s5_num = rs.getLong("s5_num");
                s5_pri = rs.getDouble("s5_pri");
                dl_dt = rs.getString("dl_dt");

                while (i < gap) {
                    if (!rs.next())
                        break;
                    else {
                        String datestr = dl_dt.substring(0, 10);
                        String datestr2 = rs.getString("dl_dt")
                                .substring(0, 10);
                        if (!id.equals(rs.getString("id"))
                                || !datestr.equals(datestr2)) {
                            log.info("Not Equal id1:" + id + " id2:"
                                    + rs.getString("id"));
                            log.info("Not Equal datestr1:" + datestr
                                    + " datestr2:" + rs.getString("dl_dt"));
                            break;
                        }
                    }
                    i++;
                }
                if (i == gap) {
                    i = 0;
                    log.info("dst ft_id:" + ft_id + " ft_id2:"
                            + rs.getLong("ft_id"));
                    cur_pri = rs.getDouble("cur_pri") - cur_pri;
                    dl_stk_num = rs.getLong("dl_stk_num") - dl_stk_num;
                    dl_mny_num = rs.getDouble("dl_mny_num") - dl_mny_num;
                    b1_num = rs.getLong("b1_num") - b1_num;
                    b1_pri = rs.getDouble("b1_pri") - b1_pri;
                    b2_num = rs.getLong("b2_num") - b2_num;
                    b2_pri = rs.getDouble("b2_pri") - b2_pri;
                    b3_num = rs.getLong("b3_num") - b3_num;
                    b3_pri = rs.getDouble("b3_pri") - b3_pri;
                    b4_num = rs.getLong("b4_num") - b4_num;
                    b4_pri = rs.getDouble("b4_pri") - b4_pri;
                    b5_num = rs.getLong("b5_num") - b5_num;
                    b5_pri = rs.getDouble("b5_pri") - b5_pri;
                    s1_num = rs.getLong("s1_num") - s1_num;
                    s1_pri = rs.getDouble("s1_pri") - s1_pri;
                    s2_num = rs.getLong("s2_num") - s2_num;
                    s2_pri = rs.getDouble("s2_pri") - s2_pri;
                    s3_num = rs.getLong("s3_num") - s3_num;
                    s3_pri = rs.getDouble("s3_pri") - s3_pri;
                    s4_num = rs.getLong("s4_num") - s4_num;
                    s4_pri = rs.getDouble("s4_pri") - s4_pri;
                    s5_num = rs.getLong("s5_num") - s5_num;
                    s5_pri = rs.getDouble("s5_pri") - s5_pri;

                    ft_id = rs.getLong("ft_id");
                    dl_dt = rs.getString("dl_dt");

                    sql = "insert into stkddf values (" + ft_id + "," + gap
                            + ",'" + id + "'," + cur_pri + "," + dl_stk_num
                            + "," + dl_mny_num + "," + b1_num + "," + b1_pri
                            + "," + b2_num + "," + b2_pri + "," + b3_num + ","
                            + b3_pri + "," + b4_num + "," + b4_pri + ","
                            + b5_num + "," + b5_pri + "," + s1_num + ","
                            + s1_pri + "," + s2_num + "," + s2_pri + ","
                            + s3_num + "," + s3_pri + "," + s4_num + ","
                            + s4_pri + "," + s5_num + "," + s5_pri + ","
                            + "to_date('" + dl_dt
                            + "', 'YYYY-MM-DD HH24:MI:SS'))";

                    log.info(sql);

                    stm2.executeUpdate(sql);
                }
            }
            stm2.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            log.error("Inserting stkDDF error:" + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    public String getWorkResult() {
        return "";
    }

    public String getWorkName() {
        return "CalStkDDF";
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
        return false;
    }

}
