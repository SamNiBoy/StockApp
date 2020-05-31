package com.sn.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.Stock2;

    public class StockDataProcess {
	
	    static Logger log = Logger.getLogger(StockDataProcess.class);
	    /**
	     * @param args
	     */
	    public static void main(String[] args) {
	        // TODO Auto-generated method stub
	    	VOLPRICEHISTRO v = getPriceVolHistro(null, "002291", 1, 5, 0);
	    	v.printVals();
	    	v = getPriceVolHistro(null, "002291", 1, 5, 1);
	    	v.printVals();
	    }
	    
	    // id_of_histro: 0, the biggest vol histro, etc.
	    public static VOLPRICEHISTRO getPriceVolHistro(Stock2 s, String stk, int days, int parts, int id_of_histro)
	    {
	        
	    	String id = (s == null) ? stk : s.getID();
	    	String dte_end = (s == null) ? " left(sysdate(), 10) " : ("'" + s.getDl_dt().toString().substring(0, 10) + "'");
	    	String dte_begin = "";
	    	ArrayList<VOLPRICEHISTRO> lv = null;
	    	String PK = "";
	    	
	    	if (s != null) {
	    	    PK = id + "@" + s.getDl_dt().toString().substring(0, 10);
	    	    lv = VOLPRICEHISTRO.vph.get(PK);
	    	    if (lv != null) {
	    	    	return lv.get(id_of_histro);
	    	    }
	    	}
	    	
   	    	lv = new ArrayList<VOLPRICEHISTRO>();
   	    	Connection con = DBManager.getConnection();
	        try {
	        	
	        	Statement stm = con.createStatement();
	            String sql = "select distinct left(dl_dt, 10) dte "
	        		+ "     from stkdat2 s "
	        		+ "    where s.id = '" + id +"' "
	        		+ "      and left(s.dl_dt, 10) < " + dte_end
	        		+ "     order by dte desc";
	            
	            log.info(sql);
	    	    ResultSet rs = stm.executeQuery(sql);
	    	    
	    	    int cnt = days;
	    	    while (cnt > 0) {
	    	    	rs.next();
	    	    	dte_begin = rs.getString("dte");
	    	    	cnt--;
	    	    }
	    	    
	    	    rs.close();
	    	    stm.close();
	        
	    	    stm = con.createStatement();
	            sql = "select min(cur_pri) min_cur_pri, max(cur_pri) max_cur_pri "
	        		+ "     from stkdat2 s "
	        		+ "    where s.id = '" + id +"' "
	        		+ "      and left(s.dl_dt, 10) >= '" + dte_begin
	        		+ "'      and left(s.dl_dt, 10) < " + dte_end;
	        
	            log.info(sql);
	            rs = stm.executeQuery(sql);
	        	rs.next();
	        	
	        	double max_pri = rs.getDouble("max_cur_pri");
	        	double min_pri = rs.getDouble("min_cur_pri");
	        	
	        	rs.close();
	        	stm.close();
	            
	            sql = "select left(dl_dt, 10) dte, ft_id, cur_pri, dl_stk_num "
	            		+ "     from stkdat2 s "
	            		+ "    where s.id = '" + id +"' "
	            		+ "      and left(s.dl_dt, 10) >= '" + dte_begin
	            		+ "'      and left(s.dl_dt, 10) < " + dte_end
	            		+ "    order by ft_id ";
	            
	
	        	String pre_dte = "", cur_dte = "";
	        	int pre_dl_stk_num = 0, cur_dl_stk_num = 0;
	        	double cur_pri = 0.0;
	        	int vol [] = new int[parts];
	        	
	        	log.info(sql);
	        	stm = con.createStatement();
	        	rs = stm.executeQuery(sql);
	        	
	        	int totalVol = 0;
	        	
	        	while(rs.next()) {
	        		
	        		cur_dte = rs.getString("dte");
	        		cur_dl_stk_num = rs.getInt("dl_stk_num");
	        		cur_pri = rs.getDouble("cur_pri");
	        		//log.info("Process record, cur_dte:" + cur_dte + ", cur_dl_stk_num:" + cur_dl_stk_num + ", cur_pri:" + cur_pri + " with min_pri:" + min_pri + ", max_pri:" + max_pri);
	        		if (pre_dte.length() <= 0 || !pre_dte.equals(cur_dte)) {
	        			pre_dte = cur_dte;
	        			pre_dl_stk_num = cur_dl_stk_num;
	        			int idx = (int)Math.floor((cur_pri - min_pri) * 0.999 / (max_pri - min_pri) * parts);
	        			
	        			log.info("Adding vol:" + cur_dl_stk_num + " into buket:" + idx);
	        			vol[idx] += cur_dl_stk_num;
	        			totalVol += cur_dl_stk_num;
	        			
	        			continue;
	        		}
	        		else {
	        			int detVol = cur_dl_stk_num - pre_dl_stk_num;
	        			int idx = (int)Math.floor((cur_pri - min_pri) * 0.999/ (max_pri - min_pri) * parts);
	        			
	        			log.info("Adding det vol:" + detVol + " into buket:" + idx);
	        			vol[idx] += detVol;
	        			totalVol += detVol;
	        			pre_dte = cur_dte;
	        			pre_dl_stk_num = cur_dl_stk_num;
	        		}
	        	}
	        	rs.close();
	        	stm.close();
	        	
	        	for(int i = 0; i<parts; i++) {
	        		log.info("vol[" + i + "] = " + vol[i] + ", within price range:[" + ((max_pri - min_pri) * i / parts + min_pri) + ", " + ((max_pri - min_pri) * (i + 1) / parts + min_pri) + "], pct:" + vol[i] * 1.0 / totalVol);
	        		VOLPRICEHISTRO v = new VOLPRICEHISTRO(((max_pri - min_pri) * i / parts + min_pri), ((max_pri - min_pri) * (i + 1) / parts + min_pri), vol[i], vol[i] * 1.0 / totalVol, parts , id, days);
	        		lv.add(v);
	        	}
	        	Comparator<VOLPRICEHISTRO> cmp =new VOLPRICEHISTROComparator();
	        	Collections.sort(lv, cmp);
	        	
	        	if (PK.length() > 0) {
	        	    VOLPRICEHISTRO.vph.put(PK, lv);
	        	}
	        	
//	        	for(VOLPRICEHISTRO v : lv) {
//	        		v.printVals();
//	        	}
	        }
	        catch(Exception e) {
	        	log.error(e.getMessage(), e);
	        }
	        finally {
	        	try {
					con.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error(e.getMessage(), e);
				}
	        }
		return lv.get(id_of_histro);
	}
}
