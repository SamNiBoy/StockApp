package com.sn.sim;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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
import com.sn.mail.reporter.SimTraderObserverable;
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
import com.sn.work.task.SuggestStock;

public class SimTrader implements IWork{

    static Logger log = Logger.getLogger(SimTrader.class);

    SimTraderObserverable sto = new SimTraderObserverable();
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

    public SimTrader(long id, long dbn, boolean onlyGzStk) {
        initDelay = id;
        delayBeforNxtStart = dbn;
        simOnGzStk = onlyGzStk;
    }

    static public void main(String[] args) throws Exception {
        SimTrader st = new SimTrader(0, 0, true);
        st.run();
    }

	private static void resetTest() {
		String sql;
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "delete from tradedtl where acntid in (select acntid from CashAcnt where dft_acnt_flg = 0)";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			
			stm = con.createStatement();
			sql = "delete from tradehdr where acntid in (select acntid from CashAcnt where dft_acnt_flg = 0)";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			
			stm = con.createStatement();
			sql = "delete from CashAcnt where  dft_acnt_flg = 0";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void start() {
        SimTrader st = new SimTrader(0, 0, false);
	    WorkManager.submitWork(st);
	}
	
    public void run() {
    	
        LocalDateTime lt = LocalDateTime.now();
        int hr = lt.getHour();
        int mnt = lt.getMinute();
        
        int time = hr*100 + mnt;
        log.info("SimWork, time:" + time);
        // Only run after 15:20 PM.
        while (time < 1530) {
            try {
				Thread.currentThread().sleep(30*60*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            lt = LocalDateTime.now();
            hr = lt.getHour();
            mnt = lt.getMinute();
            time = hr*100 + mnt;
            log.info("SimWork, time2:" + time);
        }
        
        resetTest();
        // SimStockDriver.addStkToSim("000727");
        Connection con = DBManager.getConnection();
        String sql = "";
        if (simOnGzStk) {
            sql = "select * from usrStk where gz_flg = 1 and openID ='osCWfs-ZVQZfrjRK0ml-eEpzeop0'";
        }
        else {
            sql = "select * from stk where id in "
            	+ "(select s.id from stkdlyinfo s "
            	+ "  where s.yt_cls_pri <= 20 and s.yt_cls_pri >= 10 "
            	+ "    and not exists (select 'x' from stkdlyinfo s2 where s2.id = s.id and s2.dt > s.dt))";
        }

        ArrayList<String> stks = new ArrayList<String>();
        
        ArrayList<SimWorker> workers = new ArrayList<SimWorker>();
        try {
            Statement stm = con.createStatement();
            int batch = 0;
            int batcnt = 0;
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);
            while (rs.next()) {
                stks.add(rs.getString("id"));
                batch++;
                if (batch == 250) {
                    batcnt++;
                    log.info("Now have 250 stocks to sim, start a worker for it.");
                    SimWorker sw;
                    try {
                        sw = new SimWorker(0, 0, "SimWorker" + batcnt);
                        workers.add(sw);
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
                    workers.add(sw);
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
            for (SimWorker sw : workers) {
		        WorkManager.waitUntilWorkIsDone(sw.getWorkName());
            }
            //Send mail to user for top10 best and worst.
            sto.update();
            
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