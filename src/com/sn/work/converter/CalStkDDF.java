package com.sn.work.converter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;


import com.sn.db.DBManager;
import com.sn.work.itf.IWork;

public class CalStkDDF implements IWork {

    static Connection con = DBManager.getConnection();
    /* Initial delay before executing work.
     */
    long initDelay = 0;

    /* Seconds delay befor executing next work.
     */
    long delayBeforNxtStart = 60000;
    
    TimeUnit tu = TimeUnit.MILLISECONDS;
    
    static Logger log = Logger.getLogger(CalStkDDF.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
    	CalStkDDF fsd = new CalStkDDF(1, 3);
        fsd.run();
    }
    
    public CalStkDDF(long id, long dbn)
    {
        initDelay = id;
        delayBeforNxtStart = dbn;        
    }
    
    /*
     * var hq_str_sh601318=
     * "中国平安,86.30,86.31,84.41,86.30,83.70,84.38,84.40,156070902,13235768984,2200,84.38,20300,84.37,12800,84.36,24100,84.35,3000,84.33,40750,84.40,54800,84.42,400,84.44,3300,84.45,2500,84.46,2015-05-15,15:04:06,00"
     * ;
     */

    private String lstStkDat = "";
    
    public void run()
    {
        // TODO Auto-generated method stub
        String str;
        
        try {
        	Statement stm = con.createStatement();
        	ResultSet rs;
        	String sql = "select ft_id, id, " +
        			     "cur_pri," +
                         " dl_stk_num," +
                         " dl_mny_num," +
                         " b1_num," +
                         " b1_pri," +
                         " b2_num," +
                         " b2_pri," +
                         " b3_num," +
                         " b3_pri," +
                         " b4_num," +
                         " b4_pri," +
                         " b5_num," +
                         " b5_pri," +
                         " s1_num," +
                         " s1_pri," +
                         " s2_num," +
                         " s2_pri," +
                         " s3_num," +
                         " s3_pri," +
                         " s4_num," +
                         " s4_pri," +
                         " s5_num," +
                         " s5_pri" +
                         " from stkdat2 t1 " +
                         " left join stkddf t2" +
                         "   on t1.id = t2.id " +
                         "  and to_char(t1.dl_dt, 'yyyy-mm-dd') = to_char(t2.dl_dt, 'yyyy-mm-dd') " +
                         "  and to_char(t1.dl_dt, 'HH24:MI:SS') > to_char(t2.dl_dt, 'HH24:MI:SS') ";
        	
        	rs = stm.executeQuery(sql);
        	
        	long ft_id;
        	String id;
        	int gap;
        	double cur_pri;
        	long dl_stk_num;
        	double dl_mny_num;
        	long b1_num;
        	double b1_pri;
        	long b2_num;
        	double b2_pri;
        	long b3_num;
        	double b3_pri;
        	long b4_num;
        	double b4_pri;
        	long b5_num;
        	double b5_pri;
        	long s1_num;
        	double s1_pri;
        	long s2_num;
        	double s2_pri;
        	long s3_num;
        	double s3_pri;
        	long s4_num;
        	double s4_pri;
        	long s5_num;
        	double s5_pri;

        	gap = 5;
        	int i = 0;
        	while (rs.next())
        	{
        		ft_id = rs.getLong("ft_id");
        		id = rs.getString("id");
        		cur_pri = rs.getDouble("cur_pri");
        		dl_stk_num = rs.getLong("dl_stk_num");
        		dl_mny_num = rs.getDouble("dl_mny_num");
        		b1_num = rs.getLong("b1_num");
        		b1_pri = rs.getDouble("b1_pri");
        		b2_num = rs.getLong("b2_num");
        		b2_pri = rs.getDouble("b2_pri");
        		b3_num = rs.getLong("b3_num");
        		b3_pri = rs.getDouble("b3_pri");
        		b4_num = rs.getLong("b4_num");
        		b4_pri = rs.getDouble("b4_pri");
        		b5_num = rs.getLong("b5_num");
        		b5_pri = rs.getDouble("b5_pri");
        		s1_num = rs.getLong("s1_num");
        		s1_pri = rs.getDouble("s1_pri");
        		s2_num = rs.getLong("s2_num");
        		s2_pri = rs.getDouble("s2_pri");
        		s3_num = rs.getLong("s3_num");
        		s3_pri = rs.getDouble("s3_pri");
        		s4_num = rs.getLong("s4_num");
        		s4_pri = rs.getDouble("s4_pri");
        		s5_num = rs.getLong("s5_num");
        		s5_pri = rs.getDouble("s5_pri");
        		
        	    while (i < gap)
        	    {
        	    	if (!rs.next())
        	    		break;
        	    	else {
        	    		if (!id.equals(rs.getString("id")))
        	    			break;
        	    	}
        	    	i++;
        	    }
        	    if (i == gap)
        	    {
        	    	cur_pri = rs.getDouble("cur_pri") - cur_pri;
        	    	dl_stk_num = rs.getLong("dl_stk_num") - dl_stk_num;
        	    	dl_mny_num = rs.getDouble("dl_mny_num") - dl_mny_num;
        	    	b1_num = rs.getLong("b1_num") - b1_num;
        	    	b1_pri = rs.getDouble("b1_pri") - b1_pri;
        	    	b2_num = rs.getLong("b2_num") - b2_num;
        	    	b2_pri = rs.getDouble("b2_pri") - b2_pri;
        	    	b3_num = rs.getLong("b3_num") - b3_num;
        	    	b3_pri = rs.getDouble("b3_pri") - b3_pri;
        	    	b4_num = rs.getLong("b4_num") - b4_num;
        	    	b4_pri = rs.getDouble("b4_pri") - b4_pri;
        	    	b5_num = rs.getLong("b5_num") - b5_num;
        	    	b5_pri = rs.getDouble("b5_pri") - b5_pri;
        	    	s1_num = rs.getLong("s1_num") - s1_num;
        	    	s1_pri = rs.getDouble("s1_pri") - s1_pri;
        	    	s2_num = rs.getLong("s2_num") - s2_num;
        	    	s2_pri = rs.getDouble("s2_pri") - s2_pri;
        	    	s3_num = rs.getLong("s3_num") - s3_num;
        	    	s3_pri = rs.getDouble("s3_pri") - s3_pri;
        	    	s4_num = rs.getLong("s4_num") - s4_num;
        	    	s4_pri = rs.getDouble("s4_pri") - s4_pri;
        	    	s5_num = rs.getLong("s5_num") - s5_num;
        	    	s5_pri = rs.getDouble("s5_pri") - s5_pri;
        	    	
        	    	sql = "insert into stkddf values (" +
        	    	        ft_id + "," +
        	    			id + "," +
        	    	        gap + "," +
        	    			cur_pri + "," +
        	    			dl_stk_num + "," +
        	    			dl_mny_num + "," +
        	    			b1_num + "," +
        	    			b1_pri + "," +
        	    			b2_num + "," +
        	    			b2_pri + "," +
        	    			b3_num + "," +
        	    			b3_pri + "," +
        	    			b4_num + "," +
        	    			b4_pri + "," +
        	    			b5_num + "," +
        	    			b5_pri + "," +
        	    			s1_num + "," +
        	    			s1_pri + "," +
        	    			s2_num + "," +
        	    			s2_pri + "," +
        	    			s3_num + "," +
        	    			s3_pri + "," +
        	    			s4_num + "," +
        	    			s4_pri + "," +
        	    			s5_num + "," +
        	    			s5_pri + ")";
        	    	
        	    	stm.executeUpdate(sql);
        	    }
        	}
        } catch (Exception e) {
        	
        }
    }
    
    static void ExactDatForstkDat2()
    {
        Statement stm = null;
        String sql = "insert into stkDat2 " +
                     "select  ft_id," +
                             "id," +
                             "td_opn_pri," +
                             "yt_cls_pri," +
                             "cur_pri," +
                             "td_hst_pri," +
                             "td_lst_pri," +
                             "b1_bst_pri," +
                             "s1_bst_pri," +
                             "dl_stk_num," +
                             "dl_mny_num," +
                             "b1_num," +
                             "b1_pri," +
                             "b2_num," +
                             "b2_pri," +
                             "b3_num," +
                             "b3_pri," +
                             "b4_num," +
                             "b4_pri," +
                             "b5_num," +
                             "b5_pri," +
                             "s1_num," +
                             "s1_pri," +
                             "s2_num," +
                             "s2_pri," +
                             "s3_num," +
                             "s3_pri," +
                             "s4_num," +
                             "s4_pri," +
                             "s5_num," +
                             "s5_pri," +
                             "dl_dt" +
                       " from stkdat s1 " +
                       "where not exists (select 'x' from stkDat2 s2 where s2.ft_id = s1.ft_id) " +
                       "  and not exists (select 'x' from stkDat s3 where s3.id = s1.id and s3.dl_dt = s1.dl_dt and s3.ft_id < s1.ft_id) " +
                       "  and s1.cur_pri > 0";
        log.info(sql);
        try{
            int cnt = 0;
            stm = con.createStatement();
            cnt = stm.executeUpdate(sql);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally {
            try {
                stm.close();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }            
        }
    }
    
    
    public String getWorkResult()
    {
        return "";
    }
    
    public String getWorkName()
    {
        return "FetchStockData";
    }
    
    public long getInitDelay()
    {
        return initDelay;
    }
    
    public long getDelayBeforeNxt()
    {
        return delayBeforNxtStart;
    }

    public TimeUnit getTimeUnit()
    {
        return tu;
    }
    
    public boolean isCycleWork()
    {
        return true;
    }

}
