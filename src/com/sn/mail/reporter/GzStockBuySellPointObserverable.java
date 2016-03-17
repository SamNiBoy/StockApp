package com.sn.mail.reporter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import com.sn.db.DBManager;
import com.sn.mail.reporter.MailSenderType;
import com.sn.mail.reporter.MailSenderFactory;
import com.sn.mail.reporter.SimpleMailSender;
import com.sn.reporter.WCMsgSender;
import com.sn.stock.Stock;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.Stock.Follower;

public class GzStockBuySellPointObserverable extends Observable {

    static Logger log = Logger.getLogger(GzStockBuySellPointObserverable.class);

    static Connection con = DBManager.getConnection();
    public String subject;
    public String content;
    private boolean hasSentMail = false;
    private boolean needSentMail = false;
    private List<StockBuySellEntry> sbse = new ArrayList<StockBuySellEntry>();

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public boolean hasSentMail() {
        return hasSentMail;
    }

    public GzStockBuySellPointObserverable(List<StockBuySellEntry> s) {
        this.addObserver(StockObserver.globalObs);
        sbse.addAll(s);
    }
    
    public boolean setData(List<StockBuySellEntry> dat) {
        sbse.addAll(dat);
        return true;
    }

    static public void main(String[] args) {
    }

    public void update() {
        needSentMail = false;
        String returnStr = "";  
        SimpleDateFormat f = new SimpleDateFormat(" HH:mm:ss");  
        Date date = new Date();  
        returnStr = f.format(date);
        subject = content = "";
        subject = StockMarket.getShortDesc() + returnStr;
        content = StockMarket.getLongDsc() + "<br>" + getBody();
        if (needSentMail) {
            this.setChanged();
            this.notifyObservers(this);
            hasSentMail = true;
            sbse.clear();
        }
    }
    public String getBody() {
        if (sbse.isEmpty()) {
            needSentMail = false;
            return "";
        }
        
        needSentMail = true;
        StringBuffer body = new StringBuffer();
        body.append("<table bordre = 1>" +
        "<tr>" +
        "<th> ID</th> " +
        "<th> Name</th> " +
        "<th> Price</th> " +
        "<th> Buy/Sell</th> " +
        "<th> Time</th></tr>");
        
        DecimalFormat df = new DecimalFormat("##.##");
        
        for (StockBuySellEntry e : sbse) {
            body.append("<tr> <td>" + e.id + "</td>" +
            "<td> " + e.name + "</td>" +
            "<td> " + df.format(e.price) + "</td>" +
            "<td> " + (e.is_buy_point ? "B" : "S") + "</td>" +
            "<td> " + e.timestamp + "</td></tr>");
        }
        
        return body.toString();
    }
}