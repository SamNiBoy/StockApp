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
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import com.sn.db.DBManager;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.IWork;
import com.sn.task.JobScheduler;
import com.sn.task.WorkManager;
import com.sn.task.ga.StockParamSearch;

import oracle.net.aso.s;

@DisallowConcurrentExecution
public class CalStkTopnVOL implements Job {

    static Logger log = Logger.getLogger(CalStkTopnVOL.class);

    /**
     * @param args
     * @throws JobExecutionException 
     */
    public static void main(String[] args) throws JobExecutionException {
        // TODO Auto-generated method stub
    	CalStkTopnVOL fsd = new CalStkTopnVOL();
        fsd.execute(null);
    }

    public CalStkTopnVOL() {
    }
    
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        // TODO Auto-generated method stub
        log.info("Running CalStkTopnVOL task begin");
        Connection con = DBManager.getConnection();
        
        int sim_days = ParamManager.getIntParam("SIM_DAYS", "SIMULATION", null);
        int sim_gz_stock = ParamManager.getIntParam("SIM_ON_GZ_STOCK_ONLY", "SIMULATION", null);
        int topN = 5;
        
        try {
        	Statement stm = null;
        	stm = con.createStatement();
        	String sql = "select distinct left(dl_dt, 10) lst_dte from stkdat2 where id = '000001' order by lst_dte desc";
        	
        	ResultSet rs = stm.executeQuery(sql);
        	
        	int day_cnt = 0;
        	boolean has_day_ran = false;
        	while(day_cnt < sim_days && rs.next()) {
        		day_cnt++;
        		String on_dte = rs.getString("lst_dte");
        		
        		sql = "select id from stk";
        		
        		if (sim_gz_stock == 1) {
        			sql = "select id from usrstk " + (day_cnt == 1 ? " where not exists (select 'x' from StockTopnVOL where usrstk.id = StockTopnVOL.id and StockTopnVOL.before_dt = '" + on_dte + "')" : "");
        		}
        		
        		if (day_cnt > 1 && !has_day_ran) {
        			log.info("skip calculate stock topn vol as it available.");
        			rs.close();
        			stm.close();
        			return;
        		}
        		Statement stm2 = con.createStatement();
        		log.info(sql);
        		ResultSet rs2 = stm2.executeQuery(sql);
        		
        		while(rs2.next()) {
        			if (!has_day_ran) {
        				sql = "delete from StockTopnVOL";
        	        	log.info(sql);
        	        	Statement stmt = con.createStatement();
        	        	stmt.execute(sql);
        	        	stmt.close();
        			}
        			has_day_ran = true;
        			String id = rs2.getString("id");
        		    sql = "select * from (select s1.id, " 
        				+ "		s1.dl_stk_num dl_stk_num1, "
        				+ "		s2.dl_stk_num dl_stk_num2, "
        				+ "		s2.dl_stk_num -s1.dl_stk_num detaval, "
        				+ "     s2.ft_id s2_ft_id, "
        				+ "		s1.dl_dt dl_dt1, "
        				+ "		s2.dl_dt dl_dt2 "
        				+ "		from stkdat2 s1 "
        				+ "		join stkdat2 s2 "
        				+ "		on s1.id = s2.id "
        				+ "		where s2.ft_id = (select min(ft_id) from stkdat2 s3 where s3.id = '" + id + "' and s3.ft_id > s1.ft_id) " 
        				+ "		and s1.id = '" + id + "' "
        				+ "		and left(s1.dl_dt + interval " + topN + " day, 10) >= '" + on_dte + "' "
        				+ "     and left(s1.dl_dt, 10) = left(s2.dl_dt, 10) "
        				+ "		order by detaval desc "
        				+ "		limit " + 10 * topN + ") ts order by detaval";
        		
        		    log.info(sql);
        		    
        		    Statement stm3 = con.createStatement();
        		    ResultSet rs3 = stm3.executeQuery(sql);
        		    
        		    if (rs3.next()) {
        		    	String dbid = rs3.getString("id");
        		    	if (dbid != null && dbid.length() >0) {
        		    		int detminval = rs3.getInt("detaval");
        		    		int s2_ft_id = rs3.getInt("s2_ft_id");
        		    		sql = "insert into StockTopnVOL values ('" + id + "','" + on_dte + "'," + topN + "," + detminval + "," + s2_ft_id + ", sysdate())";
        		    		log.info(sql);
        		    		
        		    		Statement stm4 = con.createStatement();
        		    		stm4.execute(sql);
        		    		stm4.close();
        		    	}
        		    	else {
        		    		log.info("skip stock:" + id + " as no value from stkdat2.");
        		    	}
        		    }
        		    rs3.close();
        		    stm3.close();
        		}
        		rs2.close();
        		stm2.close();
        	}
        	rs.close();
        	stm.close();
        }
        catch(Exception e) {
				log.error(e.getMessage(), e);
        }
        finally {
        	try {
				con.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				log.error(e1.getMessage(), e1);
			}
        }
        log.info("Running CalStkTopnVOL task end");
    }
}
