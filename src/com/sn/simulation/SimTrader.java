package com.sn.simulation;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import com.sn.cashAcnt.CashAcnt;
import com.sn.cashAcnt.CashAcntManger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.mail.MailSenderType;
import com.sn.mail.SimTraderObserverable;
import com.sn.mail.MailSenderFactory;
import com.sn.mail.SimpleMailSender;
import com.sn.mail.StockObserver;
import com.sn.mail.StockObserverable;
import com.sn.strategy.ITradeStrategy;
import com.sn.STConstants;
import com.sn.strategy.TradeStrategyGenerator;
import com.sn.strategy.TradeStrategyImp;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.suggest.SuggestStock;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.trader.StockTrader;
import com.sn.wechat.WCMsgSender;
import com.sn.task.WorkManager;
import com.sn.task.IWork;

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
    
    private CountDownLatch threadsCountDown = null;
    
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
			    sql = "delete from tradedtl where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
			    
			    stm = con.createStatement();
			    sql = "delete from tradehdr where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
			    
			    stm = con.createStatement();
			    sql = "delete from CashAcnt where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
                
            }
            else 
            {
			    Statement stm = con.createStatement();
			    sql = "update tradedtl set acntid = concat(acntid, '_GZ') where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
			    
			    stm = con.createStatement();
			    sql = "update tradehdr set acntid = concat(acntid, '_GZ')  where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
			    
			    stm = con.createStatement();
			    sql = "update CashAcnt set acntid = concat(acntid, '_GZ') where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + "%'";
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
        
        /*if(week.equals(DayOfWeek.SATURDAY) || week.equals(DayOfWeek.SUNDAY))
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
        
        //Only run at every night after 18 clock.
        if (hr < 18)
        {
            log.info("SimTrader skipped because of hour:" + hr + " less than 18:00.");
            return;
        }*/
        
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
                        //We randomly select 50% data for simulation.
                            sql = "select * from (select distinct s.id" + 
                                  "                 from stkdat2 s" + 
                                  "                where cur_pri < 80" + 
                                  "                  and cur_pri > 5" + 
                                  "                  and left(dl_dt, 10) = (select left(max(s2.dl_dt), 10) from stkdat2 s2)" +
                                  "               ) tmp" +
                                  " where floor(1+rand()*100) <= 50" +
                                  "                order by id";
                    }
                    
                    ArrayList<String> stks = new ArrayList<String>();
                    
                    ArrayList<SimWorker> workers = new ArrayList<SimWorker>();
                    
                    
                    int rowid = 0;
                    int batcnt = 0;
                    
                    log.info(sql);
                    
                    stm = con.createStatement();
                    
                    rs = stm.executeQuery(sql);
                    
                    int total_stock_cnt = 0;
                    
                    if (rs.last())
                    {
                         total_stock_cnt = rs.getRow();
                         rs.first();
                    }
                    
                    int stock_cnt_per_thread = ParamManager.getIntParam("SIM_STOCK_COUNT_FOR_EACH_THREAD", "SIMULATION");
                    int thread_cnt = ParamManager.getIntParam("SIM_THREADS_COUNT", "SIMULATION");
                    
                    int total_batch = total_stock_cnt / stock_cnt_per_thread;
                    
                    if (total_stock_cnt % stock_cnt_per_thread != 0)
                    {
                        total_batch++;
                    }
                    
                    log.info("Total " + total_stock_cnt + " stocks to simulate with each batch size:" + stock_cnt_per_thread + " and total:" + total_batch + " batches.");
                    
                    
                    while (rs.next()) {
                        
                        stks.add(rs.getString("id"));
                        
                        rowid++;
                        
                        if (rowid % stock_cnt_per_thread == 0) {
                            
                            batcnt++;
                            
                            log.info("Now have " + stock_cnt_per_thread + " stocks to sim, start a worker for batcnt:" + batcnt + " of total batch:" + total_batch);
                            
                            SimWorker sw;
                            
                            try {
                                sw = new SimWorker(0, 0, "SimWorker" + batcnt + "/" + total_batch);
                                
                                sw.addStksToWorker(stks);
                                
                                stks.clear();
                                
                                workers.add(sw);
                                
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                return;
                            }
                            
                            if (batcnt % thread_cnt == 0)
                            {
                                threadsCountDown = new CountDownLatch(workers.size());
                                
                                for (SimWorker w : workers)
                                {
                                    w.setThreadsCountDown(threadsCountDown);
                                    WorkManager.submitWork(sw);
                                }
                                
                                try {
                                    log.info("SimTrader waiting for SimWorkers finish before next round of batch");
                                    
                                    threadsCountDown.await();
                                    
                                    workers.clear();
                                    
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                    log.info("threads_to_run.await exception:" + e.getMessage());
                                }
                            }
                        }
                    }
                    
                    rs.close();
                    stm.close();
                    
                    if (rowid % stock_cnt_per_thread != 0) {
                        batcnt++;
                        log.info("Last have " + rowid + " stocks to sim, start a worker for batcnt:" + batcnt);
                        SimWorker sw;
                        try {
                            sw = new SimWorker(0, 0, "SimWorker" + batcnt + "/" + total_batch);
                            
                            sw.addStksToWorker(stks);
                            
                            workers.add(sw);
                            
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            return;
                        }
                        
                        threadsCountDown = new CountDownLatch(workers.size());
                        
                        for (SimWorker w : workers)
                        {
                            w.setThreadsCountDown(threadsCountDown);
                            WorkManager.submitWork(sw);
                        }
                        
                        try {
                            
                            log.info("last part SimTrader waiting for SimWorkers finish before next round of batch");
                            
                            threadsCountDown.await();
                            
                            workers.clear();
                            
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            log.info("last part threads_to_run.await exception:" + e.getMessage());
                        }
                    }
                    log.info("Now end simulate trading, sending mail...");
                
                    //Send mail to user for top10 best and worst.
                    sto.update();
                    
                    pre_sim_time = LocalDateTime.now();
                    
                    simOnGzStk = false;
                }
                
                log.info("now to run archive and purge...");
                
                archiveStockData();
                
                log.info("SimTrader end...");
                
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
        
        int arc_days_old = ParamManager.getIntParam("ARCHIVE_DAYS_OLD", "ARCHIVE");
        int purge_days_old = ParamManager.getIntParam("PURGE_DAYS_OLD", "ARCHIVE");
        
        String sim_acnt = ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT");
        
        try {
            String sql = "select count(*) cnt, max(left(dl_dt, 10)) lst, min(left(dl_dt, 10)) fst from stkdat2 where dl_dt < sysdate() - interval " + arc_days_old + " day";
            stm = con.createStatement();
            
            rs = stm.executeQuery(sql);
            
            rs.next();
            
            long rowcnt = rs.getLong("cnt");
            String fstDay = rs.getString("fst");
            String lstDay = rs.getString("lst");
            
            log.info("Archiving stkDat2 table with " + rowcnt + " rows from day:" + fstDay + " to day:" + lstDay);
            
            rs.close();
            stm.close();
            
            sql = "insert into arc_stkdat2 select * from stkdat2 where dl_dt < sysdate() - interval " +arc_days_old + " day";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
            
            sql = "delete from stkdat2 where dl_dt < sysdate() - interval " + arc_days_old + " day";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
            
            log.info("Archived " + rowcnt + " rows from stkDat2 table.");
            
            log.info("Archiving non simulation cashacnt table...");
            
            //For trading data: cashacnt, tradehdr, tradedtl, we only archive, but not purge.
            sql = "insert into arc_cashacnt select concat(acntid, '_', left(add_dt, 10)), init_mny, used_mny, used_mny_hrs, pft_mny, max_mny_per_trade, max_useable_pct,add_dt from cashacnt where acntid not like '" + sim_acnt + "%'";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
            sql = "delete from cashacnt where acntid not like '" + sim_acnt + "%'";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
            
            log.info("Archiving non simulation tradehdr table...");
            
            sql = "insert into arc_tradehdr select * from tradehdr where acntid not like '" + sim_acnt + "%'";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();;
            sql = "delete from tradehdr where acntid not like '" + sim_acnt + "%'";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
            
            log.info("Archiving non simulation tradedtl table...");
            
            sql = "insert into arc_tradedtl select * from tradedtl where acntid not like '" + sim_acnt + "%'";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
            sql = "delete from tradedtl where acntid not like '" + sim_acnt + "%'";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
            
            
            log.info("Purge arc_stkdat2 table which is older than " + purge_days_old + " days");
            sql = "delete from arc_stkdat2 where dl_dt < sysdate() - interval " + purge_days_old + " day";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
            
            log.info("Archive and Purge process completed!");
            
        }
        catch (Exception e)
        {
            
        }
        finally {
            try {
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.info("archiving data exception:" + e.getMessage());
            }
        }
    }
}