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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.mail.reporter.RecommandStockObserverable;
import com.sn.sim.strategy.selector.stock.AvgClsPriStockSelector;
import com.sn.sim.strategy.selector.stock.ClosePriceTrendStockSelector;
import com.sn.sim.strategy.selector.stock.DefaultStockSelector;
import com.sn.sim.strategy.selector.stock.IStockSelector;
import com.sn.sim.strategy.selector.stock.KeepGainStockSelector;
import com.sn.sim.strategy.selector.stock.KeepLostStockSelector;
import com.sn.sim.strategy.selector.stock.PriceStockSelector;
import com.sn.sim.strategy.selector.stock.StddevStockSelector;
import com.sn.stock.RawStockData;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.work.WorkManager;
import com.sn.work.fetcher.GzRawStockDataConsumer;
import com.sn.work.fetcher.StockDataFetcher;
import com.sn.work.itf.IWork;
import com.sn.work.monitor.MonitorGzStockData;
import com.sn.work.monitor.MonitorStockData;

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
		selectors.add(new DefaultStockSelector());
		//selectors.add(new PriceStockSelector());
		selectors.add(new StddevStockSelector());
		selectors.add(new AvgClsPriStockSelector());
//		selectors.add(new ClosePriceTrendStockSelector());
//		selectors.add(new KeepGainStockSelector());
//		selectors.add(new KeepLostStockSelector());
	}

	public void run() {
		// TODO Auto-generated method stub
		boolean suggest_flg = false;
		boolean loop_nxt_stock = false;

//        StockDataFetcher.lock.lock();
//        try {
//            log.info("Waiting before start SuggestStock stocks...");
//            StockDataFetcher.finishedOneRoundFetch.await();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//        finally {
//            StockDataFetcher.lock.unlock();
//        }

		resetSuggestion();
		
		try {
			Map<String, Stock2> stks = StockMarket.getStocks();
			int tryCnt = 10;
			boolean tryHarderCriteria = false;
			while(tryCnt-- > 0) {
			    for (String stk : stks.keySet()) {
			    	Stock2 s = stks.get(stk);
			    	for (IStockSelector slt : selectors) {
			    		if (slt.isMandatoryCriteria() && !slt.isGoodStock(s, null)) {
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
			    			if (slt.isGoodStock(s, null)) {
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
			    	rso.addStockToSuggest(stocksWaitForMail);
			    	rso.update();
			    	stocksWaitForMail.clear();
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
						sql = "update usrStk set gz_flg = 1, suggested_by = 'SYSTEMUPDATE' where openID = '" + openID
								+ "' and id = '" + s.getID() + "'";
					}
				} else {
					sql = "insert into usrStk values ('" + openID + "','" + s.getID() + "',1,'SYSTEM',sysdate)";
				}
				rs2.close();
				stm2.close();

				if (sql.length() > 0) {
					log.info(sql);
					stm2 = con.createStatement();
					stm2.execute(sql);
					con.commit();
					stm2.close();
				}
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void resetSuggestion() {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		try {
			sql = "update usrStk set gz_flg = 0 where gz_flg = 1 and suggested_by in ('SYSTEM','SYSTEMUPDATE')";
			log.info(sql);
			stm = con.createStatement();
			stm.execute(sql);
			con.commit();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
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
