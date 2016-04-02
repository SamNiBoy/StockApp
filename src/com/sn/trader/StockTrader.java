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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;

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

		int seconds_to_delay = 1;
		StockBuySellEntry r1 = new StockBuySellEntry("600503", "abcdef", 6.5, true, Timestamp.valueOf(LocalDateTime.now()));
		StockBuySellEntry r2 = new StockBuySellEntry("000975", "abcdef", 9.0, false, Timestamp.valueOf(LocalDateTime.now()));
		StockBuySellEntry r3 = new StockBuySellEntry("600871", "abcdef", 9.5, false, Timestamp.valueOf(LocalDateTime.now()));
		StockBuySellEntry r4 = new StockBuySellEntry("002269", "abcdef", 9.9, true, Timestamp.valueOf(LocalDateTime.now()));
		StockBuySellEntry r5 = new StockBuySellEntry("002269", "abcdef", 9.4, false, Timestamp.valueOf(LocalDateTime.now()));
		StockBuySellEntry r6 = new StockBuySellEntry("000975", "abcdef", 9.2, false, Timestamp.valueOf(LocalDateTime.now()));

		try {
			Thread.currentThread().sleep(seconds_to_delay);
			tradeStock(r1);
			Thread.currentThread().sleep(seconds_to_delay);
			tradeStock(r2);		
			Thread.currentThread().sleep(seconds_to_delay);
			tradeStock(r3);
			Thread.currentThread().sleep(seconds_to_delay);
			tradeStock(r4);	
			Thread.currentThread().sleep(seconds_to_delay);
			tradeStock(r5);	
			Thread.currentThread().sleep(seconds_to_delay);
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

		if (saveTradeRecord(stk, openID)) {
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
			//Here once after we trade a stock, clear it's historic memory data.
			ConcurrentHashMap<String, Stock2> chm = StockMarket
	        .getGzstocks();
			Stock2 s = chm.get(stk.id);
			if (s != null) {
				log.info("After trade " + s.getName() + " clear InjectedRaw Data...");
				s.getSd().clearInjectRawData();
			}
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

	private static boolean saveTradeRecord(StockBuySellEntry stk, String openID) {
		if (tradeStocks == null || !tradeStocks.contains(stk.id)) {
			log.info("stock " + stk.id + " is not available for trade.");
			return false;
		}

		// If recently we lost continuously overall, stop trading.
		if (stopTradeForPeriod(openID, 3)) {
			log.info("Now account " + openID + " trade unsuccess for 3 days, stop trade.");
			return false;
		}
		
		// If recently we lost continuously for the stock, stop trading.
		if (stopTradeForStock(openID, stk, 3)) {
			log.info("Now account " + openID + " trade unsuccess for 3 days for stock:" + stk.name + ", stop trade.");
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
	
	private static boolean stopTradeForPeriod(String openID, int days) {
		String sql;
		boolean shouldStopTrade = false;
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql =   "select * from SellBuyRecord "
				  + " where openID ='" + openID + "'"
			      + "   and dl_dt >= sysdate - " + days
//			      + "   and to_char(dl_dt, 'hh24:mi:ss') > '08:00:00'"
//			      + "   and to_char(dl_dt, 'hh24:mi:ss') < '16:00:00'"
				  + " order by stkid, sb_id";
			log.info(sql);
			ResultSet rs = stm.executeQuery(sql);
			
			String pre_stkID = "";
			String stkID = "";
			int incCnt = 0;
			int descCnt = 0;
			
			double pre_price = 0.0;
			double price = 0.0;
			
			int pre_buy_flg = 1;
			int buy_flg = 1;
			
			while (rs.next()) {
				
				stkID = rs.getString("stkid");
				price = rs.getDouble("price");
				buy_flg = rs.getInt("buy_flg");
				
				if (pre_stkID.length() > 0 && stkID.equals(pre_stkID)) {
					log.info("stock:" + stkID + " buy_flg:" + buy_flg + " with price:" + price + " and pre_buy_flg:" + pre_buy_flg + " with price:" + pre_price);
					if (buy_flg == 1 && pre_buy_flg == 0) {
						if (price < pre_price) {
							incCnt++;
						}
						else {
							descCnt++;
						}
					}
					else if (buy_flg == 0 && pre_buy_flg == 1) {
						if (price > pre_price) {
							incCnt++;
						}
						else {
							descCnt++;
						}
					}
					else {
						log.info("continue buy or sell does not means success or fail trade, continue.");
					}
					pre_stkID = stkID;
					pre_price = price;
					pre_buy_flg = buy_flg;
				}
				else {
					pre_stkID = stkID;
					pre_price = price;
					pre_buy_flg = buy_flg;
					continue;
				}
			}
			
			// For all stocks traded, if there are 20 times fail, stop trading.
			if ((incCnt + descCnt) > 20 && descCnt * 1.0 / (incCnt + descCnt) > 0.5) {
				log.info("For passed " + days + " days, trade descCnt:" + descCnt + ", 50% more than incCnt:" + incCnt + " stop trade!");
				shouldStopTrade = true;
			}
			else {
				log.info("For passed " + days + " days, trade descCnt:" + descCnt + ", less than 50% incCnt:" + incCnt + " continue trade!");
				if ((incCnt + descCnt) <= 20) {
					log.info("because total trade times is less or equal than 20!");
				}
				shouldStopTrade = false;
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return shouldStopTrade;
	}
	
	private static boolean stopTradeForStock(String openID, StockBuySellEntry stk, int days) {
		String sql;
		boolean shouldStopTrade = false;
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql =   "select * from SellBuyRecord "
				  + " where openID ='" + openID + "'"
				  + "   and stkid ='" + stk.id
			      + "'  and dl_dt >= sysdate - " + days
//			      + "   and to_char(dl_dt, 'hh24:mi:ss') > '08:00:00'"
//			      + "   and to_char(dl_dt, 'hh24:mi:ss') < '16:00:00'"
				  + " order by stkid, sb_id";
			log.info(sql);
			ResultSet rs = stm.executeQuery(sql);
			
			String pre_stkID = "";
			String stkID = "";
			int incCnt = 0;
			int descCnt = 0;
			
			double pre_price = 0.0;
			double price = 0.0;
			
			int pre_buy_flg = 1;
			int buy_flg = 1;
			
			while (rs.next()) {
				
				stkID = rs.getString("stkid");
				price = rs.getDouble("price");
				buy_flg = rs.getInt("buy_flg");
				
				if (pre_stkID.length() > 0) {
					log.info("stock:" + stk.name + " buy_flg:" + buy_flg + " with price:" + price + " and pre_buy_flg:" + pre_buy_flg + " with price:" + pre_price);
					if (buy_flg == 1 && pre_buy_flg == 0) {
						if (price < pre_price) {
							incCnt++;
						}
						else {
							descCnt++;
						}
					}
					else if (buy_flg == 0 && pre_buy_flg == 1) {
						if (price > pre_price) {
							incCnt++;
						}
						else {
							descCnt++;
						}
					}
					else {
						log.info("continue buy or sell does not means success or fail trade, continue.");
					}
					pre_price = price;
					pre_buy_flg = buy_flg;
				}
				else {
					pre_stkID = stkID;
					pre_price = price;
					pre_buy_flg = buy_flg;
					continue;
				}
			}
			
			// For specific stock, if there are 50% lost, stop trading.
			if ((incCnt + descCnt) > 5 && descCnt * 1.0 / (incCnt + descCnt) > 0.5) {
				log.info("For passed " + days + " days, for stock:" + stk.name + " trade descCnt:" + descCnt + " 50 % more than incCnt:" + incCnt + " stop trade!");
				shouldStopTrade = true;
			}
			else {
				log.info("For passed " + days + " days, for stock:" + stk.name + " trade descCnt:" + descCnt + " less than 50% incCnt:" + incCnt + " continue trade!");
				if ((incCnt + descCnt) <= 5) {
					log.info("because total trade times is less or equal than 5!");
				}
				shouldStopTrade = false;
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return shouldStopTrade;
	}
	
}
