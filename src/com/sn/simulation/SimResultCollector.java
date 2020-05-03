package com.sn.simulation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import com.sn.cashAcnt.CashAcnt;
import com.sn.cashAcnt.CashAcntManger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.mail.MailSenderType;
import com.sn.mail.SimTraderObserverable;
import com.sn.mail.MailSenderFactory;
import com.sn.mail.SimpleMailSender;
import com.sn.mail.StockObserver;
import com.sn.mail.StockObserverable;
import com.sn.strategy.ITradeStrategy;
import com.sn.STConstants;
import com.sn.strategy.TradeStrategyGenerator;
import com.sn.strategy.TradeStrategyImp;
import com.sn.strategy.algorithm.ga.Algorithm;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.suggest.SuggestStock;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.trader.StockTrader;
import com.sn.wechat.WCMsgSender;
import com.sn.task.WorkManager;
import com.sn.task.IWork;

public class SimResultCollector implements Job{

    static Logger log = Logger.getLogger(SimResultCollector.class);

    static Connection con = null;

    public SimResultCollector() {

    }

    static public void main(String[] args) throws Exception {
    	SimResultCollector st = new SimResultCollector();
        //st.run();
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
         log.info("now to run SimResultCollector...");
         
         StockMarket.calSimData();
         
         log.info("SimResultCollector end...");
    }
}