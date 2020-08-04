package com.sn.task.calstkstats;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import com.sn.db.DBManager;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.IWork;
import com.sn.task.JobScheduler;
import com.sn.task.WorkManager;
import com.sn.task.ga.StockParamSearch;

import oracle.net.aso.s;

@DisallowConcurrentExecution
public class GetStockAvgPri implements Job {

    static Logger log = Logger.getLogger(GetStockAvgPri.class);

    private String urllnk = "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=sz002095&scale=60&ma=no&datalen=1023";
    double avgpri5 = 0.0;
    double avgpri10 = 0.0;
    double avgpri30 = 0.0;
    double yt_cls_pri = 0.0;
    /**
     * @param args
     * @throws JobExecutionException 
     */
    public static void main(String[] args) throws JobExecutionException {
        // TODO Auto-generated method stub
    	GetStockAvgPri fsd = new GetStockAvgPri();
        fsd.execute(null);
    }

    public GetStockAvgPri() {
    }
    
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        // TODO Auto-generated method stub
        log.info("Running GetStockAvgPri task begin");
    	String stockID = "";
    	String stockArea = "";
    	Connection con = null;
    	
    	try {
    		con = DBManager.getConnection();
    		Statement stm = con.createStatement();
    		String sql = "select id, area from stk where not exists (select 'x' from stkavgpri where stk.id = stkavgpri.id and stkavgpri.add_dt = (select left(max(dl_dt), 10) from stkdat2 where id = '000001')) order by id";
    		
    		log.info(sql);
    		
    		ResultSet rs = stm.executeQuery(sql);
    		
    		while (rs.next()) {
    			
    			stockID = rs.getString("id");
    			stockArea = rs.getString("area");
    		    urllnk = urllnk.replaceFirst("symbol=.*?&", "symbol=" + stockArea + stockID + "&");
                log.info("Fetching..." + urllnk);
                URL url = new URL(urllnk);
                InputStream is = null;
                try {
                    is = url.openStream();
                }
                catch(Exception ee) {
                	log.error(ee.getMessage(), ee);
                	continue;
                }
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String lines;
                StringBuffer sb = new StringBuffer("");
                while ((lines = br.readLine()) != null) {
                    lines = new String(lines.getBytes(), "utf-8");
                    sb.append(lines);
                }
                
                if (sb.toString().indexOf("[") >= 0) {
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
                		yt_cls_pri = avgpri5 += sda.getJSONObject(i).getDouble("close");
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
                
                if (avgpri5 > 0 && avgpri10 > 0 && avgpri30 > 0)
                {
        	    	saveAvgPriceToDb(stockID, lst_dte);
                }
        	    Thread.sleep(3000);
                }
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
        log.info("Running GetStockAvgPri task end");
    }
    
    private boolean saveAvgPriceToDb(String id, String for_dte) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean saveDataSuccess = false;
    	try {
    		String sql = "insert into stkAvgPri values ('" + id + "', '" + for_dte + "', round(" + yt_cls_pri + ", 2), round(" + avgpri5 + ", 2),  round(" + avgpri10 + ", 2), round(" + avgpri30 + ", 2), sysdate())";
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
}
