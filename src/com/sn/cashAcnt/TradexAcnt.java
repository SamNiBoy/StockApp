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
import com.sn.mail.reporter.StockObserverable;
import com.sn.sim.SimStockDriver;
import com.sn.sim.strategy.imp.STConstants;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.trader.TradexAccount;
import com.sn.trader.TradexCpp;
import com.sn.trader.TradexStockInHand;

public class TradexAcnt implements ICashAccount {

	static Logger log = Logger.getLogger(CashAcnt.class);
    private TradexCpp tc = new TradexCpp();
    private TradexAccount ta = null;
    private TradexStockInHand tsih = null;
    
    private String actId;
    private double initMny;
    private double usedMny;
    private double usedMny_Hrs;
    private double maxMnyPerTrade = STConstants.DFT_MAX_MNY_PER_TRADE;
    private double pftMny;
    private double maxUsePct=STConstants.DFT_MAX_USE_PCT;

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

	public double getMaxMnyForTrade() {

		double useableMny = ta.getUsable_mny();
		if (useableMny > maxMnyPerTrade) {
			return maxMnyPerTrade;
		} else {
			return useableMny;
		}
	}

	public boolean loadAcnt() {

		log.info("load TradexAcnt info from Tradex system");
		try {
            ta = tc.processLoadAcnt();
            
            actId = ta.getAcntID();
            initMny = ta.getInit_mny();
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
				+ "<td> " + this.maxMnyPerTrade + "<td> " + this.maxUsePct + "</td>" + "<td> " + dt + "</td> </tr> </table>";
        
		
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
        
		loadAcnt();
		
		log.info("##################################################################################################");
		log.info("|AccountId|InitMny\t|UseableMny\t|WithdrawableMny|Total Mny\t|Stock Mny\t|MaxMnyPerTrade\t|MaxUsePct\t|");
		log.info("|" + ta.getAcntID() + "\t|" + df.format(ta.getInit_mny()) + "\t|" + df.format(ta.getUsable_mny()) + "\t|" + df.format(ta.getFetchable_mny()) +"\t|" + df.format(ta.getTotal_value()) + "\t|"
		        + df.format(ta.getStock_value())
				+ "\t\t|" + this.maxMnyPerTrade + "\t|" + df.format(this.maxUsePct) + "\t|");
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
        try {
            tsih = tc.processQueryStockInHand(stkId);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.info("in getSellalbeAmt errored:" + e.getMessage());
            return 0;
        }
        log.info("in getSellalbeAmt get sellable qty:" + tsih.getAvailable_qty());
        return tsih.getAvailable_qty();
    }

    public int getUnSellableAmt(String stkId, String sellDt) {
        // TODO Auto-generated method stub
        try {
            tsih = tc.processQueryStockInHand(stkId);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.info("in getUnSellableAmt errored:" + e.getMessage());
            return 0;
        }
        log.info("in getUnSellableAmt get unsellable qty:" + tsih.getFrozen_qty());
        return tsih.getFrozen_qty();
    }

    public double getUsedMnyHrs() {
        // TODO Auto-generated method stub
        return usedMny_Hrs;
    }
}
