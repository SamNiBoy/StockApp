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
import com.sn.sim.strategy.selector.stock.DefaultStockSelector;
import com.sn.sim.strategy.selector.stock.IStockSelector;
import com.sn.sim.strategy.selector.stock.KeepGainStockSelector;
import com.sn.sim.strategy.selector.stock.KeepLostStockSelector;
import com.sn.sim.strategy.selector.stock.PriceStockSelector;
import com.sn.stock.RawStockData;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.work.WorkManager;
import com.sn.work.fetcher.GzRawStockDataConsumer;
import com.sn.work.itf.IWork;
import com.sn.work.monitor.MonitorGzStockData;
import com.sn.work.monitor.MonitorStockData;

public class SuggestStock implements IWork {

	Connection con = DBManager.getConnection();
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

	static Logger log = Logger.getLogger(SuggestStock.class);

	public static String getResMsg() {
		return resMsg;
	}

	public static void setResMsg(String resMsg) {
		SuggestStock.resMsg = resMsg;
	}

	static public boolean start() {
		if (self == null) {
			self = new SuggestStock(0, 60000);
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
		selectors.add(new PriceStockSelector());
		selectors.add(new KeepGainStockSelector());
		selectors.add(new KeepLostStockSelector());
	}

	public void run() {
		// TODO Auto-generated method stub
		boolean suggest_flg = false;
		boolean loop_nxt_stock = false;

		try {
			Map<String, Stock2> stks = StockMarket.getStocks();
			for (String stk : stks.keySet()) {
				Stock2 s = stks.get(stk);
				for (IStockSelector slt : selectors) {
					if (s == null) {
						log.info(" s is NULL!!!");
					}
					else {
						log.info("S.ID:" + s.getID());
					}
					if (slt.isMandatoryCriteria() && !slt.isGoodStock(s, null)) {
						loop_nxt_stock = true;
						break;
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
						suggestStock(s);
					}
					suggest_flg = false;
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
