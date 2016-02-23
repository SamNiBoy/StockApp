package com.sn.sim;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import com.sn.cashAcnt.CashAcnt;
import com.sn.cashAcnt.CashAcntManger;
import com.sn.db.DBManager;
import com.sn.mail.reporter.MailSenderType;
import com.sn.mail.reporter.MailSenderFactory;
import com.sn.mail.reporter.SimpleMailSender;
import com.sn.mail.reporter.StockObserver;
import com.sn.mail.reporter.StockObserverable;
import com.sn.reporter.WCMsgSender;
import com.sn.sim.strategy.ITradeStrategy;
import com.sn.sim.strategy.imp.TradeStrategyGenerator;
import com.sn.sim.strategy.imp.TradeStrategyImp;
import com.sn.stock.Stock;
import com.sn.stock.Stock.Follower;

public class SimTrader extends Observable {

    static Logger log = Logger.getLogger(SimTrader.class);

    static Connection con = null;
    static List<ITradeStrategy> strategies = new ArrayList<ITradeStrategy>();
    static ITradeStrategy strategy = null;

    public SimTrader() throws Exception {
        this.addObserver(StockObserver.globalObs);
        strategies.addAll(TradeStrategyGenerator.generatorStrategies());
    }

    static public void main(String[] args) throws Exception {
        
        SimStockDriver.addStkToSim("002397");
        SimStockDriver.addStkToSim("600503");
        SimStockDriver.setStartEndSimDt("2016-02-17", "2016-02-19");
        
        SimStockDriver.loadStocks();
        StockObserverable.stocks = SimStockDriver.simstocks;
        SimTrader st = new SimTrader();
        
        if (!SimStockDriver.initData()) {
            log.info("can not init SimStockDriver...");
            return;
        }
        
        for (ITradeStrategy cs : strategies) {
            strategy = cs;
            int StepCnt = 0;
            log.info("Now start simulate trading...");
            while (SimStockDriver.step()) {
                log.info("Simulate step:" + (++StepCnt));
                st.run();
            }
            strategy.reportTradeStat(st);
            SimStockDriver.startOver();
        }
        
        SimStockDriver.finishStep();
        log.info("Now end simulate trading.");

    }

    public void run() {
        if (buyStock() || sellStock()) {
            reportTradeStat();
        }
    }
    
    public void update() {
    }
    
    private boolean buyStock()
    {
        log.info("strat buyStock...");
        boolean hasBoughtStock = false;

        for (String stock : StockObserverable.stocks.keySet()) {
            Stock s = StockObserverable.stocks.get(stock);
            if (strategy.isGoodStockToSelect(s) && strategy.isGoodPointtoBuy(s)) {
                if (strategy.buyStock(s)) {
                    String dt = s.getDl_dt().toString().substring(0,10);
                    strategy.calProfit(dt);
                    hasBoughtStock = true;
                }
            }
        }
        log.info("end buyStock...");
        return hasBoughtStock;
    }
    
    private boolean sellStock()
    {
        log.info("start sellStock...");
        boolean hasSoldStock = false;

        for (String stock : StockObserverable.stocks.keySet()) {
            Stock s = StockObserverable.stocks.get(stock);
            if (strategy.isGoodPointtoSell(s)) {
                if (strategy.sellStock(s)) {
                    String dt = s.getDl_dt().toString().substring(0,10);
                    strategy.calProfit(dt);
                    hasSoldStock = true;
                }
            }
        }
        log.info("end sellStock...");
        return hasSoldStock;
    }
    
    private boolean reportTradeStat() {
        log.info("reportBuySellStat...");

        boolean rs = strategy.reportTradeStat(this);
        
        return rs;
    }
}