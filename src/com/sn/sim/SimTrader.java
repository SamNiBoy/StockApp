package com.sn.sim;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
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
import com.sn.sim.strategy.imp.STConstants;
import com.sn.sim.strategy.imp.TradeStrategyGenerator;
import com.sn.sim.strategy.imp.TradeStrategyImp;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.trader.StockTrader;
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
    private long delayBeforNxtStart = 23 * 60 * 60;

    private TimeUnit tu = TimeUnit.MILLISECONDS;
    
    private boolean simOnGzStk = true;
    private LocalDateTime pre_sim_time = null;
    
    public String resMsg = "Initial msg for work SimTrader.";
    
    static Connection con = null;
    SimStockDriver ssd = new SimStockDriver();

    public SimTrader(long id, long dbn) {
        initDelay = id;
        delayBeforNxtStart = dbn;
    }

    static public void main(String[] args) throws Exception {
        SimTrader st = new SimTrader(0, 0);
        st.run();
    }

	private static void resetTest(boolean simgzstk) {
		String sql;
		try {
			Connection con = DBManager.getConnection();
            if (simgzstk)
            {
			    Statement stm = con.createStatement();
			    sql = "delete from tradedtl where acntid like '" + STConstants.ACNT_SIM_PREFIX + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
			    
			    stm = con.createStatement();
			    sql = "delete from tradehdr where acntid like '" + STConstants.ACNT_SIM_PREFIX + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
			    
			    stm = con.createStatement();
			    sql = "delete from CashAcnt where acntid like '" + STConstants.ACNT_SIM_PREFIX + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
                
            }
            else 
            {
			    Statement stm = con.createStatement();
			    sql = "update tradedtl set acntid = concat(acntid, '_GZ') where acntid like '" + STConstants.ACNT_SIM_PREFIX + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
			    
			    stm = con.createStatement();
			    sql = "update tradehdr set acntid = concat(acntid, '_GZ')  where acntid like '" + STConstants.ACNT_SIM_PREFIX + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
			    
			    stm = con.createStatement();
			    sql = "update CashAcnt set acntid = concat(acntid, '_GZ') where acntid like '" + STConstants.ACNT_SIM_PREFIX + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
                
            }
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void start() {
        log.info("Starting task SimTrader...");
        SimTrader st = new SimTrader(5, 30 * 60 * 1000);
	    WorkManager.submitWork(st);
	}
	
    public void run() {
    	
        LocalDateTime lt = LocalDateTime.now();
        int hr = lt.getHour();
        int mnt = lt.getMinute();
        
        int time = hr*100 + mnt;
        log.info("SimWork, time:" + time);
        DayOfWeek week = lt.getDayOfWeek();
        
        if(week.equals(DayOfWeek.SATURDAY) || week.equals(DayOfWeek.SUNDAY))
        {
            log.info("SimTrader skipped because of weekend, goto sleep 8 hours.");
            try {
                Thread.currentThread().sleep(8 * 60 * 60 * 1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }
        
        //Only run at every night after 22 clock.
        if (hr < 22)
        {
            log.info("SimTrader skipped because of hour:" + hr + " less than 22:00.");
            return;
        }
        
        LocalDateTime n = LocalDateTime.now();
        
        if (pre_sim_time != null && n.getHour() - pre_sim_time.getHour() < 12)
        {
            log.info("SimTrader previous ran at:" + pre_sim_time.toString() + " which is within 12 hours, skip run it again.");
            return;
        }
        
        
        // SimStockDriver.addStkToSim("000727");
        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        String sql = "";
        
        simOnGzStk = true;
        
        /*
         * we simulate twice:
         * 1. Simuate gz_flg stocks which should be finished quickly.
         * 2. Simulate all stocks to find best options.
         */
        
        try {
                for (int i = 0; i < 2; i++) {
                    
                    resetTest(simOnGzStk);
                    
                    if (simOnGzStk) {
                        sql = "select distinct id from usrStk where gz_flg = 1 order by id";
                    }
                    else {
                            sql = "select distinct s.id" + 
                                  "  from stkdat2 s" + 
                                  " where cur_pri < 80" + 
                                  "   and cur_pri > 5" + 
                                  "   and left(dl_dt, 10) = (select left(max(s2.dl_dt), 10) from stkdat2 s2)" +
                                  " order by id ";
                    }
                    
                    ArrayList<String> stks = new ArrayList<String>();
                    
                    ArrayList<SimWorker> workers = new ArrayList<SimWorker>();
                    
                    
                    int batch = 0;
                    int batcnt = 0;
                    
                    log.info(sql);
                    
                    stm = con.createStatement();
                    
                    rs = stm.executeQuery(sql);
                    
                    while (rs.next()) {
                        stks.add(rs.getString("id"));
                        batch++;
                        if (batch == 250) {
                            batcnt++;
                            log.info("Now have 250 stocks to sim, start a worker for batcnt:" + batcnt);
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
                    
                    rs.close();
                    stm.close();
                    
                    if (batch > 0) {
                        batcnt++;
                        log.info("Last have " + batch + " stocks to sim, start a worker for batcnt:" + batcnt);
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
                    log.info("Now end simulate trading.");
                
                    try {
                         for (SimWorker sw : workers) {
        	 	            WorkManager.waitUntilWorkIsDone(sw.getWorkName());
                         }
                         //Send mail to user for top10 best and worst.
                         sto.update();
                         
                         archiveStockData();
                         
                         pre_sim_time = LocalDateTime.now();
                         
                         simOnGzStk = false;
                         
        	         } catch (InterruptedException e) {
        	 	         // TODO Auto-generated catch block
        	 	         e.printStackTrace();
        	         }
                }
          } catch (SQLException e) {
              e.printStackTrace();
          }
          finally {
          	try {
                  con.close();
          	}
          	catch(Exception e2) {
          		log.info(e2.getMessage());
          	}
          }
       //WorkManager.shutdownWorks();

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
        return true;
    }
    
    private void archiveStockData() {
        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        
        try {
            String sql = "select count(*) cnt, max(left(dl_dt, 10)) lst, min(left(dl_dt, 10)) fst from stkdat2 where dl_dt < sysdate() - interval " + STConstants.ARCHIVE_DAYS_OLD + " day";
            stm = con.createStatement();
            
            rs = stm.executeQuery(sql);
            
            rs.next();
            
            long rowcnt = rs.getLong("cnt");
            String fstDay = rs.getString("fst");
            String lstDay = rs.getString("lst");
            
            log.info("Archiving " + rowcnt + " rows from day:" + fstDay + " to day:" + lstDay);
            
            rs.close();
            stm.close();;
            
            sql = "delete from stkdat2 where dl_dt < sysdate() - interval " + STConstants.ARCHIVE_DAYS_OLD + " day";
            stm = con.createStatement();
            
            stm.execute(sql);
            
            log.info("Archived " + rowcnt + " rows.");
        }
        catch (Exception e)
        {
            
        }
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.info("archiving data exception:" + e.getMessage());
            }
        }
    }
}