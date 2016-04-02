package com.sn.mail.reporter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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

import com.sn.db.DBManager;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;

public class RecommandStockObserverable extends Observable {

    static Logger log = Logger.getLogger(RecommandStockObserverable.class);

    private List<Stock2> stocksToSuggest = new ArrayList<Stock2>();
    private List<RecommandStockSubscriber> ms = new ArrayList<RecommandStockSubscriber>();

    public void addStockToSuggest(List<Stock2> lst) {
    	stocksToSuggest.clear();
    	stocksToSuggest.addAll(lst);
    }
	public class RecommandStockSubscriber{
		String openID;
		String mail;
		List<Stock2> stockSuggested = new ArrayList<Stock2>();
		RecommandStockSubscriber(String oid, String ml) {
			openID = oid;
			mail = ml;
		}
		public String subject;
		public String content;
		boolean alreadySuggested(Stock2 s) {
			if (stockSuggested.contains(s)) {
				log.info("Stock:" + s.getID() + " already suggested to user:" + openID);
				return true;
			}
			return false;
		}
		public boolean setSuggested(Stock2 s) {
			stockSuggested.add(s);
			return true;
		}
	}
	
    public List<RecommandStockSubscriber> getRecommandStockSubscribers() {
    	return ms;
    }
    private boolean loadMailScb() {
    	boolean load_success = false;
    	try {
    		Connection con = DBManager.getConnection();
    		Statement stm = con.createStatement();
    		String sql = "select  u.* from usr u where u.mail is not null and u.suggest_stock_enabled = 1";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		String openId;
    		String mail;
    		while (rs.next()) {
    			openId = rs.getString("openID");
    			mail = rs.getString("mail");
    			log.info("loading mailsubscriber:" + openId + ", mail:" + mail);
    			ms.add(new RecommandStockSubscriber(openId, mail));
    			load_success = true;
    		}
    		rs.close();
    		stm.close();
    		con.close();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	return load_success;
    }
    
    private boolean buildMailforSubscribers() {
        if (ms.size() == 0 || stocksToSuggest.size() == 0) {
        	log.info("No user subscribed or no stocks to suggest, no need to send mail.");
        	return false;
        }
        String returnStr = "";
        SimpleDateFormat f = new SimpleDateFormat(" HH:mm:ss");  
        Date date = new Date();  
        returnStr = f.format(date);
        String subject = "推荐信" + returnStr;
        StringBuffer body;
        boolean usr_need_mail = false;
        boolean generated_mail = false;
        
        for (RecommandStockSubscriber u : ms) {
        	u.subject = subject;
        	u.content = "";
        	body = new StringBuffer();
        	usr_need_mail = false;
            body.append("<table bordre = 1>" +
                    "<tr>" +
                    "<th> ID</th> " +
                    "<th> Name</th> " +
                    "<th> Price</th></tr>");
            DecimalFormat df = new DecimalFormat("##.##");
            for (Stock2 s : stocksToSuggest) {
            	if (!u.alreadySuggested(s)) {
                    body.append("<tr> <td>" + s.getID() + "</td>" +
                    "<td> " + s.getName() + "</td>" +
                    "<td> " + df.format(s.getCur_pri() == null ? 0 : s.getCur_pri()) + "</td></tr>");
                    usr_need_mail = true;
                    generated_mail = true;
                    u.setSuggested(s);
            	}
            }
            if (usr_need_mail) {
                u.content = body.toString();
            }
            else {
            	u.subject = "";
            	u.content = "";
            }
        }
        return generated_mail;
    }

    public RecommandStockObserverable() {
        this.addObserver(StockObserver.globalObs);
        loadMailScb();
    }
    
    static public void main(String[] args) {
    }

    public void update() {
        if (buildMailforSubscribers()) {
            this.setChanged();
            this.notifyObservers(this);
        }
    }
}