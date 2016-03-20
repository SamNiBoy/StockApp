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

public class AddMail implements IWork {

    /*
     * Initial delay before executing work.
     */
    long initDelay = 0;
    String frmUsr = "";
    String mail = "";
    static String resContent = "Initial AddMail result is not set.";

    public String bell = "this is notify use.";

    /*
     * Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 60000;

    TimeUnit tu = TimeUnit.MILLISECONDS;

    static Logger log = Logger.getLogger(AddMail.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
    	AddMail fsd = new AddMail(1, 0, "abc", "601318");
        fsd.run();
    }

    public AddMail(long id, long dbn, String fu, String ml) {
        initDelay = id;
        delayBeforNxtStart = dbn;
        frmUsr = fu;
        mail = ml;
    }

    /*
     * var hq_str_sh601318=
     * "�й�ƽ��,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
     * ;
     */

    public void run() {
        // TODO Auto-generated method stub
        try {
        	Connection con = DBManager.getConnection();
            log.info("Now start AddMail work...");
            Statement mainStm = con.createStatement();
            String sql = "update usr set mail = '" + mail + "' where openID = '" + frmUsr + "'";
            log.info("AddMail:" + sql);
            mainStm.executeUpdate(sql);
            con.commit();
            con.close();
            resContent = "成功添加邮箱:" + mail;
        } catch (Exception e) {
            log.error("Error: " + e.getMessage());
            e.printStackTrace();
            resContent = "添加邮箱:" + mail + "异常:" + e.getMessage();
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
        return "AddMail";
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
