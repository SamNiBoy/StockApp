package com.sn.sim;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import com.sn.cashAcnt.CashAcnt;
import com.sn.cashAcnt.CashAcntManger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.mail.reporter.MailSenderType;
import com.sn.mail.reporter.MailSenderFactory;
import com.sn.mail.reporter.SimpleMailSender;
import com.sn.mail.reporter.StockObserver;
import com.sn.mail.reporter.StockObserverable;
import com.sn.reporter.WCMsgSender;
import com.sn.sim.strategy.ITradeStrategy;
import com.sn.sim.strategy.imp.TradeStrategyGenerator;
import com.sn.sim.strategy.imp.TradeStrategyImp;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class SimTrader implements IWork{

    static Logger log = Logger.getLogger(SimTrader.class);

    /*
     * Initial delay before executing work.
     */
    private long initDelay = 0;

    /*
     * Seconds delay befor executing next work.
     */
    private long delayBeforNxtStart = 5;

    private TimeUnit tu = TimeUnit.MILLISECONDS;
    
    private boolean simOnGzStk = true;
    
    public String resMsg = "Initial msg for work SimTrader.";
    
    static Connection con = null;
    SimStockDriver ssd = new SimStockDriver();

    public SimTrader(long id, long dbn, boolean onlyGzStk) throws Exception {
        initDelay = id;
        delayBeforNxtStart = dbn;
        simOnGzStk = onlyGzStk;
    }

    static public void main(String[] args) throws Exception {
        SimTrader st = new SimTrader(0, 0, true);
        st.run();
    }

    public void run() {
        // SimStockDriver.addStkToSim("000727");
        Connection con = DBManager.getConnection();
        String sql = "";
        if (simOnGzStk) {
            sql = "select * from stk where gz_flg = 1 ";
        }
        else {
            sql = "select * from stk where rownum < 100";
        }

        ArrayList<String> stks = new ArrayList<String>();
        try {
            Statement stm = con.createStatement();
            int batch = 0;
            int batcnt = 0;
            ResultSet rs = stm.executeQuery(sql);
            while (rs.next()) {
                stks.add(rs.getString("id"));
                batch++;
                if (batch == 50) {
                    batcnt++;
                    log.info("Now have 10 stocks to sim, start a worker for it.");
                    SimWorker sw;
                    try {
                        sw = new SimWorker(0, 0, "SimWorker" + batcnt);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        return;
                    }
                    sw.addStksToWorker(stks);
                    stks.clear();
                    WorkManager.submitWork(sw);
                    batch = 0;
                }
            }

            if (batch > 0) {
                batcnt++;
                log.info("Now have " + batch + " stocks to sim, start a worker for it.");
                SimWorker sw;
                try {
                    sw = new SimWorker(0, 0, "SimWorker" + batcnt);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return;
                }
                sw.addStksToWorker(stks);
                WorkManager.submitWork(sw);
                batch = 0;
            }
            rs.close();
            stm.close();
            con.close();
            log.info("Now end simulate trading.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            WorkManager.shutdownWorksAfterFinish();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public long getDelayBeforeNxt() {
        // TODO Auto-generated method stub
        return delayBeforNxtStart;
    }

    @Override
    public long getInitDelay() {
        // TODO Auto-generated method stub
        return initDelay;
    }

    @Override
    public TimeUnit getTimeUnit() {
        // TODO Auto-generated method stub
        return tu;
    }

    @Override
    public String getWorkName() {
        // TODO Auto-generated method stub
        return "SimTrader";
    }

    @Override
    public String getWorkResult() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isCycleWork() {
        // TODO Auto-generated method stub
        return false;
    }

}