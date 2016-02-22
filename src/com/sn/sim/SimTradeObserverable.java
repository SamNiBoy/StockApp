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
import com.sn.stock.Stock;
import com.sn.stock.Stock.Follower;

public class SimTradeObserverable extends Observable {

    static Logger log = Logger.getLogger(SimTradeObserverable.class);

    static Connection con = null;
    public String subject;
    public String content;
    static private boolean hasSentMail = false;
    static private boolean needSentMail = false;
    static private boolean useDftAcnt = true;

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public boolean hasSentMail() {
        return hasSentMail;
    }

    public SimTradeObserverable() {
        this.addObserver(StockObserver.globalObs);
    }

    static public void main(String[] args) {
        
        SimStockDriver.addStkToSim("002397");
        SimStockDriver.addStkToSim("600503");
        SimStockDriver.setStartEndSimDt("2016-02-18", "2016-02-19");
        
        SimStockDriver.loadStocks();
        StockObserverable.stocks = SimStockDriver.simstocks;
        SimTradeObserverable ppo = new SimTradeObserverable();
        
        if (!SimStockDriver.initData()) {
            log.info("can not init SimStockDriver...");
            return;
        }
        needSentMail = false;
        int StepCnt = 0;
        log.info("Now start simulate trading...");
        while (SimStockDriver.step()) {
            log.info("Simulate step:" + (++StepCnt));
            ppo.update();
        }
        
        SimStockDriver.finishStep();
        log.info("Now end simulate trading.");

        if (needSentMail) {
            ppo.setChanged();
            ppo.notifyObservers(ppo);
            hasSentMail = true;
        }
    }

    public void update() {
        String Summary = "";
        if (buyStock() ||
            sellStock()) {
            needSentMail = true;
            Summary = reportBuySellStat();
        }
        String returnStr = "";  
        SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");  
        Date date = new Date();  
        returnStr = f.format(date);
        subject = content = "";
        subject = "Profit Information " + returnStr;
        content = Summary + "<br/>";
        if (needSentMail) {
            this.setChanged();
            this.notifyObservers(this);
            hasSentMail = true;
            needSentMail = false;
        }
    }
    
    private boolean buyStock()
    {
        log.info("strat buyStock...");
        List<CashAcnt> Acnts = null;
        boolean hasBoughtStock = false;

        if (this.useDftAcnt) {
            Acnts = CashAcntManger.getDftAcnt();
        }
        else {
            Acnts = CashAcntManger.getAllAcnts();
        }
        for (String stock : StockObserverable.stocks.keySet()) {
            Stock s = StockObserverable.stocks.get(stock);
            if (s.isGoodCandidateForBuy()) {
                for(CashAcnt a: Acnts) {
                    if (a.buyStock(s)) {
                        a.calProfit();
                        hasBoughtStock = true;
                    }
                }
            }
        }
        log.info("end buyStock...");
        return hasBoughtStock;
    }
    
    private boolean sellStock()
    {
        log.info("start sellStock...");
        List<CashAcnt> Acnts = null;
        boolean hasSoldStock = false;

        if (this.useDftAcnt) {
            Acnts = CashAcntManger.getDftAcnt();
        }
        else {
            Acnts = CashAcntManger.getAllAcnts();
        }
        for (String stock : StockObserverable.stocks.keySet()) {
            Stock s = StockObserverable.stocks.get(stock);
            if (s.isGoodCandidateForSell()) {
                for(CashAcnt a: Acnts) {
                    if (a.sellStock(s)) {
                        a.calProfit();
                        hasSoldStock = true;
                    }
                }
            }
        }
        log.info("end sellStock...");
        return hasSoldStock;
    }
    
    private String reportBuySellStat() {
        log.info("reportBuySellStat...");
        List<CashAcnt> Acnts = null;
        String msg = "";

        if (this.useDftAcnt) {
            Acnts = CashAcntManger.getDftAcnt();
        }
        else {
            Acnts = CashAcntManger.getAllAcnts();
        }
        for(CashAcnt a: Acnts) {
            msg += a.reportAcntProfitWeb();
        }
        return msg;
    }
}