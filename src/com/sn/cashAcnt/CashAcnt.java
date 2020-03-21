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

public class CashAcnt implements ICashAccount {

	static Logger log = Logger.getLogger(CashAcnt.class);
	private String actId;
	private double initMny;
	private double usedMny;
	private double pftMny;
	private int splitNum;
	private double maxUsePct;
	private boolean dftAcnt;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

        CashAcnt ac = new CashAcnt("ACNT000975");
        ac.reportAcntProfitWeb();
	}

	public CashAcnt(String id) {

		loadAcnt(id);
	}

	public double getMaxAvaMny() {

		double useableMny = initMny * maxUsePct;
		if ((useableMny - usedMny) > initMny / splitNum) {
			return initMny / splitNum;
		} else {
			return useableMny - usedMny;
		}
	}

	public boolean loadAcnt(String id) {
		Connection con = DBManager.getConnection();
		String sql = "select * from CashAcnt where acntId = '" + id + "'";

		log.info("load cashAcnt info from db:" + id);
		log.info(sql);
		try {
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);

			if (rs.next()) {
				actId = id;
				initMny = rs.getDouble("init_mny");
				usedMny = rs.getDouble("used_mny");
				pftMny = rs.getDouble("pft_mny");
				splitNum = rs.getInt("split_num");
				dftAcnt = rs.getInt("dft_acnt_flg") > 0;
				maxUsePct = rs.getDouble("max_useable_pct");
				log.info("actId:" + actId + " initMny:" + initMny + " usedMny:" + usedMny + " calMny:" + pftMny
						+ " splitNum:" + splitNum + " max_useable_pct:" + maxUsePct + " dftAcnt:" + dftAcnt);
			}
			rs.close();
			con.close();
			con = null;
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public String getActId() {
		return actId;
	}

	public void setActId(String actId) {
		this.actId = actId;
	}

	public double getInitMny() {
		return initMny;
	}

	public void setInitMny(double initMny) {
		this.initMny = initMny;
	}

	public double getUsedMny() {
		return usedMny;
	}

	public void setUsedMny(double usedMny) {
		this.usedMny = usedMny;
	}

	public double getPftMny() {
		return pftMny;
	}

	public void setPftMny(double calMny) {
		this.pftMny = calMny;
	}

	public int getSplitNum() {
		return splitNum;
	}

	public void setSplitNum(int splitNum) {
		this.splitNum = splitNum;
	}

	public boolean isDftAcnt() {
		return dftAcnt;
	}

	public void setDftAcnt(boolean dftAcnt) {
		this.dftAcnt = dftAcnt;
	}

	public int getSellableAmt(String stkId, String sellDt) {
		Connection con = DBManager.getConnection();
		String sql = "select case when sum(b.amount) is null then 0 else sum(b.amount) end SellableAmt from TradeDtl b "
				+ "      where b.stkId = '" + stkId + "'" + "        and acntId = '" + actId + "'"
				+ "        and b.buy_flg = 1 " + "        and left(dl_dt, 10) < '" + sellDt + "' "
				+ "      order by b.seqnum";
		ResultSet rs = null;

		int sellableAmt = 0;
		int soldAmt = 0;

		try {
			Statement stm = con.createStatement();
			log.info(sql);
			rs = stm.executeQuery(sql);
			if (rs.next()) {
				sellableAmt = rs.getInt("SellableAmt");
			}

			rs.close();
			stm.close();

		    stm = con.createStatement();
		    sql = "select case when sum(b.amount) is null then 0 else sum(b.amount) end SoldAmt from TradeDtl b "
		    		+ "      where b.stkId = '" + stkId + "'" + "        and acntId = '" + actId + "'"
		    		+ "        and b.buy_flg = 0 " + "      order by b.seqnum";
            
		    log.info(sql);
		    rs = stm.executeQuery(sql);
		    if (rs.next()) {
		    	soldAmt = rs.getInt("SoldAmt");
		    }
		    rs.close();
		    stm.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		log.info("sellable/Sold Amt for :" + stkId + " is (" + sellableAmt + "/" + soldAmt + ")");
		return sellableAmt - soldAmt;
	}

	public int getUnSellableAmt(String stkId, String sellDt) {
		Connection con = DBManager.getConnection();
		String sql = "select case when sum(b.amount) is null then 0 else sum(b.amount) end unSellableAmt from TradeDtl b "
				+ "      where b.stkId = '" + stkId + "'" + "        and b.acntId = '" + actId + "'"
				+ "        and b.buy_flg = 1 " + "        and left(b.dl_dt, 10) = '" + sellDt + "' "
				+ "      order by b.seqnum";
		ResultSet rs = null;

		int unSellableAmt = 0;

		try {
			Statement stm = con.createStatement();
			log.info(sql);
			rs = stm.executeQuery(sql);
			if (rs.next()) {
				unSellableAmt = rs.getInt("unSellableAmt");
			}
			rs.close();
			stm.close();
			con.close();
			con = null;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		log.info("unsellable Amt for :" + stkId + " is (" + unSellableAmt + ")");
		return unSellableAmt;
	}

	public boolean calProfit(String ForDt, Map<String, Stock2> stockSet) {
		Connection con = DBManager.getConnection();

		String sql = "select stkId from TradeHdr h where h.acntId = '" + actId + "'";

		ResultSet rs = null;
		pftMny = 0;
		double in_hand_stk_mny = 0;
		try {
			Statement stm = con.createStatement();
			rs = stm.executeQuery(sql);
			Map<String, Stock2> stks = stockSet;
			while (rs.next()) {
				String stkId = rs.getString("stkId");
				Stock2 s = stks.get(stkId);

				int inHandMnt = getSellableAmt(stkId, ForDt) + getUnSellableAmt(stkId, ForDt);

				log.info("in hand amt:" + inHandMnt + " price:" + s.getCur_pri() + " with cost:" + usedMny);
				in_hand_stk_mny = inHandMnt * s.getCur_pri();

				sql = "update TradeHdr set in_hand_stk_mny = " + in_hand_stk_mny + ", in_hand_stk_price =" + s.getCur_pri()
						+ ", in_hand_qty = " + inHandMnt + " where acntId ='" + actId + "' and stkId ='" + stkId + "'";
				Statement stm2 = con.createStatement();
				log.info(sql);
				stm2.execute(sql);
				stm2.close();
			}

			rs.close();
			stm.close();

			sql = "select sum(in_hand_stk_mny) in_hand_stk_mny from TradeHdr h where acntId = '" + actId + "'";

			stm = con.createStatement();
			rs = stm.executeQuery(sql);

			double in_hand_stk_mny2 = 0.0;
			if (rs.next()) {
			    in_hand_stk_mny2 = rs.getDouble("in_hand_stk_mny");
				sql = "update CashAcnt set pft_mny = " + in_hand_stk_mny2 + " - used_mny where acntId = '" + actId + "'";
				Statement stm2 = con.createStatement();

				pftMny = in_hand_stk_mny2 - usedMny;

				log.info(sql);

				stm2.execute(sql);
				stm2.close();
			}
			rs.close();
			stm.close();
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
			log.info("calProfit returned with exception:" + e.getMessage());
			return false;
		}
		return true;
	}

	public String reportAcntProfitWeb() {
		String msg = "<b>Account: " + this.actId + " profit report</b><br/>";
		msg += "<table border = 1>" + "<tr>" + "<th> Cash Account</th> " + "<th> Init Money </th> "
				+ "<th> Used Money </th> " + "<th> Split Number </th> " + "<th> MaxUse Pct</th> "
				+ "<th> Default Account</th> " + "<th> Account Profit</th> " + "<th> Report Date</th> </tr> ";

		String dt = "";
		SimpleDateFormat f = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
		Date date = new Date();
		dt = f.format(date);

		msg += "<tr> <td>" + actId + "</td>" + "<td> " + initMny + "</td>" + "<td> " + usedMny + "</td>" + "<td> "
				+ splitNum + "</td>" + "<td> " + maxUsePct + "</td>" + "<td> " + (dftAcnt ? "yes" : "No") + "</td>"
				+ "<td> " + pftMny + "</td>" + "<td> " + dt + "</td> </tr> </table>";
        
		
	      String headTran = "<b>Transactions header information:</b><br/>";
    	  String detailTran = "";

	      headTran += "<table border = 1>" + "<tr>" + "<th> Cash Account</th> " + "<th> Stock Id </th> "
	                + "<th> In Hand Quantity</th> " + "<th> Stock Price </th> " + "<th> In Hand Stock Money</th> " + "<th> Add Date</th> </tr> ";

	      Connection con = DBManager.getConnection();
	      String sql = "select  acntid, stkid, in_hand_qty, in_hand_stk_price, in_hand_stk_mny,   date_format(add_dt, '%Y-%m-%d %T') add_dt"
	                + " from TradeHdr h"
	                + " where h.acntId ='" + actId + "' order by h.stkId";

	      try {
	            Statement stm = con.createStatement();
	            ResultSet rs = stm.executeQuery(sql);
	            if (rs.next()) {
	                headTran += "<tr> <td>" + actId + "</td>" + "<td> " + rs.getString("stkId") + "</td>" + "<td> "
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
				+ " where d.acntId ='" + actId + "' order by d.stkId, d.seqnum ";

            rs.close();
            stm.close();
           
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			while (rs.next()) {
				detailTran += "<tr> <td>" + actId + "</td>" + "<td> " + rs.getString("stkId") + "</td>" + "<td> "
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
		String pftPct = df.format(pftMny / initMny * 100);
		String profit = df.format(pftMny);
		log.info("##################################################################################################");
		log.info("|AccountId\t|InitMny\t|UsedMny\t|PftMny\t|SplitNum\t|MaxUsePct\t|DftAcnt\t|ProfictPct|Profit|");
		log.info("|" + actId + "\t|" + df.format(initMny) + "\t\t|" + df.format(usedMny) + "\t\t|" + df.format(pftMny)
				+ "\t|" + splitNum + "\t\t|" + df.format(maxUsePct) + "\t\t|" + dftAcnt + "\t\t|" + pftPct + "%\t|"
				+ profit + "\t|");
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
					+ "from TradeHdr where acntId = '" + actId + "' order by stkid ";
			//log.info(sql);
			ResultSet rs = stm.executeQuery(sql);
			log.info("=======================================================================================");
			log.info("|AccountID\t|StockID\t|InHandStkMny\t|InHandQty\t|PftPrice|TranDt|");
			while (rs.next()) {
				log.info("|" + rs.getString("acntId") + "\t|" + rs.getString("stkId") + "\t\t|"
						+ rs.getString("in_hand_stk_mny") + "\t|" + rs.getInt("in_hand_qty") + "\t\t|"
						+ df.format(rs.getDouble("in_hand_stk_price")) + "\t|" + rs.getString("add_dt") + "|");
				Statement stmdtl = con.createStatement();
				String sqldtl = "select stkid, seqnum, price, amount, date_format(dl_dt, '%Y-%m-%d %T') dl_dt, buy_flg "
						+ "  from tradedtl where acntId ='" + actId + "' order by seqnum";
				//log.info(sql);

				ResultSet rsdtl = stmdtl.executeQuery(sqldtl);
				log.info("StockID\tSeqnum\tPrice\tAmount\tB/S\tsubTotal\tTranDt");
				while (rsdtl.next()) {
					log.info(rsdtl.getString("stkid") + "\t" + rsdtl.getInt("seqnum") + "\t"
							+ df.format(rsdtl.getDouble("price")) + "\t" + rsdtl.getInt("amount") + "\t"
							+ (rsdtl.getInt("buy_flg") > 0 ? "B" : "S") + "\t"
							+ df.format((rsdtl.getInt("buy_flg") > 0 ? -1 : 1) * rsdtl.getDouble("price")
									* rsdtl.getInt("amount"))
							+ "\t\t" + rsdtl.getString("dl_dt") + "\t");
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
		Connection con = DBManager.getConnection();
		Statement stm = null;
		String sql = "delete from cashacnt where dft_acnt_flg = 1";
		try {
			stm = con.createStatement();
			stm.execute(sql);
			stm.close();
			stm = con.createStatement();
			sql = "insert into cashacnt values('testCashAct001',50000,0,0,8,0.5,1,sysdate())";
			stm.execute(sql);
			stm.close();
			con.close();
			loadAcnt(actId);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean hasStockInHand(Stock2 s) {
		// TODO Auto-generated method stub
		log.info("now check if stock " + s.getName() + " in hand with price:" + s.getCur_pri() + " against CashAcount: "
				+ actId);

		Connection con = DBManager.getConnection();
		boolean hasStockInHand = false;
		try {
			String sql = "select sum(case when buy_flg = 1 then 1 else -1 end * amount) inHandQty "
					+ "  from Tradedtl d " + " where d.stkId = '" + s.getID() + "'" + "   and d.acntId = '" + actId
					+ "'";

			log.info(sql);
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);
			if (rs.next() && rs.getInt("inHandQty") > 0) {
				hasStockInHand = true;
			} else {
				hasStockInHand = false;
			}
			rs.close();
			stm.close();
			con.close();

			return hasStockInHand;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public double getInHandStockCostPrice(Stock2 s) {
		// TODO Auto-generated method stub
		log.info("get stock " + s.getName() + " in hand with current price:" + s.getCur_pri()
				+ " cost price against CashAcount: " + actId);

		Connection con = DBManager.getConnection();
		try {
			String sql = "select * from Tradedtl d  where d.stkId = '" + s.getID() + "'" + "  and d.acntId = '" + actId
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
		log.info("check stock " + s.getName() + " in hand used money ratio against CashAcount: " + actId);

		Connection con = DBManager.getConnection();
		try {
			String sql = "select sum(case when buy_flg = 1 then 1 else -1 end * amount * price) costMny from Tradedtl d  where d.stkId = '"
					+ s.getID() + "'" + "  and d.acntId = '" + actId + "'";

			log.info(sql);
			Statement stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);

			double ratio = 0;
			if (rs.next()) {
				log.info("costMny:" + rs.getDouble("costMny") + ", initMny:" + initMny);

				if (rs.getDouble("costMny") > 0) {
					ratio = rs.getDouble("costMny") / initMny;
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
		log.info("check stock " + s.getName() + " last buy price against CashAcount: " + actId);

		Connection con = DBManager.getConnection();
		try {
			String sql = "select * from Tradedtl d  where d.stkId = '" + s.getID() + "'" + "  and d.acntId = '" + actId
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
}
