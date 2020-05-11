package com.sn.task.suggest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sn.db.DBManager;
import com.sn.mail.RecommandStockObserverable;
import com.sn.STConstants;
import com.sn.task.fetcher.StockDataFetcher;
import com.sn.task.suggest.selector.AvgClsPriStockSelector;
import com.sn.task.suggest.selector.AvgsBreakingSelector;
import com.sn.task.suggest.selector.ClosePriceUpSelector;
import com.sn.task.suggest.selector.DealMountStockSelector;
import com.sn.task.suggest.selector.DefaultStockSelector;
import com.sn.task.suggest.selector.KeepGainStockSelector;
import com.sn.task.suggest.selector.PriceShakingStockSelector;
import com.sn.task.suggest.selector.PriceStockSelector;
import com.sn.task.suggest.selector.StddevStockSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.IStockSelector;
import com.sn.task.WorkManager;
import com.sn.task.IWork;

public class SuggestStock implements Job {

	int maxLstNum = 50;

	List<IStockSelector> selectors = new LinkedList<IStockSelector>();

	static SuggestStock self = null;
	
	static List<Stock2> stocksWaitForMail = new LinkedList<Stock2>();
	
	static RecommandStockObserverable rso = new RecommandStockObserverable();
	
	static String on_dte="";
	static boolean needMail = true;

	static Logger log = Logger.getLogger(SuggestStock.class);

	/**
	 * @param args
	 * @throws JobExecutionException 
	 */
	public static void main(String[] args) throws JobExecutionException {
		// TODO Auto-generated method stub
		SuggestStock fsd = new SuggestStock("2020-05-11", false);
		fsd.execute(null);
		log.info("Main exit");
		//WorkManager.submitWork(fsd);
	}
	
	public SuggestStock() {

		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
        
		try {
			sql = "select left(max(dl_dt), 10) sd from stkdat2";
			log.info(sql);
			stm = con.createStatement();
			ResultSet rs = stm.executeQuery(sql);
			
			rs.next();
			
			on_dte = rs.getString("sd");
			
			rs.close();
			
		} catch (Exception e) {
			e.printStackTrace();
            log.error(e.getMessage() + " with error code:" + e.getCause()); 
		}
        finally {
			try {
    			stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
               log.error(e.getMessage() + " with error code:" + e.getErrorCode()); 
            }
        }
	
	}

	public SuggestStock(String s, boolean nm) {

		on_dte = s;
		needMail = nm;
	}
	
	private void initSelector() {
		selectors.clear();
		//initDelay = id;
		//delayBeforNxtStart = dbn;
		//selectors.add(new DefaultStockSelector());
		selectors.add(new PriceStockSelector());
		//selectors.add(new StddevStockSelector());
		//selectors.add(new DealMountStockSelector());
		//selectors.add(new ClosePriceUpSelector(on_dte));
		//selectors.add(new PriceShakingStockSelector(on_dte));
		selectors.add(new AvgsBreakingSelector(on_dte));
		//selectors.add(new KeepGainStockSelector());
//		selectors.add(new KeepLostStockSelector());
	}

    public void execute(JobExecutionContext context)
            throws JobExecutionException {
		// TODO Auto-generated method stub
		boolean suggest_flg = false;
		boolean loop_nxt_stock = false;

        /*StockDataFetcher.lock.lock();
        try {
            log.info("Waiting finishedOneRoundFetch before start SuggestStock stocks...");
            StockDataFetcher.finishedOneRoundFetch.await();
        }
        catch (Exception e) {
            e.printStackTrace();
            log.error("Waiting finishedOneRondFetch before run SuggestStock errored:" + e.getMessage() + ", code:" + e.getCause());
        }
        finally {
            StockDataFetcher.lock.unlock();
        }*/
        

		resetSuggestion();
		
		try {
			Map<String, Stock2> stks = StockMarket.getStocks();
			int NumOfStockToSuggest = ParamManager.getIntParam("NUM_STOCK_TO_SUGGEST", "SUGGESTER", null);
			int NumOfStockForTrade = ParamManager.getIntParam("NUM_STOCK_IN_TRADE", "TRADING", null);
			int tryCnt = 5;
			boolean tryHarderCriteria = false;
			while(tryCnt-- > 0) {
			    for (String stk : stks.keySet()) {
			    	Stock2 s = stks.get(stk);
                    
			    	/*if (!s.getID().equals("002349") && !s.getID().equals("603200") && !s.getID().equals("600882"))
			    	{
			    	    continue;
			    	}*/
			    	for (IStockSelector slt : selectors) {
			    		if (slt.isMandatoryCriteria() && !slt.isTargetStock(s, null)) {
			    			loop_nxt_stock = true;
			    			break;
			    		}
			    		else {
			    			suggest_flg = true;
			    		}
			    	}
			    	if (!loop_nxt_stock) {
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
			    		if (suggest_flg) {
			    			if(s.getCur_pri() == null || s.getCur_pri() <= 0) {
			    			    s.getSd().LoadData();
			    			}
			    			stocksWaitForMail.add(s);
			    		}
			    		suggest_flg = false;
			    	}
			    	loop_nxt_stock = false;
			    }
			    if (stocksWaitForMail.size() < NumOfStockForTrade && tryCnt > 0) {
			    	log.info("stocksWaitForMail is " + stocksWaitForMail.size() + " less than NumOfStockForTrade:" + NumOfStockForTrade + ", tryHarderCriteria set to false");
			    	tryHarderCriteria = false;
			    }
			    else if (stocksWaitForMail.size() > NumOfStockToSuggest) {
			    	log.info("stocksWaitForMail has " + stocksWaitForMail.size() + " which is more than " + NumOfStockToSuggest + ", tryHarderCriteria set to true");
			    	tryHarderCriteria = true;
			    	stocksWaitForMail.clear();
			    }
			    else {
			    	for (Stock2 s2 : stocksWaitForMail) {
		    			suggestStock(s2);
			    	}
			    	electStockforTrade();
			    	if (stocksWaitForMail.size() > 0 && needMail) {
			    	    rso.addStockToSuggest(stocksWaitForMail);
			    	    rso.update();
			    	    stocksWaitForMail.clear();
			    	}
			    	break;
			    }
			    log.info("Now recommand result is not good, adjust criteris to recommand");
			    for (IStockSelector slt : selectors) {
			    	slt.adjustCriteria(tryHarderCriteria);
			    }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("SuggestStock Now exit!!!");
	}

	private void suggestStock(Stock2 s) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		ResultSet rs = null;
		try {
		    String system_role_for_suggest = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", s.getID());
			sql = "select * from usr where suggest_stock_enabled = 1";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			while (rs.next()) {
				String openID = rs.getString("openID");
				sql = "select gz_flg from usrStk where openID = '" + openID + "' and id = '" + s.getID() + "'";
				Statement stm2 = con.createStatement();
				ResultSet rs2 = stm2.executeQuery(sql);
				sql = "";
				if (rs2.next()) {
					if (rs2.getLong("gz_flg") == 0) {
						sql = "update usrStk set gz_flg = 1, stop_trade_mode_flg = 0, suggested_by = '" + system_role_for_suggest + "', mod_dt = sysdate() " + ", suggested_by_selector = '" + s.getSuggestedBy() + "', suggested_comment = '" +
					s.getSuggestedComment() + "' where openID = '" + openID
								+ "' and id = '" + s.getID() + "'";
					}
				} else {
					sql = "insert into usrStk values ('" + openID + "','" + s.getID() + "',1,0,'" + system_role_for_suggest + "','" + s.getSuggestedBy() + "','" + s.getSuggestedComment() + "', sysdate(), sysdate())";
				}
				rs2.close();
				stm2.close();

				if (sql.length() > 0) {
					log.info(sql);
					stm2 = con.createStatement();
					stm2.execute(sql);
					stm2.close();
				}
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        finally {
			try {
     			stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                log.error(e.getMessage() + " with error code: " + e.getErrorCode());
            }
            
        }
	}
	
	public static void electStockforTrade() {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		ResultSet rs = null;
		try {
	        String system_role_for_suggest = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
			sql = "select * from usr where suggest_stock_enabled = 1";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			while (rs.next()) {
				String openID = rs.getString("openID");
				sql = "select id from usrStk where openID = '" + openID + "' and stop_trade_mode_flg = 0 and gz_flg = 1 and suggested_by <> '" + system_role_for_suggest + "' order by id";
				log.info(sql);
				Statement stm2 = con.createStatement();
				ResultSet rs2 = stm2.executeQuery(sql);
				sql = "";
				while (rs2.next()) {
					String stkid = rs2.getString("id");
					if (shouldStockExitTrade(stkid)) {
						putStockToStopTradeMode(stkid);
					}
				}
				rs2.close();
				stm2.close();
				
				int num_stok_in_trade = ParamManager.getIntParam("NUM_STOCK_IN_TRADE", "TRADING", null);
				sql = "select count(*) cnt from usrStk where openID = '" + openID + "' and stop_trade_mode_flg = 0 and gz_flg = 1 and suggested_by <> '" + system_role_for_suggest + "' order by id";
				
				stm2 = con.createStatement();
				rs2 = stm2.executeQuery(sql);
				
				rs2.next();
				
				int num_in_trading = rs2.getInt("cnt");
				
				log.info("Need to keep:"+ num_stok_in_trade + " to trade, with:" + num_in_trading + " in trading already.");
				
				int new_num_to_trade = num_stok_in_trade - num_in_trading;
				
				if (new_num_to_trade > 0) {
					moveStockToTrade(new_num_to_trade);
				}
				rs2.close();
				stm2.close();
			}
			rs.close();
			stm.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
        finally {
			try {
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage() + " with error code: " + e.getErrorCode());
            }
        }
	}
	
	private static void moveStockToTrade(int maxCnt) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		ResultSet rs = null;
		Set<String> stockMoved = new HashSet<String>();
		int grantCnt = 0;
		if (maxCnt <= 0) {
			log.info("maxCnt must be > 0 for move stockToTrade.");
			return;
		}
        
		String system_role_for_suggest = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
		String system_role_for_trade = ParamManager.getStr2Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
		try {
			sql = "select distinct s.id, ss.max_score from usrStk s left join (select stock, max(score) max_score from stockparamsearch group by stock) ss on s.id = ss.stock "
				+ "where s.gz_flg = 1 "
				+ "  and s.suggested_by in ('" + system_role_for_suggest + "') "
				+ "  and s.stop_trade_mode_flg = 0 "
				+ "  order by case when ss.max_score is null then 0 else ss.max_score end desc, s.suggested_comment desc ";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			while (rs.next() && maxCnt-- > 0) {
				String id = rs.getString("id");
				sql = "update usrStk set suggested_by = '" + system_role_for_trade + "',  gz_flg = 1, stop_trade_mode_flg = 0, mod_dt = sysdate() where id ='" + id + "'";
				Statement stm2 = con.createStatement();
				stm2.execute(sql);
				stm2.close();
				grantCnt++;
				stockMoved.add(id);
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage() + " with error code: " + e.getErrorCode());
            }
        }
		
		/*Iterator<Stock2> it = stocksWaitForMail.iterator();
		while(it.hasNext())
		{
			Stock2 s = it.next();
			if (!stockMoved.contains(s.getID())) {
				log.info("remove stock:" + s.getID() + " as it is not moved for trade.");
				it.remove();
			}
		}*/
		log.info("Total granted:" + grantCnt + " stocks for trading.");
	}
	
	private static void putStockToStopTradeMode(String stkid) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		try {
			sql = "update usrStk set stop_trade_mode_flg = 1, mod_dt = sysdate() where id = '" + stkid + "'";
			log.info(sql);
			stm = con.createStatement();
			stm.execute(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage() + " with error code: " + e.getErrorCode());
            }
        }
	}
	
	/*
	 * This method return true if the stock was suggested by selector AvgsBreakingSelector, when:
	 * 1. The close price is < max(3 avg prices: 13, 26, 48 days).
	 * 2. Yt_cls_pri was > max(3 avg prices: 13, 26, 48 days).
	 */
	public static boolean checkClosePriDownThroughAvgs(String stkid)
	{
    	
    	Connection con = DBManager.getConnection();
    	
        double avgpri13 = 0.0;
        double avgpri26 = 0.0;
        double avgpri48 = 0.0;
        double cur_pri = 0.0;
        double yt_cls_pri = 0.0;
    	try {
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    		String sql = "select * from stkAvgPri where id = '" + stkid + "' and avgpri1 is not null and avgpri2 is not null and avgpri3 is not null and add_dt = '" + on_dte + "'"
    				    + " and exists (select 'x' from usrstk u where u.id = stkAvgPri.id and u.suggested_by_selector = 'AvgsBreakingSelector')";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		if (rs.next()) {
                avgpri13 = rs.getDouble("avgpri1");
                avgpri26 = rs.getDouble("avgpri2");
                avgpri48 = rs.getDouble("avgpri3");
    		}
    		
    		rs.close();
    		stm.close();
    		
    		//No need to do below if > 0 price is not got.
    		if (avgpri13 > 0)
    		{
                double maxpri = avgpri13 > (avgpri26 > avgpri48 ? avgpri26 : avgpri48) ? avgpri13 : (avgpri26 > avgpri48 ? avgpri26 : avgpri48);
                double minpri = avgpri13 < (avgpri26 < avgpri48 ? avgpri26 : avgpri48) ? avgpri13 : (avgpri26 < avgpri48 ? avgpri26 : avgpri48);
                
                sql = "select cur_pri, yt_cls_pri from stkdat2 where id = '" +stkid + "' and ft_id = (select max(s2.ft_id) from stkdat2 s2 where s2.id = stkdat2.id and left(s2.dl_dt, 10) = '" + on_dte + "')";
                
    		    log.info(sql);
    		    stm = con.createStatement();
    		    rs = stm.executeQuery(sql);
    		    
    		    if (rs.next()) {
    		    	cur_pri = rs.getDouble("cur_pri");
    		    	yt_cls_pri = rs.getDouble("yt_cls_pri");
    		    }
    		    
    		    rs.close();
    		    stm.close();
    		    
    		    log.info("Check if stock:" + stkid + " price is breaking down highest avgs, cur_pri < maxpri ?" + cur_pri + " < " + maxpri + "? " + (cur_pri < maxpri) + " && yt_cls_pri > maxpri?" + yt_cls_pri + " > " + maxpri + "?" + (yt_cls_pri > maxpri));
    		    if (maxpri > minpri && minpri > 0 && cur_pri > 0 && yt_cls_pri > 0)
    		    {
    		    	if (cur_pri < maxpri && yt_cls_pri > maxpri) {
    		    		log.info("Now the stock:" + stkid + " cur_pri is breaking down higest avg price, should exit trade.");
    		    		return true;
    		    	}
    		    }
    		}
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    	}
    	finally {
    		try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage(), e);
			}
    	}
    	return false;
    }
	
	/*
	 * This method return 3 values to indicate the trend of the stock:
	 *  1: This means the min/max price of today is both higher than yesterday, up trend.
	 * -1: This means the min/max price of today is both lower than yesterday, down trend.
	 *  0: none of above.
	 */
	public static int calculateStockTrend(String stkid)
	{
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		ResultSet rs = null;
		double yt_opn_pri = 0.0;
		double yt_cls_pri = 0.0;
		double td_opn_pri = 0.0;
		double td_cls_pri = 0.0;
		long max_td_ft_id = 0;
		int trend = 0;
		
		try {
			sql = "select max(td_opn_pri) lst_opn_pri, max(yt_cls_pri) lst_cls_pri, max(td_opn_pri) td_opn_pri, max(ft_id) max_ft_id, left(dl_dt, 10) dte "
			    + "  from stkdat2 "
			    + " where id = '" + stkid + "' "
			    + "   and left(dl_dt, 10) >= left(str_to_date('" + on_dte + "', '%Y-%m-%d') - interval 15 day, 10)"
			    + "   and left(dl_dt, 10) <= left(str_to_date('" + on_dte + "', '%Y-%m-%d'), 10)"
			    + " group by left(dl_dt, 10) order by dte desc";
			
			log.info(sql);
			
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			
			if (rs.next() && rs.getDouble("td_opn_pri") > 0) {
				td_opn_pri = rs.getDouble("td_opn_pri");
				yt_cls_pri = rs.getDouble("lst_cls_pri");
				max_td_ft_id = rs.getLong("max_ft_id");
			}
			
			if (rs.next() && rs.getDouble("td_opn_pri") > 0) {
				yt_opn_pri = rs.getDouble("td_opn_pri");
			}
			
			rs.close();
			stm.close();
			
			stm = con.createStatement();
			
			sql = "select cur_pri td_cls_pri from stkdat2 "
			    + " where id = '" + stkid + "'"
			    + "   and ft_id = " + max_td_ft_id;
			
			log.info(sql);
			
			rs = stm.executeQuery(sql);
			
			rs.next();
			td_cls_pri = rs.getDouble("td_cls_pri");
			
			rs.close();
			stm.close();
			
			log.info("Got min/max price for stock:" + stkid + " yt_opn_pri/yt_cls_pri: [" + yt_opn_pri + "," + yt_cls_pri + "]" + " td_opn_pri/td_cls_pri: [" + td_opn_pri + "," + td_cls_pri + "]");
			if (yt_opn_pri > 0 && td_opn_pri > 0 && yt_cls_pri > 0 && td_cls_pri > 0) 
			{
	  			if (td_opn_pri > yt_opn_pri && td_cls_pri > yt_cls_pri) {
	  				trend = 1;
				}
				else if ((td_opn_pri < yt_opn_pri && td_cls_pri < yt_cls_pri) || ((td_cls_pri - yt_cls_pri) / yt_cls_pri < -0.05)) {
					trend = -1;
				}
			}
		}
		catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		finally {
			try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage(), e);
			}
		}
		
		log.info("calculate trend:" + trend + " for stock:" + stkid);
		return trend;
	}
	
	public static boolean shouldStockExitTrade(String stkid) {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
		ResultSet rs = null;
		boolean lost = false;
		try {
			sql = "select c.pft_mny from cashacnt c join tradehdr h on c.acntid = h.acntid where h.stkid = '" + stkid + "' and c.pft_mny < 0";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			if (rs.next()) {
				log.info("Stock:"+ stkid + " with pft_mnt:" + rs.getDouble("pft_mny") + " which is a lost, stop trade.");
				lost = true;
			}
			rs.close();
			stm.close();
			
            //int max_lost_before_exit = ParamManager.getIntParam("MAX_LOST_TIME_BEFORE_EXIT_TRADE", "TRADING", stkid);

			if (lost) {
				log.info("Stock:" + stkid + " has lost, stop trade.");
				return true;
			}
			
//			if (calculateStockTrend(stkid) < 0) {
//				log.info("stock:" + stkid + " trend is going down, put to stop trading.");
//				return true;
//			}
//			
//			if (checkClosePriDownThroughAvgs(stkid)) {
//				log.info("stock:" + stkid + " was suggested by AvgsBreakingSelector, but now the price is breaking avg price, exit trade.");
//				return true;
//			}
            int max_days_without_trade_to_exit = ParamManager.getIntParam("MAX_DAYS_WITHOUT_TRADE_BEFORE_EXIT_TRADE", "TRADING", stkid);
			sql = "select 'x' from dual where exists (select 'x' from tradedtl where stkid = '" + stkid + "'"
				+ "   and dl_dt >= sysdate() - interval " + max_days_without_trade_to_exit + " day)"
				+ " or exists (select 'y' from usrStk where gz_flg = 1 and id = '" + stkid +  "' and add_dt > sysdate() - interval " + max_days_without_trade_to_exit + " day)";
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			if (!rs.next()) {
				log.info("Stock:" + stkid + max_days_without_trade_to_exit + " days without trade record, should exit trade.");
				return true;
			}
			
			rs.close();
			
//			sql = "select 'x' from stockparam where stock = '" + stkid + "'";
//			log.info(sql);
//			stm = con.createStatement();
//			rs = stm.executeQuery(sql);
//			if (!rs.next()) {
//				log.info("Stock:" + stkid + " has no stockparam trained , should exit trade.");
//				return true;
//			}
//			
//			rs.close();
			
			
            double pct_disable_trade = ParamManager.getFloatParam("PCT_BUYSELL_THRESH_DIFF_DISABLE_TRADE", "SUGGESTER", null);
			sql = "select 'x' from stockparam b join stockparam s on b.stock = s.stock where b.stock = '" + stkid + "' and b.name = 'BUY_BASE_TRADE_THRESH' and s.name = 'SELL_BASE_TRADE_THRESH' and abs(b.fltval - s.fltval) >= " + pct_disable_trade;
			log.info(sql);
			stm = con.createStatement();
			rs = stm.executeQuery(sql);
			if (rs.next()) {
				log.info("Stock:" + stkid + "buy/sell theshold value diff > " +pct_disable_trade + " , should exit trade.");
				return true;
			}
			
			rs.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage() + " with error code: " + e.getErrorCode());
            }
        }

		return false;
	}
	
	private void resetSuggestion() {
		String sql = "";
		Connection con = DBManager.getConnection();
		Statement stm = null;
        
		initSelector();
		
		stocksWaitForMail.clear();
		
	    String system_role_for_suggest = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null);
	      
		try {
			sql = "delete from usrStk where suggested_by in ('" + system_role_for_suggest + "')";
			log.info(sql);
			stm = con.createStatement();
			stm.execute(sql);
		} catch (Exception e) {
			e.printStackTrace();
            log.error(e.getMessage() + " with error code:" + e.getCause()); 
		}
        finally {
			try {
    			stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
               log.error(e.getMessage() + " with error code:" + e.getErrorCode()); 
            }
        }
	}
}
