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

import com.sn.cashAcnt.CashAcntManger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.mail.reporter.GzStockBuySellPointObserverable;
import com.sn.sim.strategy.ITradeStrategy;
import com.sn.sim.strategy.selector.buypoint.IBuyPointSelector;
import com.sn.sim.strategy.selector.buypoint.QtyBuyPointSelector;
import com.sn.sim.strategy.selector.sellpoint.ISellPointSelector;
import com.sn.sim.strategy.selector.sellpoint.QtySellPointSelector;
import com.sn.sim.strategy.selector.stock.DefaultStockSelector;
import com.sn.sim.strategy.selector.stock.IStockSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.Stock2.StockData;

import oracle.sql.DATE;

public class StockTrader implements ITradeStrategy{

	//interface vars.
    static IStockSelector stock_selector = new DefaultStockSelector();
    static IBuyPointSelector buypoint_selector = new QtyBuyPointSelector();;
    static ISellPointSelector sellpoint_selector = new QtySellPointSelector();;
    static Map<String, ICashAccount> cash_account_map = new HashMap<String, ICashAccount>();
    
    static private List<StockBuySellEntry> stockTomail = new ArrayList<StockBuySellEntry>();
    static private GzStockBuySellPointObserverable gsbsob = new GzStockBuySellPointObserverable(stockTomail);
    
    //class vars.
	static final int MAX_TRADE_TIMES_PER_STOCK = 10;
	static final int MAX_TRADE_TIMES_PER_DAY = 40;
	static final int BUY_MORE_THEN_SELL_CNT = 2;
	static List<String> tradeStocks = new ArrayList<String>();
	static String openID = "osCWfs-ZVQZfrjRK0ml-eEpzeop0";
	
	private boolean sim_mode = false;
	
	static public Map<String, LinkedList<StockBuySellEntry>> tradeRecord = new HashMap<String, LinkedList<StockBuySellEntry>>();

	static Logger log = Logger.getLogger(StockTrader.class);

	static {
		// loadStocksForTrade("osCWfs-ZVQZfrjRK0ml-eEpzeop0");
	}

	public StockTrader(boolean is_simulation_mode) {
		sim_mode = is_simulation_mode;
    }
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		int seconds_to_delay = 5000;
		
		StockTrader st = new StockTrader(true);
		
		resetTest();
		
		Stock2 s1 = new Stock2("600503", "abcdef", StockData.SMALL_SZ);
		Stock2 s2 = new Stock2("002448", "hijklmn", StockData.SMALL_SZ);
		Stock2 s3 = new Stock2("600871", "abcdef", StockData.SMALL_SZ);
		Stock2 s4 = new Stock2("002269", "lllll", StockData.SMALL_SZ);
		
		StockMarket.addGzStocks(s1);
		StockMarket.addGzStocks(s2);
		StockMarket.addGzStocks(s3);
		StockMarket.addGzStocks(s4);
	
		try {
//			StockBuySellEntry r1 = new StockBuySellEntry("600503", "A", 6.5, true,
//					Timestamp.valueOf(LocalDateTime.of(2016, 04, 1, 10, 30)));
//			s1.getSd().getCur_pri_lst().add(6.5);
//			s1.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 1, 10, 30)));
//			
//			Thread.currentThread().sleep(seconds_to_delay);
//			tradeStock(r1);
			
			StockBuySellEntry r21 = new StockBuySellEntry("002448", "B1", 19.0, true,
					Timestamp.valueOf(LocalDateTime.of(2016, 04, 1, 10, 30)));
			s2.getSd().getCur_pri_lst().add(19.0);
			s2.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 1, 10, 30)));
			Thread.currentThread().sleep(seconds_to_delay);
			st.buyStock(s2);
			
			StockBuySellEntry r22 = new StockBuySellEntry("002448", "B2", 18.2, false,
					Timestamp.valueOf(LocalDateTime.of(2016, 04, 2, 10, 30)));
			s2.getSd().getCur_pri_lst().add(18.2);
			s2.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 2, 10, 30)));
			
			Thread.currentThread().sleep(seconds_to_delay);
			st.sellStock(s2);
			
//			StockBuySellEntry r23 = new StockBuySellEntry("002431", "B3", 8.4, true,
//					Timestamp.valueOf(LocalDateTime.of(2016, 04, 2, 10, 30)));
//			s2.getSd().getCur_pri_lst().add(8.4);
//			s2.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 2, 10, 30)));
//			
//			Thread.currentThread().sleep(seconds_to_delay);
//			tradeStock(r23);
//			
//			StockBuySellEntry r24 = new StockBuySellEntry("002431", "B4", 8.3, false,
//					Timestamp.valueOf(LocalDateTime.of(2016, 04, 3, 10, 30)));
//			s2.getSd().getCur_pri_lst().add(8.3);
//			s2.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 3, 10, 30)));
//			
//			Thread.currentThread().sleep(seconds_to_delay);
//			tradeStock(r24);
//			
//			StockBuySellEntry r25 = new StockBuySellEntry("002431", "B5", 8.7, true,
//					Timestamp.valueOf(LocalDateTime.of(2016, 04, 3, 10, 30)));
//			s2.getSd().getCur_pri_lst().add(8.7);
//			s2.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 3, 10, 30)));
//			
//			Thread.currentThread().sleep(seconds_to_delay);
//			tradeStock(r25);
//			
//			StockBuySellEntry r26 = new StockBuySellEntry("002431", "B6", 8.5, false,
//					Timestamp.valueOf(LocalDateTime.of(2016, 04, 4, 10, 30)));
//			s2.getSd().getCur_pri_lst().add(8.5);
//			s2.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 4, 10, 30)));
//			
//			Thread.currentThread().sleep(seconds_to_delay);
//			tradeStock(r26);
//			
//			StockBuySellEntry r27 = new StockBuySellEntry("002431", "B7", 8.9, true,
//					Timestamp.valueOf(LocalDateTime.of(2016, 04, 5, 10, 30)));
//			s2.getSd().getCur_pri_lst().add(8.9);
//			s2.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 5, 10, 30)));
//			
//			Thread.currentThread().sleep(seconds_to_delay);
//			tradeStock(r27);
//			
//			StockBuySellEntry r28 = new StockBuySellEntry("002431", "B8", 8.3, true,
//					Timestamp.valueOf(LocalDateTime.of(2016, 04, 6, 10, 30)));
//			s2.getSd().getCur_pri_lst().add(8.3);
//			s2.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 6, 10, 30)));
//			
//			Thread.currentThread().sleep(seconds_to_delay);
//			tradeStock(r28);
//			
//			StockBuySellEntry r3 = new StockBuySellEntry("600871", "C", 9.5, false,
//					Timestamp.valueOf(LocalDateTime.of(2016, 04, 1, 10, 30)));
//			s3.getSd().getCur_pri_lst().add(9.5);
//			s3.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 1, 10, 30)));
//			Thread.currentThread().sleep(seconds_to_delay);
//			tradeStock(r3);
//			
//			StockBuySellEntry r41 = new StockBuySellEntry("002269", "D1", 9.9, true,
//					Timestamp.valueOf(LocalDateTime.of(2016, 04, 1, 10, 30)));
//			s4.getSd().getCur_pri_lst().add(9.9);
//			s4.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 1, 10, 30)));
//			
//
//			Thread.currentThread().sleep(seconds_to_delay);
//			tradeStock(r41);
//			
//			StockBuySellEntry r42 = new StockBuySellEntry("002269", "D2", 9.4, false,
//					Timestamp.valueOf(LocalDateTime.of(2016, 04, 2, 10, 30)));
//			s4.getSd().getCur_pri_lst().add(9.4);
//			s4.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 2, 10, 30)));
//			Thread.currentThread().sleep(seconds_to_delay);
//			tradeStock(r42);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		printTradeInfor();
	}
	
	private static void resetTest() {
		String sql;
		openID = "tester";
		CashAcntManger.ACNT_PREFIX = "StockTrader";
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "delete from tradedtl where acntid in (select acntid from CashAcnt where dft_acnt_flg = 1)";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			
			stm = con.createStatement();
			sql = "delete from tradehdr where acntid in (select acntid from CashAcnt where dft_acnt_flg = 1)";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			
			stm = con.createStatement();
			sql = "delete from CashAcnt where  dft_acnt_flg = 1";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			
			stm = con.createStatement();
			sql = "delete from sellbuyrecord where  openID = '" + openID + "'";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	
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
			sql = "select s.*, u.* " + "from usrStk s," + "     usr u " + "where s.openID = u.openID "
					+ "and s.gz_flg = 1 " + "and u.openID = 'osCWfs-ZVQZfrjRK0ml-eEpzeop0' " + "and u.mail is not null "
					+ "and s.suggested_by = 'osCWfs-ZVQZfrjRK0ml-eEpzeop0'" + "and u.buy_sell_enabled = 1";

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
	
	public static ICashAccount getVirtualCashAcntForStock(String stk) {
        String AcntForStk = CashAcntManger.ACNT_PREFIX + stk;
        
        // Default account is not for SimTrader.
        boolean crt_sim_acnt = false;
        if (CashAcntManger.ACNT_PREFIX.startsWith("Sim")) {
        	crt_sim_acnt = true;
        }
        
        ICashAccount acnt = cash_account_map.get(AcntForStk);
        if (acnt == null) {
        	log.info("No cashAccount for stock:" + stk + " in memory, load from db.");
            acnt = CashAcntManger.loadAcnt(AcntForStk);
            if (acnt == null) {
            	log.info("No cashAccount for stock:" + stk + " from db, create default virtual account.");
                CashAcntManger
                .crtAcnt(AcntForStk, CashAcntManger.DFT_INIT_MNY, 0.0, 0.0, CashAcntManger.DFT_SPLIT, CashAcntManger.DFT_MAX_USE_PCT, !crt_sim_acnt);
                acnt = CashAcntManger.loadAcnt(AcntForStk);
            }
            
            if (acnt != null) {
            	log.info("put the loaded/created virtual account into memory.");
            	cash_account_map.put(AcntForStk, acnt);
            }
        }
        
        return acnt;
	}

	private static boolean createBuySellRecord(Stock2 s, String openID, boolean is_buy_flg, String qtyToTrade) {
		String sql;
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "insert into SellBuyRecord values(SEQ_SBR_PK.nextval,'" + openID + "','" + s.getID() + "'," + s.getCur_pri()
					+ "," + qtyToTrade + ","
					+ (is_buy_flg ? 1 : 0)
					+ ",to_date('" + s.getDl_dt().toString().substring(0, 19) + "', 'yyyy-mm-dd hh24:mi:ss'))";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			con.commit();
			con.close();
			// Here once after we trade a stock, clear it's historic memory
			// data.
			if (s != null) {
				log.info("After trade " + s.getName() + " clear InjectedRaw Data...");
				s.getSd().clearInjectRawData();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private static boolean createBuyTradeRecord(Stock2 s, String qtyToTrade, ICashAccount ac) {
        
		int buyMnt = Integer.valueOf(qtyToTrade);
		double occupiedMny = buyMnt * s.getCur_pri();
		
        log.info("trying to buy amount:" + qtyToTrade + " with using Mny:" + occupiedMny);
        
        log.info("now start to bug stock " + s.getName()
                + " price:" + s.getCur_pri()
                + " with money: " + ac.getMaxAvaMny()
                + " buy mount:" + qtyToTrade);

        Connection con = DBManager.getConnection();
        String sql = "select case when max(d.seqnum) is null then -1 else max(d.seqnum) end maxseq from TradeHdr h " +
                "       join TradeDtl d " +
                "         on h.stkId = d.stkId " +
                "        and h.acntId = d.acntId " +
                "      where h.stkId = '" + s.getID()+ "'" +
                "        and h.acntId = '" + ac.getActId() + "'";
        int seqnum = 0;
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) {
                if (rs.getInt("maxseq") < 0) {
                    sql = "insert into TradeHdr values('" + ac.getActId() + "','"
                    + s.getID() + "',"
                    + s.getCur_pri()*buyMnt + ","
                    + buyMnt + ","
                    + s.getCur_pri() + ",to_date('" + s.getDl_dt().toLocaleString() + "','yyyy-mm-dd hh24:mi:ss'))";
                    log.info(sql);
                    Statement stm2 = con.createStatement();
                    stm2.execute(sql);
                    stm2.close();
                    stm2 = null;
                }
                seqnum = rs.getInt("maxseq") + 1;
            }
            stm.close();
            stm = con.createStatement();
            sql = "insert into TradeDtl values('"
                + ac.getActId() + "','"
                + s.getID() + "',"
                + seqnum + ","
                + s.getCur_pri() + ", "
                + buyMnt
                + ", to_date('" + s.getDl_dt().toLocaleString() + "','yyyy-mm-dd hh24:mi:ss'), 1)";
            log.info(sql);
            stm.execute(sql);
            
            //now sync used money
            double usedMny = ac.getUsedMny();
            usedMny += occupiedMny;
            ac.setUsedMny(usedMny);
            
            stm.close();
            stm = con.createStatement();
            sql = "update CashAcnt set used_mny = " + usedMny + " where acntId = '" + ac.getActId() + "'";
            stm.execute(sql);
            con.commit();
            con.close();
            return true;
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
	
	private static boolean createSellTradeRecord(Stock2 s, String qtyToTrade, ICashAccount ac) {

        log.info("now start to sell stock " + s.getName()
                + " price:" + s.getCur_pri()
                + " against CashAcount: " + ac.getActId());

        int sellableAmt = Integer.valueOf(qtyToTrade);
        
        double relasedMny = sellableAmt * s.getCur_pri();
        Connection con = DBManager.getConnection();
        int seqnum = 0;
        try {
            String sql = "select case when max(d.seqnum) is null then -1 else max(d.seqnum) end maxseq from TradeHdr h " +
                    "       join TradeDtl d " +
                    "         on h.stkId = d.stkId " +
                    "        and h.acntId = d.acntId " +
                    "      where h.stkId = '" + s.getID()+ "'" +
                    "        and h.acntId = '" + ac.getActId() + "'";
    
            Statement stm = con.createStatement();
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) {
                if (rs.getInt("maxseq") < 0) {
                    sql = "insert into TradeHdr values('" + ac.getActId() + "','"
                    + s.getID() + "',"
                    + s.getCur_pri()*sellableAmt + ","
                    + sellableAmt + ","
                    + s.getCur_pri() + ",to_date('" + s.getDl_dt().toLocaleString() + "','yyyy-mm-dd hh24:mi:ss'))";
                    log.info(sql);
                    Statement stm2 = con.createStatement();
                    stm2.execute(sql);
                    stm2.close();
                    stm2 = null;
                }
                seqnum = rs.getInt("maxseq") + 1;
            }
            rs.close();
            stm.close();
            stm = con.createStatement();
            sql = "insert into TradeDtl values( '"
                + ac.getActId() + "','"
                + s.getID() + "',"
                + seqnum + ","
                + s.getCur_pri() + ", "
                + sellableAmt
                + ", to_date('" + s.getDl_dt().toLocaleString() + "', 'yyyy-mm-dd hh24:mi:ss'), 0)";
            log.info(sql);
            stm.execute(sql);
            
            //now sync used money
            double usedMny = ac.getUsedMny();
            usedMny -= relasedMny;
            ac.setUsedMny(usedMny);
            
            stm.close();
            stm = con.createStatement();
            sql = "update CashAcnt set used_mny = used_mny - " + relasedMny + " where acntId = '" + ac.getActId() + "'";
            log.info(sql);
            stm.execute(sql);
            con.commit();
            con.close();
            return true;
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    
    }

	private static String getTradeQty(Stock2 s, boolean is_buy_flg, String openID) {
        String sql;
        String qtyToTrade = "100";
        try {
            Connection con = DBManager.getConnection();
            Statement stm = con.createStatement();
            
            sql = "select count(*) cnt, buy_flg from SellBuyRecord "
                + " where openID = '"  + openID + "'"
            	+ " and stkid ='" + s.getID() + "'"
            	+ " group by buy_flg";
            
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);
            
            int buyCnt = 0;
            int sellCnt = 0;
            int buy_flg = 0;
            
            while (rs.next()) {
            	buy_flg = rs.getInt("buy_flg");
            	if (buy_flg == 1) {
            		buyCnt = rs.getInt("cnt");
            	}
            	else {
            		sellCnt = rs.getInt("cnt");
            	}
            }
            
            rs.close();
            stm.close();
            con.close();
            
            if (is_buy_flg) {
                if (buyCnt > sellCnt) {
                	qtyToTrade = "100";
                }
                else if (s.getCur_pri() <= 10) {
                	qtyToTrade = "200";
                }
                else {
                	qtyToTrade = "100";
                }
            }
            else {
            	//is sell trade.
                if (buyCnt < sellCnt) {
                	qtyToTrade = "100";
                }
                else if (s.getCur_pri() <= 10) {
                	qtyToTrade = "200";
                }
                else {
                	qtyToTrade = "100";
                }
            }
            log.info("For stock:" + s.getName() + " will " + (is_buy_flg ? "buy " : "sell ") + qtyToTrade + " with buyCnt:" + buyCnt + " sellCnt:" + sellCnt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return qtyToTrade;
    }

	// private static boolean tradeRecord(StockBuySellEntry stk, String openID)
	// {
	// String sql;
	// try {
	// Connection con = DBManager.getConnection();
	// Statement stm = con.createStatement();
	// sql = "update SellBuyRecord set traded_flg = 1 "
	// + "where stkid = '" + stk.id
	// + "' and openID = '" + openID
	// + "' and to_char(dl_dt, 'yyyy-mm-dd hh24:mi:ss') = '" +
	// stk.dl_dt.toString().substring(0, 19) + "'";
	//
	// log.info(sql);
	// stm.execute(sql);
	// stm.close();
	// con.commit();
	// con.close();
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// return true;
	// }

	private static boolean canTradeRecord(Stock2 s, boolean is_buy_flg, String openID) {
		if (tradeStocks == null || !tradeStocks.contains(s.getID())) {
			log.info("stock " + s.getID() + " is not available for trade.");
			return false;
		}
		
		if (isInSellMode(openID, s) && is_buy_flg) {
			log.info("Stock:" + s.getID() + " is in sell mode, can not buy it!");
			return false;
		}

		if (!skipRiskCheck(openID, is_buy_flg, s)) {

			boolean overall_risk = false;
			// If recently we lost continuously overall, stop trading.
			if (stopTradeForPeriod(openID, 5)) {
				log.info("Now account " + openID + " trade unsuccess for 3 days, stop trade.");
				overall_risk = true;
			}

			// If recently we lost continuously for the stock, stop trading.
			if (stopTradeForStock(openID, s, 5)) {
				log.info("Now account " + openID + " trade unsuccess for 3 days for stock:" + s.getName()
						+ ", stop trade.");
				if (overall_risk && !is_buy_flg) {
					log.info("Now both overall risk or risk for stock:" + s.getName()
							+ " reached, howeve it's sell trade, allow it!");
				} else {
					log.info("Skip trade for stock:" + s.getName() + " after eached it's risk.");
					return false;
				}
			} else if (overall_risk && is_buy_flg) {
				log.info("No buy allowed, as overall risk reached, even single stock:" + s.getName()
						+ "'s risk is not reached yet.");
				return false;
			}
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

		LinkedList<StockBuySellEntry> rcds = tradeRecord.get(s.getID());
		if (rcds != null) {
			if (rcds.size() >= MAX_TRADE_TIMES_PER_STOCK) {
				log.info("stock " + s.getID() + " alread trade " + rcds.size() + " times, can not trade today.");
				return false;
			} else {
				int sellCnt = 0;
				int buyCnt = 0;
				for (StockBuySellEntry sd : rcds) {
					if (sd.is_buy_point) {
						buyCnt++;
					} else {
						sellCnt++;
					}
				}
				log.info("For stock " + s.getID() + " total sellCnt:" + sellCnt + ", total buyCnt:" + buyCnt);

				// We only allow buy BUY_MORE_THEN_SELL_CNT more than sell.
				if (buyCnt >= sellCnt + BUY_MORE_THEN_SELL_CNT && is_buy_flg) {
					log.info("Bought more than sold, can won't buy again.");
					return false;
				}
				// else if (stk.is_buy_point) {
				// StockBuySellEntry lst = rcds.getLast();
				// if (!lst.is_buy_point && lst.price <= stk.price) {
				// log.info("Skip buy with higher price than previous sell.");
				// return false;
				// }
				// }
				else {
					// If we just sold/buy it, and now the price has no
					// significant change, we will not do the same trade.
					StockBuySellEntry lst = rcds.getLast();
					if (is_buy_flg == lst.is_buy_point && Math.abs((s.getCur_pri() - lst.price)) / lst.price <= 0.01) {
						log.info("Just " + (is_buy_flg ? "buy" : "sell") + " this stock with similar prices "
								+ s.getCur_pri() + "/" + lst.price + ", skip same trade.");
						return false;
					}
				}
				log.info("Adding trade record for stock as: " + s.getID());
				StockBuySellEntry stk = new StockBuySellEntry(s.getID(),
                                                              s.getName(),
                                                              s.getSd().getCur_pri_lst().get(s.getSd().getCur_pri_lst().size() - 1),
                                                              is_buy_flg,
                                                              s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1));
				stk.printStockInfo();
			    rcds.add(stk);
				return true;
			}
		} else {
			log.info("Adding first trade record for stock as: " + s.getID());
			StockBuySellEntry stk = new StockBuySellEntry(s.getID(),
                    s.getName(),
                    s.getSd().getCur_pri_lst().get(s.getSd().getCur_pri_lst().size() - 1),
                    is_buy_flg,
                    s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1));
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
			sql = "select * from SellBuyRecord " + " where openID ='" + openID + "'" + "   and dl_dt >= sysdate - "
					+ days
					// + " and to_char(dl_dt, 'hh24:mi:ss') > '08:00:00'"
					// + " and to_char(dl_dt, 'hh24:mi:ss') < '16:00:00'"
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
					log.info("stock:" + stkID + " buy_flg:" + buy_flg + " with price:" + price + " and pre_buy_flg:"
							+ pre_buy_flg + " with price:" + pre_price);
					if (buy_flg == 1 && pre_buy_flg == 0) {
						if (price < pre_price) {
							incCnt++;
						} else {
							descCnt++;
						}
					} else if (buy_flg == 0 && pre_buy_flg == 1) {
						if (price > pre_price) {
							incCnt++;
						} else {
							descCnt++;
						}
					} else {
						log.info("continue buy or sell does not means success or fail trade, continue.");
					}
					pre_stkID = stkID;
					pre_price = price;
					pre_buy_flg = buy_flg;
				} else {
					pre_stkID = stkID;
					pre_price = price;
					pre_buy_flg = buy_flg;
					continue;
				}
			}

			log.info("stopTradeForPeriod, incCnt:" + incCnt + " descCnt:" + descCnt);

			// For all stocks traded, if there are 20 times fail, stop trading.
			if ((incCnt + descCnt) > 20 && descCnt * 1.0 / (incCnt + descCnt) > 0.5) {
				log.info("For passed " + days + " days, trade descCnt:" + descCnt + ", 50% more than incCnt:" + incCnt
						+ " stop trade!");
				shouldStopTrade = true;
			} else {
				log.info("For passed " + days + " days, trade descCnt:" + descCnt + ", less than 50% incCnt:" + incCnt
						+ " continue trade!");
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

	private static boolean stopTradeForStock(String openID, Stock2 s, int days) {
		String sql;
		boolean shouldStopTrade = false;
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "select * from SellBuyRecord " + " where openID ='" + openID + "'" + "   and stkid ='" + s.getID() + "'  and dl_dt >= sysdate - " + days
					// + " and to_char(dl_dt, 'hh24:mi:ss') > '08:00:00'"
					// + " and to_char(dl_dt, 'hh24:mi:ss') < '16:00:00'"
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
					log.info("stock:" + s.getName() + " buy_flg:" + buy_flg + " with price:" + price + " and pre_buy_flg:"
							+ pre_buy_flg + " with price:" + pre_price);
					if (buy_flg == 1 && pre_buy_flg == 0) {
						if (price < pre_price) {
							incCnt++;
						} else {
							descCnt++;
						}
					} else if (buy_flg == 0 && pre_buy_flg == 1) {
						if (price > pre_price) {
							incCnt++;
						} else {
							descCnt++;
						}
					} else {
						log.info("continue buy or sell does not means success or fail trade, continue.");
					}
					pre_price = price;
					pre_buy_flg = buy_flg;
				} else {
					pre_stkID = stkID;
					pre_price = price;
					pre_buy_flg = buy_flg;
					continue;
				}
			}
			
			log.info("stopTradeForStock, incCnt:" + incCnt + " descCnt:" + descCnt);

			// For specific stock, if there are 50% lost, stop trading.
			if ((incCnt + descCnt) > 5 && descCnt * 1.0 / (incCnt + descCnt) > 0.5) {
				log.info("For passed " + days + " days, for stock:" + s.getName() + " trade descCnt:" + descCnt
						+ " 50 % more than incCnt:" + incCnt + " stop trade!");
				shouldStopTrade = true;
			} else {
				log.info("For passed " + days + " days, for stock:" + s.getName() + " trade descCnt:" + descCnt
						+ " less than 50% incCnt:" + incCnt + " continue trade!");
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

	private static boolean skipRiskCheck(String openID, boolean is_buy_flg, Stock2 s) {

		String sql;
		boolean shouldSkipCheck = false;

		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();

			// get last trade record.
			sql = "select * from SellBuyRecord " + " where openID ='" + openID + "'" + "   and stkid ='" + s.getID()
					+ "' order by sb_id desc";
			log.info(sql);
			ResultSet rs = stm.executeQuery(sql);

			double price = 0.0;
			int buy_flg = 1;

			if (rs.next()) {
				price = rs.getDouble("price");
				buy_flg = rs.getInt("buy_flg");

				if (!(is_buy_flg == (buy_flg == 1))) {
					if (!is_buy_flg && buy_flg == 1) {
						if (s.getCur_pri() > price) {
							log.info("stock:" + s.getName() + " buy_flg:" + buy_flg + " with price:" + price
									+ " which is good than previous buy with price:" + price + " skip risk check.");
							shouldSkipCheck = true;
						} else {
							log.info("previously bought, now sell with lower price, need to check risk.");
							shouldSkipCheck = false;
						}
					} else {

						// Now buy, previous sold.
						if (s.getCur_pri() > price) {
							log.info("previously sold with lower price, now want buy, need to check risk.");
							shouldSkipCheck = false;
						} else {
							log.info("previously sold with high price, now buy with lower price, skip check risk.");
							shouldSkipCheck = true;
						}
					}
				} else {
					log.info("same trade direction with previous trade, still need to check risk.");
					shouldSkipCheck = false;
				}
			} else {
				log.info("No trade record, but still need check risk for total trades.");
				shouldSkipCheck = false;
			}

			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return shouldSkipCheck;
	}

	private static boolean isInSellMode(String openID, Stock2 s) {

		String sql;
		int sell_mode_flg = 0;
		
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();

			// get last trade record.
			sql = "select sell_mode_flg from usrStk " + " where id ='" + s.getID()
					+ "'";
			log.info(sql);
			ResultSet rs = stm.executeQuery(sql);

			if (rs.next()) {
				sell_mode_flg = rs.getInt("sell_mode_flg");
			    log.info("stock:" + s.getID() + "'s sell mode:" + sell_mode_flg);
			} else {
				log.info("Looks stock:" + s.getID() + " is not in usrStk usr:" + openID + ", can not judge sell mode.");
				sell_mode_flg = 0;
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sell_mode_flg == 1;
	}
	
	// This method perform real trade and sending mail.
	public boolean performTrade(Stock2 s) {
        
		if (isGoodPointtoBuy(s) && buyStock(s)) {
        	StockBuySellEntry rc = tradeRecord.get(s.getID()).getLast();
        	rc.printStockInfo();
            stockTomail.add(rc);
        }
        else if(isGoodPointtoSell(s) && sellStock(s)) {
        	StockBuySellEntry rc = tradeRecord.get(s.getID()).getLast();
        	rc.printStockInfo();
            stockTomail.add(rc);
        }
        
        if (!stockTomail.isEmpty() && !sim_mode) {
           log.info("Now sending buy/sell stock information for " + stockTomail.size());
           gsbsob.setData(stockTomail);
           gsbsob.update();
           stockTomail.clear();
           return true;
        }
        
        if (sim_mode && stockTomail.size() > 0) {
        	stockTomail.clear();
        	return true;
        }
        return false;
	}
	@Override
	public boolean isGoodStockToSelect(Stock2 s) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isGoodPointtoBuy(Stock2 s) {
		// TODO Auto-generated method stub
		ICashAccount cash_account = getVirtualCashAcntForStock(s.getID());
        if(buypoint_selector.isGoodBuyPoint(s, null)) {
        	return true;
        }
        return false;
	}

	@Override
	public boolean isGoodPointtoSell(Stock2 s) {
		// TODO Auto-generated method stub
		ICashAccount cash_account = getVirtualCashAcntForStock(s.getID());
        if (sellpoint_selector.isGoodSellPoint(s, null)) {
        	return true;
        }
        return false;
	}

	@Override
	public boolean sellStock(Stock2 s) {
		// TODO Auto-generated method stub
		loadStocksForTrade(openID);
		ICashAccount ac = getVirtualCashAcntForStock(s.getID());
		int qtb = 0;
		
		qtb = sellpoint_selector.getSellQty(s, ac);
		
		if (qtb <= 0 && !sim_mode) {
			log.info("qty to buy/sell is zero by Virtual CashAccount, switch to sellbuyrecord to get qtyToTrade.");
			qtb = Integer.valueOf(getTradeQty(s, false, openID));
		}
		else if (qtb <= 0 && sim_mode) {
			log.info("Simulation mode, can not sell before buy!");
			return false;
		}
		
		if (canTradeRecord(s, false, openID)) {
			
			String qtyToTrade = String.valueOf(qtb);
			LocalDateTime lt = LocalDateTime.now();
			int mnt = lt.getMinute();
			while(true) {
			    try {
			        // Save string like "S600503" to clipboard for sell stock.
			        String txt = "";
			        Clipboard cpb = Toolkit.getDefaultToolkit().getSystemClipboard();
			        txt = "S" + s.getID() + qtyToTrade;
			        
			        StringSelection sel = new StringSelection(txt);
			        cpb.setContents(sel, null);
			        
			        createSellTradeRecord(s, qtyToTrade, ac);
			        
			        Map<String, Stock2> sm = new HashMap<String, Stock2>();
			        sm.put(s.getID(), s);
			        ac.calProfit(s.getDl_dt().toString().substring(0, 10), sm);
			        
			        createBuySellRecord(s, openID, false, qtyToTrade);
			        break;
			    }
			    catch (Exception e) {
			    	//e.printStackTrace();
			    	log.info("TradeStock Exception:" + e.getMessage());
			    	LocalDateTime lt2 = LocalDateTime.now();
			    	if (lt2.getMinute() != mnt) {
			    		log.info("Out of one minute scope, skip trading for:" +s.getID());
			    		return false;
			    	}
			    	else {
			    		log.info("Try again tradeStock after exception happened within one minute.");
			    	}
			    }
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean buyStock(Stock2 s) {
		// TODO Auto-generated method stub
		loadStocksForTrade(openID);
		ICashAccount ac = getVirtualCashAcntForStock(s.getID());
		int qtb = 0;
		
		qtb = buypoint_selector.getBuyQty(s, ac);
		
		if (qtb <= 0) {
			log.info("qty to buy/sell is zero by Virtual CashAccount, switch to sellbuyrecord to get qtyToTrade.");
			qtb = Integer.valueOf(getTradeQty(s, true, openID));
		}
		
		if (canTradeRecord(s, true, openID)) {
			
			String qtyToTrade = String.valueOf(qtb);
			LocalDateTime lt = LocalDateTime.now();
			int mnt = lt.getMinute();
			while(true) {
			    try {
			        // Save string like "B600503" to clipboard for buy stock.
			        String txt = "";
			        Clipboard cpb = Toolkit.getDefaultToolkit().getSystemClipboard();
			        txt = "B" + s.getID() + qtyToTrade;
			        
			        StringSelection sel = new StringSelection(txt);
			        cpb.setContents(sel, null);
			        
			        createBuyTradeRecord(s, qtyToTrade, ac);
			        
			        Map<String, Stock2> sm = new HashMap<String, Stock2>();
			        sm.put(s.getID(), s);
			        ac.calProfit(s.getDl_dt().toString().substring(0, 10), sm);
			        
			        createBuySellRecord(s, openID, true, qtyToTrade);
			        break;
			    }
			    catch (Exception e) {
			    	//e.printStackTrace();
			    	log.info("TradeStock Exception:" + e.getMessage());
			    	LocalDateTime lt2 = LocalDateTime.now();
			    	if (lt2.getMinute() != mnt) {
			    		log.info("Out of one minute scope, skip trading for:" +s.getID());
			    		return false;
			    	}
			    	else {
			    		log.info("Try again tradeStock after exception happened within one minute.");
			    	}
			    }
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean calProfit(String ForDt, Map<String, Stock2> stockSet) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean reportTradeStat() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ICashAccount getCashAccount() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCashAccount(ICashAccount ca) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean initAccount() {
		// TODO Auto-generated method stub
		return false;
	}
}
