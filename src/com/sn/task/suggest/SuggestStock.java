package com.sn.task.suggest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.mail.RecommandStockObserverable;
import com.sn.STConstants;
import com.sn.task.fetcher.StockDataFetcher;
import com.sn.task.suggest.selector.AvgClsPriStockSelector;
import com.sn.task.suggest.selector.DealMountStockSelector;
import com.sn.task.suggest.selector.DefaultStockSelector;
import com.sn.task.suggest.selector.KeepGainStockSelector;
import com.sn.task.suggest.selector.PriceShakingStockSelector;
import com.sn.task.suggest.selector.PriceStockSelector;
import com.sn.task.suggest.selector.StddevStockSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.IStockSelector;
import com.sn.task.WorkManager;
import com.sn.task.IWork;

public class SuggestStock implements IWork {

	/*
	 * Initial delay before executing work.
	 */
	long initDelay = 0;

	/*
	 * Seconds delay befor executing next work.
	 */
	long delayBeforNxtStart = 5;

	TimeUnit tu = TimeUnit.MILLISECONDS;

	int maxLstNum = 50;

	static public String resMsg = "Initial msg for work SuggestStock.";

	List<IStockSelector> selectors = new LinkedList<IStockSelector>();

	static SuggestStock self = null;
	
	static List<Stock2> stocksWaitForMail = new LinkedList<Stock2>();
	
	static RecommandStockObserverable rso = new RecommandStockObserverable();

	static Logger log = Logger.getLogger(SuggestStock.class);

	public static String getResMsg() {
		return resMsg;
	}

	public static void setResMsg(String resMsg) {
		SuggestStock.resMsg = resMsg;
	}

	static public boolean start() {
		if (self == null) {
			self = new SuggestStock(0, 30 * 60000);
			if (WorkManager.submitWork(self)) {
				resMsg = "Newly created SuggestStock and started!";
				return true;
			}
		} else if (WorkManager.canSubmitWork(self.getWorkName())) {
			if (WorkManager.submitWork(self)) {
				resMsg = "Resubmitted SuggestStock and started!";
				return true;
			}
		}
		resMsg = "Work SuggestStock is started, can not start again!";
		return false;
	}

	static public boolean stop() {
		if (self == null) {
			resMsg = "SuggestStock is null, how did you stop it?";
			return true;
		} else if (WorkManager.canSubmitWork(self.getWorkName())) {
			resMsg = "SuggestStock is stopped, but can submit again.";
			return true;
		} else if (WorkManager.cancelWork(self.getWorkName())) {
			resMsg = "SuggestStock is cancelled successfully.";
			return true;
		}
		resMsg = "SuggestStock can not be cancelled!, this is unexpected";
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SuggestStock fsd = new SuggestStock(0, 10000);
		log.info("Main exit");
		WorkManager.submitWork(fsd);
	}

	public SuggestStock(long id, long dbn) {
		initDelay = id;
		delayBeforNxtStart = dbn;
		//selectors.add(new DefaultStockSelector());
		//selectors.add(new PriceStockSelector());
		//selectors.add(new StddevStockSelector());
		//selectors.add(new DealMountStockSelector());
		selectors.add(new PriceShakingStockSelector());
		//selectors.add(new AvgClsPriStockSelector());
//		selectors.add(new ClosePriceTrendStockSelector());
		//selectors.add(new KeepGainStockSelector());
//		selectors.add(new KeepLostStockSelector());
	}

	public void run() {
		// TODO Auto-generated method stub
		boolean suggest_flg = false;
		boolean loop_nxt_stock = false;

        /*StockDataFetcher.lock.lock();
        try {
            log.info("Waiting finishedOneRoundFetch before start SuggestStock stocks...");
            StockDataFetcher.finishedOneRoundFetch.await();
        }
        catch (Exception e) {
            e.printStackTrace();
            log.error("Waiting finishedOneRondFetch before run SuggestStock errored:" + e.getMessage() + ", code:" + e.getCause());
        }
        finally {
            StockDataFetcher.lock.unlock();
        }*/
        
        LocalDateTime lt = LocalDateTime.now();
        int hr = lt.getHour();
        int mnt = lt.getMinute();
        
        int time = hr*100 + mnt;
        log.info("SuggestStock, starts now at time:" + time);
        DayOfWeek week = lt.getDayOfWeek();
        
        if(week.equals(DayOfWeek.SATURDAY) || week.equals(DayOfWeek.SUNDAY))
        {
            log.info("SuggestStock skipped because of weekend, goto sleep 8 hours.");
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
            log.info("SuggestStock skipped because of hour:" + hr + " less than 22:00.");
            return;
        }

		resetSuggestion();
		
		try {
			Map<String, Stock2> stks = StockMarket.getStocks();
			int tryCnt = 5;
			boolean tryHarderCriteria = false;
			while(tryCnt-- > 0) {
			    for (String stk : stks.keySet()) {
			    	Stock2 s = stks.get(stk);
                    
			    	/*if (!s.getID().equals("002349") && !s.getID().equals("603200") && !s.getID().equals("600882"))
			    	{
			    	    continue;
			    	}*/
			    	for (IStockSelector slt : selectors) {
			    		if (slt.isMandatoryCriteria() && !slt.isTargetStock(s, null)) {
			    			loop_nxt_stock = true;
			    			break;
			    		}
			    		else {
			    			suggest_flg = true;
			    		}
			    	}
			    	if (!loop_nxt_stock) {
			    		for (IStockSelector slt : selectors) {
			    			if (slt.isMandatoryCriteria()) {
			    				continue;
			    			}
			    			if (slt.isTargetStock(s, null)) {
			    				suggest_flg = true;
			    				if (slt.isORCriteria()) {
			    					log.info("Or criteria matched, suggest the stock:" + s.getID());
			    					break;
			    				}
			    			}
			    			else {
			    				if (slt.isORCriteria()) {
			    					log.info("Or criteria not matched, continue next criteira.");
			    					suggest_flg = false;
			    					continue;
			    				} else {
			    					suggest_flg = false;
			    					break;
			    				}
			    			}
			    		}
			    		if (suggest_flg) {
			    			stocksWaitForMail.add(s);
			    		}
			    		suggest_flg = false;
			    	}
			    	loop_nxt_stock = false;
			    }
			    if (stocksWaitForMail.size() == 0) {
			    	log.info("stocksWaitForMail is empty, tryHarderCriteria set to false");
			    	tryHarderCriteria = false;
			    }
			    else if (stocksWaitForMail.size() > 10) {
			    	log.info("stocksWaitForMail has " + stocksWaitForMail.size() + " which is more than 20, tryHarderCriteria set to true");
			    	tryHarderCriteria = true;
			    	stocksWaitForMail.clear();
			    }
			    else {
			    	for (Stock2 s2 : stocksWaitForMail) {
		    			suggestStock(s2);
			    	}
			    	electStockforTrade();
			    	if (stocksWaitForMail.size() > 0) {
			    	    rso.addStockToSuggest(stocksWaitForMail);
			    	    rso.update();
			    	    stocksWaitForMail.clear();
			    	}
			    	break;
			    }
			    log.info("Now recommand result is not good, adjust criteris to recommand");
			    for (IStockSelector slt : selectors) {
			    	slt.adjustCriteria(tryHarderCriteria);
			    }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("SuggestStock Now exit!!!");
	}

	private void suggestStock(Stock2 s) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		ResultSet rs = null;
		try {
		    String system_role_for_suggest = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", s.getID());
			sql = "select * from usr where suggest_stock_enabled = 1";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			while (rs.next()) {
				String openID = rs.getString("openID");
				sql = "select gz_flg from usrStk where openID = '" + openID + "' and id = '" + s.getID() + "'";
				Statement stm2 = con.createStatement();
				ResultSet rs2 = stm2.executeQuery(sql);
				sql = "";
				if (rs2.next()) {
					if (rs2.getLong("gz_flg") == 0) {
						sql = "update usrStk set gz_flg = 1, suggested_by = '" + system_role_for_suggest + "', mod_dt = sysdate() " + ", suggested_by_selector = '" + s.getSuggestedBy() + "', suggested_comment = '" +
					s.getSuggestedComment() + "' where openID = '" + openID
								+ "' and id = '" + s.getID() + "'";
					}
				} else {
					sql = "insert into usrStk values ('" + openID + "','" + s.getID() + "',1,0,'" + system_role_for_suggest + "','" + s.getSuggestedBy() + "','" + s.getSuggestedComment() + "', sysdate(), sysdate())";
				}
				rs2.close();
				stm2.close();

				if (sql.length() > 0) {
					log.info(sql);
					stm2 = con.createStatement();
					stm2.execute(sql);
					stm2.close();
				}
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        finally {
			try {
     			stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                log.error(e.getMessage() + " with error code: " + e.getErrorCode());
            }
            
        }
	}
	
	private void electStockforTrade() {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		ResultSet rs = null;
		int exiter = 0;
		try {
	        String system_role_for_trade = ParamManager.getStr2Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
			sql = "select * from usr where suggest_stock_enabled = 1";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			while (rs.next()) {
				String openID = rs.getString("openID");
				sql = "select id from usrStk where openID = '" + openID + "' and stop_trade_mode_flg = 0 and suggested_by in ('" + system_role_for_trade + "') and gz_flg = 1 order by id";
				Statement stm2 = con.createStatement();
				ResultSet rs2 = stm2.executeQuery(sql);
				sql = "";
				if (rs2.next()) {
					String stkid = rs2.getString("id");
					if (shouldStockExitTrade(stkid)) {
						exiter++;
						putStockToStopTradeMode(stkid);
					}
				}
				rs2.close();
				stm2.close();
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        finally {
			try {
     			stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage() + " with error code: " + e.getErrorCode());
            }
        }
		
		if (exiter > 0) {
			moveStockToTrade(exiter);
		}
	}
	
	private void moveStockToTrade(int maxCnt) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		ResultSet rs = null;
		Set<String> stockMoved = new HashSet<String>();
		int grantCnt = 0;
		if (maxCnt <= 0) {
			log.info("maxCnt must be > 0 for move stockToTrade.");
			return;
		}
        
		String system_role_for_suggest = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
		String system_role_for_trade = ParamManager.getStr2Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
		try {
			sql = "select distinct s.id from usrStk s"
				+ "where s.gz_flg = 1 "
				+ "  and s.suggested_by in ('" + system_role_for_suggest + "') "
				+ "  order by id ";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			while (rs.next() && maxCnt-- > 0) {
				String id = rs.getString("id");
				sql = "update usrStk set suggested_by = '" + system_role_for_trade + "',  gz_flg = 1, stop_trade_mode_flg = 0, add_dt = sysdate(), mod_dt = sysdate() where id ='" + id + "'";
				Statement stm2 = con.createStatement();
				stm2.execute(sql);
				con.commit();
				stm2.close();
				grantCnt++;
				stockMoved.add(id);
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage() + " with error code: " + e.getErrorCode());
            }
        }
		
		Iterator<Stock2> it = stocksWaitForMail.iterator();
		while(it.hasNext())
		{
			Stock2 s = it.next();
			if (!stockMoved.contains(s.getID())) {
				log.info("remove stock:" + s.getID() + " as it is not moved for trade.");
				it.remove();
			}
		}
		log.info("Total granted:" + grantCnt + " stocks for trading.");
	}
	
	private void putStockToStopTradeMode(String stkid) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		try {
			sql = "update usrStk set stop_trade_mode_flg = 1, mod_dt = sysdate() where id = '" + stkid + "'";
			log.info(sql);
			stm = con.createStatement();
			stm.execute(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage() + " with error code: " + e.getErrorCode());
            }
        }
	}
	
	public static boolean shouldStockExitTrade(String stkid) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		ResultSet rs = null;
		int lost_cnt = 0;
		int gain_cnt = 0;
		try {
			sql = "select * from tradedtl where stkid = '" + stkid + "' and acntid not like 'SIM%' order by seqnum";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			int pre_buy_flg = -1;
			double pre_pri = 0.0;
			while (rs.next()) {
				int buy_flg = rs.getInt("buy_flg");
				double cur_pri = rs.getDouble("price");
				
				if (pre_buy_flg != -1 && pre_buy_flg != buy_flg) {
					if (pre_buy_flg == 1 && cur_pri < pre_pri) {
						lost_cnt++;
					}
					else if (pre_buy_flg == 1 && cur_pri > pre_pri){
						gain_cnt++;
					}
					if (pre_buy_flg == 0 && cur_pri > pre_pri) {
						lost_cnt++;
					}
					else if (pre_buy_flg == 0 && cur_pri < pre_pri) {
						gain_cnt++;
					}
				}
				pre_buy_flg = buy_flg;
				pre_pri = cur_pri;
			}
			rs.close();
			stm.close();
			
            int max_lost_before_exit = ParamManager.getIntParam("MAX_LOST_TIME_BEFORE_EXIT_TRADE", "TRADING", stkid);
			log.info("Stock:" + stkid + " lost_cnt:" + lost_cnt + " gain_cnt:" + gain_cnt);
			if (lost_cnt - gain_cnt > max_lost_before_exit) {
				log.info("Lost cnt is " + max_lost_before_exit + " times more than gain_cnt, should exit trade.");
				return true;
			}
			
            int max_days_without_trade_to_exit = ParamManager.getIntParam("MAX_DAYS_WITHOUT_TRADE_BEFORE_EXIT_TRADE", "TRADING", stkid);
			sql = "select 'x' from dual where exists (select 'x' from tradedtl where stkid = '" + stkid + "' and acntid not like 'SIM%' "
				+ "   and dl_dt >= sysdate() - interval " + max_days_without_trade_to_exit + " day)"
				+ " or exists (select 'y' from usrStk where gz_flg = 1 and id = '" + stkid +  "' and add_dt > sysdate() - interval " + max_days_without_trade_to_exit + " day)";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			if (!rs.next()) {
				log.info("Stock:" + stkid + max_days_without_trade_to_exit + " days without trade record, should exit trade.");
				return true;
			}
			
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage() + " with error code: " + e.getErrorCode());
            }
        }

		return false;
	}
	
	private void resetSuggestion() {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
        
	    String system_role_for_suggest = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
	      
		try {
			sql = "update usrStk set gz_flg = 0, mod_dt = sysdate() where gz_flg = 1 and suggested_by in ('" + system_role_for_suggest + "')";
			log.info(sql);
			stm = con.createStatement();
			stm.execute(sql);
		} catch (Exception e) {
			e.printStackTrace();
            log.error(e.getMessage() + " with error code:" + e.getCause()); 
		}
        finally {
			try {
    			stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
               log.error(e.getMessage() + " with error code:" + e.getErrorCode()); 
            }
        }
	}

	public String getWorkResult() {
		return "";
	}

	public String getWorkName() {
		return "SuggestStock";
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
		return true;
	}

}
