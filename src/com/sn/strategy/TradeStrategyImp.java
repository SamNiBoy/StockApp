package com.sn.strategy;

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

import com.sn.STConstants;
import com.sn.cashAcnt.CashAcnt;
import com.sn.cashAcnt.CashAcntManger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.cashAcnt.TradexAcnt;
import com.sn.db.DBManager;
import com.sn.strategy.ITradeStrategy;
import com.sn.strategy.algorithm.IBuyPointSelector;
import com.sn.strategy.algorithm.ISellPointSelector;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.IStockSelector;
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
    String name = "Default Trade Strategy";
    
    private boolean sim_mode = false;
    
    private static TradexAcnt tradex_acnt = null;
    
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
	private static List<String> tradeStocks = new ArrayList<String>();
	private static Map<String, LinkedList<StockBuySellEntry>> tradeRecord = new HashMap<String, LinkedList<StockBuySellEntry>>();
    private static Map<String, ICashAccount> cash_account_map = new HashMap<String, ICashAccount>();
    
	public StockBuySellEntry getLstTradeRecord(Stock2 s) {
		return tradeRecord.get(s.getID()).getLast();
	}

    public boolean isGoodPointtoBuy(Stock2 s) {
        log.info("********************* BUY POINT CHECK START **********************");
        boolean good_flg = buypoint_selector.isGoodBuyPoint(s, cash_account);
        log.info("********************* BUY POINT CHECK FOR " + s.getID() + (good_flg? " PASS ":" FAIL ") + "END*************\n");
        return good_flg;
    }

    public boolean isGoodPointtoSell(Stock2 s) {
        log.info("**************** SELL POINT CHECK START ******************");
        boolean good_flg = sellpoint_selector.isGoodSellPoint(s, cash_account);
        log.info("**************** SELL POINT CHECK FOR " + s.getID() + (good_flg? " PASS ":" FAIL ") + "END**************");
        return good_flg;
    }
    
    public TradeStrategyImp(IBuyPointSelector bs,
                            ISellPointSelector ses,
                            ICashAccount ca,
                            String sn,
                            boolean sm) {
        buypoint_selector = bs;
        sellpoint_selector = ses;
        cash_account = ca;
        name = sn;
        sim_mode = sm;
    }

	public void printTradeInfor() {
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
			log.info("qty to buy is zero, can not buyStock.");
            return false;
		}
		
		if (canTradeRecord(s, true)) {
			
			String qtyToTrade = String.valueOf(qtb);
			LocalDateTime lt = LocalDateTime.now();
			int mnt = lt.getMinute();
			TradexBuySellResult tbsr = null;
			while(true) {
			    try {
			        // Save string like "B600503" to clipboard for buy stock.
			    	if (!sim_mode) {
			            /*String txt = "";
			            Clipboard cpb = Toolkit.getDefaultToolkit().getSystemClipboard();
			            txt = "B" + s.getID() + qtyToTrade;
			            
			            StringSelection sel = new StringSelection(txt);
			            cpb.setContents(sel, null);*/
			    	    tbsr = placeBuyTradeToTradex(s, qtb, s.getCur_pri());
                        if (tbsr == null)
                        {
                            log.info("failed to placeBuyOrder to Tradex, skipping create tradehdr/tradedtl record.");
                            return false;
                        }
			    	}
			        
			        createBuyTradeRecord(s, qtyToTrade, ac, tbsr);
			        
                    ac.calProfit();
                    
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
		
		if (qtb <= 0) {
			log.info("qtb is <=0, can not sell!");
			return false;
		}
		
		if (canTradeRecord(s, false)) {
			
			String qtyToTrade = String.valueOf(qtb);
			LocalDateTime lt = LocalDateTime.now();
			int mnt = lt.getMinute();
            TradexBuySellResult tbsr = null;
			while(true) {
			    try {
			        // Save string like "S600503" to clipboard for sell stock.
			    	if (!sim_mode) {
			            /*String txt = "";
			            Clipboard cpb = Toolkit.getDefaultToolkit().getSystemClipboard();
			            txt = "S" + s.getID() + qtyToTrade;
			            
			            StringSelection sel = new StringSelection(txt);
			            cpb.setContents(sel, null);*/
			    	    tbsr = placeSellTradeToTradex(s, qtb, s.getCur_pri());
			    	    if (tbsr == null)
			    	    {
			    	        log.info("failed to placeSellOrder to Tradex, skipping create tradehdr/tradedtl record.");
                            return false;
			    	    }
			    	}
			        
                    
			        createSellTradeRecord(s, qtyToTrade, ac, tbsr);
			        
                    ac.calProfit();
                    
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
    public boolean calProfit() {
        // TODO Auto-generated method stub
        return cash_account.calProfit();
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
        
        boolean result = false;
        
        log.info("####################### PERFORM TRADE FOR " + s.getID() + ":" + s.getName() + " BEGIN #######################");
		if (isGoodPointtoBuy(s) && buyStock(s)) {
        	StockBuySellEntry rc = tradeRecord.get(s.getID()).getLast();
        	rc.printStockInfo();
        	result = true;
        }
        else if(isGoodPointtoSell(s) && sellStock(s)) {
        	StockBuySellEntry rc = tradeRecord.get(s.getID()).getLast();
        	rc.printStockInfo();
        	result = true;
        }
        log.info("##################### PERFORM TRADE FOR " + s.getID() + ":" + s.getName() + " END ############################\n\n");
        return result;
	}
	
	public boolean loadStocksForTrade() {
		String sql;
		tradeStocks.clear();
		try {
		    String system_trader = ParamManager.getStr2Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING");
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "select s.*, u.* " + "from usrStk s," + "     usr u " + "where s.openID = u.openID "
					+ "and s.gz_flg = 1 " + "and u.openID = '" + STConstants.openID + "' and length(u.mail) > 1 "
					+ "and s.suggested_by in ('" + STConstants.openID +"','" + system_trader + "') and u.buy_sell_enabled = 1";

			//log.info(sql);
			ResultSet rs = stm.executeQuery(sql);
			while (rs.next()) {
				//log.info("Loading stock:" + rs.getString("id") + " for user openID:" + rs.getString("openID"));
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
	
	private boolean canTradeRecord(Stock2 s, boolean is_buy_flg) {
		//For sim_mode, we don't care if user gzed the stock.
		if ((tradeStocks == null || !tradeStocks.contains(s.getID())) && !sim_mode) {
			log.info("stock " + s.getID() + " is not available for trade.");
			return false;
		}
		
		/*if (isInSellMode(s) && is_buy_flg) {
			log.info("Stock:" + s.getID() + " is in sell mode, can not buy it!");
			return false;
		}*/

		//if (!skipRiskCheck(is_buy_flg, s)) {

			//boolean overall_risk = false;
			// If recently we lost continuously overall, stop trading.
			/*if (stopTradeForPeriod(5)) {
				log.info("Now trade unsuccess for 3 days, stop trade.");
				overall_risk = true;
			}*/

			// If recently we lost continuously for the stock, stop trading.
			if (stopTradeForStock(s)) {
				log.info("Skip trade for stock:" + s.getName() + " after eached it's risk.");
				return false;
			}
		//}

		int totalCnt = 0;
		LinkedList<StockBuySellEntry> tmp;
		for (String id : tradeRecord.keySet()) {
			tmp = tradeRecord.get(id);
			totalCnt += tmp.size();
		}

		log.info("TradeStrategy " + name + " collected total traded " + totalCnt + " times.");

		int max_trades_per_days = ParamManager.getIntParam("MAX_TRADE_TIMES_PER_DAY", "TRADING");
		
		if (totalCnt >= max_trades_per_days) {
			log.info("Trade limit for a day is: " + max_trades_per_days + " can not trade today!");
			return false;
		}

		LinkedList<StockBuySellEntry> rcds = tradeRecord.get(s.getID());
		if (rcds != null) {
            
		    int max_trades_per_stock = ParamManager.getIntParam("MAX_TRADE_TIMES_PER_STOCK", "TRADING");
		    
			if (rcds.size() >= max_trades_per_stock) {
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
				
				int max_trades_buy_or_sell_per_stock = ParamManager.getIntParam("MAX_TRADE_TIMES_BUY_OR_SELL_PER_STOCK", "TRADING");
				if (buyCnt >= max_trades_buy_or_sell_per_stock || sellCnt >= max_trades_buy_or_sell_per_stock) {
					log.info("Stock:" + s.getID() + " buy/sell reached limit:" + max_trades_buy_or_sell_per_stock);
					return false;
				}
				log.info("For stock " + s.getID() + " total sellCnt:" + sellCnt + ", total buyCnt:" + buyCnt);

				// We only allow buy BUY_SELL_MAX_DIFF_CNT more than sell.
				int buy_sell_max_diff_cnt = ParamManager.getIntParam("BUY_SELL_MAX_DIFF_CNT", "TRADING");
				if (buyCnt >= sellCnt + buy_sell_max_diff_cnt && is_buy_flg) {
                    log.info("Bought more than " + buy_sell_max_diff_cnt + " times as sell can won't buy again.");
					return false;
				}
				else if (sellCnt >= buyCnt +buy_sell_max_diff_cnt && !is_buy_flg) {
                    log.info("Sold more than " + buy_sell_max_diff_cnt + " times as buy, can won't sell again.");
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
                    
		            Timestamp t0 = lst.dl_dt;
		            Timestamp t1 = s.getDl_dt();
		            
		            long millisec = t1.getTime() - t0.getTime();
		            long mins = millisec / (1000*60);
		            
		            int mins_max = ParamManager.getIntParam("MAX_MINUTES_ALLOWED_TO_KEEP_BALANCE", "TRADING");
					if (is_buy_flg == lst.is_buy_point && Math.abs((s.getCur_pri() - lst.price)) / lst.price <= 0.01 && !(mins > mins_max)) {
						log.info("Just " + (is_buy_flg ? "buy" : "sell") + " this stock with similar prices "
								+ s.getCur_pri() + "/" + lst.price + ", skip same trade.");
						return false;
					}
				}
				return true;
			}
		}
    	return true;
	}
	
	private boolean stopTradeForPeriod(int days) {
		String sql;
		boolean shouldStopTrade = false;
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
            
	         String acntId = ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT");
	            
	         if (!sim_mode) {
	             acntId = tradex_acnt.getActId();
	         }
	            
			sql = "select * from tradedtl " + " where acntId like '" + acntId + "%'" + "   and dl_dt >= sysdate() - interval "
					+ days
					+ " day order by stkid, seqnum";
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
	
	private boolean stopTradeForStock(Stock2 s) {
		String sql;
		boolean shouldStopTrade = false;
        
        String acntId = ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + s.getID();
        
        if (!sim_mode) {
            acntId = tradex_acnt.getActId();
        }
        
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "select * from tradedtl " + " where acntId ='" + acntId + "' and stkid ='" + s.getID() + "'  and left(dl_dt, 10) = left(str_to_date('" + s.getDl_dt().toString() + "', '%Y-%m-%d %H:%i:%s.%f'), 10) order by stkid, seqnum";
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
			int seqnum = -1;
			int pre_seqnum = -1;

			while (rs.next()) {

				stkID = rs.getString("stkid");
				price = rs.getDouble("price");
				buy_flg = rs.getInt("buy_flg");
				seqnum = rs.getInt("seqnum");

                log.info("stkID:" + stkID + ", price:" + price + ", buy_flg:" + buy_flg + ", seqnum:" + seqnum);
                log.info("pre_stkID:" + pre_stkID + ", pre_price:" + pre_price + ", pre_buy_flg:" + pre_buy_flg + ", pre_seqnum:" + pre_seqnum);
				if (pre_stkID.length() > 0) {
					log.info("stock:" + s.getName() + " buy_flg:" + buy_flg + " with price:" + price + " and pre_buy_flg:"
							+ pre_buy_flg + " with price:" + pre_price + " on seqnum:" + seqnum + ", pre_seqnum:" + pre_seqnum);
					if (buy_flg == 1 && pre_buy_flg == 0) {
						if (price < pre_price) {
							incCnt++;
							pre_stkID = "";
						} else {
							descCnt++;
							pre_stkID = "";
						}
					} else if (buy_flg == 0 && pre_buy_flg == 1) {
						if (price > pre_price) {
							incCnt++;
							pre_stkID = "";
						} else {
							descCnt++;
							pre_stkID = "";
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
                    pre_seqnum = seqnum;
				}
			}
			
			log.info("stopTradeForStock, incCnt:" + incCnt + " descCnt:" + descCnt);

			// For specific stock, if there are 50% lost, stop trading.
			int max_trade_lost_times = ParamManager.getIntParam("STOP_TRADE_IF_LOST_MORE_THAN_GAIN_TIMES", "TRADING");
			if ((descCnt - incCnt) >= max_trade_lost_times) {
				log.info("Stock:" + stkID + "lost time is more than " + max_trade_lost_times + " times, stop trade!");
				shouldStopTrade = true;
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return shouldStopTrade;
	}
	
	private boolean skipRiskCheck(boolean is_buy_flg, Stock2 s) {

		String sql;
		boolean shouldSkipCheck = false;

		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();

            
			String acntId = ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + s.getID();
			
			if (!sim_mode) {
			    acntId = tradex_acnt.getActId();
			}
			// get last trade record.
			sql = "select * from tradedtl " + " where acntId ='" + acntId + "'" + "   and stkId ='" + s.getID()
					+ "' order by seqnum desc";
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
	

	private static boolean isInSellMode(Stock2 s) {

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
				log.info("Looks stock:" + s.getID() + " is not in usrStk, can not judge sell mode.");
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
	
	private boolean createSellTradeRecord(Stock2 s, String qtyToTrade, ICashAccount ac, TradexBuySellResult tbsr) {

        log.info("now start to sell stock " + s.getName()
                + " price:" + s.getCur_pri()
                + " against CashAcount: " + ac.getActId());

        int sellableAmt = Integer.valueOf(qtyToTrade);
        double soldPrice = s.getCur_pri();
        
        if (!sim_mode) {
            sellableAmt = tbsr.getTrade_quantity();
            soldPrice = tbsr.getTrade_price();
        }
        
        Connection con = DBManager.getConnection();
        int seqnum = 0;
        
        double commi_rate  = ParamManager.getFloatParam("COMMISSION_RATE", "VENDOR");
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
                    /*
                     * create table if not exists TradeHdr(
                       acntId varchar(20 ) not null,
                       stkId varchar(6 ) not null,
                       in_hand_stk_mny decimal(8, 2) not null,
                       in_hand_qty int not null,
                       in_hand_stk_price decimal(8, 2) not null,
                       total_amount decimal(20, 2),
                       com_rate decimal(8, 2),
                       commission_mny decimal(8, 2),
                       add_dt datetime not null,
                       CONSTRAINT TradeHdr_PK PRIMARY KEY (acntId, stkId)
                       );
                     */
                    sql = "insert into TradeHdr values('" + ac.getActId() + "','"
                    + s.getID() + "',"
                    + soldPrice*sellableAmt + ","
                    + (-sellableAmt) + ","
                    + soldPrice + "," + soldPrice*sellableAmt + "," + commi_rate +  ","
                    + sellableAmt * soldPrice * commi_rate + ", str_to_date('" + s.getDl_dt().toString().substring(0, 19) + "','%Y-%m-%d %H:%i:%s.%f'))";
                    log.info(sql);
                    Statement stm2 = con.createStatement();
                    stm2.execute(sql);
                    stm2.close();
                    stm2 = null;
                }
                else {
                    sql = "update TradeHdr set in_hand_qty = in_hand_qty - " + sellableAmt + ", in_hand_stk_price = " + soldPrice + ", in_hand_stk_mny = in_hand_qty * " + soldPrice
                          + ", total_amount = total_amount + " + sellableAmt * soldPrice + ", commission_mny = commission_mny + " + sellableAmt * soldPrice + " * com_rate"
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
                + soldPrice + ", "
                + sellableAmt
                + ", str_to_date('" + s.getDl_dt().toString() + "', '%Y-%m-%d %H:%i:%s.%f'), 0," + (sim_mode? "null" : tbsr.getOrder_id()) + ")";
            log.info(sql);
            stm.execute(sql);
            stm.close();
            
            if (sim_mode)
            {
                //now sync used money
                /*double relasedMny = sellableAmt * s.getCur_pri();
                double usedMny = ac.getUsedMny();
                usedMny -= relasedMny;
                ac.setUsedMny(usedMny);
                
                stm = con.createStatement();
                sql = "update CashAcnt set used_mny = used_mny - " + relasedMny + " where acntId = '" + ac.getActId() + "'";
                log.info(sql);
                stm.execute(sql);*/
            }
                con.close();
            
            LinkedList<StockBuySellEntry> rcds = tradeRecord.get(s.getID());
            if (rcds != null) {
                log.info("Adding trade record for stock as: " + s.getID());
                StockBuySellEntry stk = new StockBuySellEntry(s.getID(),
                                                              s.getName(),
                                                              soldPrice,
                                                              sellableAmt,
                                                              false,
                                                              (Timestamp)s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1));
                stk.printStockInfo();
                rcds.add(stk);
            } else {
                log.info("Adding today first trade record for stock as: " + s.getID());
                StockBuySellEntry stk = new StockBuySellEntry(s.getID(),
                        s.getName(),
                        soldPrice,
                        sellableAmt,
                        false,
                        (Timestamp)s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1));
                stk.printStockInfo();
                rcds = new LinkedList<StockBuySellEntry>();
                rcds.add(stk);
                tradeRecord.put(stk.id, rcds);
            }
            return true;
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    
    }
	
	private boolean createBuyTradeRecord(Stock2 s, String qtyToTrade, ICashAccount ac, TradexBuySellResult tbsr) {
        
		int buyMnt = Integer.valueOf(qtyToTrade);
		double occupiedMny = buyMnt * s.getCur_pri();
        double buyPrice = s.getCur_pri();
        
		if (!sim_mode) {
		   buyMnt = tbsr.getTrade_quantity();
		   occupiedMny = tbsr.getTrade_amount();
           buyPrice = tbsr.getTrade_price();
		}
		
        log.info("trying to buy amount:" + qtyToTrade + " with using Mny:" + occupiedMny);
        
        log.info("now start to bug stock " + s.getName()
                + " price:" + buyPrice
                + " with money: " + ac.getMaxMnyForTrade()
                + " buy mount:" + buyMnt);

        Connection con = DBManager.getConnection();
        String sql = "select case when max(d.seqnum) is null then -1 else max(d.seqnum) end maxseq from TradeHdr h " +
                "       join TradeDtl d " +
                "         on h.stkId = d.stkId " +
                "        and h.acntId = d.acntId " +
                "      where h.stkId = '" + s.getID()+ "'" +
                "        and h.acntId = '" + ac.getActId() + "'";
        int seqnum = 0;
        double commi_rate  = ParamManager.getFloatParam("COMMISSION_RATE", "VENDOR");
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            
            if (rs.next()) {
                if (rs.getInt("maxseq") < 0) {
                    sql = "insert into TradeHdr values('" + ac.getActId() + "','"
                    + s.getID() + "',"
                    + buyPrice * buyMnt + ","
                    + buyMnt + ","
                    + buyPrice + "," + buyPrice*buyMnt + "," + commi_rate +  ","
                    + buyMnt * buyPrice * commi_rate + ",str_to_date('" + s.getDl_dt().toString() + "','%Y-%m-%d %H:%i:%s.%f'))";
                    log.info(sql);
                    Statement stm2 = con.createStatement();
                    stm2.execute(sql);
                    stm2.close();
                    stm2 = null;
                }
                else {
                    sql = "update TradeHdr set in_hand_qty = in_hand_qty + " + buyMnt + ", in_hand_stk_price = " + buyPrice + ", in_hand_stk_mny = in_hand_qty * " + buyPrice 
                         + ", total_amount = total_amount + " + buyMnt * buyPrice + ", commission_mny = commission_mny + " + buyMnt * buyPrice + " * com_rate"
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
                + buyPrice + ", "
                + buyMnt
                + ", str_to_date('" + s.getDl_dt().toString() + "','%Y-%m-%d %H:%i:%s.%f'), 1," + (sim_mode? "null":tbsr.getOrder_id()) + ")";
            log.info(sql);
            stm.execute(sql);
            stm.close();
            
            if(sim_mode)
            {
             /*   //now sync used money
                double usedMny = ac.getUsedMny();
                usedMny += occupiedMny;
                ac.setUsedMny(usedMny);
                
                stm = con.createStatement();
                sql = "update CashAcnt set used_mny = " + usedMny + " where acntId = '" + ac.getActId() + "'";
                log.info(sql);
                stm.execute(sql);*/
            }
            con.close();
            
            LinkedList<StockBuySellEntry> rcds = tradeRecord.get(s.getID());
            if (rcds != null) {
                log.info("Adding trade record for stock as: " + s.getID());
                StockBuySellEntry stk = new StockBuySellEntry(s.getID(),
                                                              s.getName(),
                                                              buyPrice,
                                                              buyMnt,
                                                              true,
                                                              (Timestamp)s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1));
                stk.printStockInfo();
                rcds.add(stk);
            } else {
                log.info("Adding today first trade record for stock as: " + s.getID());
                StockBuySellEntry stk = new StockBuySellEntry(s.getID(),
                        s.getName(),
                        buyPrice,
                        buyMnt,
                        true,
                        (Timestamp)s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1));
                stk.printStockInfo();
                rcds = new LinkedList<StockBuySellEntry>();
                rcds.add(stk);
                tradeRecord.put(stk.id, rcds);
            }
            
            return true;
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    /* Now do acutal trade to Tradex system*/
    private static TradexBuySellResult placeSellTradeToTradex(Stock2 s, int qtyToTrade, double price) {
        try {
            TradexBuySellResult tbsr = TradexCpp.processSellOrder(s.getID(), s.getArea(), qtyToTrade, price);
            
            if (!tbsr.isTranSuccess()) {
                log.error("Sell: placeSellTradeToTradex failed with error:" + tbsr.getError_code() + ", message:" + tbsr.getError_msg());
                return null;
            }
            
            if (tbsr.getTrade_quantity() < qtyToTrade) {
                log.info("Tradex placeSellTradeToTradex cancel order with param:");
                log.info("qtyToTrade:" + qtyToTrade);
                log.info("trade_quantity:" + tbsr.getTrade_quantity());
                log.info("order_id:" + tbsr.getOrder_id());
                TradexCpp.processCancelOrder(tbsr.getOrder_id());
            }
            return tbsr;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("Sell: placeSellTradeToTradex exception:" + e.getMessage());
            return null;
        } 
    }
	
    /* Now do acutal trade to Tradex system*/
    private static TradexBuySellResult placeBuyTradeToTradex(Stock2 s, int qtyToTrade, double price) {
        try {
            TradexBuySellResult tbsr = TradexCpp.processBuyOrder(s.getID(), s.getArea(), qtyToTrade, price);
            
            if (!tbsr.isTranSuccess()) {
                log.error("Buy: placeSellTradeToTradex failed with error:" + tbsr.getError_code() + ", message:" + tbsr.getError_msg());
                return null;
            }
            
            if (tbsr.getTrade_quantity() < qtyToTrade) {
                log.info("Tradex placeBuyTradeToTradex cancel order with param:");
                log.info("qtyToTrade:" + qtyToTrade);
                log.info("trade_quantity:" + tbsr.getTrade_quantity());
                log.info("order_id:" + tbsr.getOrder_id());
                TradexCpp.processCancelOrder(tbsr.getOrder_id());
            }
            return tbsr;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("Buy: placeSellTradeToTradex exception:" + e.getMessage());
            return null;
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
        	String AcntForStk = ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT") + stk;
            ICashAccount acnt = cash_account_map.get(AcntForStk);
            if (acnt == null) {
            	log.info("No cashAccount for stock:" + stk + " in memory, load from db.");
                acnt = CashAcntManger.loadAcnt(AcntForStk);
                
                double def_init_mnt = ParamManager.getFloatParam("DFT_INIT_MNY", "ACCOUNT");
                double def_max_mny_per_trade = ParamManager.getFloatParam("DFT_MAX_MNY_PER_TRADE", "ACCOUNT");
                double def_max_use_pct = ParamManager.getFloatParam("DFT_MAX_USE_PCT", "ACCOUNT");
                
                if (acnt == null) {
                	log.info("No cashAccount for stock:" + stk + " from db, create default virtual account.");
                    CashAcntManger
                    .crtAcnt(AcntForStk, def_init_mnt, 0.0, 0.0,0.0,def_max_mny_per_trade, def_max_use_pct);
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
            
            //findBestClientForTrade return true means we switch to a new account.
            String Tradexacnt = TradexCpp.findBestAccountForTrade(stk);
            
            if(tradex_acnt == null || !tradex_acnt.getActId().equals(Tradexacnt))
            {
                tradex_acnt = new TradexAcnt();
                
                ICashAccount acnt = CashAcntManger.loadAcnt(Tradexacnt);
                
                if (acnt == null) {
                    
                    log.info("No Tradex Account, create a new one.");
                    double def_init_mnt = ParamManager.getFloatParam("DFT_INIT_MNY", "ACCOUNT");
                    double def_max_mny_per_trade = ParamManager.getFloatParam("DFT_MAX_MNY_PER_TRADE", "ACCOUNT");
                    double def_max_use_pct = ParamManager.getFloatParam("DFT_MAX_USE_PCT", "ACCOUNT");
                    //create a local cashacnt record to map Tradex account, columns may not be exact same, but for profit calculation purpose.
                    CashAcntManger
                    .crtAcnt(Tradexacnt, def_init_mnt, 0.0, 0.0,0.0,def_max_mny_per_trade, def_max_use_pct);
                }
                
                if (cash_account_map.get(tradex_acnt.getActId()) == null)
                {
                    cash_account_map.put(tradex_acnt.getActId(), tradex_acnt);
                }
            }
            
            return tradex_acnt;
        }
        
	}

	@Override
	public void enableSimulationMode(boolean yes) {
		// TODO Auto-generated method stub
		sim_mode = yes;
	}

    public String getTradeStrategyName() {
        // TODO Auto-generated method stub
        return name;
    }

    public void resetStrategyStatus() {
        // TODO Auto-generated method stub
       	log.info("reset tradeStocks, tradeRecord, cash_account_map entries...");
        tradeStocks.clear();
        tradeRecord.clear();
        cash_account_map.clear();
    }
}
