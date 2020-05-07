package com.sn.srvlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.WorkManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class TradeRecord{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    static Logger log = Logger.getLogger(TradeRecord.class);
    
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	public TradeRecord () {
		
	}
	public String acnt;
	public String stock;
	public String name;
	public int seqnum;
	public double price;
	public int amount;
	public int buy_flg;
	public Timestamp dl_dt;
	public String trade_selector_name;
	public String trade_selector_comment;
	public int gz_flg;
	public int stop_trade_mode_flg;
	public double pft_mny;
	public double commission_mny;
	public double net_pft;
	public int stock_in_hand;
	public double stock_inhand_money;
	public int fake_trade;
	
	public static List<TradeRecord> readTradeRecords() {
		List<TradeRecord> lst = new ArrayList<TradeRecord>();
		Connection con = DBManager.getConnection();
		String system_suggester = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
		try {
			Statement stm = con.createStatement();
			ResultSet rs = null;
			String sql = "select c.acntid, "
					+ "          t.stkid, "
					+ "          s.name, "
					+ "          d.seqnum, "
					+ "          d.price, "
					+ "          d.amount, "
					+ "          d.buy_flg, "
					+ "          d.dl_dt, "
					+ "          d.trade_selector_name, "
					+ "          d.trade_selector_comment, "
					+ "          case when u.gz_flg is null then 0 else u.gz_flg end gz_flg, "
					+ "          case when u.stop_trade_mode_flg is null then 0 else u.stop_trade_mode_flg end stop_trade_mode_flg, "
					+ "          c.pft_mny, "
					+ "          t.commission_mny, "
					+ "          (c.pft_mny - t.commission_mny) net_pft, "
					+ "          t.in_hand_qty stock_in_hand, "
					+ "          t.in_hand_stk_mny stock_inhand_money, "
					+ "          case when u.suggested_by = '" + system_suggester + "' then  1 when u.suggested_by is null then 2 else 0 end fake_trade "
					+ "from cashacnt c "
					+ "join tradehdr t "
					+ "  on c.acntid = t.acntid "
					+ "join tradedtl d "
					+ "  on t.acntid = d.acntid "
					+ "join stk s "
					+ "  on t.stkid = s.id "
					+ "left join usrstk u "
					+ "  on s.id = u.id "
					+ " order by acntid, seqnum desc";
			
			log.info(sql);
			rs = stm.executeQuery(sql);
			
			while(rs.next()) {
				TradeRecord r = new TradeRecord();
				r.acnt = rs.getString("acntid");
				r.stock = rs.getString("stkid");
				r.name = rs.getString("name");
				r.seqnum = rs.getInt("seqnum");
				r.price = rs.getDouble("price");
				r.amount = rs.getInt("amount");
				r.buy_flg = rs.getInt("buy_flg");
				r.dl_dt = rs.getTimestamp("dl_dt");
				r.trade_selector_name = rs.getString("trade_selector_name");
				r.trade_selector_comment = rs.getString("trade_selector_comment");
				r.gz_flg = rs.getInt("gz_flg");
				r.stop_trade_mode_flg = rs.getInt("stop_trade_mode_flg");
				r.pft_mny = rs.getDouble("pft_mny");
				r.commission_mny = rs.getDouble("commission_mny");
				r.net_pft = rs.getDouble("net_pft");
				r.stock_in_hand = rs.getInt("stock_in_hand");
				r.stock_inhand_money = rs.getDouble("stock_inhand_money");
				r.fake_trade = rs.getInt("fake_trade");
				lst.add(r);
			}
			rs.close();
			stm.close();
		}
		catch(Exception e) {
			log.error(e.getCause(), e);
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getCause(), e);
			}
		}
		return lst;
	}
	
	public static String getTradeRecordsAsTableString() {
		
		List<TradeRecord> list = readTradeRecords();
		
		String str = "<div id=\"tradeRecord\"> <table border=\"0\" id=\"detail\" style=\"margin: auto\">" +
		"<thead> " +
	    "<tr>                                      " +
	    "    <td>Account ID</td>                   " +
	    "    <td>Stock ID</td>                     " +
	    "    <td>Name</td>                         " +
	    "    <td>Seqnum</td>                       " +
	    "    <td>Price</td>                        " +
	    "    <td>Amount</td>                       " +
	    "    <td>Buy or Sell</td>                  " +
	    "    <td>Deal Time</td>                    " +
	    "    <td>Trade Selector Name</td>          " +
	    "    <td>Trade Selector Comment</td>       " +
	    "    <td>Gzed</td>                         " +
	    "    <td>Stop Trade</td>                   " +
	    "</tr>                                     " +
	    "</thead>";

	    str += "<tbody> ";
	    
	    String pre_stock = "";
	         for(TradeRecord tl:list)
	         {
	        	 if (!tl.stock.equals(pre_stock)) {
	        		 pre_stock = tl.stock;
	        		 str += "<tr class=\"parent\" id=\"" + tl.stock +"\">" +
	        				   "<td>Stock:" + tl.stock + "</td> " +
	        				    "<td>Name:" + tl.name + "</td> " +
	        		            "<td>Profit:" + tl.pft_mny + "</td> " +
	        		            "<td>Commission:" + tl.commission_mny + "</td> " +
	        		            "<td>Net Profit:" + (tl.net_pft > 0 ? "+" :"") +tl.net_pft + "</td> " +
	        		            "<td>Stock In Hand:" + tl.stock_in_hand + "</td> " +
	        		            "<td>Stock InHand Money:" + tl.stock_inhand_money + "</td> " +
	        		            "<td><font size=\"3\" color=\"" + ((tl.fake_trade>0)? "red":"black") + "\">" + (tl.fake_trade > 0 ? "Granted for Trade: No" : "Granted for Trade: Yes") + "</font></td> " +
	        		        "</tr>";
	        	 }
	            str += "<tr class=\"child_" + tl.stock + "\">" +
	            "<td>" + tl.acnt + "</td> " +
	            "<td>" + tl.stock + "</td> " +
	            "<td>" + tl.name + "</td> " +
	            "<td>" + tl.seqnum + "</td> " +
	            "<td>" + tl.price + "</td> " +
	            "<td>" + tl.amount + "</td>" +
	            "<td>" + (tl.buy_flg == 1 ? "Buy" : "Sell") + "</td>" +
	            "<td>" + tl.dl_dt + "</td> " +
	            "<td>" + tl.trade_selector_name + "</td> " +
	            "<td>" + tl.trade_selector_comment + "</td> " +
	            "<td>" + tl.gz_flg + "</td> " +
	            "<td>" + tl.stop_trade_mode_flg + "</td> " +
	            "</tr>";
	         }
	         str += "</tbody></table></div>";
	         
	         //log.info(str);
	         return str;
	}
	
	public static String getTradeSummaryAsTableString() {
		
		String str = "<div id=\"tradeSummary\"> <table border=\"0\" id=\"sum\" style=\"margin: auto\">" +
		"<thead> " +
	    "<tr>    " +
	    "    <td>Account Count</td> " +
	    "    <td>Profit</td>    " +
	    "    <td>Commission</td>   " +
	    "    <td>Net Profit</td>    " +
	    "    <td>Total Used Money</td>    " +
	    "    <td>Avg Used Money Hours</td>    " +
	    "    <td>Total Buy Money</td>    " +
	    "    <td>Total Sell Money</td>    " +
	    "    <td>Total Fund Rate</td>    " +
	    "    <td>Sell Count</td>   " +
	    "    <td>Buy Count</td>   " +
	    " </tr>" +
	    "</thead>";

		Connection con = DBManager.getConnection();
		try {
			Statement stm = con.createStatement();
			ResultSet rs = null;
			String sql = "";
			DecimalFormat df = new DecimalFormat("##.##");
			
			sql = "select count(distinct c.acntid) acntcnt, "
					+ "   sum(c.pft_mny) total_pft_mny, "
					+ "   sum(t.commission_mny) total_commission_mny, "
					+ "   sum(c.pft_mny) - sum(t.commission_mny) total_net_pft, "
					+ "   sum(c.used_mny) total_used_mny, "
					+ "   sum(c.used_mny * c.used_mny_hrs) / sum(c.used_mny) avgUsedMny_Hrs,"
					+ "   sum(d.buy_mny) total_buy_mny,"
					+ "   sum(d.sell_mny) total_sell_mny,"
					+ "   (sum(c.pft_mny) - sum(t.commission_mny)) * 100.0 / ((sum(d.buy_mny) + sum(d.sell_mny)) / 2.0) fundRt, "
					+ "   sum(d.buy_cnt) total_buy_cnt, "
					+ "   sum(d.sell_cnt) total_sell_cnt "
					+ "from cashacnt c "
					+ "join tradehdr t "
					+ "  on c.acntid = t.acntid "
					+ "join (select sum(buy_flg) buy_cnt, sum(case when buy_flg = 0 then 1 else 0 end) sell_cnt, sum(case when buy_flg = 1 then price*amount else 0 end) buy_mny, sum(case when buy_flg = 0 then price*amount else 0 end) sell_mny, acntid from tradedtl group by acntid) d"
					+ "  on t.acntid = d.acntid ";
			log.info(sql);
			rs = stm.executeQuery(sql);
			
			rs.next();
			
	            str += "<tr>" +
	            "<td>" + rs.getInt("acntcnt") + "</td> " +
	            "<td>" + rs.getDouble("total_pft_mny") + "</td> " +
	            "<td>" + rs.getDouble("total_commission_mny") + "</td> " +
	            "<td>" + rs.getDouble("total_net_pft") + "</td> " +
	            "<td>" + rs.getDouble("total_used_mny") + "</td> " +
	            "<td>" + df.format(rs.getDouble("avgUsedMny_Hrs")) + "</td> " +
	            "<td>" + rs.getDouble("total_buy_mny") + "</td> " +
	            "<td>" + rs.getDouble("total_sell_mny") + "</td> " +
	            "<td>" + df.format(rs.getDouble("fundRt")) + "%</td> " +
	            "<td>" + rs.getInt("total_sell_cnt") + "</td> " +
	            "<td>" + rs.getInt("total_buy_cnt") + "</td> " +
	            "</tr>";
	            
		         str += "</table></div>";
		     
		         rs.close();
		         stm.close();
	         }
	    catch(Exception e) {
	    	
	    }
	    finally {
	    	try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage(), e);
			}
	    }

         return str;
	}
	
	public static String getSuggestStocksAsTableString() {
		
		String str = "<div id=\"suggestedStocks\"> <table border=\"0\" style=\"margin: auto; width: 100%; height:100%;\">" +
		"<thead> " +
	    "<tr>                                      " +
	    "    <td>No.</td>                     " +
	    "    <td>Stock ID</td>                     " +
	    "    <td>Name</td>                         " +
	    "    <td>Price</td>                " +
	    "    <td>Percent</td>              " +
	    "    <td>Stop Trading</td>                 " +
	    "    <td>Suggest Selector</td>             " +
	    "    <td>Suggest Comment</td>              " +
	    "</tr>                                     " +
	    "</thead>";

	    str += "<tbody> ";
	    
		Connection con = DBManager.getConnection();
		try {
			Statement stm = con.createStatement();
			ResultSet rs = null;
			String sql = "select s.id, s.name, u.stop_trade_mode_flg, u.suggested_by_selector, u.suggested_comment  from stk s join usrstk u on s.id = u.id where u.gz_flg = 1 and u.suggested_by = 'SYSTEM_SUGGESTER'";
			
			log.info(sql);
			rs = stm.executeQuery(sql);
			int i = 0;
			DecimalFormat df = new DecimalFormat("##.##");
			while(rs.next()) {
				   i++;
				   
				   double cur_pri = 0.0;
				   double pct = 0.0;
				   Statement stm2 = con.createStatement();
				   sql = "select cur_pri, (cur_pri - yt_cls_pri) / yt_cls_pri pct from stkdat2 s where s.ft_id = (select max(ft_id) max_ft_id from stkdat2 where id = '" + rs.getString("id") + "')";
				   
				   log.info(sql);
				   
				   ResultSet rs2 = stm2.executeQuery(sql);
				   if (rs2.next()) {
					   cur_pri = rs2.getDouble("cur_pri");
					   pct = rs2.getDouble("pct");
				   }
				   
				   rs2.close();
				   stm2.close();
				   
			       str += "<tr id=\"" + rs.getString("id") + "\">" +
			       "<td>" + i + "</td> " +
			       "<td>" + rs.getString("id") + "</td> " +
			       "<td>" + rs.getString("name") + "</td> " +
			       "<td>" + cur_pri + "</td> " +
			       "<td><font size=\"3\" color=\"" + ((pct>0)? "red":"green") + "\">" + df.format(pct * 100) + "%</font></td> " +
			       "<td>" + rs.getInt("stop_trade_mode_flg") + "</td> " +
			       "<td>" + rs.getString("suggested_by_selector") + "</td> " +
			       "<td>" + rs.getString("suggested_comment") + "</td> " +
			       "</tr>";
			}
			
			rs.close();
			stm.close();
			
		    str += "</tbody></table></div>";
		}
		catch(Exception e) {
			log.error(e.getCause(), e);
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getCause(), e);
			}
		}
	    return str;
	}
	
	public static boolean putStockIntoTrade(String stkid) {
		
		Connection con = DBManager.getConnection();
		try {
			Statement stm = con.createStatement();
			String sql = "update usrstk set suggested_by = 'SYSTEM_GRANTED_TRADER', stop_trade_mode_flg = 0, mod_dt = sysdate() where id = '" + stkid + "'";
			
			log.info(sql);
			stm.execute(sql);
			
			StockMarket.addGzStocks(stkid);
		}
		catch(Exception e) {
			log.error(e.getCause(), e);
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getCause(), e);
			}
		}
	    return true;
	}
	
	public static boolean putStockIntoSuggest(String stkid) {
		
		Connection con = DBManager.getConnection();
		try {
			Statement stm = con.createStatement();
			String sql = "update usrstk set suggested_by = 'SYSTEM_SUGGESTER', mod_dt = sysdate() where id = '" + stkid + "'";
			
			log.info(sql);
			stm.execute(sql);
			StockMarket.removeGzStocks(stkid);
		}
		catch(Exception e) {
			log.error(e.getCause(), e);
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getCause(), e);
			}
		}
	    return true;
	}
	
	
	public static String getTradingStocksAsTableString() {
		
		String str = "<div id=\"tradingStocks\"> <table border=\"0\" style=\"margin: auto; width: 100%; height:100%;\">" +
		"<thead> " +
	    "<tr>                                      " +
	    "    <td>No.</td>                     " +
	    "    <td>Stock ID</td>                     " +
	    "    <td>Name</td>                         " +
	    "    <td>Price</td>                " +
	    "    <td>Percent</td>              " +
	    "    <td>Stop Trading</td>                         " +
	    "    <td>Suggest Selector</td>             " +
	    "    <td>Suggest Comment</td>              " +
	    "</tr>                                     " +
	    "</thead>";

	    str += "<tbody> ";
	    
		Connection con = DBManager.getConnection();
		try {
			Statement stm = con.createStatement();
			ResultSet rs = null;
			String sql = "select s.id, s.name, u.stop_trade_mode_flg, u.suggested_by_selector, u.suggested_comment from stk s join usrstk u on s.id = u.id where u.gz_flg = 1 and u.suggested_by <> 'SYSTEM_SUGGESTER'";
			
			log.info(sql);
			rs = stm.executeQuery(sql);
			
			int i = 0;
			DecimalFormat df = new DecimalFormat("##.##");
			
			while(rs.next()) {
				   i++;
				   
				   double cur_pri = 0.0;
				   double pct = 0.0;
				   Statement stm2 = con.createStatement();
				   sql = "select cur_pri, (cur_pri - yt_cls_pri) / yt_cls_pri pct from stkdat2 s where s.ft_id = (select max(ft_id) max_ft_id from stkdat2 where id = '" + rs.getString("id") + "')";
				   
				   log.info(sql);
				   
				   ResultSet rs2 = stm2.executeQuery(sql);
				   if (rs2.next()) {
					   cur_pri = rs2.getDouble("cur_pri");
					   pct = rs2.getDouble("pct");
				   }
				   
				   rs2.close();
				   stm2.close();
				   
			       str += "<tr id=\"" + rs.getString("id") + "\">" +
			       "<td>" + i + "</td> " +
			       "<td>" + rs.getString("id") + "</td> " +
			       "<td>" + rs.getString("name") + "</td> " +
			       "<td>" + cur_pri + "</td> " +
			       "<td><font size=\"3\" color=\"" + ((pct>0)? "red":"green") + "\">" + df.format(pct * 100) + "%</font></td> " +
			       "<td>" + rs.getInt("stop_trade_mode_flg") + "</td> " +
			       "<td>" + rs.getString("suggested_by_selector") + "</td> " +
			       "<td>" + rs.getString("suggested_comment") + "</td> " +
			       "</tr>";
			}
			
			rs.close();
			stm.close();
			
		    str += "</tbody></table></div>";
		}
		catch(Exception e) {
			log.error(e.getCause(), e);
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getCause(), e);
			}
		}
	    return str;
	}
}
