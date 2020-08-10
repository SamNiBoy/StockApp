package com.sn.task.sellmode;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.mail.SellModeStockObserverable;
import com.sn.STConstants;
import com.sn.task.fetcher.StockDataFetcher;
import com.sn.task.sellmode.selector.BadTradeSellModeSelector;
import com.sn.task.suggest.selector.AvgClsPriSellModeSelector;
import com.sn.task.suggest.selector.CurPriLostSellModeSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.IStockSelector;
import com.sn.task.WorkManager;
import com.sn.task.IWork;

public class SellModeWatchDog implements IWork {

	/*
	 * Initial delay before executing work.
	 */
	static long initDelay = 0;

	/*
	 * Seconds delay befor executing next work.
	 */
	static long delayBeforNxtStart = 5;

	static TimeUnit tu = TimeUnit.MILLISECONDS;

	static public String resMsg = "Initial msg for work TradeWatchDog.";

	static List<IStockSelector> selectors = new LinkedList<IStockSelector>();

	static SellModeWatchDog self = null;
	
	static List<Stock2> stocksSellModeWaitForMail = new LinkedList<Stock2>();
	static List<Stock2> stocksUnSellModeWaitForMail = new LinkedList<Stock2>();
	
	static SellModeStockObserverable rso = new SellModeStockObserverable();
	
	static String process_dte = "";

	static Logger log = Logger.getLogger(SellModeWatchDog.class);

	public static String getResMsg() {
		return resMsg;
	}

	public static void setResMsg(String resMsg) {
		SellModeWatchDog.resMsg = resMsg;
	}

	static public boolean start() {
		self = new SellModeWatchDog(0, 5 * 60000);
		if (WorkManager.submitWork(self)) {
			resMsg = "Newly created TradeWatchDog and started!";
			return true;
		}
		return false;
	}

	static public boolean stop() {
		if (WorkManager.cancelWork(self.getWorkName())) {
			resMsg = "TradeWatchDog is cancelled successfully.";
			return true;
		}
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//SellModeWatchDog.start();
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		boolean sell_now_flg = false;
		try {
			sql = "select distinct add_dt from stkavgpri order by add_dt";
			log.info(sql);
			stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);
			while (rs.next()) {
				processInHandStockModeSetup(rs.getString("add_dt"));
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
		    try {
		    	//log.info("Closing statement and connection!");
				stm.close();
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
                log.error(e.getMessage() + " with error:" + e.getErrorCode());
			}
		}
		
	}

	public SellModeWatchDog(long id, long dbn) {
		initDelay = id;
		delayBeforNxtStart = dbn;
		//selectors.add(new DefaultSellModeSelector());
		//selectors.add(new AvgClsPriSellModeSelector());
		//selectors.add(new CurPriLostSellModeSelector());
		selectors.add(new BadTradeSellModeSelector());
	}

	public void run() {
        
        StockDataFetcher.lock.lock();
        try {
            log.info("Waiting finishedOneRondFetch before run SellModeWatchDog...");
            StockDataFetcher.finishedOneRoundFetch.await();
        }
        catch (Exception e) {
            e.printStackTrace();
            log.error("\"Waiting finishedOneRondFetch before run SellModeWatchDog errored:" + e.getMessage() + ", code:" + e.getCause());
        }
        finally {
            StockDataFetcher.lock.unlock();
        }

		// TODO Auto-generated method stub
		try {
	        Map<String, Stock2> stks = StockMarket.getGzstocks();
	        for (String stk : stks.keySet()) {
	        	Stock2 s = stks.get(stk);
	        	
	        	if (!isStockInStopTradeMode(s) && stockMatchSellMode(s)) {
            		log.info("put stock:" + s.getID() + ", name:" + s.getName() + " into sell mode.");
            		setStockStopTradeMode(s, true);
	        		stocksSellModeWaitForMail.add(s);
	        	}
	        	else if (isStockInStopTradeMode(s) && stockMatchUnSellMode(s)) {
            		log.info("put stock:" + s.getID() + ", name:" + s.getName() + " back to non-sell mode.");
            		setStockStopTradeMode(s, false);
	        		stocksUnSellModeWaitForMail.add(s);
	        	}
                else {
            		log.info("stock:" + s.getID() + ", name:" + s.getName() + " sell mode does not need to change.");
                }
	       }

	       rso.addStockToSellMode(stocksSellModeWaitForMail);
	       rso.addStockToUnsellMode(stocksUnSellModeWaitForMail);
	       rso.update();
	       stocksSellModeWaitForMail.clear();
	       stocksUnSellModeWaitForMail.clear();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    log.info("TradeWatchDog Now exit!!!");
	}
	
	private boolean stockMatchSellMode(Stock2 s) {
        boolean suggest_flg = false;

        for (IStockSelector slt : selectors) {
           	if (slt.isMandatoryCriteria() && !slt.isTargetStock(s, null)) {
           		log.info("stockMatchSellMode mandatory criteria not pass, return false.");
           		suggest_flg = false;
           		break;
           	}
           	else {
           		suggest_flg = true;
           	}
        }
        if (suggest_flg) {
        	for (IStockSelector slt : selectors) {
        		if (slt.isMandatoryCriteria()) {
        			continue;
        		}
        		if (slt.isTargetStock(s, null)) {
        			suggest_flg = true;
        			if (slt.isORCriteria()) {
        				log.info("Or criteria matched, suggest the stock:" + s.getID());
        				break;
        			}
        		}
        		else {
        			if (slt.isORCriteria()) {
        				log.info("Or criteria not matched, continue next criteira.");
        				suggest_flg = false;
        				continue;
        			} else {
        				suggest_flg = false;
        				break;
        			}
        		}
        	}
        }
        return suggest_flg;
	}
	 
	private boolean stockMatchUnSellMode(Stock2 s) {
        boolean suggest_flg = true;

        boolean pre_trade_mode = isStockInStopTradeMode(s);
       for (IStockSelector slt : selectors) {
       	   if (slt.isTargetStock(s, null)) {
       	   	   log.info("unset sell mode return false for non mandatory criteria.");
       	   	   suggest_flg = false;
       	   	   break;
       	   }
       }
       
       //If we are going to disable sell mode, make more safe check.
       if (pre_trade_mode && suggest_flg) {
           Double ytclspri = s.getYtClsPri();
           Double curPri = s.getCur_pri();
           Double opnPri = s.getOpen_pri();
           double incPct = 0.0;
           
           if (ytclspri != null && curPri != null && opnPri != null && ytclspri > 0) {
               incPct = (curPri - opnPri)/ ytclspri;
               log.info("got incPct:" + incPct);
           }
           
           double max_pct_to_disable_sell_mode = ParamManager.getIntParam("MAX_GAIN_PCT_FOR_DISABLE_SELL_MODE", "TRADING", s.getID());
           
           if (incPct < max_pct_to_disable_sell_mode) {
               log.info("cur price is incPct:" + incPct + " which is less 5% yt_cls_pri, not suggest disable sell mode.");
               suggest_flg = false;
           }
       }
        return suggest_flg;
	}
	
	public static boolean isStockInStopTradeMode(Stock2 s) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		boolean is_in_sell_mode = false;
		try {
			sql = "select stop_trade_mode_flg from usrStk where id = '" + s.getID() + "'";
			log.info(sql);
			stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);
			if (rs.next()) {
			    is_in_sell_mode = rs.getInt("stop_trade_mode_flg") == 1;
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
			log.info("stock " + s.getName() + " is not currently in sell mode!");
		}
		finally {
		    try {
		    	//log.info("Closing statement and connection!");
				stm.close();
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
                log.error(e.getMessage() + " with error:" + e.getErrorCode());
			}
		}
		log.info("Stock " + s.getName() + "'s sell mode is:" + is_in_sell_mode);
		return is_in_sell_mode;
	}
	
	public static boolean isStockInSellNowMode(Stock2 s) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		boolean sell_now_flg = false;
		try {
			sql = "select sell_now_flg from usrStk where id = '" + s.getID() + "'";
			log.info(sql);
			stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);
			if (rs.next()) {
				sell_now_flg = rs.getInt("sell_now_flg") == 1;
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
			log.info("stock " + s.getName() + " is not currently in sell_now_flg mode!");
		}
		finally {
		    try {
		    	//log.info("Closing statement and connection!");
				stm.close();
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
                log.error(e.getMessage() + " with error:" + e.getErrorCode());
			}
		}
		log.info("Stock " + s.getName() + "'s sell_now_flg mode is:" + sell_now_flg);
		return sell_now_flg;
	}
	
	public static boolean processInHandStockModeSetup(String on_dte) {
		
		log.info("Now check sell mode flag process begin.");
		synchronized (process_dte) {
			if (on_dte.equals(process_dte)) {
				log.info("sell now mode process check for date:" + on_dte + " already done, skip.");
				return true;
			}
			else {
				process_dte = on_dte;
			}
		}
		double putSellNowModeIfPctStockLost = 0.5;
		Connection con = DBManager.getConnection();
		Statement stm = null;
		
		int totalCnt = 0;
		int lostCnt = 0;
		int winCnt = 0;
		String stkLst = "";
		try {
			String sql = "select stkId from tradehdr order by stkId ";
			//log.info(sql);
			stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);
			
			while (rs.next()) {
				
				totalCnt++;
				
				String stkid = rs.getString("stkId");
				
				sql = "select * from stkavgpri s where s.id = '" + stkid + "' and add_dt < '" + on_dte + "' order by add_dt desc";
				//log.info(sql);
				
				Statement stm2 = con.createStatement();
				ResultSet rs2 = stm2.executeQuery(sql);
				
				if (rs2.next()) {
					double td_close1 = rs2.getDouble("close");
					if (rs2.next()) {
						double td_close2 = rs2.getDouble("close");
						if ((td_close1 - td_close2) / td_close1 < -0.01) {
							lostCnt++;
							if (stkLst.length() > 0) {
								stkLst += ",";
							}
							stkLst += "'" + rs2.getString("id") + "'";
							//log.info("Stock:" + stkid + " got close price lost, now lostCnt:" + lostCnt);
						}
						else if ((td_close1 - td_close2) / td_close1 > 0.01) {
							winCnt++;
						}
					}
				}
				rs2.close();
				stm2.close();
			}
			rs.close();
			
			if (totalCnt <= 0) {
				log.info("No dealed stock, skip sell mode flag process.");
				return false;
			}
			
			int total_buy_count_limit = ParamManager.getIntParam("MAX_BUY_TIMES_TOTAL_LIMIT", "TRADING", null);
			log.info("on_dte:" + on_dte + " check if put all stock in sell now mode by looking at lost pct: " + (lostCnt * 1.0 / totalCnt) + ", lostCnt:" + lostCnt + ", totalCnt:" + totalCnt + ", if reach buy limit:" + total_buy_count_limit);
			log.info("on_dte:" + on_dte + " check if put all stock in sell now mode by looking at win pct: " + (winCnt * 1.0 / totalCnt) + ", winCnt:" + winCnt + ", totalCnt:" + totalCnt + ", if reach buy limit:" + total_buy_count_limit);
			
			if (totalCnt >= total_buy_count_limit && lostCnt * 1.0 / totalCnt >= putSellNowModeIfPctStockLost) {
				sql = "update usrstk set sell_now_flg = 1, suggested_comment = 'SYSTEM_PUT_TO_SELL' where sell_now_flg = 0 " +
			           " and id in (select right(acntId, 6) from cashacnt where pft_mny < 0) "; //here, we only care stock in lost, this is for scenario after we buy a lot of stock and then big drop.
			          //" and id in (" + stkLst + ")";
				log.info(sql);
				Statement stm3 = con.createStatement();
				stm3.execute(sql);
				stm3.close();
				log.info("success put all stock into sell now mode.");
				return true;
			}
			else {
				sql = "update usrstk set sell_now_flg = 0 where sell_now_flg = 1 and suggested_comment = 'SYSTEM_PUT_TO_SELL'";
				log.info(sql);
				Statement stm3 = con.createStatement();
				stm3.execute(sql);
				stm3.close();
				log.info("success remmoved all stock from sell now mode.");
				return true;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
		    try {
		    	//log.info("Closing statement and connection!");
				stm.close();
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
                log.error(e.getMessage() + " with error:" + e.getErrorCode());
			}
		}
		return false;
	}

	private void setStockStopTradeMode(Stock2 s, boolean to_stop_trade_mode_flg) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		try {
			sql = "update usrStk set stop_trade_mode_flg = " + (to_stop_trade_mode_flg ?  "1": "0") + " where id = '" + s.getID() + "'";
			log.info(sql);
			stm = con.createStatement();
			stm.execute(sql);
		} catch (Exception e) {
			e.printStackTrace();
            log.error(e.getMessage() + " with error0:" + e.getCause());
		}
		finally {
		    try {
		    	//log.info("Closing statement and connection!");
				stm.close();
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
                log.error(e.getMessage() + " with error:" + e.getErrorCode());
			}
		}
	}
	
	public String getWorkResult() {
		return "";
	}

	public String getWorkName() {
		return "SellModeWatchDog";
	}

	public long getInitDelay() {
		return initDelay;
	}

	public long getDelayBeforeNxt() {
		return delayBeforNxtStart;
	}

	public TimeUnit getTimeUnit() {
		return tu;
	}

	public boolean isCycleWork() {
		return true;
	}

}
