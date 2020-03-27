package com.sn.cashAcnt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.mail.reporter.StockObserverable;
import com.sn.sim.SimStockDriver;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.trader.TradexAccount;
import com.sn.trader.TradexCpp;
import com.sn.trader.TradexStockInHand;

public class TradexAcnt implements ICashAccount {

	static Logger log = Logger.getLogger(CashAcnt.class);
    private TradexCpp tc = new TradexCpp();
    private TradexAccount ta = null;
    private double max_mny_per_trade = 10000;
    private double max_pct_for_stock = 0.8;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	    TradexAcnt ac = new TradexAcnt();
        ac.reportAcntProfitWeb();
	}

	public TradexAcnt() {

		loadAcnt();
	}

	public double getMaxAvaMny() {

		double useableMny = ta.getUsable_mny();
		if (useableMny > max_mny_per_trade) {
			return max_mny_per_trade;
		} else {
			return useableMny;
		}
	}

	public boolean loadAcnt() {

		log.info("load TradexAcnt info from Tradex system");
		try {
            ta = tc.processLoadAcnt();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
	     	log.info("load TradexAcnt error:" + e.getMessage());
             return false;
        }
		return true;
	}

	public String getActId() {
		return ta.getAcntID();
	}

	public double getInitMny() {
		return ta.getInit_mny();
	}

	public double getPftMny() {
		return 0;
	}

	public int getSplitNum() {
		return ta.getSplitNum();
	}

	public boolean isDftAcnt() {
		return false;
	}

	public boolean calProfit(String ForDt, Map<String, Stock2> stockSet) {
		Connection con = DBManager.getConnection();
        Statement stm = null;
        
		String sql = "select stkId from TradeHdr h where h.acntId = '" + ta.getAcntID() + "'";

		ResultSet rs = null;
		double in_hand_stk_mny = 0;
		try {
            stm = con.createStatement();
			rs = stm.executeQuery(sql);
			Map<String, Stock2> stks = stockSet;
			while (rs.next()) {
				String stkId = rs.getString("stkId");
				Stock2 s = stks.get(stkId);

                TradexStockInHand tsih;
                try {
                    tsih = tc.processQueryStockInHand(stkId);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
	     			log.info("CalProfit processQueryStockInHand error:" + e.getMessage());
                     return false;
                }
				int inHandMnt = tsih.getAvailable_qty();

				log.info("in hand amt:" + inHandMnt + " price:" + s.getCur_pri());
				in_hand_stk_mny = inHandMnt * s.getCur_pri();

				sql = "update TradeHdr set in_hand_stk_mny = " + in_hand_stk_mny + ", in_hand_stk_price =" + s.getCur_pri()
						+ ", in_hand_qty = " + inHandMnt + " where acntId ='" + ta.getAcntID() + "' and stkId ='" + stkId + "'";
				Statement stm2 = con.createStatement();
				log.info(sql);
				stm2.execute(sql);
				stm2.close();
			}

		} catch (SQLException e) {
			e.printStackTrace();
			log.info("calProfit returned with exception:" + e.getMessage());
			return false;
		}
		finally {
			try {
                rs.close();
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		}
		return true;
	}

    /*
    private double init_mny;
    private double usable_mny;
    private double fetchable_mny;
    private double stock_value;
    private double total_value;*/
	public String reportAcntProfitWeb() {
		String msg = "<b>Account: " + ta.getAcntID() + " report</b><br/>";
		msg += "<table border = 1>" + "<tr>" + "<th> Cash Account</th> " + "<th> Init Money </th> "
				+ "<th> Useable Money </th> " + "<th> Withdrawable Money</th> "+ "<th> Stock Value</th> "+ "<th> Total Value</th> "
		        +"<th> Max Money per Trade</th> " + "<th> MaxUse Pct</th> " + "<th> Report Date</th> </tr> ";

		String dt = "";
		SimpleDateFormat f = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
		Date date = new Date();
		dt = f.format(date);

		msg += "<tr> <td>" + ta.getAcntID() + "</td>" + "<td> " + ta.getInit_mny() + "</td>" + "<td> " + ta.getUsable_mny() + "</td>" + "<td> "
				+ ta.getFetchable_mny() + "</td>" + "<td> " + ta.getStock_value() + "</td>" + "<td> " + ta.getTotal_value() + "</td>"
				+ "<td> " + this.max_mny_per_trade + "<td> " + this.max_pct_for_stock + "</td>" + "<td> " + dt + "</td> </tr> </table>";
        
		
	      String headTran = "<b>Transactions header information:</b><br/>";
    	  String detailTran = "";

	      headTran += "<table border = 1>" + "<tr>" + "<th> Cash Account</th> " + "<th> Stock Id </th> "
	                + "<th> In Hand Quantity</th> " + "<th> Stock Price </th> " + "<th> In Hand Stock Money</th> " + "<th> Add Date</th> </tr> ";

	      Connection con = DBManager.getConnection();
	      String sql = "select  acntid, stkid, in_hand_qty, in_hand_stk_price, in_hand_stk_mny,   date_format(add_dt, '%Y-%m-%d %T') add_dt"
	                + " from TradeHdr h"
	                + " where h.acntId ='" + ta.getAcntID() + "' order by h.stkId";

	      try {
	            Statement stm = con.createStatement();
	            ResultSet rs = stm.executeQuery(sql);
	            if (rs.next()) {
	                headTran += "<tr> <td>" + ta.getAcntID() + "</td>" + "<td> " + rs.getString("stkId") + "</td>" + "<td> "
	                        + rs.getLong("in_hand_qty") + "</td>" + "<td> " + rs.getDouble("in_hand_stk_price") + "</td>" + "<td> "
	                        + rs.getInt("in_hand_stk_mny") + "</td>" 
	                        + "<td> " + rs.getString("add_dt") + "</td></tr>";
	            }

	        headTran += "</table>";

		    detailTran = "<b>Transaction details:</b><br/>";
            
		    detailTran += "<table border = 1>" + "<tr>" + "<th> Cash Account</th> " + "<th> Stock Id </th> "
				+ "<th> Sequence Number </th> " + "<th> Price </th> " + "<th> Amount </th> " + "<th> Buy/Sell </th> "
				+ "<th> Transaction Date</th> </tr> ";

		    sql = "select stkId," + "           seqnum," + "           round(price, 2) price," + "           amount,"
				+ "           buy_flg," + "           date_format(dl_dt, '%Y-%m-%d %T') dl_dt" + " from TradeDtl d "
				+ " where d.acntId ='" + ta.getAcntID() + "' order by d.stkId, d.seqnum ";

            rs.close();
            stm.close();
           
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			while (rs.next()) {
				detailTran += "<tr> <td>" + ta.getAcntID() + "</td>" + "<td> " + rs.getString("stkId") + "</td>" + "<td> "
						+ rs.getInt("seqnum") + "</td>" + "<td> " + rs.getDouble("price") + "</td>" + "<td> "
						+ rs.getInt("amount") + "</td>" + "<td> " + (rs.getInt("buy_flg") > 0 ? "B" : "S") + "</td>"
						+ "<td> " + rs.getString("dl_dt") + "</td></tr>";
			}

			detailTran += "</table>";
			rs.close();
			stm.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		String totMsg = msg + headTran + detailTran;
		log.info("got profit information:\n" + totMsg);

		return totMsg;

	}

	public void printAcntInfo() {
		DecimalFormat df = new DecimalFormat("##.##");
		log.info("##################################################################################################");
		log.info("|AccountId\t|InitMny\t|UseableMny\t|WithdrawableMny\t|MaxMnyPerTrade\t|MaxUsePct\t|");
		log.info("|" + ta.getAcntID() + "\t|" + df.format(ta.getInit_mny()) + "\t\t|" + df.format(ta.getUsable_mny()) + "\t\t|" + df.format(ta.getFetchable_mny())
				+ "\t|" + this.max_mny_per_trade + "\t\t|" + df.format(this.max_pct_for_stock) + "\t\t|");
		log.info("##################################################################################################");
	}

	@Override
	public void printTradeInfo() {
		// TODO Auto-generated method stub
		Connection con = DBManager.getConnection();
		Statement stm = null;
		String sql = null;
		DecimalFormat df = new DecimalFormat("##.##");
		try {
			stm = con.createStatement();
			sql = "select acntId," + "     stkId," + "    round(in_hand_stk_mny, 2) in_hand_stk_mny, " + "    in_hand_qty, "
					+ "    round(in_hand_stk_price, 2) in_hand_stk_price, " + "    date_format(add_dt, '%Y-%m-%d %T') add_dt "
					+ "from TradeHdr where acntId = '" + ta.getAcntID() + "' order by stkid ";
			//log.info(sql);
			ResultSet rs = stm.executeQuery(sql);
			log.info("=======================================================================================");
			log.info("|AccountID\t|StockID\t|InHandStkMny\t|InHandQty\t|CurStockPrice|TranDt|");
			while (rs.next()) {
				log.info("|" + rs.getString("acntId") + "\t|" + rs.getString("stkId") + "\t\t|"
						+ rs.getString("in_hand_stk_mny") + "\t|" + rs.getInt("in_hand_qty") + "\t\t|"
						+ df.format(rs.getDouble("in_hand_stk_price")) + "\t|" + rs.getString("add_dt") + "|");
				Statement stmdtl = con.createStatement();
				String sqldtl = "select stkid, seqnum, price, amount, date_format(dl_dt, '%Y-%m-%d %T') dl_dt, buy_flg, order_id "
						+ "  from tradedtl where acntId ='" + ta.getAcntID() + "' order by seqnum";
				//log.info(sql);

				ResultSet rsdtl = stmdtl.executeQuery(sqldtl);
				log.info("StockID\tSeqnum\tPrice\tAmount\tB/S\tsubTotal\tTranDt\tOrder_id");
				while (rsdtl.next()) {
					log.info(rsdtl.getString("stkid") + "\t" + rsdtl.getInt("seqnum") + "\t"
							+ df.format(rsdtl.getDouble("price")) + "\t" + rsdtl.getInt("amount") + "\t"
							+ (rsdtl.getInt("buy_flg") > 0 ? "B" : "S") + "\t"
							+ df.format((rsdtl.getInt("buy_flg") > 0 ? -1 : 1) * rsdtl.getDouble("price")
									* rsdtl.getInt("amount"))
							+ "\t\t" + rsdtl.getString("dl_dt") + "\t" + rsdtl.getString("order_id"));
				}
				rsdtl.close();
				stmdtl.close();
			}
			log.info("=======================================================================================");
			rs.close();
			stm.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean initAccount() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasStockInHand(Stock2 s) {
		// TODO Auto-generated method stub
		log.info("now check if stock " + s.getName() + " in hand with price:" + s.getCur_pri() + " against CashAcount: "
				+ ta.getAcntID());

		TradexStockInHand StockInHand;
        try {
            StockInHand = tc.processQueryStockInHand(s.getID());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

		return StockInHand.getAvailable_qty() > 0;
	}

	@Override
	public double getInHandStockCostPrice(Stock2 s) {
		// TODO Auto-generated method stub
		log.info("get stock " + s.getName() + " in hand with current price:" + s.getCur_pri()
				+ " cost price against CashAcount: " + ta.getAcntID());

		Connection con = DBManager.getConnection();
		try {
			String sql = "select * from Tradedtl d  where d.stkId = '" + s.getID() + "'" + "  and d.acntId = '" + ta.getAcntID()
					+ "' order by seqnum";

			log.info(sql);
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);
			int inhandQty = 0;
			double costmny = 0.0;
			double costpri = 0.0;
			while (rs.next()) {
				if (rs.getInt("buy_flg") > 0) {
					inhandQty += rs.getInt("amount");
					costmny += rs.getInt("amount") * rs.getDouble("price");
				} else {
					inhandQty -= rs.getInt("amount");
					costmny -= rs.getInt("amount") * rs.getDouble("price");
				}

				if (inhandQty == 0) {
					if (costmny > 0) {
						log.info("Sole all stock, but lost money:" + costmny);
						costmny = 0;
					} else {
						log.info("Sole all stock, but gain money:" + -costmny);
						costmny = 0;
					}
				}
			}
			log.info("InhandQty:" + inhandQty + ", costmny:" + costmny);
			if (inhandQty > 0) {
				costpri = costmny / inhandQty;
				log.info("costpri:" + costpri);
			}
			rs.close();
			stm.close();
			con.close();
			return costpri;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public double getStockCostRatio(Stock2 s) {
		// TODO Auto-generated method stub
		log.info("check stock " + s.getName() + " in hand used money ratio against CashAcount: " + ta.getAcntID());

		Connection con = DBManager.getConnection();
		try {
			String sql = "select sum(case when buy_flg = 1 then 1 else -1 end * amount * price) costMny from Tradedtl d  where d.stkId = '"
					+ s.getID() + "'" + "  and d.acntId = '" + ta.getAcntID() + "'";

			log.info(sql);
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);

			double ratio = 0;
			if (rs.next()) {
				log.info("costMny:" + rs.getDouble("costMny") + ", initMny:" + ta.getInit_mny());

				if (rs.getDouble("costMny") > 0) {
					ratio = rs.getDouble("costMny") / ta.getInit_mny();
				}
				log.info("ratio:" + ratio);
			}

			rs.close();
			stm.close();
			con.close();
			return ratio;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public Double getLstBuyPri(Stock2 s) {
		// TODO Auto-generated method stub
		log.info("check stock " + s.getName() + " last buy price against CashAcount: " + ta.getAcntID());

		Connection con = DBManager.getConnection();
		try {
			String sql = "select * from Tradedtl d  where d.stkId = '" + s.getID() + "'" + "  and d.acntId = '" + ta.getAcntID()
					+ "' order by seqnum desc";

			log.info(sql);
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);

			Double lstbuypri = 0.0;
			if (rs.next()) {
				lstbuypri = rs.getDouble("price");
				log.info("get last buy price:" + lstbuypri);
			}
			rs.close();
			stm.close();
			con.close();
			return lstbuypri;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

    public double getUsedMny() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setUsedMny(double usedMny) {
        // TODO Auto-generated method stub
        
    }

    public int getSellableAmt(String stkId, String sellDt) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getUnSellableAmt(String stkId, String sellDt) {
        // TODO Auto-generated method stub
        return 0;
    }
}
