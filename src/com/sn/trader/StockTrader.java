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
import com.sn.mail.GzStockBuySellPointObserverable;
import com.sn.strategy.ITradeStrategy;
import com.sn.strategy.TradeStrategyGenerator;
import com.sn.strategy.algorithm.buypoint.QtyBuyPointSelector;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.strategy.algorithm.sellpoint.QtySellPointSelector;
import com.sn.task.suggest.selector.DefaultStockSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.Stock2.StockData;

import oracle.sql.DATE;

public class StockTrader {

	//interface vars.
    ITradeStrategy strategy = TradeStrategyGenerator.generatorDefaultStrategy(false);
    private List<StockBuySellEntry> stockTomail = new ArrayList<StockBuySellEntry>();
    private Map<String, StockBuySellEntry> lstTradeForStocks= new HashMap<String, StockBuySellEntry>();
    private GzStockBuySellPointObserverable gsbsob = new GzStockBuySellPointObserverable(stockTomail);
	private boolean sim_mode = false;
	
    private static StockTrader tradexTrader = new StockTrader(false);
    private static StockTrader simTrader = new StockTrader(true);
    
    public static StockTrader getTradexTrader()
    {
        return tradexTrader;
    }
    
    public static StockTrader getSimTrader()
    {
        return simTrader;
    }
    
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
        
        
        int stock2_queue_sz = ParamManager.getIntParam("STOCK2_QUEUE_SIZE", "TRADING");
        
        Stock2 s1 = new Stock2("600503", "abcdef", "sh", stock2_queue_sz);
        Stock2 s2 = new Stock2("000975", "hijklmn", "sz", stock2_queue_sz);
        Stock2 s3 = new Stock2("600871", "abcdef", "sh", stock2_queue_sz);
        Stock2 s4 = new Stock2("002269", "lllll", "sh", stock2_queue_sz);
        
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
            else {
                log.info("had a new trade, refresh balance information:");
                log.info("pre id:" + pre.id);
                log.info("pre name:" + pre.name);
                log.info("pre price:" + pre.price);
                log.info("pre quantity:" + pre.quantity);
                log.info("pre buy_flg:" + pre.is_buy_point);
                
                log.info("cur id:" + rc.id);
                log.info("cur name:" + rc.name);
                log.info("cur price:" + rc.price);
                log.info("cur quantity:" + rc.quantity);
                log.info("cur buy_flg:" + rc.is_buy_point);
                
                if (rc.is_buy_point == pre.is_buy_point){
                    log.info("Same direction trade, refresh lstTradeForStocks.");
                    pre.price = (pre.price * pre.quantity + rc.price * rc.quantity) / (pre.quantity + rc.quantity);
                    pre.quantity = (pre.quantity + rc.quantity);
                    lstTradeForStocks.put(pre.id, pre);
                }
                else {
                    if (pre.quantity == rc.quantity)
                    {
                        log.info("Revserse trade with same qty, clean up lstTradeForStocks.");
                        lstTradeForStocks.remove(rc.id);
                    }
                    else if (pre.quantity > rc.quantity)
                    {
                        log.info("pre quantity is  > cur quanaity, subtract cur quantity and refresh the map");
                        pre.quantity -= rc.quantity;
                        lstTradeForStocks.put(pre.id, pre);
                    }
                    else {
                        log.info("pre quantity is  < cur quanaity, update cur quantity and refresh the map");
                        rc.quantity -= pre.quantity;
                        lstTradeForStocks.put(pre.id, rc);
                    }
                }
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
    
    public Map<String, StockBuySellEntry> getLstTradeForStocks() {
        return lstTradeForStocks;
    }
}