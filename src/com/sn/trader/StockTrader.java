package com.sn.trader;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.StockBuySellEntry;

import oracle.sql.DATE;

public class StockTrader {

	static final int MAX_TRADE_TIMES_PER_STOCK = 10;
	static final int MAX_TRADE_TIMES_PER_DAY = 40;
	static final int BUY_MORE_THEN_SELL_CNT = 2;
	static List<String> tradeStocks = new ArrayList<String>();
	static Map<String, LinkedList<StockBuySellEntry>> tradeRecord = new HashMap<String, LinkedList<StockBuySellEntry>>();

	static Logger log = Logger.getLogger(StockTrader.class);

	static {
		//loadStocksForTrade("osCWfs-ZVQZfrjRK0ml-eEpzeop0");
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		StockBuySellEntry r1 = new StockBuySellEntry("600503", "abcdef", 6.5, true, Timestamp.valueOf(LocalDateTime.now()));
		StockBuySellEntry r2 = new StockBuySellEntry("000975", "abcdef", 9.5, false, Timestamp.valueOf(LocalDateTime.now()));
		StockBuySellEntry r3 = new StockBuySellEntry("600871", "abcdef", 9.5, false, Timestamp.valueOf(LocalDateTime.now()));
		StockBuySellEntry r4 = new StockBuySellEntry("002269", "abcdef", 9.5, true, Timestamp.valueOf(LocalDateTime.now()));
		StockBuySellEntry r5 = new StockBuySellEntry("000975", "abcdef", 9.3, true, Timestamp.valueOf(LocalDateTime.now()));
		StockBuySellEntry r6 = new StockBuySellEntry("000975", "abcdef", 9.28, true, Timestamp.valueOf(LocalDateTime.now()));

		try {
			Thread.currentThread().sleep(7000);
			tradeStock(r1);
			Thread.currentThread().sleep(7000);
			tradeStock(r2);		
			Thread.currentThread().sleep(7000);
			tradeStock(r3);
			Thread.currentThread().sleep(7000);
			tradeStock(r4);	
			Thread.currentThread().sleep(7000);
			tradeStock(r5);	
			Thread.currentThread().sleep(7000);
			tradeStock(r6);	
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		printTradeInfor();
	}
	
	public static void printTradeInfor() {
		log.info("Print real trade record as:");
		
		LinkedList<StockBuySellEntry> tmp;
		for (String id : tradeRecord.keySet()) {
			tmp = tradeRecord.get(id);
			for (StockBuySellEntry e : tmp) {
				e.printStockInfo();
			}
		}
		log.info("End print real trade record");
	}

	public static boolean loadStocksForTrade(String openID) {
		String sql;
		tradeStocks.clear();
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "select s.*, u.* "
			    + "from usrStk s,"
			    + "     usr u "
			    + "where s.openID = u.openID "
			    + "and s.gz_flg = 1 "
			    + "and u.openID = '" + openID + "' "
			    + "and u.mail is not null "
			    + "and s.suggested_by = 'osCWfs-ZVQZfrjRK0ml-eEpzeop0'"
			    + "and u.buy_sell_enabled = 1";

			log.info(sql);
			ResultSet rs = stm.executeQuery(sql);
			while (rs.next()) {
				log.info("Loading stock:" + rs.getString("id") + " for user openID:" + rs.getString("openID"));
				tradeStocks.add(rs.getString("id"));
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	public static boolean tradeStock(StockBuySellEntry stk) {
		
		String openID = "osCWfs-ZVQZfrjRK0ml-eEpzeop0";
		loadStocksForTrade(openID);

		if (saveTradeRecord(stk)) {
    		//Save string like "S600503" to clipboard for sell stock.
    		String txt = "";
    		Clipboard cpb = Toolkit.getDefaultToolkit().getSystemClipboard();
    		if (stk.is_buy_point) {
    			txt = "B" + stk.id;
    		}
    		else {
    			txt = "S" + stk.id;
    		}
    		StringSelection sel = new StringSelection(txt);
    		cpb.setContents(sel, null);
    		createRecord(stk, openID);
    		//tradeRecord(stk, openID);
			return true;
		}
		else {
			return false;
		}
	}
	
	private static boolean createRecord(StockBuySellEntry stk, String openID) {
		String sql;
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "insert into SellBuyRecord values(SEQ_SBR_PK.nextval,'"
			      + openID + "','"
				  + stk.id + "',"
			      +stk.price + ",to_date('"
				  + stk.dl_dt.toString().substring(0, 19) + "', 'yyyy-mm-dd hh24:mi:ss'),"
			      + (stk.is_buy_point ? 1 : 0) +
			      ", 1)";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			con.commit();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
//	private static boolean tradeRecord(StockBuySellEntry stk, String openID) {
//		String sql;
//		try {
//			Connection con = DBManager.getConnection();
//			Statement stm = con.createStatement();
//			sql = "update SellBuyRecord set traded_flg = 1 "
//				  + "where stkid = '" + stk.id
//				  + "' and openID = '" + openID
//				  + "' and to_char(dl_dt, 'yyyy-mm-dd hh24:mi:ss') = '" + stk.dl_dt.toString().substring(0, 19) + "'";
//
//			log.info(sql);
//			stm.execute(sql);
//			stm.close();
//			con.commit();
//			con.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return true;
//	}

	private static boolean saveTradeRecord(StockBuySellEntry stk) {
		if (tradeStocks == null || !tradeStocks.contains(stk.id)) {
			log.info("stock " + stk.id + " is not available for trade.");
			return false;
		}

		int totalCnt = 0;
		LinkedList<StockBuySellEntry> tmp;
		for (String id : tradeRecord.keySet()) {
			tmp = tradeRecord.get(id);
			totalCnt += tmp.size();
		}

		log.info("Total traded " + totalCnt + " times.");

		if (totalCnt >= MAX_TRADE_TIMES_PER_DAY) {
			log.info("Trade limit for a day is: " + MAX_TRADE_TIMES_PER_DAY + " can not trade today!");
			return false;
		}

		LinkedList<StockBuySellEntry> rcds = tradeRecord.get(stk.id);
		if (rcds != null) {
			if (rcds.size() >= MAX_TRADE_TIMES_PER_STOCK) {
				log.info("stock " + stk.id + " alread trade " + rcds.size() + " times, can not trade today.");
				return false;
			} else {
				int sellCnt = 0;
				int buyCnt = 0;
				for (StockBuySellEntry sd : rcds) {
					if (sd.is_buy_point) {
						buyCnt++;
					}
					else {
						sellCnt++;
					}
				}
				log.info("For stock " + stk.id + " total sellCnt:" + sellCnt + ", total buyCnt:" + buyCnt);
				
				// We only allow buy BUY_MORE_THEN_SELL_CNT more than sell.
				if (buyCnt >= sellCnt + BUY_MORE_THEN_SELL_CNT && stk.is_buy_point) {
					log.info("Bought more than sold, can won't buy again.");
					return false;
				}
//				else if (stk.is_buy_point) {
//					StockBuySellEntry lst = rcds.getLast();
//					if (!lst.is_buy_point && lst.price <= stk.price) {
//						log.info("Skip buy with higher price than previous sell.");
//						return false;
//					}
//				}
				else {
					// If we just sold/buy it, and now the price has no significant change, we will not do the same trade.
					StockBuySellEntry lst = rcds.getLast();
					if (stk.is_buy_point == lst.is_buy_point && Math.abs((stk.price - lst.price)) / lst.price <= 0.01) {
						log.info("Just " +(stk.is_buy_point ? "buy":"sell") + " this stock with similar prices " + stk.price + "/" + lst.price + ", skip same trade.");
						return false;
					}
				}
				log.info("Adding trade record for stock as: " + stk.id);
				stk.printStockInfo();
				rcds.add(stk);
				return true;
			}
		} else {
			log.info("Adding first sell record for stock as: " + stk.id);
			stk.printStockInfo();
			rcds = new LinkedList<StockBuySellEntry>();
			rcds.add(stk);
			tradeRecord.put(stk.id, rcds);
			return true;
		}
	}
}
