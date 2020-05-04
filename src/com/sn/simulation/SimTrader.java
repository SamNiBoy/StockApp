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
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

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
import com.sn.strategy.algorithm.ga.Algorithm;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.suggest.SuggestStock;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.trader.StockTrader;
import com.sn.wechat.WCMsgSender;
import com.sn.task.WorkManager;
import com.sn.task.IWork;

public class SimTrader implements Job{

    static Logger log = Logger.getLogger(SimTrader.class);

    SimTraderObserverable sto = new SimTraderObserverable();
    
    private boolean simOnGzStk = true;
    
    private CountDownLatch threadsCountDown = null;
    
    static Connection con = null;
    SimStockDriver ssd = new SimStockDriver();

    public SimTrader() {

    }

    static public void main(String[] args) throws Exception {
        SimTrader st = new SimTrader();
        //st.run();
    }

	private static void resetTest(boolean simgzstk) {
		String sql;
		try {
			Connection con = DBManager.getConnection();
            if (simgzstk)
            {
			    Statement stm = con.createStatement();
			    sql = "delete from tradedtl where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", null) + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
			    
			    stm = con.createStatement();
			    sql = "delete from tradehdr where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", null) + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
			    
			    stm = con.createStatement();
			    sql = "delete from CashAcnt where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", null) + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
                
            }
            else 
            {
			    Statement stm = con.createStatement();
			    sql = "update tradedtl set acntid = concat(acntid, '_GZ') where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", null) + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
			    
			    stm = con.createStatement();
			    sql = "update tradehdr set acntid = concat(acntid, '_GZ')  where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", null) + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
			    
			    stm = con.createStatement();
			    sql = "update CashAcnt set acntid = concat(acntid, '_GZ') where acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", null) + "%'";
			    log.info(sql);
			    stm.execute(sql);
			    stm.close();
                
            }
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
    	
    	log.info("Start to run SimTrader...");
    	
    	Connection con = DBManager.getConnection();
    	try {
        	int sim_days = ParamManager.getIntParam("SIM_DAYS", "SIMULATION", null);
        	simOnGzStk = true;
        	
            resetTest(simOnGzStk);
            StockMarket.clearSimData();
            StockMarket.startSim();
    		ParamManager.loadStockParam();
            
        	for (int i=0; i<sim_days; i++) {
        		
        		int shift_days = (sim_days - i);
        		
        		String system_role_for_suggest = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
				int num_stok_in_trade = ParamManager.getIntParam("NUM_STOCK_IN_TRADE", "TRADING", null);
				
				String sql = "select count(*) cnt from usrStk where stop_trade_mode_flg = 0 and gz_flg = 1 and suggested_by <> '" + system_role_for_suggest + "'";
				
				log.info(sql);
				Statement stm = con.createStatement();
				ResultSet rs = stm.executeQuery(sql);
				
				rs.next();
				
				int num_in_trading = rs.getInt("cnt");
				
				rs.close();
				stm.close();
				
				int new_num_to_trade = num_stok_in_trade - num_in_trading;
				
				log.info("Need to keep:"+ num_stok_in_trade + " to trade, with:" + num_in_trading + " in trading already, resugget more stocks.");
				
				if (new_num_to_trade > 0)
				{
        		    sql =  "select left(max(dl_dt) - interval " + shift_days + " day, 10) sd, left(max(dl_dt) - interval " + (shift_days - 1) + " day, 10) ed from stkdat2";
        		    
        		    log.info(sql);
        		    stm = con.createStatement();
        		    rs = stm.executeQuery(sql);
        		    
        		    if (rs.next() && rs.getString("sd") != null)
        		    {
        		    	String sd = rs.getString("sd");
        		    	String ed = rs.getString("ed");
        		    	SuggestStock ss = new SuggestStock(sd, ed);
        		    	ss.execute(null);
        		    }
        		    
        		    rs.close();
        		    stm.close();
				}
        		
        		stm = con.createStatement();
        		
        		sql = "update param set intval = " + shift_days + " where name = 'SIM_SHIFT_DAYS'";
        		log.info(sql);
        		stm.execute(sql);
        		stm.close();
        		runSim();
        	}
        	
        	//simOnGzStk = false;
        	//log.info("Start to run SimTrader on all stocks...");
            //resetTest(simOnGzStk);
        	//runSim();
        	
            //Send mail to user for top10 best and worst.
            sto.update();
        	
            log.info("now to run archive and purge...");
            
            archiveStockData();
            
            StockMarket.clearDegreeMap();
            StockMarket.stopSim();
            saveSimResult();

            
        	log.info("Finishing run SimTrader...");
    	}
    	catch(Exception e) {
    		log.error(e.getMessage(), e);
    	}
    	finally {
    		try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage(), e);
			}
    	}
    }
	
    public void runSim()
    {
        //log.info("Before start simuation, waiting for GA Algorithm task finished."); 	
        //synchronized(Algorithm.class) {
        
        // SimStockDriver.addStkToSim("000727");
        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        String sql = "";
        
        ArrayList<ITradeStrategy> slst = new ArrayList<ITradeStrategy>();
        
        ITradeStrategy s = TradeStrategyGenerator.generatorStrategy(true);
        
        slst.add(s);
        
        //ITradeStrategy s1 = TradeStrategyGenerator.generatorStrategy1(true);
        
        //slst.add(s1);
        
        /*
         * we simulate twice:
         * 1. Simuate gz_flg stocks which should be finished quickly.
         * 2. Simulate all stocks to find best options.
         */
        
        try {
        	ITradeStrategy strategy = null;
        	
        	for(ITradeStrategy its : slst) {
        		
        		strategy = its;
                
                StockMarket.clearDegreeMap();
                
                for (int i = 0; i < 1; i++) {
                    
                    strategy.resetStrategyStatus();
                    
                    if (simOnGzStk) {
                        sql = "select distinct id from usrStk where gz_flg = 1 and stop_trade_mode_flg = 0 order by id";
                    }
                    else {
                        //We randomly select 50% data for simulation.
                            sql = "select * from stk " +
                                  " where floor(1+rand()*100) <= 100 " +
                                  "   and id not in (select id from usrStk where gz_flg = 1) " +
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
                    
                    int stock_cnt_per_thread = ParamManager.getIntParam("SIM_STOCK_COUNT_FOR_EACH_THREAD", "SIMULATION", null);
                    int thread_cnt = ParamManager.getIntParam("SIM_THREADS_COUNT", "SIMULATION", null);
                    
                    int total_batch = total_stock_cnt / stock_cnt_per_thread;
                    
                    if (total_stock_cnt % stock_cnt_per_thread != 0)
                    {
                        total_batch++;
                    }
                    
                    log.info("Total " + total_stock_cnt + " stocks to simulate with each batch size:" + stock_cnt_per_thread + " and total:" + total_batch + " batches.");
                    
                    
                    do {
                        
                        stks.add(rs.getString("id"));
                        
                        rowid++;
                        
                        if (rowid % stock_cnt_per_thread == 0) {
                            
                            batcnt++;
                            
                            log.info("Now have " + stock_cnt_per_thread + " stocks to sim, start a worker for batcnt:" + batcnt + " of total batch:" + total_batch);
                            
                            SimWorker sw;
                            
                            try {
                                sw = new SimWorker(0, 0, "SimWorker" + batcnt + "/" + total_batch, strategy);
                                
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
                                    WorkManager.submitWork(w);
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
                    } while (rs.next());
                    
                    rs.close();
                    stm.close();
                    
                    if (rowid % stock_cnt_per_thread != 0) {
                        batcnt++;
                        log.info("Last have " + rowid + " stocks to sim, start a worker for batcnt:" + batcnt);
                        SimWorker sw;
                        try {
                            sw = new SimWorker(0, 0, "SimWorker" + batcnt + "/" + total_batch, strategy);
                            
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
                            WorkManager.submitWork(w);
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
                }
        	}
                
            strategy.resetStrategyStatus();
            log.info("SimTrader end...");
                
          } catch (Exception e) {
              e.printStackTrace();
              log.info("What expection happened:" + e.getMessage());
          }
          finally {
          	try {
                  con.close();
          	}
          	catch(Exception e2) {
          		log.info(e2.getMessage());
          	}
          }
      //}
       //WorkManager.shutdownWorks();
    }
    
    public boolean saveSimResult()
    {
         Connection con = null;
         Statement stm = null;
         
         try {
        	 con = DBManager.getConnection();
             String sql = "insert into simresult select tmp.strategy_name, tmp.dl_dt, count(distinct(ac.acntid)) ACNTCNT, " + 
             		"                                   sum(ac.used_mny) totUsedMny," + 
             		"                                   sum(ac.used_mny * ac.used_mny_hrs) / sum(ac.used_mny) avgUsedMny_Hrs," + 
             		"        				           avg(ac.pft_mny) avgPft, " + 
             		"        				           sum(ac.pft_mny) totPft, " + 
             		"                                   sum(h.commission_mny) total_commission_mny," + 
             		"                                   sum(ac.pft_mny)  - sum(h.commission_mny) netPft," + 
             		"                                   (sum(ac.pft_mny)  - sum(h.commission_mny)) / (sum(h.total_amount) /2.0) funPft," + 
             		"        				           sum(tmp.buyCnt) buyCnt," + 
             		"        				           sum(tmp.sellCnt) sellCnt," + 
             		"        				           sysdate()" +
             		"        				     from cashacnt ac, " + 
             		"        				          (select sum(case when td.buy_flg = 1 then 1 else 0 end) buyCnt, " + 
             		"        				                  sum(case when td.buy_flg  = 1 then 0 else 1 end) sellCnt, " + 
             		"        				                  th.acntid, " + 
             		"        				                  td.strategy_name, " + 
             		"        				                  left(td.dl_dt, 10) dl_dt " + 
             		"        				             from tradehdr th, tradedtl td" + 
             		"        			                where th.acntid = td.acntid " + 
             		"        			                  and th.stkid = td.stkid " + 
             		"        			                 group by th.acntid, td.strategy_name, left(td.dl_dt, 10)) tmp, " + 
             		"                                  TradeHdr h" + 
             		"        			         where ac.acntid = tmp.acntid" + 
             		"                               and ac.acntid = h.acntid " + 
             		"        			           and ac.acntid like 'SIM%'" + 
             		"        			           group by tmp.strategy_name, tmp.dl_dt";
             log.info(sql);
             stm = con.createStatement();
             stm.execute(sql);
         }
         catch (Exception e)
         {
             log.error(e.getMessage(),e);
         }
         finally {
             try {
                 stm.close();
                 con.close();
             } catch (SQLException e) {
                 // TODO Auto-generated catch block
                 e.printStackTrace();
                 log.error(e.getMessage(), e);
             }
         }
         return true;
     }
    
    private void archiveStockData() {
        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        
        int arc_days_old = ParamManager.getIntParam("ARCHIVE_DAYS_OLD", "ARCHIVE", null);
        int purge_days_old = ParamManager.getIntParam("PURGE_DAYS_OLD", "ARCHIVE", null);
        
        String sim_acnt = ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", null);
        String sql;
        long rowcnt = 0;
        
        try {
            sql = "select count(*) cnt, max(left(dl_dt, 10)) lst, min(left(dl_dt, 10)) fst from stkdat2 where dl_dt < sysdate() - interval " + arc_days_old + " day";
            stm = con.createStatement();
            
            rs = stm.executeQuery(sql);
            
            rs.next();
            
            rowcnt = rs.getLong("cnt");
            String fstDay = rs.getString("fst");
            String lstDay = rs.getString("lst");
            
            log.info("Archiving stkDat2 table with " + rowcnt + " rows from day:" + fstDay + " to day:" + lstDay);
            
            rs.close();
            stm.close();
        }
        catch(Exception e)
        {
            log.info("check rowcnt exception:" + e.getMessage());
        }
            
        try {
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
        }
        catch (Exception e)
        {
            log.info("insert/delete stkdat2 exception:" + e.getMessage());
        }
        
        try {
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
        }
        catch (Exception e)
        {
            log.info("insert/delete cashacnt exception:" + e.getMessage());
        }
        
        
        try {
            log.info("Archiving non simulation tradehdr table...");
            
            sql = "insert into arc_tradehdr select concat(acntid, '_', left(add_dt, 10)), stkid, in_hand_stk_mny,in_hand_qty, in_hand_stk_price,total_amount, com_rate, commission_mny, add_dt from tradehdr where acntid not like '" + sim_acnt + "%'";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();;
            sql = "delete from tradehdr where acntid not like '" + sim_acnt + "%'";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
        }
        catch (Exception e)
        {
             log.info(" insert/delete tradehdr exception:" + e.getMessage());
        }            
            
            
        try {
            log.info("Archiving non simulation tradedtl table...");
            
            sql = "insert into arc_tradedtl select concat(acntid, '_', left(dl_dt, 10)), stkid, seqnum, price, amount, dl_dt, buy_flg, order_id, trade_selector_name, trade_selector_comment from tradedtl where acntid not like '" + sim_acnt + "%'";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
            sql = "delete from tradedtl where acntid not like '" + sim_acnt + "%'";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
        }
        catch (Exception e)
        {
           log.info("insert/delete tradedtl exception:" + e.getMessage());
        }        
        
        
        try {
            log.info("Purge arc_stkdat2 table which is older than " + purge_days_old + " days");
            sql = "delete from arc_stkdat2 where dl_dt < sysdate() - interval " + purge_days_old + " day";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            stm.close();
            
            
        }
        catch (Exception e)
        {
           log.info("delete arc_stddat2 exception:" + e.getMessage());
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
        log.info("Archive and Purge process completed!");
    }
}