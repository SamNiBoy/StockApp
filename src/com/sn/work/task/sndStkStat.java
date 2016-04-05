package com.sn.work.task;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.sn.db.DBManager;
import com.sn.work.itf.IWork;

public class sndStkStat implements IWork {

    static Connection con = DBManager.getConnection();
    /*
     * Initial delay before executing work.
     */
    long initDelay = 0;

    /*
     * Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 60000;

    TimeUnit tu = TimeUnit.MILLISECONDS;
    
    private static String token = "";

    static Logger log = Logger.getLogger(sndStkStat.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        sndStkStat sss = new sndStkStat(0, 0);
        sss.run();
    }

    public sndStkStat(long id, long dbn) {
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
                log.info("Now start sndStkStat work...");
                if (token.length() <= 0) {
                    getToken();
                }
                
                if (token.length() > 0) {
                    String s = getStkInfo();
                    String toUsr ="osCWfs-ZVQZfrjRK0ml-eEpzeop0";

                    String ul = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=" + token;
                    try {
                        URL url = new URL(ul);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setRequestMethod("POST");
                        connection.setUseCaches(false);
                        connection.setInstanceFollowRedirects(true);
                        connection.setRequestProperty("Content-Type",
                                "application/x-www-form-urlencoded");

                        connection.connect();

                        //POST请求
                        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                        JSONObject obj = new JSONObject();
                        obj.put("touser", toUsr);
                        obj.put("msgtype", "text");
                        obj.put("text.content", s);

                        out.writeBytes(obj.toString());
                        out.flush();
                        out.close();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String lines;
                        StringBuffer sb = new StringBuffer("");
                        while ((lines = reader.readLine()) != null) {
                            lines = new String(lines.getBytes(), "utf-8");
                            sb.append(lines);
                        }
                        System.out.println(sb);
                        reader.close();
                        connection.disconnect();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        }
    }
    
    private String getStkInfo() {

        String res = "";
        // TODO Auto-generated method stub
        try {
            log.info("Now sndStkStat get stkInfo...");

            Statement mainStm = con.createStatement();
            ResultSet mainRs;
            DecimalFormat df = new DecimalFormat("##.##");
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
            res = "IncCnt:[" + mainRs.getString("incNum") + ", "
                    + df.format(mainRs.getDouble("avgIncPct")) + "]\n"
                    + "DscCnt:[" + mainRs.getString("dscNum") + ", "
                    + df.format(mainRs.getDouble("avgDscPct")) + "]\n"
                    + "equNum:[" + mainRs.getString("equNum") + "]\n";

            mainRs.close();
            sql = "select (sd.cur_pri - sd.td_opn_pri) / sd.td_opn_pri stkIncPct," +
                    "      sd.cur_pri," +
                    "      to_char(sd.dl_dt, 'yyyy-mm-dd HH24:MI:SS') tm, " +
                    "      s.name, " +
                    "      s.id "
                    + " from stkdat2 sd, stk s "
                    + "where sd.id in (select id from monStk)"
                    + "  and sd.id = s.id "
                    + "  and sd.ft_id = (select max(ft_id) from stkdat2 where id =s.id)"
                    + "  and to_char(sd.dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') ";
            log.info("sndStkStat:" + sql);
            mainRs = mainStm.executeQuery(sql);
            if (mainRs.next()) {
                res += "Stock:[" + mainRs.getString("id") + mainRs.getString("name") + ", incPct:"
                        + df.format(mainRs.getDouble("stkIncPct"))
                        + "\nCurPri:" + df.format(mainRs.getDouble("cur_pri"))
                        + "\nTime:" + mainRs.getString("tm") + "]";
            } else {
                res += "no infor for stock from monStk.";
            }
            log.info("sndStkStat:" + res);
        } catch (Exception e) {
            log.error("Error: " + e.getMessage());
            e.printStackTrace();
        }
        return res;
    }
    
    private static boolean getToken() {
        String APPID="wx23bd4a8c2f6afbef";
        String APPSECRET="d67ad4ccd309506bdad0313098dfbe42";
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + APPID + "&secret=" + APPSECRET;
        URL ul;
        try {
            ul = new URL(url);
            InputStream is = ul.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String str = "", txt ="";
            while((str = br.readLine()) != null)
            {
                txt += str;
            }
            log.info(txt);
            int ib = txt.indexOf(":\"");
            int ie = txt.indexOf("\",\"");
            token = txt.substring(ib + 2, ie);
            log.info("got token:" + token);
            br.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public String getWorkResult() {
        return "";
    }

    public String getWorkName() {
        return "sndStkStat";
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
