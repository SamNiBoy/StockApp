package com.sn.work.task;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.mail.reporter.RecommandStockObserverable;
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.selector.stock.AvgClsPriStockSelector;
import com.sn.trade.strategy.selector.stock.DefaultStockSelector;
import com.sn.trade.strategy.selector.stock.IStockSelector;
import com.sn.trade.strategy.selector.stock.KeepGainStockSelector;
import com.sn.trade.strategy.selector.stock.LimitClsPriStockSelector;
import com.sn.trade.strategy.selector.stock.PriceStockSelector;
import com.sn.trade.strategy.selector.stock.QtyEnableTradeStockSelector;
import com.sn.trade.strategy.selector.stock.StddevStockSelector;
import com.sn.trade.strategy.selector.stock.TopGainStockSelector;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

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
	
	static List<Stock> stocksWaitForMail = new LinkedList<Stock>();
	
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
		selectors.add(new PriceStockSelector());
		selectors.add(new StddevStockSelector());
		selectors.add(new LimitClsPriStockSelector());
		//selectors.add(new QtyEnableTradeStockSelector());
		//selectors.add(new AvgClsPriStockSelector());
//		selectors.add(new ClosePriceTrendStockSelector());
//		selectors.add(new TopGainStockSelector());
//		selectors.add(new KeepLostStockSelector());
	}

	public void run() {
		// TODO Auto-generated method stub
		boolean mandatory_pass_flg = true;
		boolean suggest_flg = false;

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
			Map<String, Stock> stks = StockMarket.getStocks();
			int tryCnt = 10;
			boolean tryHarderCriteria = false;
			while(tryCnt-- > 0) {
			    for (String stk : stks.keySet()) {
			    	Stock s = stks.get(stk);
			    	for (IStockSelector slt : selectors) {
			    		if (slt.isMandatoryCriteria() && !slt.isTargetStock(s, null)) {
			    			mandatory_pass_flg = false;
			    			break;
			    		}
			    		else if (slt.isMandatoryCriteria()){
			    			mandatory_pass_flg = true;
			    			
			    			// If the mandatory criteria also an OR criteria, then enough to suggest this stock.
			    			if (slt.isORCriteria()) {
			    				suggest_flg = true;
			    			}
			    		}
			    	}
			    	if (mandatory_pass_flg) {
			    		for (IStockSelector slt : selectors) {
			    			if (suggest_flg) {
			    				log.info("Mandatory criteria also Or criteria matched, suggest stock directly!");
			    				break;
			    			}
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
			    			    log.info("Non mandatory criteria not matched, continue next criteira.");
			    				suggest_flg = false;
			    				continue;
			    			}
			    		}
			    		if (suggest_flg) {
			    			stocksWaitForMail.add(s);
			    		}
			    		suggest_flg = false;
			    	}
			    	mandatory_pass_flg = false;
			    }
			    if (stocksWaitForMail.size() == 0) {
			    	log.info("stocksWaitForMail is empty, tryHarderCriteria set to false");
			    	tryHarderCriteria = false;
			    }
			    else if (stocksWaitForMail.size() > 10) {
			    	log.info("stocksWaitForMail has " + stocksWaitForMail.size() + " which is more than 10, tryHarderCriteria set to true");
			    	tryHarderCriteria = true;
			    	stocksWaitForMail.clear();
			    }
			    else {
			    	for (Stock s2 : stocksWaitForMail) {
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

	private void suggestStock(Stock s) {
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
				sql = "select gz_flg, sell_mode_flg, suggested_by from usrStk where openID = '" + openID + "' and id = '" + s.getID() + "'";
				Statement stm2 = con.createStatement();
				ResultSet rs2 = stm2.executeQuery(sql);
				sql = "";
				if (rs2.next()) {
					if (rs2.getLong("gz_flg") == 0) {
						if (rs2.getLong("sell_mode_flg") == 0) {
						    sql = "update usrStk set gz_flg = 1, suggested_by = '" + STConstants.SUGGESTED_BY_FOR_SYSTEMUPDATE + "', suggested_sellmode_by = '', add_dt = sysdate where openID = '" + openID
								+ "' and id = '" + s.getID() + "'";
						}
						else {
							log.info("Stock:" + s.getID() + " sell mode:" + rs2.getLong("sell_mode_flg") + " is true, can not suggest it!");
						}
					}
					else {
						log.info("Stock gz_flg: " + rs2.getLong("gz_flg") + " already gzed, and suggested by:" + rs2.getString("suggested_by"));
					}
				} else {
					sql = "insert into usrStk values ('" + openID + "','" + s.getID() + "',1,0,'SYSTEM','',sysdate)";
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
	
	private void electStockforTrade() {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		ResultSet rs = null;
		int exiter = 0;
		int totCnt = 0;
		try {
			sql = "select * from usr where suggest_stock_enabled = 1";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			while (rs.next()) {
				String openID = rs.getString("openID");
				sql = "select id from usrStk where openID = '" + openID + "' and sell_mode_flg = 0 and suggested_by in ('" + STConstants.SUGGESTED_BY_FOR_SYSTEMGRANTED + "','" + STConstants.SUGGESTED_BY_FOR_USER + "') and gz_flg = 1 order by id";
				Statement stm2 = con.createStatement();
				ResultSet rs2 = stm2.executeQuery(sql);
				sql = "";
				while (rs2.next()) {
					totCnt++;
					String stkid = rs2.getString("id");
					if (shouldStockExitTrade(stkid)) {
						exiter++;
						putStockToSellMode(stkid);
					}
				}
				rs2.close();
				stm2.close();
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		int newStocksNum = (exiter < (STConstants.MAX_NUM_STOCKS_FOR_TRADE - totCnt) ? (STConstants.MAX_NUM_STOCKS_FOR_TRADE - totCnt) : exiter);
		
		log.info("MAX_NUM_STOCKS_FOR_TRADE:" + STConstants.MAX_NUM_STOCKS_FOR_TRADE + ", totCnt: " + totCnt + ", exiter:" + exiter + ", newStocksNum:" + newStocksNum);
		
		if (newStocksNum > 0) {
			moveStockToTrade(newStocksNum);
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
		try {
			sql = "select s.id from usrStk s, stkdlyinfo i "
				+ "where s.id = i.id "
				+ "  and s.gz_flg = 1 "
				+ "  and s.sell_mode_flg = 0 "
				+ "  and s.suggested_by in ('" + STConstants.SUGGESTED_BY_FOR_SYSTEM + "','" + STConstants.SUGGESTED_BY_FOR_SYSTEMUPDATE + "') "
				+ "  and not exists (select 'x' from stkdlyinfo i2 where i2.id = i.id and i2.dt > i.dt) "
				+ "  order by abs(i.td_cls_pri - 20) ";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			while (rs.next() && maxCnt-- > 0) {
				String id = rs.getString("id");
				sql = "update usrStk set suggested_by = '" + STConstants.SUGGESTED_BY_FOR_SYSTEMGRANTED + "',  gz_flg = 1, sell_mode_flg = 0, suggested_sellmode_by = '', add_dt = sysdate where id ='" + id + "'";
				Statement stm2 = con.createStatement();
				stm2.execute(sql);
				con.commit();
				stm2.close();
				grantCnt++;
				stockMoved.add(id);
				// Now add stock to gzStocks.
				StockMarket.addGzStocks(id);
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Iterator<Stock> it = stocksWaitForMail.iterator();
		while(it.hasNext())
		{
			Stock s = it.next();
			if (!stockMoved.contains(s.getID())) {
				log.info("remove stock:" + s.getID() + " as it is not moved for trade.");
				it.remove();
			}
		}
		log.info("Total granted:" + grantCnt + " stocks for trading.");
	}
	
	private void putStockToSellMode(String stkid) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		try {
			sql = "update usrStk set sell_mode_flg = 1, suggested_sellmode_by = 'SuggestStock', add_dt = sysdate where id = '" + stkid + "'";
			log.info(sql);
			stm = con.createStatement();
			stm.execute(sql);
			stm.close();
			
			// If the stock sold out, put gz_flg to false.
			sql =   " update usrStk u set gz_flg = 0 "
					+ "where u.sell_mode_flg = 1"
					+ "  and u.id = '" + stkid + "'"
					+ "  and (exists(select 'x' from "
                    + "                   (select sum(amount) totamt, buy_flg from tradedtl where stkid = u.id and acntid like 'ACNT%' group by buy_flg) t1, "
                    + "                   (select sum(amount) totamt, buy_flg from tradedtl where stkid = u.id and acntid like 'ACNT%' group by buy_flg) t2 "
                    + "                   where (t1.buy_flg = 1 and t2.buy_flg = 0 and t1.totamt <= t2.totamt) or t1.buy_flg is null) "
                    + "        or not exists (select 'x' from tradedtl where stkid = u.id)) ";
			log.info(sql);
			stm = con.createStatement();
			stm.execute(sql);
			stm.close();
			con.commit();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static boolean shouldStockExitTrade(String stkid) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		ResultSet rs = null;
		int lost_cnt = 0;
		int gain_cnt = 0;
		boolean should_exit = false;
		try {
			sql = "select * from tradedtl where stkid = '" + stkid + "' and acntid like 'ACNT%' order by seqnum";
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
			
			log.info("Stock:" + stkid + " lost_cnt:" + lost_cnt + " gain_cnt:" + gain_cnt);
			if (lost_cnt - gain_cnt > STConstants.MAX_LOST_TIME_BEFORE_EXIT_TRADE) {
				log.info("Lost cnt is " + STConstants.MAX_LOST_TIME_BEFORE_EXIT_TRADE + " times more than gain_cnt, should exit trade.");
				should_exit = true;
			}
			
			sql = "select 'x' from dual where exists (select 'x' from tradedtl where stkid = '" + stkid + "' and acntid like 'ACNT%' "
				+ "   and dl_dt >= sysdate - " + STConstants.MAX_DAYS_WITHOUT_TRADE_BEFORE_EXIT_TRADE + ")"
				+ " or exists (select 'y' from usrStk where gz_flg = 1 and id = '" + stkid +  "' and add_dt > sysdate - " + STConstants.MAX_DAYS_WITHOUT_TRADE_BEFORE_EXIT_TRADE + ")";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			if (!rs.next()) {
				log.info("Stock:" + stkid + STConstants.MAX_DAYS_WITHOUT_TRADE_BEFORE_EXIT_TRADE + " days without trade record, should exit trade.");
				should_exit = true;
			}
			
			rs.close();
			stm.close();
			
    		sql = "select avg(dev) dev from ("
 				   + "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, to_char(dl_dt, 'yyyy-mm-dd') atSuggestStockDay "
 				   + "  from stkdat2 "
 				   + " where id ='" + stkid + "'"
 				   + "   and to_char(dl_dt, 'yyyy-mm-dd') >= to_char(sysdate - " + STConstants.DEV_CALCULATE_DAYS + ", 'yyyy-mm-dd')"
 				   + " group by to_char(dl_dt, 'yyyy-mm-dd'))";
 		    log.info(sql);
 		    stm = con.createStatement();
 		    rs = stm.executeQuery(sql);
 		    if (rs.next()) {
 		    	 double dev = rs.getDouble("dev");
 		    	 log.info("Stock: " + stkid + "'s " + STConstants.DEV_CALCULATE_DAYS + " dev is:" + dev + ", MIN_DEV_BEFORE_EXIT_TRADE:" + STConstants.MIN_DEV_BEFORE_EXIT_TRADE);
 		    	if (dev < STConstants.MIN_DEV_BEFORE_EXIT_TRADE) {
 		    		log.info("Stock:" + stkid + " dev value:"+ dev + " reached min threshold value " +STConstants.MIN_DEV_BEFORE_EXIT_TRADE + ", should exit trade.");
 		    		should_exit = true;
 		    	}
 		    }
 		
 		    rs.close();
 		    stm.close();
		    con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return should_exit;
	}
	
	private void resetSuggestion() {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		try {
			sql = "update usrStk set gz_flg = 0 where gz_flg = 1 and suggested_by in ('" + STConstants.SUGGESTED_BY_FOR_SYSTEM + "','" + STConstants.SUGGESTED_BY_FOR_SYSTEMUPDATE + "')";
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
