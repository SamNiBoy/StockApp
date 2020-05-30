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
    
    SimStockDriver ssd = new SimStockDriver();
    private ArrayList<String> stks = new ArrayList<String>();
    private int total_stock_cnt = 0;
    private ITradeStrategy strategy = null;

    public SimTrader() {

    }

    static public void main(String[] args) throws Exception {
        SimTrader st = new SimTrader();
        st.execute(null);
    }

	public static void resetTest(boolean simgzstk) {
		String sql;
		try {
			Connection con = DBManager.getConnection();
			Statement stm = null;
            if (simgzstk)
            {
			    stm = con.createStatement();
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
			    stm = con.createStatement();
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
            
			stm = con.createStatement();
			sql = "delete from sellbuyrecord where crt_by = 'TEST'";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
    	
    	log.info("Start to run SimTrader...");
    	
    	Connection con = DBManager.getConnection();
    	String tradeDate = "";
    	String preDate = "";
    	try {
        	int sim_days = ParamManager.getIntParam("SIM_DAYS", "SIMULATION", null);
        	simOnGzStk = true;
    		Statement stm = null;
    		ResultSet rs = null;
    		String sql = "";
    		boolean disable_suggest_stock = true;
    		boolean no_enough_date = false;
    		
            //ITradeStrategy s1 = TradeStrategyGenerator.generatorStrategy1(true);
            
            //slst.add(s1);
        	
            resetTest(true);
            StockMarket.clearSimData();
            StockMarket.startSim();
    		ParamManager.loadStockParam();
    		
    		strategy = TradeStrategyGenerator.generatorStrategy(true);
    		
    		strategy.resetStrategyStatus();
            
        	for (int i=0; i<sim_days; i++) {
        		
        		int shift_days = (sim_days - i);
        		
        		no_enough_date = false;
        		
        		sql =  "select left(dl_dt, 10) dte from stkdat2 where id = '000001' group by left(dl_dt, 10) order by dte desc";
        		    
        		log.info(sql);

        		stm = con.createStatement();
        		rs = stm.executeQuery(sql);
        		
        		int cnt = shift_days;
        		
        		while(cnt > 1) {
        			if (!rs.next()) {
        				no_enough_date = true;
        				break;
        			}
        			cnt--;
        		}
        		
        		if (no_enough_date || !rs.next()) {
        			no_enough_date = true;
        		}
        		else {
        			tradeDate = rs.getString("dte");
        		}
        		
        		if (no_enough_date || !rs.next()) {
        			no_enough_date = true;
        		}
        		else {
        			preDate = rs.getString("dte");
        		}
        		
        		rs.close();
        		stm.close();
        		
        		if (no_enough_date) {
        			continue;
        		}
        		
        		log.info("Suggest stock on date:" + preDate + " and sim trading on date:" + tradeDate);
        		if (!disable_suggest_stock)
        		{
        			//SuggestStock ss = new SuggestStock(preDate, false);
        			//ss.execute(null);
        			SuggestStock.setOnDte(preDate);
        			SuggestStock.calStockParam();
        			ParamManager.refreshAllParams();
        		}
        		
        		if (!loadStocksForSim(simOnGzStk))
        			continue;
        		
        		rs.close();
        		stm.close();
        		
        		stm = con.createStatement();
        		
        		sql = "update param set intval = " + shift_days + " where name = 'SIM_SHIFT_DAYS'";
        		log.info(sql);
        		stm.execute(sql);
        		stm.close();
        		
//        		stm = con.createStatement();
        		
//        		sql = "update param set str2 = str1, str1 = concat(str1, '" + tradeDate + "_') where name = 'ACNT_SIM_PREFIX'";
//        		log.info(sql);
//        		stm.execute(sql);
//        		stm.close();
        		runSim(strategy);
        		
//        		stm = con.createStatement();
//        		sql = "update param set str1 = str2, str2 = null where name = 'ACNT_SIM_PREFIX'";
//        		log.info(sql);
//        		stm.execute(sql);
//        		stm.close();
        	}
    		
//        	simOnGzStk = false;
//        	log.info("Start to run SimTrader on all stocks...");
//            resetTest(simOnGzStk);
//        	runSim();
//        	
//            //Send mail to user for top10 best and worst.
//            sto.update();
        	
            log.info("now to run archive and purge...");
            
            archiveStockData();
            
            strategy.resetStrategyStatus();
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
    
    private boolean loadStocksForSim(boolean simOnGzStk) {
    	
    	String sql = "";
        Statement stm = null;
        ResultSet rs = null;
        Connection con = null;
        stks.clear();
        
        if (simOnGzStk) {
            sql = "select distinct id from usrStk where gz_flg = 1 and stop_trade_mode_flg = 0 and suggested_by <> 'SYSTEM_SUGGESTER' order by id";
        }
        else {
            //We randomly select 5% data for simulation.
                sql = "select * from stk " +
                      " where floor(1+rand()*100) <= 100 " +
                      "   and id not in (select id from usrStk where gz_flg = 1) " +
                      "                order by id";
        }
        
        try {
            log.info(sql);
            con = DBManager.getConnection();
            stm = con.createStatement();
            rs = stm.executeQuery(sql);
            
            if (rs.last())
            {
                 total_stock_cnt = rs.getRow();
                 rs.first();
            }
            
            if (total_stock_cnt > 0) {
                do {
                    stks.add(rs.getString("id"));
                }
                while (rs.next());
            }
            
            rs.close();
            stm.close();
            
            log.info("Total loaded:" + stks.size() + " stocks for simulation.");
        }
        catch (Exception e) {
        	log.error(e.getMessage(), e);
        }
        finally {
          	try {
                  con.close();
          	}
          	catch(Exception e2) {
          		log.info(e2.getMessage());
          	}
          }
        return total_stock_cnt > 0;
    }
	
    public void runSim(ITradeStrategy strategy)
    {
        //log.info("Before start simuation, waiting for GA Algorithm task finished."); 	
        //synchronized(Algorithm.class) {
        
        // SimStockDriver.addStkToSim("000727");

        /*
         * we simulate twice:
         * 1. Simuate gz_flg stocks which should be finished quickly.
         * 2. Simulate all stocks to find best options.
         */
        
        try {
                StockMarket.clearDegreeMap();
                
                for (int i = 0; i < 1; i++) {
                    
                    ArrayList<SimWorker> workers = new ArrayList<SimWorker>();
                    
                    int rowid = 0;
                    int batcnt = 0;
                    
                    int stock_cnt_per_thread = ParamManager.getIntParam("SIM_STOCK_COUNT_FOR_EACH_THREAD", "SIMULATION", null);
                    int thread_cnt = ParamManager.getIntParam("SIM_THREADS_COUNT", "SIMULATION", null);
                    
                    int total_batch = total_stock_cnt / stock_cnt_per_thread;
                    
                    if (total_stock_cnt % stock_cnt_per_thread != 0)
                    {
                        total_batch++;
                    }
                    
                    if (total_stock_cnt == 0) {
                    	log.info("no valid stocks to simulation, return.");
                    	return;
                    }
                    
                    log.info("Total " + total_stock_cnt + " stocks to simulate with each batch size:" + stock_cnt_per_thread + " and total:" + total_batch + " batches.");
                    
                    ArrayList<String> batch_stks = new ArrayList<String>();
                    
                    for (String stk : stks)
                    {
                    	batch_stks.add(stk);
                    	
                        rowid++;
                        
                        if (rowid % stock_cnt_per_thread == 0) {
                            
                            batcnt++;
                            
                            log.info("Now have " + stock_cnt_per_thread + " stocks to sim, start a worker for batcnt:" + batcnt + " of total batch:" + total_batch);
                            
                            SimWorker sw;
                            
                            try {
                                sw = new SimWorker(0, 0, "SimWorker" + batcnt + "/" + total_batch, strategy);
                                
                                sw.addStksToWorker(batch_stks);
                                
                                batch_stks.clear();
                                
                                workers.add(sw);
                                
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                return;
                            }
                            
                            if (batcnt % thread_cnt == 0 || (batcnt < thread_cnt && rowid == stks.size()))
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
                    }
                    
                    if (rowid % stock_cnt_per_thread != 0) {
                        batcnt++;
                        log.info("Last have " + rowid + " stocks to sim, start a worker for batcnt:" + batcnt);
                        SimWorker sw;
                        try {
                            sw = new SimWorker(0, 0, "SimWorker" + batcnt + "/" + total_batch, strategy);
                            
                            sw.addStksToWorker(batch_stks);
                            
                            batch_stks.clear();
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
            log.info("SimTrader end...");
                
          } catch (Exception e) {
              e.printStackTrace();
              log.info("What expection happened:" + e.getMessage());
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
             		"                                   sum(ac.used_mny * ac.used_mny_hrs) / case when sum(ac.used_mny)=0 then 1 else sum(ac.used_mny) end avgUsedMny_Hrs," + 
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
            
            sql = "insert into arc_tradedtl select concat(acntid, '_', left(dl_dt, 10)), stkid, seqnum, price, amount, dl_dt, buy_flg, order_id, trade_selector_name, trade_selector_comment, strategy_name from tradedtl where acntid not like '" + sim_acnt + "%'";
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