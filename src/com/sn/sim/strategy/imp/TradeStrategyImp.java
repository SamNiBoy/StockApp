package com.sn.sim.strategy.imp;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.CashAcnt;
import com.sn.cashAcnt.CashAcntManger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.cashAcnt.TradexAcnt;
import com.sn.db.DBManager;
import com.sn.sim.strategy.ITradeStrategy;
import com.sn.sim.strategy.selector.buypoint.IBuyPointSelector;
import com.sn.sim.strategy.selector.sellpoint.ISellPointSelector;
import com.sn.sim.strategy.selector.stock.IStockSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.trader.TradexBuySellResult;
import com.sn.trader.TradexCpp;

public class TradeStrategyImp implements ITradeStrategy {

    static Logger log = Logger.getLogger(TradeStrategyImp.class);
    
    //interface vars.
    IBuyPointSelector buypoint_selector = null;
    ISellPointSelector sellpoint_selector = null;
    ICashAccount cash_account = null;
    
    private boolean sim_mode = false;
    
    private static TradexCpp tradex_trader = new TradexCpp();
    private static TradexAcnt tradex_acnt = new TradexAcnt();
    
    public IBuyPointSelector getBuypoint_selector() {
        return buypoint_selector;
    }

    public void setBuypoint_selector(IBuyPointSelector buypointSelector) {
        buypoint_selector = buypointSelector;
    }

    public ISellPointSelector getSellpoint_selector() {
        return sellpoint_selector;
    }

    public void setSellpoint_selector(ISellPointSelector sellpointSelector) {
        sellpoint_selector = sellpointSelector;
    }

    public ICashAccount getCash_account() {
        return cash_account;
    }

    public void setCash_account(ICashAccount cashAccount) {
        cash_account = cashAccount;
    }

    // non interface vars.
	static List<String> tradeStocks = new ArrayList<String>();
	static public Map<String, LinkedList<StockBuySellEntry>> tradeRecord = new HashMap<String, LinkedList<StockBuySellEntry>>();
    static Map<String, ICashAccount> cash_account_map = new HashMap<String, ICashAccount>();
    
	public StockBuySellEntry getLstTradeRecord(Stock2 s) {
		return tradeRecord.get(s.getID()).getLast();
	}

    public boolean isGoodPointtoBuy(Stock2 s) {
        return buypoint_selector.isGoodBuyPoint(s, cash_account);
    }

    public boolean isGoodPointtoSell(Stock2 s) {
        return sellpoint_selector.isGoodSellPoint(s, cash_account);
    }
    
    public TradeStrategyImp(IBuyPointSelector bs,
                            ISellPointSelector ses,
                            ICashAccount ca) {
        buypoint_selector = bs;
        sellpoint_selector = ses;
        cash_account = ca;
    }

	public static void printTradeInfor() {
		log.info("Print real trade record as:");

		LinkedList<StockBuySellEntry> tmp;
		for (String id : tradeRecord.keySet()) {
			tmp = tradeRecord.get(id);
			for (StockBuySellEntry e : tmp) {
				e.printStockInfo();
			}
		}
		log.info("End print real trade record");
	}
	
	@Override
	public boolean buyStock(Stock2 s) {
		// TODO Auto-generated method stub
		loadStocksForTrade();
		ICashAccount ac = getCashAcntForStock(s.getID());
		int qtb = 0;
		
		qtb = buypoint_selector.getBuyQty(s, ac);
		
		if (qtb <= 0) {
			log.info("qty to buy/sell is zero by Virtual CashAccount, switch to sellbuyrecord to get qtyToTrade.");
			qtb = getTradeQty(s, true, STConstants.openID);
		}
		
		if (canTradeRecord(s, true, STConstants.openID)) {
			
			String qtyToTrade = String.valueOf(qtb);
			LocalDateTime lt = LocalDateTime.now();
			int mnt = lt.getMinute();
            int order_id = -1;
			while(true) {
			    try {
			        // Save string like "B600503" to clipboard for buy stock.
			    	if (!sim_mode) {
			            /*String txt = "";
			            Clipboard cpb = Toolkit.getDefaultToolkit().getSystemClipboard();
			            txt = "B" + s.getID() + qtyToTrade;
			            
			            StringSelection sel = new StringSelection(txt);
			            cpb.setContents(sel, null);*/
                        order_id = placeBuyTradeToTradex(s, qtb, s.getCur_pri());
                        if (order_id < 0)
                        {
                            log.info("failed to placeBuyOrder to Tradex, skipping create tradehdr/tradedtl record.");
                            break;
                        }
			    	}
			        
			        createBuyTradeRecord(s, qtyToTrade, ac, order_id);
			        
			    	if (!sim_mode) {
			            createBuySellRecord(s, STConstants.openID, true, qtyToTrade);
			    	}
			    	else {
	                    Map<String, Stock2> sm = new HashMap<String, Stock2>();
	                    sm.put(s.getID(), s);
	                    log.info("TradeStock date string:" + s.getDl_dt().toString().substring(0, 10));
	                    ac.calProfit(s.getDl_dt().toString().substring(0, 10), sm);
			    	}
			        break;
			    }
			    catch (Exception e) {
			    	//e.printStackTrace();
			    	log.info("TradeStock Exception:" + e.getMessage());
			    	LocalDateTime lt2 = LocalDateTime.now();
			    	if (lt2.getMinute() != mnt) {
			    		log.info("Out of one minute scope, skip trading for:" +s.getID());
			    		return false;
			    	}
			    	else {
			    		log.info("Try again tradeStock after exception happened within one minute.");
			    	}
			    }
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean sellStock(Stock2 s) {
		// TODO Auto-generated method stub
		loadStocksForTrade();
		ICashAccount ac = getCashAcntForStock(s.getID());
		int qtb = 0;
		
		qtb = sellpoint_selector.getSellQty(s, ac);
		
		if (qtb <= 0 && !sim_mode) {
			log.info("qty to buy/sell is zero by Virtual CashAccount, switch to sellbuyrecord to get qtyToTrade.");
			qtb = getTradeQty(s, false, STConstants.openID);
		}
		else if (qtb <= 0 && sim_mode) {
			log.info("Simulation mode, can not sell before buy!");
			return false;
		}
		
		if (canTradeRecord(s, false, STConstants.openID)) {
			
			String qtyToTrade = String.valueOf(qtb);
			LocalDateTime lt = LocalDateTime.now();
			int mnt = lt.getMinute();
            int order_id = -1;
			while(true) {
			    try {
			        // Save string like "S600503" to clipboard for sell stock.
			    	if (!sim_mode) {
			            /*String txt = "";
			            Clipboard cpb = Toolkit.getDefaultToolkit().getSystemClipboard();
			            txt = "S" + s.getID() + qtyToTrade;
			            
			            StringSelection sel = new StringSelection(txt);
			            cpb.setContents(sel, null);*/
                        order_id = placeSellTradeToTradex(s, qtb, s.getCur_pri());
			    	    if (order_id < 0)
			    	    {
			    	        log.info("failed to placeSellOrder to Tradex, skipping create tradehdr/tradedtl record.");
                            break;
			    	    }
			    	}
			        
                    
			        createSellTradeRecord(s, qtyToTrade, ac, order_id);
			        
			    	if (!sim_mode) {
			            createBuySellRecord(s, STConstants.openID, false, qtyToTrade);
			    	}
			    	else {
	                    Map<String, Stock2> sm = new HashMap<String, Stock2>();
	                    sm.put(s.getID(), s);
	                    ac.calProfit(s.getDl_dt().toString().substring(0, 10), sm);
			    	}
			        break;
			    }
			    catch (Exception e) {
			    	//e.printStackTrace();
			    	log.info("TradeStock Exception:" + e.getMessage());
			    	LocalDateTime lt2 = LocalDateTime.now();
			    	if (lt2.getMinute() != mnt) {
			    		log.info("Out of one minute scope, skip trading for:" +s.getID());
			    		return false;
			    	}
			    	else {
			    		log.info("Try again tradeStock after exception happened within one minute.");
			    	}
			    }
			}
			return true;
		} else {
			return false;
		}
	}

    @Override
    public boolean calProfit(String ForDt, Map<String, Stock2>stockSet) {
        // TODO Auto-generated method stub
        return cash_account.calProfit(ForDt, stockSet);
    }

    @Override
    public boolean reportTradeStat() {
        // TODO Auto-generated method stub
    	for (String acnt :cash_account_map.keySet()) {
    		ICashAccount ac = cash_account_map.get(acnt);
            ac.printAcntInfo();
            ac.printTradeInfo();
    	}
        return false;
    }

    @Override
    public ICashAccount getCashAccount() {
        // TODO Auto-generated method stub
        return cash_account;
    }

    @Override
    public boolean initAccount() {
        // TODO Auto-generated method stub
        return cash_account.initAccount();
    }

    @Override
    public void setCashAccount(ICashAccount ca) {
        // TODO Auto-generated method stub
        setCash_account(ca);
    }

	@Override
	public boolean performTrade(Stock2 s) {
        
		if (isGoodPointtoBuy(s) && buyStock(s)) {
        	StockBuySellEntry rc = tradeRecord.get(s.getID()).getLast();
        	rc.printStockInfo();
        	return true;
        }
        else if(isGoodPointtoSell(s) && sellStock(s)) {
        	StockBuySellEntry rc = tradeRecord.get(s.getID()).getLast();
        	rc.printStockInfo();
        	return true;
        }
        return false;
	}
	
	public static boolean loadStocksForTrade() {
		String sql;
		tradeStocks.clear();
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "select s.*, u.* " + "from usrStk s," + "     usr u " + "where s.openID = u.openID "
					+ "and s.gz_flg = 1 " + "and u.openID = '" + STConstants.openID + "' and length(u.mail) > 1 "
					+ "and s.suggested_by in ('" + STConstants.openID +"','" + STConstants.SUGGESTED_BY_FOR_SYSTEMGRANTED + "') and u.buy_sell_enabled = 1";

			log.info(sql);
			ResultSet rs = stm.executeQuery(sql);
			while (rs.next()) {
				log.info("Loading stock:" + rs.getString("id") + " for user openID:" + rs.getString("openID"));
				tradeStocks.add(rs.getString("id"));
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private boolean canTradeRecord(Stock2 s, boolean is_buy_flg, String openID) {
		//For sim_mode, we don't care if user gzed the stock.
		if ((tradeStocks == null || !tradeStocks.contains(s.getID())) && !sim_mode) {
			log.info("stock " + s.getID() + " is not available for trade.");
			return false;
		}
		
		if (isInSellMode(openID, s) && is_buy_flg) {
			log.info("Stock:" + s.getID() + " is in sell mode, can not buy it!");
			return false;
		}

		if (!skipRiskCheck(openID, is_buy_flg, s)) {

			boolean overall_risk = false;
			// If recently we lost continuously overall, stop trading.
			if (stopTradeForPeriod(openID, 5)) {
				log.info("Now account " + openID + " trade unsuccess for 3 days, stop trade.");
				overall_risk = true;
			}

			// If recently we lost continuously for the stock, stop trading.
			if (stopTradeForStock(openID, s, 5)) {
				log.info("Now account " + openID + " trade unsuccess for 3 days for stock:" + s.getName()
						+ ", stop trade.");
				if (overall_risk && !is_buy_flg) {
					log.info("Now both overall risk or risk for stock:" + s.getName()
							+ " reached, howeve it's sell trade, allow it!");
				} else {
					log.info("Skip trade for stock:" + s.getName() + " after eached it's risk.");
					return false;
				}
			} else if (overall_risk && is_buy_flg) {
				log.info("No buy allowed, as overall risk reached, even single stock:" + s.getName()
						+ "'s risk is not reached yet.");
				return false;
			}
		}

		int totalCnt = 0;
		LinkedList<StockBuySellEntry> tmp;
		for (String id : tradeRecord.keySet()) {
			tmp = tradeRecord.get(id);
			totalCnt += tmp.size();
		}

		log.info("Total traded " + totalCnt + " times.");

		if (totalCnt >= STConstants.MAX_TRADE_TIMES_PER_DAY) {
			log.info("Trade limit for a day is: " + STConstants.MAX_TRADE_TIMES_PER_DAY + " can not trade today!");
			return false;
		}

		LinkedList<StockBuySellEntry> rcds = tradeRecord.get(s.getID());
		if (rcds != null) {
			if (rcds.size() >= STConstants.MAX_TRADE_TIMES_PER_STOCK) {
				log.info("stock " + s.getID() + " alread trade " + rcds.size() + " times, can not trade today.");
				return false;
			} else {
				int sellCnt = 0;
				int buyCnt = 0;
				for (StockBuySellEntry sd : rcds) {
					if (sd.is_buy_point) {
						buyCnt++;
					} else {
						sellCnt++;
					}
				}
				
				if (buyCnt >= STConstants.MAX_TRADE_TIMES_BUY_OR_SELL_PER_STOCK || sellCnt >= STConstants.MAX_TRADE_TIMES_BUY_OR_SELL_PER_STOCK) {
					log.info("Stock:" + s.getID() + " buy/sell reached limit:" + STConstants.MAX_TRADE_TIMES_BUY_OR_SELL_PER_STOCK);
					return false;
				}
				log.info("For stock " + s.getID() + " total sellCnt:" + sellCnt + ", total buyCnt:" + buyCnt);

				// We only allow buy BUY_MORE_THEN_SELL_CNT more than sell.
				if (buyCnt >= sellCnt + STConstants.BUY_MORE_THEN_SELL_CNT && is_buy_flg) {
					log.info("Bought more than sold, can won't buy again.");
					return false;
				}
				// else if (stk.is_buy_point) {
				// StockBuySellEntry lst = rcds.getLast();
				// if (!lst.is_buy_point && lst.price <= stk.price) {
				// log.info("Skip buy with higher price than previous sell.");
				// return false;
				// }
				// }
				else {
					// If we just sold/buy it, and now the price has no
					// significant change, we will not do the same trade.
					StockBuySellEntry lst = rcds.getLast();
					if (is_buy_flg == lst.is_buy_point && Math.abs((s.getCur_pri() - lst.price)) / lst.price <= 0.01) {
						log.info("Just " + (is_buy_flg ? "buy" : "sell") + " this stock with similar prices "
								+ s.getCur_pri() + "/" + lst.price + ", skip same trade.");
						return false;
					}
				}
				log.info("Adding trade record for stock as: " + s.getID());
				StockBuySellEntry stk = new StockBuySellEntry(s.getID(),
                                                              s.getName(),
                                                              (double)s.getSd().getCur_pri_lst().get(s.getSd().getCur_pri_lst().size() - 1),
                                                              is_buy_flg,
                                                              (Timestamp)s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1));
				stk.printStockInfo();
			    rcds.add(stk);
				return true;
			}
		} else {
			log.info("Adding today first trade record for stock as: " + s.getID());
			StockBuySellEntry stk = new StockBuySellEntry(s.getID(),
                    s.getName(),
                    (double)s.getSd().getCur_pri_lst().get(s.getSd().getCur_pri_lst().size() - 1),
                    is_buy_flg,
                    (Timestamp)s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1));
			stk.printStockInfo();
			rcds = new LinkedList<StockBuySellEntry>();
			rcds.add(stk);
			tradeRecord.put(stk.id, rcds);
			return true;
		}
	}
	
	private static boolean stopTradeForPeriod(String openID, int days) {
		String sql;
		boolean shouldStopTrade = false;
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "select * from SellBuyRecord " + " where openID ='" + openID + "'" + "   and dl_dt >= sysdate() - interval "
					+ days
					+ " day order by stkid, sb_id";
			log.info(sql);
			ResultSet rs = stm.executeQuery(sql);

			String pre_stkID = "";
			String stkID = "";
			int incCnt = 0;
			int descCnt = 0;

			double pre_price = 0.0;
			double price = 0.0;

			int pre_buy_flg = 1;
			int buy_flg = 1;

			while (rs.next()) {

				stkID = rs.getString("stkid");
				price = rs.getDouble("price");
				buy_flg = rs.getInt("buy_flg");

				if (pre_stkID.length() > 0 && stkID.equals(pre_stkID)) {
					log.info("stock:" + stkID + " buy_flg:" + buy_flg + " with price:" + price + " and pre_buy_flg:"
							+ pre_buy_flg + " with price:" + pre_price);
					if (buy_flg == 1 && pre_buy_flg == 0) {
						if (price < pre_price) {
							incCnt++;
						} else {
							descCnt++;
						}
					} else if (buy_flg == 0 && pre_buy_flg == 1) {
						if (price > pre_price) {
							incCnt++;
						} else {
							descCnt++;
						}
					} else {
						log.info("continue buy or sell does not means success or fail trade, continue.");
					}
					pre_stkID = stkID;
					pre_price = price;
					pre_buy_flg = buy_flg;
				} else {
					pre_stkID = stkID;
					pre_price = price;
					pre_buy_flg = buy_flg;
					continue;
				}
			}

			log.info("stopTradeForPeriod, incCnt:" + incCnt + " descCnt:" + descCnt);

			// For all stocks traded, if there are 20 times fail, stop trading.
			if ((incCnt + descCnt) > 20 && descCnt * 1.0 / (incCnt + descCnt) > 0.5) {
				log.info("For passed " + days + " days, trade descCnt:" + descCnt + ", 50% more than incCnt:" + incCnt
						+ " stop trade!");
				shouldStopTrade = true;
			} else {
				log.info("For passed " + days + " days, trade descCnt:" + descCnt + ", less than 50% incCnt:" + incCnt
						+ " continue trade!");
				if ((incCnt + descCnt) <= 20) {
					log.info("because total trade times is less or equal than 20!");
				}
				shouldStopTrade = false;
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return shouldStopTrade;
	}
	
	private static boolean stopTradeForStock(String openID, Stock2 s, int days) {
		String sql;
		boolean shouldStopTrade = false;
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "select * from SellBuyRecord " + " where openID ='" + openID + "'" + "   and stkid ='" + s.getID() + "'  and dl_dt >= sysdate() - interval " + days
					+ " day order by stkid, sb_id";
			log.info(sql);
			ResultSet rs = stm.executeQuery(sql);

			String pre_stkID = "";
			String stkID = "";
			int incCnt = 0;
			int descCnt = 0;

			double pre_price = 0.0;
			double price = 0.0;

			int pre_buy_flg = 1;
			int buy_flg = 1;

			while (rs.next()) {

				stkID = rs.getString("stkid");
				price = rs.getDouble("price");
				buy_flg = rs.getInt("buy_flg");

				if (pre_stkID.length() > 0) {
					log.info("stock:" + s.getName() + " buy_flg:" + buy_flg + " with price:" + price + " and pre_buy_flg:"
							+ pre_buy_flg + " with price:" + pre_price);
					if (buy_flg == 1 && pre_buy_flg == 0) {
						if (price < pre_price) {
							incCnt++;
						} else {
							descCnt++;
						}
					} else if (buy_flg == 0 && pre_buy_flg == 1) {
						if (price > pre_price) {
							incCnt++;
						} else {
							descCnt++;
						}
					} else {
						log.info("continue buy or sell does not means success or fail trade, continue.");
					}
					pre_price = price;
					pre_buy_flg = buy_flg;
				} else {
					pre_stkID = stkID;
					pre_price = price;
					pre_buy_flg = buy_flg;
					continue;
				}
			}
			
			log.info("stopTradeForStock, incCnt:" + incCnt + " descCnt:" + descCnt);

			// For specific stock, if there are 50% lost, stop trading.
			if ((incCnt + descCnt) > 5 && descCnt * 1.0 / (incCnt + descCnt) > 0.5) {
				log.info("For passed " + days + " days, for stock:" + s.getName() + " trade descCnt:" + descCnt
						+ " 50 % more than incCnt:" + incCnt + " stop trade!");
				shouldStopTrade = true;
			} else {
				log.info("For passed " + days + " days, for stock:" + s.getName() + " trade descCnt:" + descCnt
						+ " less than 50% incCnt:" + incCnt + " continue trade!");
				if ((incCnt + descCnt) <= 5) {
					log.info("because total trade times is less or equal than 5!");
				}
				shouldStopTrade = false;
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return shouldStopTrade;
	}
	
	private static boolean skipRiskCheck(String openID, boolean is_buy_flg, Stock2 s) {

		String sql;
		boolean shouldSkipCheck = false;

		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();

			// get last trade record.
			sql = "select * from SellBuyRecord " + " where openID ='" + openID + "'" + "   and stkid ='" + s.getID()
					+ "' order by sb_id desc";
			log.info(sql);
			ResultSet rs = stm.executeQuery(sql);

			double price = 0.0;
			int buy_flg = 1;

			if (rs.next()) {
				price = rs.getDouble("price");
				buy_flg = rs.getInt("buy_flg");

				if (!(is_buy_flg == (buy_flg == 1))) {
					if (!is_buy_flg && buy_flg == 1) {
						if (s.getCur_pri() > price) {
							log.info("stock:" + s.getName() + " buy_flg:" + buy_flg + " with price:" + price
									+ " which is good than previous buy with price:" + price + " skip risk check.");
							shouldSkipCheck = true;
						} else {
							log.info("previously bought, now sell with lower price, need to check risk.");
							shouldSkipCheck = false;
						}
					} else {

						// Now buy, previous sold.
						if (s.getCur_pri() > price) {
							log.info("previously sold with lower price, now want buy, need to check risk.");
							shouldSkipCheck = false;
						} else {
							log.info("previously sold with high price, now buy with lower price, skip check risk.");
							shouldSkipCheck = true;
						}
					}
				} else {
					log.info("same trade direction with previous trade, still need to check risk.");
					shouldSkipCheck = false;
				}
			} else {
				log.info("No trade record, but still need check risk for total trades.");
				shouldSkipCheck = false;
			}

			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return shouldSkipCheck;
	}
	

	private static boolean isInSellMode(String openID, Stock2 s) {

		String sql;
		int sell_mode_flg = 0;
		
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();

			// get last trade record.
			sql = "select sell_mode_flg from usrStk " + " where id ='" + s.getID()
					+ "'";
			log.info(sql);
			ResultSet rs = stm.executeQuery(sql);

			if (rs.next()) {
				sell_mode_flg = rs.getInt("sell_mode_flg");
			    log.info("stock:" + s.getID() + "'s sell mode:" + sell_mode_flg);
			} else {
				log.info("Looks stock:" + s.getID() + " is not in usrStk usr:" + openID + ", can not judge sell mode.");
				sell_mode_flg = 0;
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sell_mode_flg == 1;
	}
	
	private boolean createSellTradeRecord(Stock2 s, String qtyToTrade, ICashAccount ac, int order_id) {

        log.info("now start to sell stock " + s.getName()
                + " price:" + s.getCur_pri()
                + " against CashAcount: " + ac.getActId());

        int sellableAmt = Integer.valueOf(qtyToTrade);
        
        double relasedMny = sellableAmt * s.getCur_pri();
        Connection con = DBManager.getConnection();
        int seqnum = 0;
        try {
            String sql = "select case when max(d.seqnum) is null then -1 else max(d.seqnum) end maxseq from TradeHdr h " +
                    "       join TradeDtl d " +
                    "         on h.stkId = d.stkId " +
                    "        and h.acntId = d.acntId " +
                    "      where h.stkId = '" + s.getID()+ "'" +
                    "        and h.acntId = '" + ac.getActId() + "'";
    
            Statement stm = con.createStatement();
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) {
                if (rs.getInt("maxseq") < 0) {
                    sql = "insert into TradeHdr values('" + ac.getActId() + "','"
                    + s.getID() + "',"
                    + s.getCur_pri()*sellableAmt + ","
                    + sellableAmt + ","
                    + s.getCur_pri() + ",str_to_date('" + s.getDl_dt().toString().substring(0, 19) + "','%Y-%m-%d %H:%i:%s.%f'))";
                    log.info(sql);
                    Statement stm2 = con.createStatement();
                    stm2.execute(sql);
                    stm2.close();
                    stm2 = null;
                }
                else {
                    sql = "update TradeHdr set in_hand_qty = in_hand_qty - " + sellableAmt + ", in_hand_stk_price = " + s.getCur_pri() + ", in_hand_stk_mny = in_hand_qty * " + s.getCur_pri()
                         + " where acntId = '" + ac.getActId() + "' and stkId = '" + s.getID() + "'";
                    log.info(sql);
                    Statement stm2 = con.createStatement();
                    stm2.execute(sql);
                    stm2.close();
                    stm2 = null;
                }

                seqnum = rs.getInt("maxseq") + 1;
            }
            rs.close();
            stm.close();
            stm = con.createStatement();
            sql = "insert into TradeDtl (acntId, stkId, seqnum, price, amount, dl_dt, buy_flg, order_id) values( '"
                + ac.getActId() + "','"
                + s.getID() + "',"
                + seqnum + ","
                + s.getCur_pri() + ", "
                + sellableAmt
                + ", str_to_date('" + s.getDl_dt().toString() + "', '%Y-%m-%d %H:%i:%s.%f'), 0," + order_id + ")";
            log.info(sql);
            stm.execute(sql);
            stm.close();
            
            if (sim_mode)
            {
                //now sync used money
                double usedMny = ac.getUsedMny();
                usedMny -= relasedMny;
                ac.setUsedMny(usedMny);
                
                stm = con.createStatement();
                sql = "update CashAcnt set used_mny = used_mny - " + relasedMny + " where acntId = '" + ac.getActId() + "'";
                log.info(sql);
                stm.execute(sql);
                con.close();
            }
            return true;
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    
    }
	
	private static int getTradeQty(Stock2 s, boolean is_buy_flg, String openID) {
        String sql;
        int qtyToTrade = 100;
        try {
            Connection con = DBManager.getConnection();
            Statement stm = con.createStatement();
            
            sql = "select count(*) cnt, buy_flg from SellBuyRecord "
                + " where openID = '"  + openID + "'"
            	+ " and stkid ='" + s.getID() + "'"
            	+ " group by buy_flg";
            
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);
            
            int buyCnt = 0;
            int sellCnt = 0;
            int buy_flg = 0;
            
            while (rs.next()) {
            	buy_flg = rs.getInt("buy_flg");
            	if (buy_flg == 1) {
            		buyCnt = rs.getInt("cnt");
            	}
            	else {
            		sellCnt = rs.getInt("cnt");
            	}
            }
            
            rs.close();
            stm.close();
            con.close();
            
            if (is_buy_flg) {
                if (buyCnt > sellCnt) {
                	qtyToTrade = 100;
                }
                else if (s.getCur_pri() <= 20) {
                	qtyToTrade = 200;
                }
                else {
                	qtyToTrade = 100;
                }
            }
            else {
            	//is sell trade.
                if (s.getCur_pri() <= 20) {
                	qtyToTrade = 200;
                }
                else {
                	qtyToTrade = 100;
                }
            }
            log.info("For stock:" + s.getName() + " will " + (is_buy_flg ? "buy " : "sell ") + qtyToTrade + " with buyCnt:" + buyCnt + " sellCnt:" + sellCnt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return qtyToTrade;
    }
	
	private boolean createBuyTradeRecord(Stock2 s, String qtyToTrade, ICashAccount ac, int order_id) {
        
		int buyMnt = Integer.valueOf(qtyToTrade);
		double occupiedMny = buyMnt * s.getCur_pri();
		
        log.info("trying to buy amount:" + qtyToTrade + " with using Mny:" + occupiedMny);
        
        log.info("now start to bug stock " + s.getName()
                + " price:" + s.getCur_pri()
                + " with money: " + ac.getMaxAvaMny()
                + " buy mount:" + qtyToTrade);

        Connection con = DBManager.getConnection();
        String sql = "select case when max(d.seqnum) is null then -1 else max(d.seqnum) end maxseq from TradeHdr h " +
                "       join TradeDtl d " +
                "         on h.stkId = d.stkId " +
                "        and h.acntId = d.acntId " +
                "      where h.stkId = '" + s.getID()+ "'" +
                "        and h.acntId = '" + ac.getActId() + "'";
        int seqnum = 0;
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) {
                if (rs.getInt("maxseq") < 0) {
                    sql = "insert into TradeHdr values('" + ac.getActId() + "','"
                    + s.getID() + "',"
                    + s.getCur_pri()*buyMnt + ","
                    + buyMnt + ","
                    + s.getCur_pri() + ",str_to_date('" + s.getDl_dt().toString() + "','%Y-%m-%d %H:%i:%s.%f'))";
                    log.info(sql);
                    Statement stm2 = con.createStatement();
                    stm2.execute(sql);
                    stm2.close();
                    stm2 = null;
                }
                else {
                    sql = "update TradeHdr set in_hand_qty = in_hand_qty + " + buyMnt + ", in_hand_stk_price = " + s.getCur_pri() + ", in_hand_stk_mny = in_hand_qty * " + s.getCur_pri()
                         + " where acntId = '" + ac.getActId() + "' and stkId = '" + s.getID() + "'";
                    log.info(sql);
                    Statement stm2 = con.createStatement();
                    stm2.execute(sql);
                    stm2.close();
                    stm2 = null;
                }
                seqnum = rs.getInt("maxseq") + 1;
            }
            stm.close();
            stm = con.createStatement();
            sql = "insert into TradeDtl (acntId, stkId, seqnum, price, amount, dl_dt, buy_flg, order_id) values('"
                + ac.getActId() + "','"
                + s.getID() + "',"
                + seqnum + ","
                + s.getCur_pri() + ", "
                + buyMnt
                + ", str_to_date('" + s.getDl_dt().toString() + "','%Y-%m-%d %H:%i:%s.%f'), 1," + order_id + ")";
            log.info(sql);
            stm.execute(sql);
            stm.close();
            
            if(sim_mode)
            {
                //now sync used money
                double usedMny = ac.getUsedMny();
                usedMny += occupiedMny;
                ac.setUsedMny(usedMny);
                
                stm = con.createStatement();
                sql = "update CashAcnt set used_mny = " + usedMny + " where acntId = '" + ac.getActId() + "'";
                log.info(sql);
                stm.execute(sql);
                con.close();
            }
            return true;
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    /* Now do acutal trade to Tradex system*/
    private static int placeSellTradeToTradex(Stock2 s, int qtyToTrade, double price) {
        try {
            TradexBuySellResult tbsr = tradex_trader.processSellOrder(s.getID(), s.getArea(), qtyToTrade, price);
            
            if (!tbsr.isTranSuccess()) {
                log.error("Sell: placeSellTradeToTradex failed with error:" + tbsr.getError_code() + ", message:" + tbsr.getError_msg());
                return -1;
            }
            return tbsr.getOrder_id();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("Sell: placeSellTradeToTradex exception:" + e.getMessage());
            return -1;
        } 
    }
	
    /* Now do acutal trade to Tradex system*/
    private static int placeBuyTradeToTradex(Stock2 s, int qtyToTrade, double price) {
        try {
            TradexBuySellResult tbsr = tradex_trader.processBuyOrder(s.getID(), s.getArea(), qtyToTrade, price);
            
            if (!tbsr.isTranSuccess()) {
                log.error("Buy: placeSellTradeToTradex failed with error:" + tbsr.getError_code() + ", message:" + tbsr.getError_msg());
                return -1;
            }
            return tbsr.getOrder_id();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("Buy: placeSellTradeToTradex exception:" + e.getMessage());
            return -1;
        } 
    }
	
	private static boolean createBuySellRecord(Stock2 s, String openID, boolean is_buy_flg, String qtyToTrade) {
		String sql;
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "insert into SellBuyRecord select case when max(sb_id) is null then 1 else max(sb_id) + 1 end,'" + openID + "','" + s.getID() + "'," + s.getCur_pri()
					+ "," + qtyToTrade + ","
					+ (is_buy_flg ? 1 : 0)
					+ ",str_to_date('" + s.getDl_dt().toString() + "', '%Y-%m-%d %H:%i:%s.%f') from SellBuyRecord";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			con.close();
			// Here once after we trade a stock, clear it's historic memory
			// data.
			if (s != null) {
				log.info("After trade " + s.getName() + " clear InjectedRaw Data...");
				s.getSd().clearInjectRawData();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public ICashAccount getCashAcntForStock(String stk) {
        
        if (sim_mode) {
        	String AcntForStk = STConstants.ACNT_SIM_PREFIX + stk;
            ICashAccount acnt = cash_account_map.get(AcntForStk);
            if (acnt == null) {
            	log.info("No cashAccount for stock:" + stk + " in memory, load from db.");
                acnt = CashAcntManger.loadAcnt(AcntForStk);
                if (acnt == null) {
                	log.info("No cashAccount for stock:" + stk + " from db, create default virtual account.");
                    CashAcntManger
                    .crtAcnt(AcntForStk, STConstants.DFT_INIT_MNY, 0.0, 0.0, STConstants.DFT_SPLIT, STConstants.DFT_MAX_USE_PCT, true);
                    acnt = CashAcntManger.loadAcnt(AcntForStk);
                }
                
                if (acnt != null) {
                	log.info("put the loaded/created virtual account into memory.");
                	cash_account_map.put(AcntForStk, acnt);
                }
            }
            return acnt;
        }
        else {
            if (cash_account_map.get(tradex_acnt.getActId()) == null)
            {
                cash_account_map.put(tradex_acnt.getActId(), tradex_acnt);
            }
            return tradex_acnt;
        }
        
	}

	@Override
	public void enableSimulationMode(boolean yes) {
		// TODO Auto-generated method stub
		sim_mode = yes;
	}
}
