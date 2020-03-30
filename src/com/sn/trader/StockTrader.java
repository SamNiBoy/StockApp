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
import com.sn.sim.strategy.imp.TradeStrategyGenerator;
import com.sn.sim.strategy.selector.buypoint.QtyBuyPointSelector;
import com.sn.sim.strategy.selector.sellpoint.QtySellPointSelector;
import com.sn.sim.strategy.selector.suggest.DefaultStockSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.Stock2.StockData;

import oracle.sql.DATE;

public class StockTrader {

	//interface vars.
    ITradeStrategy strategy = TradeStrategyGenerator.generatorDefaultStrategy(false);
    static private List<StockBuySellEntry> stockTomail = new ArrayList<StockBuySellEntry>();
    static private Map<String, StockBuySellEntry> lstTradeForStocks= new HashMap<String, StockBuySellEntry>();
    static private GzStockBuySellPointObserverable gsbsob = new GzStockBuySellPointObserverable(stockTomail);
	private boolean sim_mode = false;
	
	static Logger log = Logger.getLogger(StockTrader.class);

	static {
		// loadStocksForTrade("osCWfs-ZVQZfrjRK0ml-eEpzeop0");
	}

	public ITradeStrategy getStrategy() {
		return strategy;
	}
	public void setStrategy(ITradeStrategy s) {
		strategy = s;
		strategy.enableSimulationMode(sim_mode);
	}
	public StockTrader(boolean is_simulation_mode) {
		sim_mode = is_simulation_mode;
		strategy.enableSimulationMode(is_simulation_mode);
    }
    
	public static void doTest() throws Exception {

	    List<ITradeStrategy> strategies = new ArrayList<ITradeStrategy>();
        
	    strategies.addAll(TradeStrategyGenerator.generatorStrategies(false));
	    
        int seconds_to_delay = 5000;
        
        Timestamp t1 = Timestamp.valueOf(LocalDateTime.of(2016, 04, 1, 10, 30));
        Timestamp t2 = Timestamp.valueOf(LocalDateTime.of(2016, 04, 1, 10, 33));
        
        System.out.println("t2 - t1 = " + (t2.getTime() - t1.getTime()));
        
        StockTrader st = new StockTrader(true);
        
        resetTest();
        
        Stock2 s1 = new Stock2("600503", "abcdef", "sh", StockData.SMALL_SZ);
        Stock2 s2 = new Stock2("000975", "hijklmn", "sz", StockData.SMALL_SZ);
        Stock2 s3 = new Stock2("600871", "abcdef", "sh", StockData.SMALL_SZ);
        Stock2 s4 = new Stock2("002269", "lllll", "sh", StockData.SMALL_SZ);
        
        StockMarket.addGzStocks(s1);
        StockMarket.addGzStocks(s2);
        StockMarket.addGzStocks(s3);
        StockMarket.addGzStocks(s4);
    
        try {
            
            for (ITradeStrategy cs : strategies) {
                
                st.setStrategy(cs);
                
                s2.getSd().getCur_pri_lst().add(19.0);
                s2.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 1, 10, 30)));
                Thread.currentThread().sleep(seconds_to_delay);
                st.strategy.performTrade(s2);
                
                
                s2.getSd().getCur_pri_lst().add(17.0);
                s2.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 1, 10, 30)));
                Thread.currentThread().sleep(seconds_to_delay);
                st.strategy.performTrade(s2);
                
                s2.getSd().getCur_pri_lst().add(28.2);
                s2.getSd().getDl_dt_lst().add(Timestamp.valueOf(LocalDateTime.of(2016, 04, 2, 10, 30)));
                
                Thread.currentThread().sleep(seconds_to_delay);
                st.strategy.performTrade(s2);
                //st.strategy.sellStock(s2);
                
                st.strategy.reportTradeStat();
            }
            
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    
	}
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
	    doTest();
	}
	
	private static void resetTest() {
		String sql;
		String openID = "tester";
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "delete from tradedtl where acntid in (select acntid from CashAcnt where acntid like 'SIM%')";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			
			stm = con.createStatement();
			sql = "delete from tradehdr where acntid in (select acntid from CashAcnt where acntid like 'SIM%')";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			
			stm = con.createStatement();
			sql = "delete from CashAcnt where acntid like 'SIM%'";
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
        
		if (strategy.performTrade(s)) {
        	StockBuySellEntry rc = strategy.getLstTradeRecord(s);
        	rc.printStockInfo();
            stockTomail.add(rc);
            
            StockBuySellEntry pre = lstTradeForStocks.get(rc.id);
            
            if (pre == null)
            {
                log.info("Stock " + rc.id + " did new trade, add to lstTradeForStocks.");
                lstTradeForStocks.put(rc.id, rc);
            }
            else if (rc.is_buy_point == pre.is_buy_point){
                log.info("Stock " + rc.id + " did same direction trade, refresh lstTradeForStocks.");
                pre.price = (pre.price * pre.quantity + rc.price * rc.quantity) / (pre.quantity + rc.quantity);
                pre.quantity = (pre.quantity + rc.quantity);
                lstTradeForStocks.put(pre.id, pre);
            }
            else {
                log.info("Stock " + rc.id + " did revserse trade, clean up lstTradeForStocks.");
                lstTradeForStocks.remove(rc.id);
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
    
    public static Map<String, StockBuySellEntry> getLstTradeForStocks() {
        return lstTradeForStocks;
    }
}