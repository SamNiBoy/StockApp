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
import com.sn.work.WorkManager;
import com.sn.work.fetcher.FetchStockData;

public class EnaUsrBuySell implements IWork {

    static Connection con = DBManager.getConnection();
    /*
     * Initial delay before executing work.
     */
    long initDelay = 0;
    String frmUsr = "";
    boolean enable_buy_sell_flg = false;
    static String resContent = "Initial AddMail result is not set.";

    public String bell = "this is notify use.";

    /*
     * Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 60000;

    TimeUnit tu = TimeUnit.MILLISECONDS;

    static Logger log = Logger.getLogger(EnaUsrBuySell.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
    	EnaUsrBuySell fsd = new EnaUsrBuySell(1, 0, "abc", true);
        fsd.run();
    }

    public EnaUsrBuySell(long id, long dbn, String fu, boolean enable) {
        initDelay = id;
        delayBeforNxtStart = dbn;
        frmUsr = fu;
        enable_buy_sell_flg = enable;
    }

    /*
     * var hq_str_sh601318=
     * "�й�ƽ��,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
     * ;
     */

    public void run() {
        // TODO Auto-generated method stub
        try {
            log.info("Now start EnaUsrBuySell work...");
            Statement mainStm = con.createStatement();
            String sql;
            if (enable_buy_sell_flg) {
                sql = "update usr set buy_sell_enabled = 1 where openID = '" + frmUsr + "'";
            }
            else {
            	sql = "update usr set buy_sell_enabled = 0 where openID = '" + frmUsr + "'";
            }
            log.info("EnaUsrBuySell:" + sql);
            mainStm.executeUpdate(sql);
            con.commit();
            mainStm.close();
            if (enable_buy_sell_flg) {
                sql = "select 'x' from usr where mail is not null";
                mainStm = con.createStatement();
                ResultSet rs = mainStm.executeQuery(sql);
                if (!rs.next()) {
                        resContent = "已经开启买卖信息提示，请发送邮箱地址进行订阅。";
                    }
                else {
                	resContent = "已经开启买卖信息提示，您的邮箱将收到买卖提示信息。";
                }
                rs.close();
                mainStm.close();
            }
            else {
            	resContent = "已停止买卖信息提示。";
            }
        } catch (Exception e) {
            log.error("Error: " + e.getMessage());
            e.printStackTrace();
            resContent = "异常:" + e.getMessage();
        }
    }

    public String getWorkResult() {
    	try{
    	    WorkManager.waitUntilWorkIsDone(this.getWorkName());
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
        return resContent;
    }

    public String getWorkName() {
        return "EnaUsrBuySell";
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
