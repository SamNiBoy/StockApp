package com.sn.mail.reporter;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.trader.StockTrader;

public class SimTraderObserverable extends Observable {

    static Logger log = Logger.getLogger(SimTraderObserverable.class);

    private List<SimTradeSubscriber> ms = new ArrayList<SimTradeSubscriber>();

	public class SimTradeSubscriber{
		String openID;
		String mail;
		SimTradeSubscriber(String oid, String ml) {
			openID = oid;
			mail = ml;
		}
		public String subject;
		public String content;
	}
	
    public List<SimTradeSubscriber> getMailSubscribers() {
    	return ms;
    }
    private boolean loadMailScb() {
    	boolean load_success = false;
    	ms.clear();
    	try {
    		Connection con = DBManager.getConnection();
    		Statement stm = con.createStatement();
    		String sql = "select u.* from usr u where u.mail is not null and u.buy_sell_enabled = 1";
    		log.info(sql);
    		ResultSet rs = stm.executeQuery(sql);
    		String openId;
    		String mail;
    		while (rs.next()) {
    			openId = rs.getString("openID");
    			mail = rs.getString("mail");
    			log.info("loading mailsubscriber:" + openId + ", mail:" + mail);
    			ms.add(new SimTradeSubscriber(openId, mail));
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
        
        loadMailScb();

        if (ms.size() == 0) {
        	log.info("No user subscribed sim trade, no need to send mail.");
        	return false;
        }
        String returnStr = "";  
        SimpleDateFormat f = new SimpleDateFormat(" HH:mm:ss");  
        Date date = new Date();  
        returnStr = f.format(date);
        String subject = "Stock Sim Result " + returnStr;
        StringBuffer body;
        boolean usr_need_mail = false;
        boolean generated_mail = false;
        
        for (SimTradeSubscriber u : ms) {
        	
        	u.subject = "";
        	u.content = "";
        	body = new StringBuffer();
        	usr_need_mail = false;
        	
            body.append("<table bordre = 1>" +
                    "<tr>" +
                    "<th> ACNTCNT</th> " +
                    "<th> AvgPft</th> " +
                    "<th> AvgPP</th> " +
                    "<th> AvgBuyCnt</th> " +
                    "<th> AvgSellCnt</th></tr>");
            
            DecimalFormat df = new DecimalFormat("##.##");
            int ACNTCNT = 0;
            double avgPft = 0.0;
            double avgPP = 0.0;
            int avgBuyCnt = 0;
            int avgSellCnt = 0;
            
            try {
        		Connection con = DBManager.getConnection();
        		Statement stm = con.createStatement();
        		String sql = "select count(distinct(ac.acntid)) ACNTCNT, "
        				   + "       avg(ac.pft_mny - ac.used_mny) avgPft, "
        				   + "       avg((ac.pft_mny - ac.used_mny) / case when ac.used_mny = 0 then 1 else ac.used_mny end) avgPP,"
        				   + "       avg(tmp.buyCnt) buyCnt,"
        				   + "       avg(tmp.sellCnt) sellCnt "
        				   + " from cashacnt ac, "
        				   + "      (select sum(case when td.buy_flg = 1 then 1 else 0 end) buyCnt, "
        				   + "              sum(case when td.buy_flg  = 1 then 0 else 1 end) sellCnt, "
        				   + "              th.acntid "
        				   + "         from tradehdr th, tradedtl td"
        			       + "        where th.acntid = td.acntid "
        			       + "          and th.stkid = td.stkid "
        			       + "         group by th.acntid ) tmp"
        			       + " where ac.acntid = tmp.acntid"
        			       + "   and ac.dft_acnt_flg = 0";
        		
        		log.info(sql);
        		
        		ResultSet rs = stm.executeQuery(sql);
        		
        		if (rs.next()) {
        			ACNTCNT = rs.getInt("ACNTCNT");
        			avgPft = rs.getDouble("avgPft");
        			avgPP = rs.getDouble("avgPP");
        			avgBuyCnt = rs.getInt("buyCnt");
        			avgSellCnt = rs.getInt("sellCnt");
        		}
        		
        		if (ACNTCNT > 0) {
        			usr_need_mail = true;
        		}
        		else {
        			usr_need_mail = false;
        		}
        		
                if (usr_need_mail) {
                    body.append("<tr> <td>" + ACNTCNT + "</td>" +
                    "<td> " + df.format(avgPft) + "</td>" +
                    "<td> " + df.format(avgPP) + "</td>" +
                    "<td> " + avgBuyCnt + "</td>" +
                    "<td> " + avgSellCnt + "</td></tr>");
                    generated_mail = true;
                }
                
        		if (usr_need_mail) {
        			rs.close();
        			stm.close();
        			
                    body.append("<table bordre = 1>" +
                            "<tr>" +
                            "<th> ID </th> " +
                            "<th> Acnt_ID </th> " +
                            "<th> Init_Mny </th> " +
                            "<th> Used_Mny </th> " +
                            "<th> Pft_Mny </th> " +
                            "<th> Split_Num </th> " +
                            "<th> Max_Useable_Pct </th> " +
                            "<th> Dft_Acnt_flg </th> " +
                            "<th> Profit</th>" +
                            "<th> Proft_Pct</th>" +
                            "<th> Add_Dt </th></tr>");
                    
        			int ID = 1, TopN = 10;
        			
        			stm = con.createStatement();
        			
        			sql = "select ca.pft_mny - ca.used_mny profit, "
        			    + "       (ca.pft_mny - ca.used_mny) / case when ca.used_mny = 0 then 1 else ca.used_mny end PP, "
        			    + "       to_char(sysdate, 'yyyy-mm-dd hh24:mi:ss') add_dte, "
        			    + "       ca.* "
        			    + "  from cashacnt ca "
        			    + "   where ca.dft_acnt_flg = 0"
        			    + " order by (ca.pft_mny - ca.used_mny) desc ";
        			
        			log.info(sql);
        			rs = stm.executeQuery(sql);
        			
        			while (rs.next() && TopN > 0) {
                        body.append("<tr> <td>" + ID + "</td>" +
                                "<td> " + rs.getString("acntid") + "</td>" +
                                "<td> " + df.format(rs.getDouble("init_mny")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("used_mny")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("pft_mny")) + "</td>" +
                                "<td> " + rs.getInt("split_num") + "</td>" +
                                "<td> " + df.format(rs.getDouble("max_useable_pct")) + "</td>" +
                                "<td> " + rs.getInt("dft_acnt_flg") + "</td>" +
                                "<td> " + df.format(rs.getDouble("profit")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("PP")) + "</td>" +
                                "<td> " + rs.getString("add_dte") + "</td></tr>");
                        TopN--;
                        ID++;
        			}

        			rs.close();
        			stm.close();
        			
                    body.append("<table bordre = 1>" +
                            "<tr>" +
                            "<th> ID </th> " +
                            "<th> Acnt_ID </th> " +
                            "<th> Init_Mny </th> " +
                            "<th> Used_Mny </th> " +
                            "<th> Pft_Mny </th> " +
                            "<th> Split_Num </th> " +
                            "<th> Max_Useable_Pct </th> " +
                            "<th> Dft_Acnt_flg </th> " +
                            "<th> Profit</th>" +
                            "<th> Proft_Pct</th>" +
                            "<th> Add_Dt </th></tr>");
                    
        			ID = 1;
        			TopN = 10;
        			
        			stm = con.createStatement();
        			
        			sql = "select ca.pft_mny - ca.used_mny profit, "
        			    + "       (ca.pft_mny - ca.used_mny) / case when ca.used_mny = 0 then 1 else ca.used_mny end PP, "
        			    + "       to_char(sysdate, 'yyyy-mm-dd hh24:mi:ss') add_dte, "
        			    + "       ca.* "
        			    + "  from cashacnt ca "
        			    + "  where ca.dft_acnt_flg = 0"
        			    + " order by (ca.pft_mny - ca.used_mny) asc ";
        			
        			log.info(sql);
        			rs = stm.executeQuery(sql);
        			
        			while (rs.next() && TopN > 0) {
                        body.append("<tr> <td>" + ID + "</td>" +
                                "<td> " + rs.getString("acntid") + "</td>" +
                                "<td> " + df.format(rs.getDouble("init_mny")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("used_mny")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("pft_mny")) + "</td>" +
                                "<td> " + rs.getInt("split_num") + "</td>" +
                                "<td> " + df.format(rs.getDouble("max_useable_pct")) + "</td>" +
                                "<td> " + rs.getInt("dft_acnt_flg") + "</td>" +
                                "<td> " + df.format(rs.getDouble("profit")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("PP")) + "</td>" +
                                "<td> " + rs.getString("add_dte") + "</td></tr>");
                        TopN--;
                        ID++;
        			}
        		}
        		rs.close();
        		stm.close();
        		con.close();
            }
            catch (Exception e) {
            	e.printStackTrace();
            }
            
            if (generated_mail) {
                u.content = body.toString();
                u.subject = subject;
            }
            else {
            	u.subject = "";
            	u.content = "";
            }
        }
        return generated_mail;
    }

    public SimTraderObserverable() {
        this.addObserver(StockObserver.globalObs);
        loadMailScb();
    }
    
    static public void main(String[] args) {
    	SimTraderObserverable so = new SimTraderObserverable();
    	so.update();
    }

    public void update() {
        if (buildMailforSubscribers()) {
            this.setChanged();
            this.notifyObservers(this);
        }
    }
}