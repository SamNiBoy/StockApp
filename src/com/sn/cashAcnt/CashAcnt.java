package com.sn.cashAcnt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.mail.StockObserverable;
import com.sn.simulation.SimStockDriver;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class CashAcnt implements ICashAccount {

	static Logger log = Logger.getLogger(CashAcnt.class);
	private String actId;
	private double initMny;
	private double usedMny;
	private double usedMny_Hrs;
	private double pftMny;
	private double maxMnyPerTrade;
	private double maxUsePct;

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

	public double getMaxMnyForTrade() {
		return maxMnyPerTrade;
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
				usedMny_Hrs = rs.getDouble("used_mny_hrs");
				pftMny = rs.getDouble("pft_mny");
				maxMnyPerTrade = rs.getDouble("max_mny_per_trade");
				maxUsePct = rs.getDouble("max_useable_pct");
				log.info("actId:" + actId + " initMny:" + initMny + " usedMny:" + usedMny + " usedMny_Hrs:" + usedMny_Hrs + " calMny:" + pftMny
						+ " maxMnyPerTrade:" + maxMnyPerTrade + " max_useable_pct:" + maxUsePct);
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

	public double getMaxMnyPerTrade() {
		return maxMnyPerTrade;
	}

	public void setMaxMnyPerTrade(int maxMnyPerTrade) {
		this.maxMnyPerTrade = maxMnyPerTrade;
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

	public boolean calProfit() {
        
	    double my_releasedMny = 0;
	    double my_occupiedMny = 0; //This money means used amount during trading, you need to keep this amount in accourt to assure trade success.
	    double my_usedMny_Hrs = 0;
	    double my_delta_mny = 0;
        Timestamp pre_dl_dt = null;
    	Timestamp dl_dt = null;
	    
		Connection con = DBManager.getConnection();
		ResultSet rs = null;
		try {
			Statement stm = con.createStatement();
            
     		String sql = "select * from Tradedtl d where d.acntId = '" + actId + "' order by stkId, seqnum";
             
            log.info(sql); 
            
			rs = stm.executeQuery(sql);
    		double occupiedMny = 0.0;
            
			while (rs.next()) {
				boolean buy_flg = rs.getBoolean("buy_flg");
				double amount = rs.getDouble("amount");
				double price = rs.getDouble("price");
            	dl_dt = rs.getTimestamp("dl_dt");
                
				double dealMny = 0.0;
                
    		    dealMny = amount * price;
                
                log.info("calculate this time occupied mny with param:");
                log.info("buy_flg:" + buy_flg);
                log.info("amount:" + amount);
                log.info("price:" + price);
                log.info("dealMny:" + dealMny);
                
    		    occupiedMny = 0.0;
                
				if (buy_flg) {
				    if (dealMny > my_releasedMny) {
         			     occupiedMny = dealMny - my_releasedMny;
       				     my_releasedMny = 0;
				    }
				    else {
         			     occupiedMny = 0;
         			     my_releasedMny = my_releasedMny - dealMny;
				    }
				    my_delta_mny -= dealMny;
				}
				else {
				    my_releasedMny += amount * price;
				    my_delta_mny += dealMny;
				}
                
                
				my_occupiedMny += occupiedMny;
                
                log.info("calculated this time occupiedMny:" + occupiedMny + ", total occupiedMny:" + my_occupiedMny);
                
                if (pre_dl_dt == null)
                {
                    pre_dl_dt = dl_dt; 
                    log.info("first deail time:" + pre_dl_dt.toString());
                }
			}
            
			
			if (pre_dl_dt != null && my_occupiedMny > 0) {
			    my_usedMny_Hrs = (dl_dt.getTime() - pre_dl_dt.getTime()) / 3600000.0;
			}
            
		    //pftMny = my_releasedMny;
            usedMny = my_occupiedMny;
            usedMny_Hrs = my_usedMny_Hrs;
            
            log.info("calculated pftMny:" + pftMny + ", useMny:" + usedMny + ", usedMny_Hrs:" + usedMny_Hrs);

            rs.close();
            stm.close();
            
		    sql = "update CashAcnt set pft_mny = " + my_delta_mny + " + (select sum(in_hand_qty * in_hand_stk_price) from TradeHdr where acntId = '" + actId + "')" +
		          ", used_mny = " + usedMny +
		          ", used_mny_hrs = " + usedMny_Hrs +
		          " where acntId = '" + actId + "'";
            
		    stm = con.createStatement();
            
		    log.info(sql);
            
		    stm.execute(sql);
            
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
				+ "<th> Used Money </th> " + "<th> Used Money Hrs</th> "+ "<th> Max Money per Trade</th> " + "<th> MaxUse Pct</th> "
				+ "<th> Account Profit</th> " + "<th> Report Date</th> </tr> ";

		String dt = "";
		SimpleDateFormat f = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
		Date date = new Date();
		dt = f.format(date);

		msg += "<tr> <td>" + actId + "</td>" + "<td> " + initMny + "</td>" + "<td> " + usedMny + "</td>"+ "<td> " + usedMny_Hrs + "</td>" + "<td> "
				+ maxMnyPerTrade + "</td>" + "<td> " + maxUsePct + "</td>" + "<td> " + "</td>"
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
		log.info("|AccountId\t|InitMny\t|UsedMny\t|UsedMny_Hrs\\t|PftMny\t|maxMnyPerTrade\t|MaxUsePct\t|ProfictPct|Profit|");
		log.info("|" + actId + "\t|" + df.format(initMny) + "\t\t|" + df.format(usedMny) + "\t\t|" + df.format(usedMny_Hrs) + "\t\t|" + df.format(pftMny)
				+ "\t|" + maxMnyPerTrade + "\t\t|" + df.format(maxUsePct) + "\t\t|" + pftPct + "%\t|"
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
		String sql = "delete from cashacnt where acntid = 'testCashAct001'";
		try {
			stm = con.createStatement();
			stm.execute(sql);
			stm.close();
			stm = con.createStatement();
			sql = "insert into cashacnt values('testCashAct001',50000,0,0,0,10000,0.8,sysdate())";
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

    public double getUsedMnyHrs() {
        // TODO Auto-generated method stub
        return usedMny_Hrs;
    }

	@Override
	public boolean refreshProfitWithCurPri(Stock2 s) {
		// TODO Auto-generated method stub
		log.info("refresh stock " + s.getName() + " profit with latest price, CashAcount: " + actId);
		
		if (s.getCur_pri() == null) {
			log.info("skip refresh as cur_pri is null.");
			return false;
		}

		Connection con = DBManager.getConnection();
		try {
			
			String sql = "select 'x' from tradehdr where in_hand_qty > 0 and acntId = '" + actId + "'";
			
			Statement stm = con.createStatement();
			
			log.info(sql);
			ResultSet rs = stm.executeQuery(sql);
			
			if (rs.next()) {
				rs.close();
				stm.close();
				
			    sql = "update tradehdr set in_hand_stk_price = " + s.getCur_pri() + ", in_hand_stk_mny = in_hand_qty * " + s.getCur_pri()
			               + "  where acntId = '" + actId + "'";
                
			    log.info(sql);
			    stm = con.createStatement();
			    stm.execute(sql);
			    stm.close();
			    
			    log.info("now recalculate profit after update cur_pri...");
			    calProfit();
			    log.info("after calProfit call in refreshProfitWithCurPri.");
			}
			else {
				log.info("No tradehdr record, no need to refresh profit.");
				rs.close();
				stm.close();
				return false;
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			log.info("exception happened here?");
			log.error(e.getMessage(), e);
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error(e.getCause(), e);
			}
		}
		return true;
	}
}
