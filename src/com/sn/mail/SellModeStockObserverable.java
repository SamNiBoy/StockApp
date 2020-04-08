package com.sn.mail;

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
import com.sn.STConstants;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;

public class SellModeStockObserverable extends Observable {

    static Logger log = Logger.getLogger(SellModeStockObserverable.class);

    private List<Stock2> stocksToSellMode = new ArrayList<Stock2>();
    private List<Stock2> stocksToUnSellMode = new ArrayList<Stock2>();
    private List<SellModeStockSubscriber> ms = new ArrayList<SellModeStockSubscriber>();

    public void addStockToSellMode(List<Stock2> lst) {
    	stocksToSellMode.clear();
    	stocksToSellMode.addAll(lst);
    }
    public void addStockToUnsellMode(List<Stock2> lst) {
    	stocksToUnSellMode.clear();
    	stocksToUnSellMode.addAll(lst);
    }
	public class SellModeStockSubscriber{
		String openID;
		String mail;
		List<Stock2> stockMailed = new ArrayList<Stock2>();
		SellModeStockSubscriber(String oid, String ml) {
			openID = oid;
			mail = ml;
		}
		public String subject;
		public String content;
		boolean alreadyMailed(Stock2 s) {
			if (stockMailed.contains(s)) {
				log.info("Stock:" + s.getID() + " already Mailed to user:" + openID);
				return true;
			}
			return false;
		}
        boolean StockGzedBySubscriber(Stock2 s) {
    	    boolean gzed_flg = false;
  		    Connection con = DBManager.getConnection();
   		    Statement stm = null;
    	    try {
    	    	stm = con.createStatement();
    	    	String sql = "select 'check gzed stock before sending mail to user' from usrStk where gz_flg = 1 and suggested_by <> '" + ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null) +"' and openID = '" + openID + "' and id = '" + s.getID() + "'";
    	    	log.info(sql);
    	    	ResultSet rs = stm.executeQuery(sql);
    	    	if (rs.next()) {
    	    	    gzed_flg = true;
    	    	}
    	    	rs.close();
    	    }
    	    catch (Exception e) {
    	    	e.printStackTrace();
                log.error(e.getMessage() + " errored:" + e.getCause());
    	    }
    	    finally {
    	    	try {
                    stm.close();
            		con.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    log.error(e.getMessage() + " errored:" + e.getErrorCode());
                }
    	    }
            log.info("get user gzed_flg:" + gzed_flg);
    	    return gzed_flg;
        }
		public boolean setMailed(Stock2 s) {
			stockMailed.add(s);
			return true;
		}
	}
	
    public List<SellModeStockSubscriber> getSellModeStockSubscribers() {
    	return ms;
    }
    private boolean loadMailScb() {
    	boolean load_success = false;
  		Connection con = DBManager.getConnection();
   		Statement stm = null;
    	try {
    		stm = con.createStatement();
    		String sql = "select  u.* from usr u where length(u.mail) > 1 and u.suggest_stock_enabled = 1";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		String openId;
    		String mail;
    		while (rs.next()) {
    			openId = rs.getString("openID");
    			mail = rs.getString("mail");
    			log.info("loading mailsubscriber:" + openId + ", mail:" + mail);
    			ms.add(new SellModeStockSubscriber(openId, mail));
    			load_success = true;
    		}
    		rs.close();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
            log.error(e.getMessage() + " errored:" + e.getCause());
    	}
    	finally {
    		try {
                stm.close();
        		con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage() + " errored:" + e.getErrorCode());
            }
    	}
    	return load_success;
    }
    
    private boolean buildMailforSubscribers() {
        if (ms.size() == 0 || (stocksToSellMode.size() == 0 && stocksToUnSellMode.size() == 0)) {
        	log.info("No user subscribed or no stocks to for sell/unsell mode, no need to send mail.");
        	return false;
        }
        String returnStr = "";
        SimpleDateFormat f = new SimpleDateFormat(" HH:mm:ss");  
        Date date = new Date();  
        returnStr = f.format(date);
        String subject = "买卖模式变化" + returnStr;
        StringBuffer body;
        boolean usr_need_mail = false;
        boolean generated_mail = false;
        
        for (SellModeStockSubscriber u : ms) {
        	u.subject = subject;
        	u.content = "";
        	body = new StringBuffer();
        	usr_need_mail = false;
            body.append("<table bordre = 1>" +
                    "<tr>" +
                    "<th> ID</th> " +
                    "<th> Name</th> " +
                    "<th> Is Sell Mode</th> " +
                    "<th> Price</th></tr>");
            DecimalFormat df = new DecimalFormat("##.##");
            for (Stock2 s : stocksToSellMode) {
            	if (!u.alreadyMailed(s) && u.StockGzedBySubscriber(s)) {
                    body.append("<tr> <td>" + s.getID() + "</td>" +
                    "<td> " + s.getName() + "</td>" +
                    "<td>  1 </td>" +
                    "<td> " + df.format(s.getCur_pri() == null ? 0 : s.getCur_pri()) + "</td></tr>");
                    usr_need_mail = true;
                    generated_mail = true;
                    u.setMailed(s);
            	}
            }
            
            for (Stock2 s : stocksToUnSellMode) {
            	if (!u.alreadyMailed(s)) {
                    body.append("<tr> <td>" + s.getID() + "</td>" +
                    "<td> " + s.getName() + "</td>" +
                    "<td>  0 </td>" +
                    "<td> " + df.format(s.getCur_pri() == null ? 0 : s.getCur_pri()) + "</td></tr>");
                    usr_need_mail = true;
                    generated_mail = true;
                    u.setMailed(s);
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

    public SellModeStockObserverable() {
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