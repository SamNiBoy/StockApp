package com.sn.strategy.algorithm.buypoint;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.STConstants;
import com.sn.strategy.TradeStrategyImp;
import com.sn.strategy.algorithm.IBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.DefaultBuyPointSelector;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.sellmode.SellModeWatchDog;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trader.StockTrader;
import com.sn.util.StockDataProcess;
import com.sn.util.VOLPRICEHISTRO;

public class AvgPriceBrkBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(AvgPriceBrkBuyPointSelector.class);
	
    private boolean sim_mode;
    private String selector_name = "AvgPriceBrkBuyPointSelector";
    private String selector_comment = "";
    private String urllnk = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=sz002095&scale=60&ma=no&datalen=1023";
    
    double avgpri5 = 0.0;
    double avgpri10 = 0.0;
    double avgpri30 = 0.0;
    double lst_yt_cls_pri = 0.0;
    
    public AvgPriceBrkBuyPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {
        
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        
        StockBuySellEntry sbs = lstTrades.get(stk.getID());
        Timestamp t1 = stk.getDl_dt();
        
        long hour = t1.getHours();
        long minutes = t1.getMinutes();
        
        if (hour != 9 || minutes != 30) {
        	log.info("only deal at 9:30");
        	return false;
        }
        
//        int hour_for_balance = ParamManager.getIntParam("HOUR_TO_KEEP_BALANCE", "TRADING", stk.getID());
//        int mins_for_balance = ParamManager.getIntParam("MINUTE_TO_KEEP_BALANCE", "TRADING", stk.getID());
//        
//        if ((hour * 100 + minutes) >= (hour_for_balance * 100 + mins_for_balance))
//        {
//            if (sbs == null || (sbs != null && sbs.is_buy_point))
//            {
//                log.info("Hour:" + hour + ", Minute:" + minutes);
//                log.info("Close to market shutdown time, no need to break balance");
//                return false;
//            }
//        }
        
        boolean csd = SellModeWatchDog.isStockInStopTradeMode(stk);
        
        if (csd == true && (sbs == null || sbs.is_buy_point))
        {
            log.info("Stock:" + stk.getID() + " is in sell mode and in balance/bought, no need to break balance.");
            return false;
        }
        
        boolean con1 = getAvgPriceFromSina(stk, ac, 0) && (lst_yt_cls_pri > avgpri5);
        boolean con2 = getAvgPriceFromSina(stk, ac, 1) && (lst_yt_cls_pri < avgpri5);
        boolean con3 = getAvgPriceFromSina(stk, ac, 2) && (lst_yt_cls_pri < avgpri5);
        boolean con4 = getAvgPriceFromSina(stk, ac, 3) && (lst_yt_cls_pri < avgpri5);
        
        if (con1 && con2 && con3 && con4)
        {
		    stk.setTradedBySelector(this.selector_name);
		    stk.setTradedBySelectorComment("past 3 days lower than 5 days avipri, now bigger than 5 days avgpri, buy!");
		    return true;
        }

		return false;
	}
	
    private boolean getAvgPriceFromSina(Stock2 s, ICashAccount ac, int shftDays) {
    	
    	urllnk = urllnk.replaceFirst("symbol=.*?&", "symbol=" + s.getArea() + s.getID() + "&");
    	
    	boolean gotDataSuccess = false;
    	
    	try {
    		
    		if (!getAvgPriceFromDb(s, shftDays))
    		{
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
                
                avgpri5 = 0.0;
                avgpri10 = 0.0;
                avgpri30 = 0.0;
                lst_yt_cls_pri = 0.0;
                int k = 0;
                String lst_dte = "";
                int step = 4;
                int sdays = shftDays;
                for (int i = (sda.length() - 1); i >= 0; i -= step)
                {
                	k++;
                	
                	if (k == 1) {
                		lst_dte = sda.getJSONObject(i).getString("day").substring(0, 10);
                		lst_yt_cls_pri = sda.getJSONObject(i).getDouble("close");
                		if (s.getDl_dt().toString().substring(0, 10).compareTo(lst_dte) <= 0) {
                			step = 1;
                			k = 0;
                			continue;
                		}
                		else {
                			if (sdays > 0) {
                			    i = i - (sdays - 1) * 4;
                			    step = 4;
                			    k = 0;
                			    sdays = 0;
                			    continue;
                			}
                			step = 4;
                		}
                	}
                	
                	if (k <= 5) {
                	    avgpri5 += sda.getJSONObject(i).getDouble("close");
                    	if (k == 5)
                    	{
                    		avgpri5 /= 5;
                    	}
                	}
                	
                	if (k <= 10) {
                	    avgpri10 += sda.getJSONObject(i).getDouble("close");
                    	if (k == 10)
                    	{
                    		avgpri10 /= 10;
                    	}
                	}
                	
                	if (k <= 30) {
                	    avgpri30 += sda.getJSONObject(i).getDouble("close");
                    	if (k == 30)
                    	{
                    		avgpri30 /= 30;
                    	}
                	}
                }
                log.info("lst_dte:" + lst_dte + ", s.getDl_dt().toString().substring(0, 10):" + s.getDl_dt().toString().substring(0, 10));
                
                if (avgpri5 > 0 && avgpri10 > 0 && avgpri30 > 0)
                {
                	gotDataSuccess = true;
        	    	saveAvgPriceToDb(s, lst_dte);
                	Thread.sleep(3000);
                }
    		}
    		else {
    			gotDataSuccess = true;
    		}
            log.info("stock:" + s.getID() + " got avgpri5:" + avgpri5 + ", avgpri10:" + avgpri10 + ", avgpri30:" + avgpri30 + ", lst_yt_cls_pri:" + lst_yt_cls_pri + " with shftDays:" + shftDays);
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    	}
        return gotDataSuccess;
    }
    
    private boolean saveAvgPriceToDb(Stock2 s, String for_dte) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean saveDataSuccess = false;
    	try {
    		String sql = "insert into stkAvgPri values ('" + s.getID() + "', '" + for_dte + "', round(" + lst_yt_cls_pri + ", 2), round(" + avgpri5 + ", 2), round(" + avgpri10 + ", 2), round(" + avgpri30 + ", 2), sysdate())";
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
    
    private boolean getAvgPriceFromDb(Stock2 s, int shftDays) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean gotDataSuccess = false;
    	try {
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = null;
    		
    	    String sql = "select * from stkAvgPri where id = '" + s.getID() + "' and avgpri1 is not null and avgpri2 is not null and avgpri3 is not null and add_dt < '" + s.getDl_dt().toString().substring(0, 10) + "' order by add_dt desc";
    		log.info(sql);
    		
    		stm = con.createStatement();
    		rs = stm.executeQuery(sql);
    		
    		int cnt = shftDays;
    		
    		while(cnt > 0) {
    			rs.next();
    			cnt--;
    		}
    		
    		if (rs.next()) {
                avgpri5 = rs.getDouble("avgpri1");
                avgpri10 = rs.getDouble("avgpri2");
                avgpri30 = rs.getDouble("avgpri3");
                lst_yt_cls_pri = rs.getDouble("yt_cls_pri");
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
    
	@Override
	public int getBuyQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        double useableMny = 0;
        int buyMnt = 0;
        int maxMnt = 0;
        
        Map<String, StockBuySellEntry> lstTrades = TradeStrategyImp.getLstTradeForStocks();
        StockBuySellEntry sbs = lstTrades.get(s.getID());
	    if (sbs != null && !sbs.is_buy_point)
	    {
	    	buyMnt = sbs.quantity;
	    	log.info("stock:" + s.getID() + " with qty:" + sbs.quantity + " already, buy same back");
	    }
	    else if (ac != null) {
	    	int cnt = TradeStrategyImp.getBuySellCount(s.getID(), s.getDl_dt().toString().substring(0, 10), true);
	    	
	    	cnt++;
	    	
            useableMny = ac.getMaxMnyForTrade();
            //maxMnt = (int)(useableMny * cnt/s.getCur_pri()) / 100 * 100;
            maxMnt = (int)(useableMny /s.getCur_pri()) / 100 * 100;
            
           	buyMnt = maxMnt;
            log.info("getBuyQty, useableMny:" + useableMny + " buyMnt:" + buyMnt + " maxMnt:" + maxMnt);
        }
		return buyMnt;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
    

}
