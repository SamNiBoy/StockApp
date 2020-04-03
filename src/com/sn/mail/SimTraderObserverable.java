package com.sn.mail;

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
import com.sn.STConstants;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
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
    		String sql = "select u.* from usr u where length(u.mail) > 1 and u.buy_sell_enabled = 1";
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
        String subject = "回测结果:";
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
                    "<th> Account Count</th> " +
                    "<th> Catagory </th> " +
                    "<th> Total Used Money</th> " +
                    "<th> Avg Used Money Hours</th> " +
                    "<th> Avg Profit</th> " +
                    "<th> Total Profit</th> " +
                    "<th> Avg Profit Percent(Used Money)</th> " +
                    "<th> Total Commission</th> " +
                    "<th> Net profit</th> " +
                    "<th> Tot Buy Count</th> " +
                    "<th> Tot Sell Count</th></tr>");
            
            DecimalFormat df = new DecimalFormat("##.##");
            int ACNTCNT = 0;
            double avgPft = 0.0;
            double avgPP = 0.0;
            double avgBuyCnt = 0;
            double avgSellCnt = 0;
            String cat = "";
            double totUsedMny = 0.0;
            double avgUsedMny_Hrs = 0.0;
            double totPft = 0.0;
            double total_commission_mny = 0.0;
            double netPft = 0.0;
       		Connection con = DBManager.getConnection();
            
            try {
        		Statement stm = con.createStatement();
                
        		
                String sql = "select count(distinct(ac.acntid)) ACNTCNT, "
                        + "       sum(ac.pft_mny) totPft, "
                        + "       sum(h.commission_mny) total_commission_mny"
                        + " from cashacnt ac, "
                        + "      (select sum(case when td.buy_flg = 1 then 1 else 0 end) buyCnt, "
                        + "              sum(case when td.buy_flg  = 1 then 0 else 1 end) sellCnt, "
                        + "              th.acntid "
                        + "         from tradehdr th, tradedtl td"
                        + "        where th.acntid = td.acntid "
                        + "          and th.stkid = td.stkid "
                        + "         group by th.acntid ) tmp, "
                        + "      TradeHdr h"
                        + " where ac.acntid = tmp.acntid"
                        + "   and ac.acntid = h.acntid "
                        + "   and ac.acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + "%'";
             
             log.info(sql);
             
             ResultSet rs = stm.executeQuery(sql);
             
             if (rs.next()) {
                 int cnt = rs.getInt("ACNTCNT");
                 double allPft = rs.getDouble("totPft");
                 double allComm = rs.getDouble("total_commission_mny");
                 
                 subject += "股票数:" + cnt + ", 毛利:" + allPft + ", 佣金:" + allComm;
             }
             
             rs.close();
             stm.close();
             
        		       sql = "select count(distinct(ac.acntid)) ACNTCNT, "
                           + "       sum(ac.used_mny) totUsedMny,"
                           + "       sum(ac.used_mny * ac.used_mny_hrs) / sum(ac.used_mny) avgUsedMny_Hrs,"
        				   + "       avg(ac.pft_mny) avgPft, "
        				   + "       sum(ac.pft_mny) totPft, "
        				   + "       avg((ac.pft_mny) / ac.used_mny) avgPP,"
                           + "       sum(h.commission_mny) total_commission_mny,"
                           + "       sum(ac.pft_mny)  - sum(h.commission_mny) netPft,"
        				   + "       sum(tmp.buyCnt) buyCnt,"
        				   + "       sum(tmp.sellCnt) sellCnt, "
        				   + "       case when ac.pft_mny > 0 then 1 when ac.pft_mny = 0 then 0 else -1 end cat"
        				   + " from cashacnt ac, "
        				   + "      (select sum(case when td.buy_flg = 1 then 1 else 0 end) buyCnt, "
        				   + "              sum(case when td.buy_flg  = 1 then 0 else 1 end) sellCnt, "
        				   + "              th.acntid "
        				   + "         from tradehdr th, tradedtl td"
        			       + "        where th.acntid = td.acntid "
        			       + "          and th.stkid = td.stkid "
        			       + "         group by th.acntid ) tmp, "
                           + "      TradeHdr h"
        			       + " where ac.acntid = tmp.acntid"
                           + "   and ac.acntid = h.acntid "
        			       + "   and ac.acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + "%'"
        			       + "   group by case when ac.pft_mny > 0 then 1 when ac.pft_mny = 0 then 0 else -1 end "
        			       + "   order by cat";
        		
        		log.info(sql);
        		
        		stm = con.createStatement();
        		rs = stm.executeQuery(sql);
        		
        		while (rs.next()) {
                    /*
                    "<th> Account Count</th> " +
                    "<th> Catagory </th> " +
                    "<th> Total Used Money</th> " +
                    "<th> Total Used Money Hours</th> " +
                    "<th> Avg Profit</th> " +
                    "<th> Total Profit</th> " +
                    "<th> Avg Profit Percent(Used Money)</th> " +
                    "<th> Total Commission</th> " +
                    "<th> Net profit</th> " +
                    "<th> Tot Buy Count</th> " +
                    "<th> Tot Sell Count</th></tr>");*/
        			ACNTCNT = rs.getInt("ACNTCNT");
        			cat = (rs.getInt("cat") == 1 ? "涨":(rs.getInt("cat") == 0 ? "平" : "跌"));
        			totUsedMny = rs.getDouble("totUsedMny");
        			avgUsedMny_Hrs = rs.getDouble("avgUsedMny_Hrs");
        			avgPft = rs.getDouble("avgPft");
        			totPft = rs.getDouble("totPft");
        			avgPP = rs.getDouble("avgPP");
        			total_commission_mny = rs.getDouble("total_commission_mny");
        			netPft = rs.getDouble("netPft");
        			avgBuyCnt = rs.getDouble("buyCnt");
        			avgSellCnt = rs.getDouble("sellCnt");
        			
                    log.info("ACNTCNT:" + ACNTCNT);
        		    if (ACNTCNT > 0) {
        		    	usr_need_mail = true;
                        if (usr_need_mail) {
                            body.append("<tr> <td>" + ACNTCNT + "</td>" +
                            "<td> " + cat + "</td>" +
                            "<td> " + df.format(totUsedMny) + "</td>" +
                            "<td> " + df.format(avgUsedMny_Hrs) + "</td>" +
                            "<td> " + df.format(avgPft) + "</td>" +
                            "<td> " + df.format(totPft) + "</td>" +
                            "<td> " + df.format(avgPP) + "</td>" +
                            "<td> " + df.format(total_commission_mny) + "</td>" +
                            "<td> " + df.format(netPft) + "</td>" +
                            "<td> " + df.format(avgBuyCnt) + "</td>" +
                            "<td> " + df.format(avgSellCnt) + "</td></tr>");
                            generated_mail = true;
                        }
        		    }
        		}
                
                log.info("usr_need_mail:" + usr_need_mail);
        		if (usr_need_mail) {
        			rs.close();
        			stm.close();
        			
                    body.append("<br><table bordre = 1>" +
                            "<tr>" +
                            "<th> ID </th> " +
                            "<th> Acnt ID </th> " +
                            "<th> Name </th> " +
                            "<th> Init Mny </th> " +
                            "<th> Used Mny </th> " +
                            "<th> Used Mny Hours</th> " +
                            "<th> Profit Mny </th> " +
                            "<th> Max Mny Per Trade</th> " +
                            "<th> Max Useable Pct </th> " +
                            "<th> Profit</th>" +
                            "<th> Profit Pct</th>" +
                            "<th> Add Date </th></tr>");
                    
        			int ID = 1, TopN = 10;
        			
        			stm = con.createStatement();
        			
        			sql = "select ca.pft_mny profit, "
        			    + "       ca.pft_mny / ca.used_mny PP, "
        			    + "       left(sysdate(), 10) add_dte, "
                        + "       s.name, "
        			    + "       ca.* "
        			    + "  from cashacnt ca, "
                        + "       stk s"
        			    + "   where ca.acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + "%'"
                        + "     and right(ca.acntid, 6) = s.id "
        			    + " order by ca.pft_mny desc ";
        			
        			log.info(sql);
        			rs = stm.executeQuery(sql);
        			
        			while (rs.next() && TopN > 0) {
                        body.append("<tr> <td>" + ID + "</td>" +
                                "<td> " + rs.getString("acntid") + "</td>" +
                                "<td> " + rs.getString("name") + "</td>" +
                                "<td> " + df.format(rs.getDouble("init_mny")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("used_mny")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("used_mny_hrs")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("pft_mny")) + "</td>" +
                                "<td> " + rs.getInt("max_mny_per_trade") + "</td>" +
                                "<td> " + df.format(rs.getDouble("max_useable_pct")) + "</td>" +
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
                            "<th> Name </th> " +
                            "<th> Acnt ID </th> " +
                            "<th> Init Mny </th> " +
                            "<th> Used Mny </th> " +
                            "<th> Used Mny Hours</th> " +
                            "<th> Profit Mny </th> " +
                            "<th> Max Mny Per Trade </th> " +
                            "<th> Max Useable Pct </th> " +
                            "<th> Profit</th>" +
                            "<th> Profit Pct</th>" +
                            "<th> Add Date </th></tr>");
                    
        			ID = 1;
        			TopN = 10;
        			
        			stm = con.createStatement();
        			
                    sql = "select ca.pft_mny profit, "
                            + "       ca.pft_mny / ca.used_mny PP, "
                            + "       left(sysdate(), 10) add_dte, "
                            + "       s.name, "
                            + "       ca.* "
                            + "  from cashacnt ca, "
                            + "       stk s"
                            + "   where ca.acntid like '" + ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + "%'"
                            + "     and right(ca.acntid, 6) = s.id "
        			        + " order by ca.pft_mny asc ";
        			
        			log.info(sql);
        			rs = stm.executeQuery(sql);
        			
        			while (rs.next() && TopN > 0) {
                        body.append("<tr> <td>" + ID + "</td>" +
                                "<td> " + rs.getString("acntid") + "</td>" +
                                "<td> " + rs.getString("name") + "</td>" +
                                "<td> " + df.format(rs.getDouble("init_mny")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("used_mny")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("used_mny_hrs")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("pft_mny")) + "</td>" +
                                "<td> " + rs.getInt("max_mny_per_trade") + "</td>" +
                                "<td> " + df.format(rs.getDouble("max_useable_pct")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("profit")) + "</td>" +
                                "<td> " + df.format(rs.getDouble("PP")) + "</td>" +
                                "<td> " + rs.getString("add_dte") + "</td></tr>");
                        TopN--;
                        ID++;
        			}
        		}
        		rs.close();
        		stm.close();
            }
            catch (Exception e) {
            	e.printStackTrace();
            }
            finally {
                try {
                    con.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    log.info("close db connection error:" + e.getMessage());
                }
            }
            
            log.info("Need mail?" + generated_mail);
            if (generated_mail) {
                u.content = body.toString();
                u.subject = subject;
            }
            else {
            	u.subject = "";
            	u.content = "";
            }
            log.info("end SimTraderObserver...");
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