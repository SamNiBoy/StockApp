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
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;

public class GzStockBuySellPointObserverable extends Observable {

	public class MailSubscriber{
		String openID;
		String mail;
		List<String> gzStkLst = new ArrayList<String>();
		public boolean gzStk(String stkId) {
			return gzStkLst.contains(stkId);
		}
		MailSubscriber(String oid, String ml, String stkId) {
			openID = oid;
			mail = ml;
			gzStkLst.add(stkId);
		}
		public String subject;
		public String content;
	}
    static Logger log = Logger.getLogger(GzStockBuySellPointObserverable.class);

    static Connection con = DBManager.getConnection();
    private List<StockBuySellEntry> sbse = new ArrayList<StockBuySellEntry>();
    private List<MailSubscriber> ms = new ArrayList<MailSubscriber>();

    public List<MailSubscriber> getMailSubscribers() {
    	return ms;
    }
    private boolean loadMailScb() {
    	boolean load_success = false;
    	try {
    		Statement stm = con.createStatement();
    		String sql = "select s.*, u.* from usrStk s, usr u where s.openID = u.openID and s.gz_flg = 1 and u.mail is not null and u.buy_sell_enabled = 1";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		String openId;
    		String mail;
    		String stkId;
    		while (rs.next()) {
    			openId = rs.getString("openID");
    			mail = rs.getString("mail");
    			stkId = rs.getString("id");
    			log.info("loading mailsubscriber:" + openId + ", mail:" + mail + ", stock:" + stkId);
    			ms.add(new MailSubscriber(openId, mail, stkId));
    			load_success = true;
    		}
    		rs.close();
    		stm.close();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	return load_success;
    }
    
    private boolean buildMailforSubscribers() {
        if (ms.size() == 0) {
        	log.info("No user subscribed an buy/sell, no need to send mail.");
        	return false;
        }
        String returnStr = "";  
        SimpleDateFormat f = new SimpleDateFormat(" HH:mm:ss");  
        Date date = new Date();  
        returnStr = f.format(date);
        String subject = StockMarket.getShortDesc() + returnStr;
        StringBuffer body;
        boolean usr_need_mail = false;
        
        for (MailSubscriber u : ms) {
        	u.subject = subject;
        	u.content = "";
        	body = new StringBuffer();
            body.append("<table bordre = 1>" +
                    "<tr>" +
                    "<th> ID</th> " +
                    "<th> Name</th> " +
                    "<th> Price</th> " +
                    "<th> Buy/Sell</th> " +
                    "<th> Time</th></tr>");
            DecimalFormat df = new DecimalFormat("##.##");
            for (StockBuySellEntry e : sbse) {
            	if (u.gzStk(e.id)) {
                    body.append("<tr> <td>" + e.id + "</td>" +
                    "<td> " + e.name + "</td>" +
                    "<td> " + df.format(e.price) + "</td>" +
                    "<td> " + (e.is_buy_point ? "B" : "S") + "</td>" +
                    "<td> " + e.timestamp + "</td></tr>");
                    usr_need_mail = true;
            	}
            }
            u.content = body.toString();
        }
        return usr_need_mail;
    }

    public GzStockBuySellPointObserverable(List<StockBuySellEntry> s) {
        this.addObserver(StockObserver.globalObs);
        sbse.addAll(s);
        loadMailScb();
    }
    
    public boolean setData(List<StockBuySellEntry> dat) {
        sbse.addAll(dat);
        return true;
    }

    static public void main(String[] args) {
    }

    public void update() {
        if (buildMailforSubscribers()) {
            this.setChanged();
            this.notifyObservers(this);
            sbse.clear();
        }
    }
}