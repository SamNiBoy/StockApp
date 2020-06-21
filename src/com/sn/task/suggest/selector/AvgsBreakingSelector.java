package com.sn.task.suggest.selector;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.json.JSONArray;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.strategy.TradeStrategyImp;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.IStockSelector;
import com.sn.task.suggest.SuggestStock;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

/*
 * This selector choose stock with below criteria:
 * 1. Average price for 13, 30, 60 days are close within 10% range.
 * 2. If the price is raising, and lowest price > max(above 3 average prices).
 */
public class AvgsBreakingSelector implements IStockSelector {

    static Logger log = Logger.getLogger(ClosePriceUpSelector.class);
    
    private String suggest_by = "AvgsBreakingSelector";
    private String urllnk = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=sz002095&scale=60&ma=no&datalen=1023";
    double MAX_AVGPRC_PCT = ParamManager.getFloatParam("MAX_AVGPRC_PCT", "SUGGESTER", null);
    double maxagvprcpct = MAX_AVGPRC_PCT;
    double avgpri13 = 0.0;
    double avgpri26 = 0.0;
    double avgpri48 = 0.0;
    String on_dte = "";
    boolean isCheckHistoryData = false;
    
    public AvgsBreakingSelector (String s) {
    	on_dte = s;
    }
    
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	
    	boolean isGoodStock = false;
    	
    	try {
    		
    		//first of all, make sure the VOL is plused for the passed 7 days, 6 times bigger than other 6 days.
    		if (!s.isVOLPlused(7, 6.0/7.0)) {
    			log.info("Stock:" + s.getID() + "'s VOL is not plused in the passed 7 days, return false");
    			return false;
    		}
    		
    		avgpri13 = 0.0;
    		avgpri26 = 0.0;
    		avgpri48 = 0.0;
    		
    		if (!getAvgPriceFromDb(s, ac) && !getAvgPriceFromSina(s, ac))
    		{
    			log.info("not able to get average price data for stock:" + s.getID() + " skip suggest it.");
    			return false;
    		}

    		
            log.info("got stock avg price, avg13:" + avgpri13 + ", avg26:" + avgpri26 + ", avgpri48:" + avgpri48);
            
            if (!(avgpri13 > avgpri26 && avgpri26 > avgpri48))
            {
            	log.info("13, 26, and 48 are not ascending order, return false");
            	return false;
            }
            
            double maxpri = avgpri13;
            double minpri = avgpri48;
            
            if (s.getYtClsPri() == null) {
            	s.getSd().LoadData();
            }
            
            log.info("maxpri - minpri:[" + maxpri + "-" + minpri + "] = " + (maxpri - minpri) + " <= " + maxagvprcpct + " * yt_cls_pri:" + s.getYtClsPri() + " =:" + s.getYtClsPri() * maxagvprcpct + "?" + ((maxpri - minpri) < s.getYtClsPri() * 0.1));
            
            if((maxpri - minpri) <= s.getYtClsPri() * maxagvprcpct) {
            	
            	log.info("cur_pri > maxpri" + "=" + s.getCur_pri() + " > " + maxpri + "? " + (s.getCur_pri() > maxpri) + " && yt_cls_pri < maxpri? " + "=" + s.getYtClsPri() + " < " + maxpri + "? " + (s.getYtClsPri() < maxpri));
            	
            	if (s.getCur_pri() > maxpri && s.getYtClsPri() < maxpri)
            	{
                    s.setSuggestedBy(this.suggest_by);
                    s.setSuggestedComment("Avg price for 13, 26, and 48 days is " + maxagvprcpct + " pct close to yt_cls_pri, and cur_pri:" + s.getCur_pri() + " is above max_avgpri:" + maxpri + " which is higher than yt_cls_pri:" + s.getYtClsPri());
                    s.setSuggestedscore((maxpri - minpri) / s.getYtClsPri());
            	    isGoodStock = true;
            	}
            }
        	
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    	}
        
    	log.info("for stock:" + s.getID() + ", calculated isGoodStock:" + isGoodStock);
    	
        return isGoodStock;
    }
    
    private boolean getAvgPriceFromDb(Stock2 s, ICashAccount ac) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean gotDataSuccess = false;
    	try {
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    		String sql = "select 'x' from stkdat2 where id = '" + s.getID() + "' having(left(max(dl_dt) - interval 1 day, 10)) > '" + on_dte + "'";
    		
    		log.info(sql);
    		rs = stm.executeQuery(sql);
    		
    		if (rs.next()) {
    			isCheckHistoryData = true;
    		}
    		else {
    			isCheckHistoryData = false;
    		}
    		
    		rs.close();
    		stm.close();
    		
    	    sql = "select * from stkAvgPri where id = '" + s.getID() + "' and avgpri1 is not null and avgpri2 is not null and avgpri3 is not null and add_dt >= '" + on_dte + "' order by add_dt asc";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		if (rs.next()) {
                avgpri13 = rs.getDouble("avgpri1");
                avgpri26 = rs.getDouble("avgpri2");
                avgpri48 = rs.getDouble("avgpri3");
                gotDataSuccess = true;
    		}
    		
    		rs.close();
    		stm.close();
    		
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
    	return gotDataSuccess;
    }
    
    private boolean saveAvgPriceToDb(Stock2 s, String for_dte) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean saveDataSuccess = false;
    	try {
    		String sql = "insert into stkAvgPri values ('" + s.getID() + "', '" + for_dte + "', round(" + avgpri13 + ", 2), round(" + avgpri26 + ", 2), round(" + avgpri48 + ", 2), sysdate())";
    		log.info(sql);
    		
    		Statement stm = con.createStatement();
    		stm.execute(sql);
    		stm.close();
    		
    		saveDataSuccess = true;
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    		saveDataSuccess = false;
    	}
    	finally {
    		try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage(), e);
				saveDataSuccess = false;
			}
    	}
    	return saveDataSuccess;
    }
    
    private boolean getAvgPriceFromSina(Stock2 s, ICashAccount ac) {
    	
    	if (isCheckHistoryData) {
    		log.info("can not get history data from Sina!");
    		return false;
    	}
    	urllnk = urllnk.replaceFirst("symbol=.*?&", "symbol=" + s.getArea() + s.getID() + "&");
    	
    	boolean gotDataSuccess = false;
    	
    	try {
            log.info("Fetching..." + urllnk);
            URL url = new URL(urllnk);
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String lines;
            StringBuffer sb = new StringBuffer("");
            while ((lines = br.readLine()) != null) {
                lines = new String(lines.getBytes(), "utf-8");
                sb.append(lines);
            }
            
            JSONArray sda = new JSONArray(sb.toString());
            
            //sda should has length of 48 days data like(total 192 objects):
            /*
             * [{"day":"2020-02-25 10:30:00","open":"23.270","high":"23.270","low":"22.850","close":"23.050","volume":"4487022"},
             *  {"day":"2020-02-25 11:30:00","open":"23.040","high":"23.100","low":"22.000","close":"22.480","volume":"3658192"},
             *  {"day":"2020-02-25 14:00:00","open":"22.480","high":"23.190","low":"22.460","close":"23.170","volume":"2074860"},
             *  {"day":"2020-02-25 15:00:00","open":"23.170","high":"23.400","low":"23.110","close":"23.130","volume":"1984741"}...]
             */

            
            int k = 0;
            String lst_dte = "";
            for (int i = sda.length() - 1; i >= 0; i -= 4)
            {
            	k++;
            	
            	if (k == 1) {
            		lst_dte = sda.getJSONObject(i).getString("day").substring(0, 10);
            	}
            	
            	if (k <= 13) {
            	    avgpri13 += sda.getJSONObject(i).getDouble("close");
                	if (k == 13)
                	{
                		avgpri13 /= 13;
                	}
            	}
            	
            	if (k <= 26) {
            	    avgpri26 += sda.getJSONObject(i).getDouble("close");
                	if (k == 26)
                	{
                		avgpri26 /= 26;
                	}
            	}
            	
            	if (k <= 48) {
            	    avgpri48 += sda.getJSONObject(i).getDouble("close");
                	if (k == 48)
                	{
                		avgpri48 /= 48;
                	}
            	}
            }
            
            if (avgpri13 > 0 && avgpri26 > 0 && avgpri48 > 0)
            {
            	gotDataSuccess = true;
        		saveAvgPriceToDb(s, lst_dte);
            }
        	Thread.sleep(3000);
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    	}
        return gotDataSuccess;
    }
	@Override
	public boolean isORCriteria() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean isMandatoryCriteria() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		if (harder) {
			maxagvprcpct -= 0.01;
		}
		else {
			maxagvprcpct += 0.01;
		}
		
		if (maxagvprcpct <= 0) {
			log.info("maxagvprcpct reached 0, user 0.01");
			maxagvprcpct = 0.01;
		}
		else if (maxagvprcpct > MAX_AVGPRC_PCT) {
			log.info("maxagvprcpct reached MAX_AVGPRC_PCT, user MAX_AVGPRC_PCT:" + MAX_AVGPRC_PCT);
			maxagvprcpct = MAX_AVGPRC_PCT;
		}
		log.info("AvgsBreakingSelector is mandatory criteria, adjusted to value:" + maxagvprcpct + " with harder:" + harder);
		return true;
	}
}
