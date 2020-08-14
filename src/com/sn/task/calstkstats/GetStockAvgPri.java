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
import java.sql.Timestamp;
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
    double td_open_pri = 0.0;
    double td_cls_pri = 0.0;
    double td_high = 0.0;
    double td_low = 0.0;
    
    boolean run_for_first_day = true;
    
    /**
     * @param args
     * @throws JobExecutionException 
     */
    public static void main(String[] args) throws JobExecutionException {
        // TODO Auto-generated method stub
    	GetStockAvgPri fsd = new GetStockAvgPri();
        fsd.genShIndex();
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
    		String sql = "select id, area from stk where not exists (select 'x' from stkAvgPri a where a.id = stk.id and a.add_dt = sysdate()) order by id";
    		
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
                
                    for(int j = 0; j<75; j++) {
                        int k = 0;
                        String lst_dte = "";
                        
                        avgpri5 = 0.0;
                        avgpri10 = 0.0;
                        avgpri30 = 0.0;
                        td_open_pri = 0.0;
                        td_cls_pri = 0.0;
                        td_high = 0.0;
                        td_low = 0.0;
                        
                        for (int i = sda.length() - 1 - j * 4; i >= 0; i -= 4)
                        {
                        	if (i < 0)
                        		break;
                        	k++;
                        	
                        	if (k == 1) {
                        		lst_dte = sda.getJSONObject(i).getString("day").substring(0, 10);
                        		td_open_pri = sda.getJSONObject(i).getDouble("open");
                        		td_high = sda.getJSONObject(i).getDouble("high");
                        		td_low = sda.getJSONObject(i).getDouble("low");
                        		td_cls_pri = sda.getJSONObject(i).getDouble("close");
                        		
                        		while (!sda.getJSONObject(i).getString("day").substring(11, 16).equals("15:00")){
                        			log.info("time incorrect, shifting record:" + sda.getJSONObject(i).getString("day").substring(11, 16));
                        			i--;
                            		lst_dte = sda.getJSONObject(i).getString("day").substring(0, 10);
                            		td_high = sda.getJSONObject(i).getDouble("high");
                            		td_low = sda.getJSONObject(i).getDouble("low");
                            		td_cls_pri = sda.getJSONObject(i).getDouble("close");
                            		log.info("lst_dte:" + lst_dte + ", td_cls_pri:" + td_cls_pri);
                        		}
                        		
                        		i--;
                        		if (i<0)
                        			break;
                        		td_high = sda.getJSONObject(i).getDouble("high") > td_high ? sda.getJSONObject(i).getDouble("high") : td_high;
                        		td_low = sda.getJSONObject(i).getDouble("low") < td_low ? sda.getJSONObject(i).getDouble("low") : td_low;
                        		
                        		i--;
                        		if (i<0)
                        			break;
                        		td_high = sda.getJSONObject(i).getDouble("high") > td_high ? sda.getJSONObject(i).getDouble("high") : td_high;
                        		td_low = sda.getJSONObject(i).getDouble("low") < td_low ? sda.getJSONObject(i).getDouble("low") : td_low;
                        		
                        		i--;
                        		if (i<0)
                        			break;
                        		td_open_pri = sda.getJSONObject(i).getDouble("open");
                        		td_high = sda.getJSONObject(i).getDouble("high") > td_high ? sda.getJSONObject(i).getDouble("high") : td_high;
                        		td_low = sda.getJSONObject(i).getDouble("low") < td_low ? sda.getJSONObject(i).getDouble("low") : td_low;
                        		
                        		i+=3;
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
                        
                        if (k < 30) {
                        	break;
                        }
                        
                        if (avgpri5 > 0 && avgpri10 > 0 && avgpri30 > 0)
                        {
        	            	saveAvgPriceToDb(stockID, lst_dte);
                        }
                        
                        if (run_for_first_day)
                        	break;
                    }
                    genSimDataForStock(stockID);
                    Thread.sleep(2500);
                }
    		}
    		rs.close();
    		stm.close();
    		
    		genShIndex();
//    		sql = "insert into stkdat2_sim select * from stkdat2 where dl_dt like '% 09:30%' and not exists (select 'x' from stkdat2_sim where stkdat2_sim.ft_id = stkdat2.ft_id)";
//    		log.info("now copy 09:30 data to stkdat2_sim table");
//    		stm = con.createStatement();
//    		stm.execute(sql);
//    		stm.close();
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
    
    private boolean genSimDataForStock(String stkID) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean genDataSuccess = false;
    	try {
    		
    		String sql = "select * from stkAvgPri where id = '" + stkID + "' order by add_dt desc";
    		log.info(sql);
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = stm.executeQuery(sql);
    		
    		while (rs.next()) {
    			
    			String add_dt = rs.getString("add_dt");
    			double open = rs.getDouble("open");
    			double high = rs.getDouble("high");
    			double low = rs.getDouble("low");
    			double close = rs.getDouble("close");
    			
    			boolean skip_flg = false;
    			
    			Statement stm0 = con.createStatement();
    			ResultSet rs0 = null;
    			
    			sql = "select 'x' from stkdat2_sim s where s.id = '" + stkID + "' and left(dl_dt, 10) = '" + add_dt + "'";
    			
    			rs0 = stm0.executeQuery(sql);
    			
    			if (rs0.next()) {
    				skip_flg = true;
    			}
    			
    			rs0.close();
    			stm0.close();
    			
    			if (skip_flg) {
    				log.info("stock:" + stkID + " data for date:" + add_dt + " already available, skip creation.");
    			}
    			else
    			{
    		        sql = "insert into stkdat2_sim (ft_id,"
    		            + " id,"
    		            + " td_opn_pri,"
    		            + " yt_cls_pri,"
    		            + " cur_pri,"
    		            + " td_hst_pri,"
    		            + " td_lst_pri,"
    		            + " b1_bst_pri,"
    		            + " s1_bst_pri,"
    		            + " dl_stk_num,"
    		            + " dl_mny_num,"
    		            + " b1_num,"
    		            + " b1_pri,"
    		            + " b2_num,"
    		            + " b2_pri,"
    		            + " b3_num,"
    		            + " b3_pri,"
    		            + " b4_num,"
    		            + " b4_pri,"
    		            + " b5_num,"
    		            + " b5_pri,"
    		            + " s1_num,"
    		            + " s1_pri,"
    		            + " s2_num,"
    		            + " s2_pri,"
    		            + " s3_num,"
    		            + " s3_pri,"
    		            + " s4_num,"
    		            + " s4_pri,"
    		            + " s5_num,"
    		            + " s5_pri,"
    		            + " dl_dt)"
    		            + " select case when max(ft_id) is null then 0 else max(ft_id) end + 1," +
    		            "'" + stkID + "',"
    		                + open + ","
    		                + close + ","
    		                + open + ","
    		                + high + ","
    		                + low + ","
    		                + open + ","
    		                + open + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ","
    		                + 10000 + ", "
    		                + 10000 + ","
    		            + "str_to_date('" + add_dt +" 09:30:00" + "', '%Y-%m-%d %H:%i:%s') from stkdat2_sim";
    		    		log.info(sql);
    		    
    		        Statement stm1 = con.createStatement();
    		        stm1.execute(sql);
    		        stm1.close();
    		        genDataSuccess = true;
    			}
                if (run_for_first_day)
                	break;
    		}
    		
    		rs.close();
    		stm.close();
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    		genDataSuccess = false;
    	}
    	finally {
    		try {
				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error(e.getMessage(), e);
				genDataSuccess = false;
			}
    	}
    	return genDataSuccess;
    }
    
    public static boolean genShIndex()
    {
    	try {
            String str = "";
            String stkSql = "http://hq.sinajs.cn/list=s_sh000001";
            log.info("Fetching..." + stkSql);
            URL url = new URL(stkSql);
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            if ((str = br.readLine()) != null) {
                
                str = str.replaceAll("\"", "");
                str = str.replaceAll(";", "");
                String subs = str.substring(str.indexOf(",") + 1);
                String sary[] = subs.split(",");
                
                double ShIndex  = Double.valueOf(sary[0]);
                double DeltaShIndex  = Double.valueOf(sary[1]);
                double DeltaShIndexPct  = Double.valueOf(sary[2]);
                double ShDelAmt  = Integer.valueOf(sary[3]);
                double shDelMny  = Double.valueOf(sary[4]);
                
                
                Connection con = DBManager.getConnection();
                Statement stm = null;
                
                try {
                    stm = con.createStatement();
                    
                    String sql = "select 'x' from stockIndex where left(add_dt, 10) = left(sysdate(), 10)";
                    log.info(sql);
                    
                    ResultSet rs = stm.executeQuery(sql);
                    
                    if (!rs.next())
                    {
                    	rs.close();
                    	stm.close();
                    	stm = con.createStatement();
                        sql = "insert into stockIndex select 's_sh000001', case when max(id) is null then 0 else max(id) + 1 end, "
                        		+ ShIndex + "," + DeltaShIndex + "," +  DeltaShIndexPct + ", " + ShDelAmt + ", " + shDelMny + ", sysdate() from stockIndex where indexid = 's_sh000001'";
                        log.info(sql);
                        stm.execute(sql);
                    }
                    else {
                    	log.info("stockIndex for today is already got, skip...");
                    	rs.close();
                    }
                }
                catch(SQLException e)
                {
                    e.printStackTrace();
                    log.info(e.getMessage() + " errored:" + e.getErrorCode());
                }
                finally {
                    try {
                        stm.close();
                        con.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        log.info(e.getMessage() + " errored:" + e.getErrorCode());
                    }
                }
                
                log.info("Got SH index ShIndex:" + ShIndex + ", DeltaShIndex:" + DeltaShIndex + ", DeltaShIndexPct:" + DeltaShIndexPct + ", ShDelAmt:" + ShDelAmt + ", shDelMny:" + shDelMny);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    	
    	return true;
    }
    
    private boolean saveAvgPriceToDb(String id, String for_dte) {
    	
    	Connection con = DBManager.getConnection();
    	
    	boolean saveDataSuccess = false;
    	try {
    		
    		String sql = "select 'x' from stkAvgPri where id = '" + id + "' and add_dt = '" + for_dte + "'";
    		log.info(sql);
    		
    		Statement stm = con.createStatement();
    		ResultSet rs = stm.executeQuery(sql);
    		
    		if (!rs.next()) {
    			rs.close();
    			stm.close();
    			
    		    sql = "insert into stkAvgPri values ('" + id + "', '" + for_dte + "',  round(" + td_open_pri + ", 2), round(" + td_high + ", 2), round(" + td_low + ", 2), round(" + td_cls_pri + ", 2), round(" + avgpri5 + ", 2),  round(" + avgpri10 + ", 2), round(" + avgpri30 + ", 2), sysdate())";
    		    log.info(sql);
    		    
    		    stm = con.createStatement();
    		    stm.execute(sql);
    		    stm.close();
    		    
    		    saveDataSuccess = true;
    		}
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
