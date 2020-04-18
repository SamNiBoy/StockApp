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
import java.util.concurrent.ConcurrentHashMap;
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
    List<IBuyPointSelector> buypoint_selectors = new LinkedList<IBuyPointSelector>();
    List<ISellPointSelector> sellpoint_selectors = new LinkedList<ISellPointSelector>();
    ICashAccount cash_account = null;
    String name = "Default Trade Strategy";
    
    private boolean sim_mode = false;
    
    private static TradexAcnt tradex_acnt = null;
    
    public ICashAccount getCash_account() {
        return cash_account;
    }

    public void setCash_account(ICashAccount cashAccount) {
        cash_account = cashAccount;
    }

    // non interface vars.
	private static List<String> tradeStocks = new ArrayList<String>();
	private static Map<String, LinkedList<StockBuySellEntry>> tradeRecord = new ConcurrentHashMap<String, LinkedList<StockBuySellEntry>>();
    private static Map<String, StockBuySellEntry> lstTradeForStocks = null;
    private static Map<String, ICashAccount> cash_account_map = new ConcurrentHashMap<String, ICashAccount>();
    private static boolean unbalanced_db_lstTradeForStocks_loaded = false;
    
    public static Map<String, StockBuySellEntry> getLstTradeForStocks() {
        
        if (lstTradeForStocks == null)
        {
            lstTradeForStocks = new ConcurrentHashMap<String, StockBuySellEntry>();
        }
        return lstTradeForStocks;
    }

	public StockBuySellEntry getLstTradeRecord(Stock2 s) {
		return tradeRecord.get(s.getID()).getLast();
	}


    
    public TradeStrategyImp(List<IBuyPointSelector> bs,
                            List<ISellPointSelector> ses,
                            ICashAccount ca,
                            String sn,
                            boolean sm) {
        buypoint_selectors = bs;
        sellpoint_selectors = ses;
        cash_account = ca;
        name = sn;
        sim_mode = sm;
        loadBuySellRecord();
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
	public boolean buyStock(Stock2 s, IBuyPointSelector buypoint_selector) {
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
                        
			    	    int tradeLocal = ParamManager.getIntParam("TRADING_AT_LOCAL", "TRADING", null);
                        if (tradeLocal == 1)
                        {
                            tbsr = placeBuyTradeToLocal(s, qtb, s.getCur_pri());
                            if (tbsr == null)
                            {
                                log.info("failed to placeBuyOrder to Local , skipping create tradehdr/tradedtl record.");
                                return false;
                            }
                        }
                        else {
			    	        tbsr = placeBuyTradeToTradex(s, qtb, s.getCur_pri());
                            if (tbsr == null)
                            {
                                log.info("failed to placeBuyOrder to Tradex, skipping create tradehdr/tradedtl record.");
                                return false;
                            }
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
	public boolean sellStock(Stock2 s, ISellPointSelector sellpoint_selector) {
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
                        
                        int tradeLocal = ParamManager.getIntParam("TRADING_AT_LOCAL", "TRADING", null);
                        if (tradeLocal == 1)
                        {
                            tbsr = placeSellTradeToLocal(s, qtb, s.getCur_pri());
                            if (tbsr == null)
                            {
                                log.info("failed to placeSellOrder to Local , skipping create tradehdr/tradedtl record.");
                                return false;
                            }
                        }
                        else {
			    	        tbsr = placeSellTradeToTradex(s, qtb, s.getCur_pri());
			    	        if (tbsr == null)
			    	        {
			    	            log.info("failed to placeSellOrder to Tradex, skipping create tradehdr/tradedtl record.");
                                return false;
			    	        }
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
        synchronized(cash_account_map) {
    	for (String acnt :cash_account_map.keySet()) {
    		ICashAccount ac = cash_account_map.get(acnt);
            ac.printAcntInfo();
            ac.printTradeInfo();
    	}
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
        int i = 0, j = 0;
        
        log.info("####################### PERFORM TRADE FOR " + s.getID() + ":" + s.getName() + " BEGIN #######################");
        //Here we want try trading in buy...sell...buy...sell order to keep in balance as much as possible, which is why used i,j.
        for (IBuyPointSelector bp : buypoint_selectors)
        {
            i++;
            for (ISellPointSelector sp : sellpoint_selectors)
            {
                j++;
                
                //j must be equal to i before trading.
                if (j > i)
                {
                    j = 0;
                    break;
                }
                else if (j < i)
                {
                    continue;
                }
		        if (isGoodPointtoBuy(s, bp) && buyStock(s, bp)) {
                	StockBuySellEntry rc = tradeRecord.get(s.getID()).getLast();
                	rc.printStockInfo();
                	result = true;
                }
                else if(isGoodPointtoSell(s, sp) && sellStock(s, sp)) {
                	StockBuySellEntry rc = tradeRecord.get(s.getID()).getLast();
                	rc.printStockInfo();
                	result = true;
                }
                if (result)
                {
                    break;
                }
            }
            if (result)
            {
                break;
            }
        }
        log.info("##################### PERFORM TRADE FOR " + s.getID() + ":" + s.getName() + " END ############################\n\n");
        return result;
	}
    
    public boolean isGoodPointtoBuy(Stock2 s, IBuyPointSelector bp) {
        
        boolean good_flg = false;
        log.info("********** BUY POINT " + bp.getClass().getSimpleName() + " CHECK START ***************");
        if (bp.isGoodBuyPoint(s, cash_account))
        {
            good_flg = true;
        }
        log.info("************ BUY POINT CHECK FOR " + s.getID() + (good_flg? " PASS ":" FAIL ") + "END**********\n");
        
        return good_flg;
    }

    public boolean isGoodPointtoSell(Stock2 s, ISellPointSelector sp){
        boolean good_flg = false;
        log.info("**************** SELL POINT " + sp.getClass().getSimpleName() + " CHECK START ******************");
        if (sp.isGoodSellPoint(s, cash_account))
        {
            good_flg = true;
        }
        log.info("**************** SELL POINT CHECK FOR " + s.getID() + (good_flg? " PASS ":" FAIL ") + "END**************");
        
        return good_flg;
    }
	
	public boolean loadStocksForTrade() {
		String sql;
		tradeStocks.clear();
		try {
		    String system_trader = ParamManager.getStr2Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
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
        
		
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(s.getID());
        
        if (sbs != null && sbs.is_buy_point != is_buy_flg)
        {
            log.info("We allow trading as we have unbalance to deal with.");
            return true;
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

		int max_trades_per_days = ParamManager.getIntParam("MAX_TRADE_TIMES_PER_DAY", "TRADING", s.getID());
		
		if (totalCnt >= max_trades_per_days) {
			log.info("Trade limit for a day is: " + max_trades_per_days + " can not trade today!");
			return false;
		}

		LinkedList<StockBuySellEntry> rcds = tradeRecord.get(s.getID());
		if (rcds != null) {
            
		    int max_trades_per_stock = ParamManager.getIntParam("MAX_TRADE_TIMES_PER_STOCK", "TRADING", s.getID());
		    
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
				
				int max_trades_buy_or_sell_per_stock = ParamManager.getIntParam("MAX_TRADE_TIMES_BUY_OR_SELL_PER_STOCK", "TRADING", s.getID());
				if (buyCnt >= max_trades_buy_or_sell_per_stock || sellCnt >= max_trades_buy_or_sell_per_stock) {
					log.info("Stock:" + s.getID() + " buy/sell reached limit:" + max_trades_buy_or_sell_per_stock);
					return false;
				}
				log.info("For stock " + s.getID() + " total sellCnt:" + sellCnt + ", total buyCnt:" + buyCnt);

				// We only allow buy BUY_SELL_MAX_DIFF_CNT more than sell.
				int buy_sell_max_diff_cnt = ParamManager.getIntParam("BUY_SELL_MAX_DIFF_CNT", "TRADING", s.getID());
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
                    
	                int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING", s.getID());
	                int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING", s.getID());
		            int mins_max = ParamManager.getIntParam("MAX_MINUTES_ALLOWED_TO_KEEP_BALANCE", "TRADING", s.getID());
	                
	                long hour = t1.getHours();
	                long minutes = t1.getMinutes();
		            long millisec = t1.getTime() - t0.getTime();
		            long mins = millisec / (1000*60);
                    
	                if (hour >= hour_for_balance && minutes >= mins_for_balance)
	                {
	                    log.info("Reaching " + hour_for_balance + ":" + mins_for_balance
	                             + ", Stock:" + s.getID() + " bought " + mins + " minutes agao which is less than: " + mins_max + ", but we allow sell it out");
	                    return true;
	                }
		            
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
            
	         String acntId = ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", null);
	            
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
        
        String acntId = ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", s.getID()) + s.getID();
        
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
			int max_trade_lost_times = ParamManager.getIntParam("STOP_TRADE_IF_LOST_MORE_THAN_GAIN_TIMES", "TRADING", s.getID());
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

            
			String acntId = ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", s.getID()) + s.getID();
			
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
		int stop_trade_mode_flg = 0;
		
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();

			// get last trade record.
			sql = "select stop_trade_mode_flg from usrStk " + " where id ='" + s.getID()
					+ "'";
			log.info(sql);
			ResultSet rs = stm.executeQuery(sql);

			if (rs.next()) {
				stop_trade_mode_flg = rs.getInt("stop_trade_mode_flg");
			    log.info("stock:" + s.getID() + "'s sell mode:" + stop_trade_mode_flg);
			} else {
				log.info("Looks stock:" + s.getID() + " is not in usrStk, can not judge sell mode.");
				stop_trade_mode_flg = 0;
			}
			rs.close();
			stm.close();
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stop_trade_mode_flg == 1;
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
        
        double commi_rate  = ParamManager.getFloatParam("COMMISSION_RATE", "VENDOR", null);
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
            sql = "insert into TradeDtl (acntId, stkId, seqnum, price, amount, dl_dt, buy_flg, order_id, trade_selector_name, trade_selector_comment) values( '"
                + ac.getActId() + "','"
                + s.getID() + "',"
                + seqnum + ","
                + soldPrice + ", "
                + sellableAmt
                + ", str_to_date('" + s.getDl_dt().toString() + "', '%Y-%m-%d %H:%i:%s.%f'), 0," + (sim_mode? "null" : tbsr.getOrder_id()) + ",'" + s.getTradedBySelector() + "','" + s.getTradedBySelectorComment() + "')";
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
            StockBuySellEntry stk = null;
            if (rcds != null) {
                log.info("Adding trade record for stock as: " + s.getID());
                stk = new StockBuySellEntry(s.getID(),
                                            s.getName(),
                                            soldPrice,
                                            sellableAmt,
                                            false,
                                            (Timestamp)s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1));
                stk.printStockInfo();
                rcds.add(stk);
            } else {
                log.info("Adding today first trade record for stock as: " + s.getID());
                stk = new StockBuySellEntry(s.getID(),
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
            
            updateLstTradeRecordForStock(stk);
            
            /*
             * Here once after we trade a stock, clear it's historic memory data.
             * This is important as it make a big difference for calculating some
             * critial data like max/min price and based on which buy/sell decision
             * made.
             */
            if (s != null) {
                log.info("After selling " + s.getName() + " clear InjectedRaw Data...");
                s.getSd().clearInjectRawData();
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
        double commi_rate  = ParamManager.getFloatParam("COMMISSION_RATE", "VENDOR", null);
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
            sql = "insert into TradeDtl (acntId, stkId, seqnum, price, amount, dl_dt, buy_flg, order_id, trade_selector_name, trade_selector_comment) values('"
                + ac.getActId() + "','"
                + s.getID() + "',"
                + seqnum + ","
                + buyPrice + ", "
                + buyMnt
                + ", str_to_date('" + s.getDl_dt().toString() + "','%Y-%m-%d %H:%i:%s.%f'), 1," + (sim_mode? "null":tbsr.getOrder_id()) + ",'" + s.getTradedBySelector() + "','" + s.getTradedBySelectorComment() + "')";
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
            StockBuySellEntry stk = null;
            if (rcds != null) {
                log.info("Adding trade record for stock as: " + s.getID());
                stk = new StockBuySellEntry(s.getID(),
                                            s.getName(),
                                            buyPrice,
                                            buyMnt,
                                            true,
                                            (Timestamp)s.getSd().getDl_dt_lst().get(s.getSd().getDl_dt_lst().size() -1));
                stk.printStockInfo();
                rcds.add(stk);
            } else {
                log.info("Adding today first trade record for stock as: " + s.getID());
                stk = new StockBuySellEntry(s.getID(),
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
            
            updateLstTradeRecordForStock(stk);
            
            
            /*
             * Here once after we trade a stock, clear it's historic memory data.
             * This is important as it make a big difference for calculating some
             * critial data like max/min price and based on which buy/sell decision
             * made.
             */
            if (s != null) {
                log.info("After buying " + s.getName() + " clear InjectedRaw Data...");
                s.getSd().clearInjectRawData();
            }
            
            return true;
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
	private void updateLstTradeRecordForStock(StockBuySellEntry rc)
	{
	    synchronized (lstTradeForStocks) {
            
            StockBuySellEntry pre = lstTradeForStocks.get(rc.id);
            
            if (pre == null)
            {
                log.info("Stock " + rc.id + " did new trade, add to lstTradeForStocks.");
                lstTradeForStocks.put(rc.id, rc);
                createBuySellRecord(rc);
            }
            else {
                log.info("had a new trade, refresh balance information:");
                log.info("pre id:" + pre.id);
                log.info("pre name:" + pre.name);
                log.info("pre price:" + pre.price);
                log.info("pre quantity:" + pre.quantity);
                log.info("pre buy_flg:" + pre.is_buy_point);
                
                log.info("cur id:" + rc.id);
                log.info("cur name:" + rc.name);
                log.info("cur price:" + rc.price);
                log.info("cur quantity:" + rc.quantity);
                log.info("cur buy_flg:" + rc.is_buy_point);
                
                if (rc.is_buy_point == pre.is_buy_point){
                    log.info("Same direction trade, refresh lstTradeForStocks.");
                    pre.price = (pre.price * pre.quantity + rc.price * rc.quantity) / (pre.quantity + rc.quantity);
                    pre.quantity = (pre.quantity + rc.quantity);
                    pre.dl_dt = rc.dl_dt;
                    lstTradeForStocks.put(pre.id, pre);
                    updateBuySellRecord(pre);
                }
                else {
                    if (pre.quantity == rc.quantity)
                    {
                        log.info("Revserse trade with same qty, clean up lstTradeForStocks.");
                        lstTradeForStocks.remove(rc.id);
                        removeBuySellRecord(rc);
                    }
                    else if (pre.quantity > rc.quantity)
                    {
                        log.info("pre quantity is  > cur quanaity, subtract cur quantity and refresh the map");
                        pre.quantity -= rc.quantity;
                        lstTradeForStocks.put(pre.id, pre);
                        updateBuySellRecord(pre);
                    }
                    else {
                        log.info("pre quantity is  < cur quanaity, update cur quantity and refresh the map");
                        rc.quantity -= pre.quantity;
                        lstTradeForStocks.put(rc.id, rc);
                        updateBuySellRecord(rc);
                    }
                }
            }
	    }
	}
    /* Now do acutal trade to Local GF trader.*/
    private static TradexBuySellResult placeSellTradeToLocal(Stock2 s, int qtyToTrade, double price) {
            Connection con = DBManager.getConnection();
            Statement stm = null;
        try {
            TradexBuySellResult tbsr = null;
            
            String sql = "insert into pendingTrade select '" + s.getID() + "', case when max(id) is null then 0 else max(id) + 1 end, " + qtyToTrade + ", " + price + ", 0, 0.0, 'N', null, 0, sysdate(), sysdate() from pendingTrade where stock = '" + s.getID() + "'";
            log.info(sql);
            stm = con.createStatement();
            stm.execute(sql);
            
            stm.close();
            
            int MaxTry = 7;
            do {
                
                if (MaxTry == 0)
                {
                    log.info("Attempted 7 times failed, return fail for placeSellTradeToLocal");
                    sql = "update pendingTrade set status = 'C' where stock = '" + s.getID() + "' and status = 'N'";
                    log.info(sql);
                    stm = con.createStatement();
                    stm.execute(sql);
                    return null;
                }
                sql = "select t.success_qty, t.success_price, t.order_id, t.status from pendingTrade t where t.stock = '" + s.getID() + "' and t.id = (select max(id) from pendingTrade p where p.stock = '" + s.getID() + "')";
                log.info(sql);
                stm = con.createStatement();
                ResultSet rs = stm.executeQuery(sql);
                
                if (rs.next())
                {
                    String status = rs.getString("status");
                    if (status.equals("N"))
                    {
                        log.info("Pending for GF trader to finish trading, sleep 1 second.");
                        rs.close();
                        stm.close();
                        Thread.currentThread().sleep(1000);
                        MaxTry--;
                        continue;
                    }
                    else if (status.equals("C"))
                    {
                        log.info("Trading is cancelled, return null to fail the trading");
                        return null;
                    }
                    else {
                        int trade_qty = rs.getInt("success_qty");
                        double trade_price = rs.getDouble("success_price");
                        int order_id = rs.getInt("order_id");
                        tbsr = new TradexBuySellResult(s.getID(),
                                price,
                                qtyToTrade,
                                price * qtyToTrade,
                                order_id,
                                false);
                        rs.close();
                        return tbsr;
                    }
                }
            } while (true);
        } catch (Exception e) {
            log.error("Sell: placeSellTradeToLocal exception:" + e.getMessage());
            return null;
        }
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
	
    /* Now do acutal trade to Tradex system*/
    private static TradexBuySellResult placeBuyTradeToLocal(Stock2 s, int qtyToTrade, double price) {
        Connection con = DBManager.getConnection();
        Statement stm = null;
    try {
        TradexBuySellResult tbsr = null;
        
        String sql = "insert into pendingTrade select '" + s.getID() + "', case when max(id) is null then 0 else max(id) + 1 end, " + qtyToTrade + ", " + price + ", 0, 0.0, 'N', null, 1, sysdate(), sysdate() from pendingTrade where stock = '" + s.getID() + "'";
        log.info(sql);
        stm = con.createStatement();
        stm.execute(sql);
        
        stm.close();
        
        int MaxTry = 7;
        do {
            
            if (MaxTry == 0)
            {
                log.info("Attempted 7 times failed, return fail for placeBuyTradeToLocal");
                sql = "update pendingTrade set status = 'C' where stock = '" + s.getID() + "' and status = 'N' ";
                log.info(sql);
                stm = con.createStatement();
                stm.execute(sql);
                return null;
            } 
            sql = "select t.success_qty, t.success_price, t.order_id, t.status from pendingTrade t where t.stock = '" + s.getID() + "' and t.id = (select max(id) from pendingTrade p where p.stock = '" + s.getID() + "')";
            log.info(sql);
            stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            
            if (rs.next())
            {
                String status = rs.getString("status");
                if (status.equals("N"))
                {
                    log.info("Pending for GF trader to finish trading, sleep 1 second.");
                    rs.close();
                    stm.close();
                    Thread.currentThread().sleep(1000);
                    MaxTry--;
                    continue;
                }
                else if (status.equals("C"))
                {
                    log.info("Trading is cancelled, return null to fail the trading");
                    return null;
                }
                else {
                    int trade_qty = rs.getInt("success_qty");
                    double trade_price = rs.getDouble("success_price");
                    int order_id = rs.getInt("order_id");
                    tbsr = new TradexBuySellResult(s.getID(),
                            price,
                            qtyToTrade,
                            price * qtyToTrade,
                            order_id,
                            true);
                    rs.close();
                    return tbsr;
                }
            }
        } while (true);
    } catch (Exception e) {
        log.error("Sell: placeBuyTradeToLocal exception:" + e.getMessage());
        return null;
    }
    finally {
        try {
            stm.close();
            con.close();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }
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
	
	public boolean createBuySellRecord(StockBuySellEntry rc) {
		String sql;
        
		if (sim_mode)
		{
		     log.info("Simulation mode, do not deal with SellBuyRecord.");
             return false;
		}
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "insert into SellBuyRecord values ('" + rc.id + "'," + rc.price
					+ "," + rc.quantity + ","
					+ rc.is_buy_point
					+ ",str_to_date('" + rc.dl_dt.toString() + "', '%Y-%m-%d %H:%i:%s.%f'))";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			con.close();
			// Here once after we trade a stock, clear it's historic memory
			// data.
			/*if (s != null) {
				log.info("After trade " + s.getName() + " clear InjectedRaw Data...");
				s.getSd().clearInjectRawData();
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	public boolean updateBuySellRecord(StockBuySellEntry rc) {
		String sql;
        
	    if (sim_mode)
	      {
	           log.info("Simulation mode, do not deal with SellBuyRecord.");
	           return false;
	      }
	    
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "update SellBuyRecord set price = " + rc.price
					+ ", qty = " + rc.quantity
					+ ", buy_flg = " + rc.is_buy_point
					+ ", dl_dt = str_to_date('" + rc.dl_dt.toString() + "', '%Y-%m-%d %H:%i:%s.%f')"
					+ " where stkId = '" + rc.id + "'";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			con.close();
			// Here once after we trade a stock, clear it's historic memory
			// data.
			/*if (s != null) {
				log.info("After trade " + s.getName() + " clear InjectedRaw Data...");
				s.getSd().clearInjectRawData();
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	public boolean removeBuySellRecord(StockBuySellEntry rc) {
		String sql;
        
	    if (sim_mode)
        {
             log.info("Simulation mode, do not deal with SellBuyRecord.");
             return false;
        }
	      
		try {
			Connection con = DBManager.getConnection();
			Statement stm = con.createStatement();
			sql = "delete from SellBuyRecord"
					+ " where stkId = '" + rc.id + "'";
			log.info(sql);
			stm.execute(sql);
			stm.close();
			con.close();
			// Here once after we trade a stock, clear it's historic memory
			// data.
			/*if (s != null) {
				log.info("After trade " + s.getName() + " clear InjectedRaw Data...");
				s.getSd().clearInjectRawData();
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
    
	   public void loadBuySellRecord() {
           
	        if (sim_mode)
	        {
	             log.info("Simulation mode, do not deal with SellBuyRecord.");
                 return;
	        }
	        
	       synchronized (TradeStrategyImp.class)
           {
	           if (unbalanced_db_lstTradeForStocks_loaded)
	           {
	               log.info("unbalanced SellBuyRecord already loaded, skip load again.");
                   return;
	           }
	           String sql;
               
	           lstTradeForStocks = new ConcurrentHashMap<String, StockBuySellEntry>();
               int i = 0;
               Connection con = DBManager.getConnection();
               Statement stm = null;
	           
	           try {
                    stm = con.createStatement();
                    ResultSet rs = null;
	                sql = "select t1.*, t2.name from SellBuyRecord t1 join stk t2 on t1.stkId = t2.id order by stkId";
	                log.info(sql);
	                rs = stm.executeQuery(sql);
                    
	                while (rs.next())
	                {
                        i++;
	                    String id = rs.getString("stkId");
	                    String name = rs.getString("name");
	                    double pri = rs.getDouble("price");
	                    int qty = rs.getInt("qty");
                        Timestamp dt = rs.getTimestamp("dl_dt");
                        boolean buy_flg = rs.getBoolean("buy_flg");
                        
                        log.info("Loading SellBuyRecord " + i + " with data:");
                        log.info("ID " + id);
                        log.info("Name " + name);
                        log.info("price " + pri);
                        log.info("quantity " + qty);
                        log.info("dl_dt " + dt.toString());
                        log.info("buy_flg " + buy_flg);
                        
	                    StockBuySellEntry stk = new StockBuySellEntry(id,
	                            name,
	                            pri,
	                            qty,
	                            buy_flg,
	                            dt);
                        
                        lstTradeForStocks.put(rs.getString("stkId"), stk);
	                }
	                
	                unbalanced_db_lstTradeForStocks_loaded = true;
	            } catch (Exception e) {
	                e.printStackTrace();
                    log.info("exception:" + e.getMessage());
	            }
                finally {
                    try {
                        stm.close();
                        con.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        log.info("Unexpected exception:" + e.getErrorCode());
                    }
                }
                log.info("return SellBuyRecord with " + lstTradeForStocks.size() + " records");
            }
	    }
	
	public ICashAccount getCashAcntForStock(String stk) {
        
        if (sim_mode) {
        	String AcntForStk = ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT", stk) + stk;
        	
        	synchronized (cash_account_map)
        	{
                ICashAccount acnt = cash_account_map.get(AcntForStk);
                if (acnt == null) {
                	log.info("No cashAccount for stock:" + stk + " in memory, load from db.");
                    acnt = CashAcntManger.loadAcnt(AcntForStk);
                    
                    double def_init_mnt = ParamManager.getFloatParam("DFT_INIT_MNY", "ACCOUNT", stk);
                    double def_max_mny_per_trade = ParamManager.getFloatParam("DFT_MAX_MNY_PER_TRADE", "ACCOUNT", stk);
                    double def_max_use_pct = ParamManager.getFloatParam("DFT_MAX_USE_PCT", "ACCOUNT", stk);
                    
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
                    double def_init_mnt = ParamManager.getFloatParam("DFT_INIT_MNY", "ACCOUNT", null);
                    double def_max_mny_per_trade = ParamManager.getFloatParam("DFT_MAX_MNY_PER_TRADE", "ACCOUNT", null);
                    double def_max_use_pct = ParamManager.getFloatParam("DFT_MAX_USE_PCT", "ACCOUNT", null);
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
