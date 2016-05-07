package com.sn.trade;

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
import java.util.Set;
import java.util.PrimitiveIterator.OfDouble;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.CashAcntManger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.mail.reporter.GzStockBuySellPointObserverable;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.Stock2.StockData;
import com.sn.trade.strategy.ITradeStrategy;
import com.sn.trade.strategy.imp.TradeStrategyGenerator;
import com.sn.trade.strategy.selector.buypoint.IBuyPointSelector;
import com.sn.trade.strategy.selector.buypoint.QtyBuyPointSelector;
import com.sn.trade.strategy.selector.sellpoint.ISellPointSelector;
import com.sn.trade.strategy.selector.sellpoint.QtySellPointSelector;
import com.sn.trade.strategy.selector.stock.DefaultStockSelector;
import com.sn.trade.strategy.selector.stock.IStockSelector;

import oracle.sql.DATE;

public class StockTrader {

	//interface vars.
    static Set<ITradeStrategy> strategies = TradeStrategyGenerator.generatorDefaultStrategies();
    static private List<StockBuySellEntry> stockTomail = new ArrayList<StockBuySellEntry>();
    static private GzStockBuySellPointObserverable gsbsob = new GzStockBuySellPointObserverable(stockTomail);
	private boolean sim_mode = false;
	
	static Logger log = Logger.getLogger(StockTrader.class);

	static {
		// loadStocksForTrade("osCWfs-ZVQZfrjRK0ml-eEpzeop0");
	}

	public Set<ITradeStrategy> getStrategies() {
		return strategies;
	}
	public void setStrategy(ITradeStrategy s) {
	    s.enableSimulationMode(sim_mode);
	    strategies.add(s);
	}
	public StockTrader(boolean is_simulation_mode) {
		sim_mode = is_simulation_mode;
		for (ITradeStrategy s : strategies) {
		    s.enableSimulationMode(is_simulation_mode);
		}
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
			for (ITradeStrategy s : strategies)
			    s.buyStock(s2);
			
			StockBuySellEntry r22 = new StockBuySellEntry("002448", "B2", 18.2, false,
					Timestamp.valueOf(LocalDateTime.of(2016, 04, 2, 10, 30)));
			s2.getSd().getCur_pri_lst().add(18.2);
			s2.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 2, 10, 30)));
			
			Thread.currentThread().sleep(seconds_to_delay);
			for (ITradeStrategy s : strategies)
			    s.sellStock(s2);
			
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
		
		for (ITradeStrategy s : strategies)
		    s.reportTradeStat();
	}
	
	private static void resetTest() {
		String sql;
		String openID = "tester";
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

	// This method perform real trade and sending mail.
	public boolean performTrade(Stock2 s) {
	    for (ITradeStrategy stg : strategies) {
		    if (stg.performTrade(s)) {
            	StockBuySellEntry rc = stg.getLstTradeRecord(s);
            	rc.printStockInfo();
                stockTomail.add(rc);
            }
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
}
