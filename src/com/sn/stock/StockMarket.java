package com.sn.stock;

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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.STConstants;
import com.sn.cashAcnt.CashAcnt;
import com.sn.stock.Stock2.StockData;
import com.sn.strategy.algorithm.param.ParamManager;

public class StockMarket{

    static Logger log = Logger.getLogger(StockMarket.class);
    
    static private ConcurrentHashMap<String, Stock2> stocks = new ConcurrentHashMap<String, Stock2>();
    static private ConcurrentHashMap<String, Stock2> gzstocks = new ConcurrentHashMap<String, Stock2>();
    static private ConcurrentHashMap<String, Stock2> recomstocks = null;
    static private ConcurrentHashMap<String, Double> degreesForSim = new ConcurrentHashMap<String, Double>();
    
    private static int StkNum = 0;
    private static int TotInc = 0;
    private static int TotDec = 0;
    private static int TotEql = 0;
    private static double AvgIncPct = 0.0;
    private static double AvgDecPct = 0.0;
    private static double totIncDlMny = 0.0;
    private static double totDecDlMny = 0.0;
    private static double totEqlDlMny = 0.0;
    private static double Degree = 0;
    
    private static double ShIndex = 0.0;
    private static double DeltaShIndex = 0;
    private static double DeltaShIndexPct = 0.0;
    private static long ShDelAmt = 0;
    private static double shDelMny = 0.0;
    private static String sh_lstStkDat = "";
    
    static private List<String> DeltaTSLst = new ArrayList<String>();
    static private List<Double> DeltaShIdxLst = new ArrayList<Double>();
    
    static private boolean isSimRunning = false;
    static private List<String> SimTSLst = new ArrayList<String>();
    static private List<Double> SimNetPFitLst = new ArrayList<Double>();
    static private List<Double> SimUsedMnyLst = new ArrayList<Double>();
    static private List<Double> SimCommMnyLst = new ArrayList<Double>();
    
    
    
    
    private static Timestamp pre_stats_ts = null;
    private static Timestamp cur_stats_ts = null;
    
    private static String fetch_stat = "";
    
    public static String getDeltaTSLst() {
    	String str = "[";
    	for(String k : DeltaTSLst) {
    		
    		if (str.length() > 1)
    		{
    			str += ",";
    		}
    		str += "'" + k + "'";
    	}
    	
    	str+="]";
    	return str;
    }
    
    public static String getDeltaShIdxLst() {
    	String str = "[";
    	for(Double k : DeltaShIdxLst) {
    		
    		if (str.length() > 1)
    		{
    			str += ",";
    		}
    		str += k;
    	}
    	
    	str+="]";
    	
    	return str;
    }
    
    public static String getSHIndexLngDsc()
    {    	
    	DecimalFormat df = new DecimalFormat("##.##");
    	String str = "";
    	str += "'IDX[" + df.format(ShIndex) + "] DT[" + df.format(DeltaShIndex) + "] Pct[" + df.format(DeltaShIndexPct) + "%]万手[" + df.format(ShDelAmt/10000) + "]额(亿)[" + df.format(shDelMny/10000) +"]'";
    	return str;
    }
    
    public static String getSHIndexShtDsc()
    {
    	DecimalFormat df = new DecimalFormat("##.##");
    	String str = "";
    	str += "涨跌:" + df.format(DeltaShIndex) + "\n百分比:" + df.format(DeltaShIndexPct);
    	return str;
    }
    
    public static double getSHIndexDeltaPct()
    {
    	DecimalFormat df = new DecimalFormat("##.##");
    	String str = "";
    	str = df.format(DeltaShIndexPct);
    	return Double.valueOf(str);
    }
    
    public static void clearSimData() {
    	log.info("Clear Sim Data now.");
    	SimTSLst.clear();
    	SimNetPFitLst.clear();
    	SimUsedMnyLst.clear();
    	SimCommMnyLst.clear();
    }
    
    public static void startSim() {
    	log.info("start Sim now.");
    	isSimRunning = true;
    }
    
    public static void stopSim() {
    	log.info("stop Sim now.");
    	isSimRunning = false;
    }
    
    
    public static String getSimTSLst() {
    	String str = "[";
    	for(String k : SimTSLst) {
    		
    		if (str.length() > 1)
    		{
    			str += ",";
    		}
    		str += "'" + k + "'";
    	}
    	
    	str+="]";
    	return str;
    }
    
    public static String getSimNetPFitLst() {
    	String str = "[";
    	for(Double k : SimNetPFitLst) {
    		
    		if (str.length() > 1)
    		{
    			str += ",";
    		}
    		str += k;
    	}
    	
    	str+="]";
    	
    	return str;
    }
    
    public static String getSimUsedMnyLst() {
    	String str = "[";
    	for(Double k : SimUsedMnyLst) {
    		
    		if (str.length() > 1)
    		{
    			str += ",";
    		}
    		str += k;
    	}
    	
    	str+="]";
    	
    	return str;
    }
    
    public static String getSimCommMnyLst() {
    	String str = "[";
    	for(Double k : SimCommMnyLst) {
    		
    		if (str.length() > 1)
    		{
    			str += ",";
    		}
    		str += k;
    	}
    	
    	str+="]";
    	
    	return str;
    }
    
    public static boolean calSimData()
    {
    	if (!isSimRunning) {
    		return false;
    	}
        Connection con = DBManager.getConnection();
        Statement stm = null;
        
        try {
            stm = con.createStatement();
            String sql = "select round(sum(ac.used_mny)/100000.0, 2) totUsedMny," + 
             		"                                   round(sum(h.commission_mny)/1000.0, 2) total_commission_mny," + 
             		"                                   round((sum(ac.pft_mny)  - sum(h.commission_mny))/1000.0, 2) netPft," + 
             		"                                   tmp.max_dl_dt" +
             		"        				     from cashacnt ac, " + 
             		"        				          (select max(td.dl_dt) max_dl_dt " + 
             		"        				             from tradedtl td) tmp, " + 
             		"                                  TradeHdr h" + 
             		"        			         where ac.acntid = h.acntid ";
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) {
	            Timestamp t0 = rs.getTimestamp("max_dl_dt");
	            if (t0 != null) {
	                SimTSLst.add(t0.toString());
	                SimNetPFitLst.add(rs.getDouble("netPft"));
	                SimUsedMnyLst.add(rs.getDouble("totUsedMny"));
	                SimCommMnyLst.add(rs.getDouble("total_commission_mny"));
	            }
            }
            rs.close();
        }
        catch(SQLException e)
        {
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
        
    	return true;
    }
    
    
    public static boolean refreshShIndex()
    {
    	try {
            String str = "";
            String stkSql = "http://hq.sinajs.cn/list=s_sh000001";
            log.info("Fetching..." + stkSql);
            URL url = new URL(stkSql);
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            while ((str = br.readLine()) != null) {
                if (str.equals(sh_lstStkDat))
                {
                    break;
                }
                sh_lstStkDat = str;
                
                str = str.replaceAll("\"", "");
                str = str.replaceAll(";", "");
                String subs = str.substring(str.indexOf(",") + 1);
                String sary[] = subs.split(",");
                
                ShIndex  = Double.valueOf(sary[0]);
                DeltaShIndex  = Double.valueOf(sary[1]);
                DeltaShIndexPct  = Double.valueOf(sary[2]);
                ShDelAmt  = Integer.valueOf(sary[3]);
                shDelMny  = Double.valueOf(sary[4]);
                
	            Timestamp t0 = Timestamp.valueOf(LocalDateTime.now());
                
                long hour = t0.getHours();
                long minutes = t0.getMinutes();
                
                String ts = (hour < 10 ? "0" + hour : hour) + ":" + (minutes < 10 ? "0" + minutes : minutes);
                
                DeltaTSLst.add(ts);
                DeltaShIdxLst.add(DeltaShIndex);
                
//                Connection con = DBManager.getConnection();
//                Statement stm = null;
//                
//                try {
//                    stm = con.createStatement();
//                    String sql = "insert into stockIndex select 's_sh000001', case when max(id) is null then 0 else max(id) + 1 end, "
//                    		+ ShIndex + "," + DeltaShIndex + "," +  DeltaShIndexPct + ", " + ShDelAmt + ", " + shDelMny + ", sysdate() from stockIndex where indexid = 's_sh000001'";
//                    log.info(sql);
//                    stm.execute(sql);
//                }
//                catch(SQLException e)
//                {
//                    e.printStackTrace();
//                    log.info(e.getMessage() + " errored:" + e.getErrorCode());
//                }
//                finally {
//                    try {
//                        stm.close();
//                        con.close();
//                    } catch (SQLException e) {
//                        // TODO Auto-generated catch block
//                        log.info(e.getMessage() + " errored:" + e.getErrorCode());
//                    }
//                }
                
                log.info("Got SH index ShIndex:" + ShIndex + ", DeltaShIndex:" + DeltaShIndex + ", DeltaShIndexPct:" + DeltaShIndexPct + ", ShDelAmt:" + ShDelAmt + ", shDelMny:" + shDelMny);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    	
    	return true;
    }
    
    public static String getFetch_stat() {
        return fetch_stat;
    }

    public static void setCur_stats_ts(Timestamp cur_stats_ts) {
        StockMarket.cur_stats_ts = cur_stats_ts;
    }

    private static ConcurrentHashMap<String, String> gzStockStats = new ConcurrentHashMap<String, String>();
    
    public static ConcurrentHashMap<String, String> getGzStockStats() {
        return gzStockStats;
    }

    public static String GZ_STOCK_SELECT = "select distinct s.id, s.name, s.area "
    		                             + "from stk s join usrStk u "
    		                             + "  on s.id = u.id "
    		                             + " left join cashacnt c "
    		                             + "  on s.id = right(c.acntId, 6) "
    		                             + " where u.gz_flg = 1 "
    		                             + "order by case when c.acntId is null then 2 when pft_mny >= 0 then 1 else 0 end, s.id";
    public static String GZ_STOCK_CNT_SELECT = "select count(distinct s.id) TotalCnt "
    		                             + "from stk s, usrStk u "
    		                             + "where s.id = u.id "
    		                             + "  and u.gz_flg = 1 ";
                                         //+ "  and u.suggested_by <> '" + ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null) + "'";
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        //log.info(getShortDesc());
        refreshShIndex();
    }
    
    static public boolean loadStocks() {

        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        
        stocks.clear();
        Stock2 s = null;
        int cnt = 0;
        int Total = 0;
        try {
            stm = con.createStatement();
            String sql = "select count(distinct id) totCnt from stk";
            log.info(sql);
            rs = stm.executeQuery(sql);
            rs.next();
            Total = rs.getInt("totCnt");

            log.info("Now load total:" + Total + " stocks.");
            rs = stm.executeQuery(sql);
            String id, name, area;
            
            int stock2_queue_sz = ParamManager.getIntParam("ALL_STOCK2_QUEUE_SIZE", "TRADING", null);

            rs.close();
            stm.close();
            stm = con.createStatement();
            sql = "select id, name, area from stk order by id";
            rs = stm.executeQuery(sql);
            while (rs.next()) {
                id = rs.getString("id");
                name = rs.getString("name");
                area = rs.getString("area");
                stock2_queue_sz = ParamManager.getIntParam("ALL_STOCK2_QUEUE_SIZE", "TRADING", id);
                s = new Stock2(id, name, area, stock2_queue_sz);
                stocks.put(id, s);
                cnt++;
                log.info("Load All Stocks completed:" + cnt * 1.0 / Total);
            }
            rs.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.info(e.getMessage() + " errored:" + e.getErrorCode());
            }
        }
        log.info("StockMarket loadStock " + cnt + " successed!");
        return true;
    }
    
    public static double calAvgPriSpeed() {
    	ConcurrentHashMap<String, Stock2> stocks = getStocks();
    	
    	int stock_cnt = stocks.size();
    	
    	double total_dlt_pri_pct = 0.0;
    	
    	for(Stock2 s : stocks.values()) {
    		if (s.getDltCurPri() != null && s.getYtClsPri() != null)
    		    total_dlt_pri_pct += s.getDltCurPri() / s.getYtClsPri();
    	}
    	log.info("Total stock:" + stock_cnt + ", with avg cur_pri delta pct:" + total_dlt_pri_pct / stock_cnt);
    	
    	return Math.round(total_dlt_pri_pct / stock_cnt * 1000000) / 1000.0;
    }
    
    static public void clearGzStocks() {
    	log.info("Now clear gzstocks...");
    	gzstocks.clear();
    }
    
    static public boolean loadGzStocks() {

        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        
        Stock2 s = null;
        int cnt = 0;
        int Total = 0;
        String sql = "";
        
        try {
        	
            sql = GZ_STOCK_CNT_SELECT;
            stm = con.createStatement();
            log.info(sql);
            rs = stm.executeQuery(sql);
            rs.next();
            Total = rs.getInt("TotalCnt");
            log.info("Now load Gzed total:" + Total + " stocks.");
            rs.close();
            stm.close();
            
            if (Total == gzstocks.size()) {
            	log.info("gzstocks size same as db, skip reload.");
            	return true;
            }
            
        	for(String sid : gzstocks.keySet()) {
        		sql = "select 'x' from usrstk where gz_flg = 1 and id = '" + sid + "'";
        		log.info(sql);
        		stm = con.createStatement();
        		rs = stm.executeQuery(sql);
        		if (!rs.next()) {
        			log.info("stock:" + sid + " no longer in gz list, remove it.");
        			gzstocks.remove(sid);
        		}
        		stm.close();
        		rs.close();
        	}
        	
            sql = GZ_STOCK_SELECT;
            
            String id, name, area;
            stm = con.createStatement();
            log.info(sql);
            rs = stm.executeQuery(sql);
            int stock2_queue_sz = ParamManager.getIntParam("GZ_STOCK2_QUEUE_SIZE", "TRADING", null);
            
            while (rs.next()) {
                id = rs.getString("id");
                
                if (gzstocks.get(id) != null) {
                	continue;
                }
                
                name = rs.getString("name");
                area = rs.getString("area");
                stock2_queue_sz = ParamManager.getIntParam("GZ_STOCK2_QUEUE_SIZE", "TRADING", id);
                s = new Stock2(id, name, area, stock2_queue_sz);
                
                log.info("adding stock:" + id + " name:" + name + " into gz list.");
                
                gzstocks.put(id, s);
                cnt++;
                log.info("Load GZStocks completed:" + cnt * 1.0 / Total);
            }
            rs.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            log.error(e.getMessage() + " with error0:" + e.getErrorCode());
        }
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage() + " with error:" + e.getErrorCode());
            }
        }
        log.info("StockMarket loadGzStock " + cnt + " successed!");
        return true;
    }
    
    static public boolean addGzStocks(Stock2 s) {
        gzstocks.put(s.getID(), s);
        log.info("StockMarket addGzStocks " + s.getID() + " successed!");
        return true;
    }
    
    static public boolean addGzStocks(String stkId) {

        Connection con = DBManager.getConnection();
        Statement stm = null;
        ResultSet rs = null;
        
        Stock2 s = null;
        try {
            stm = con.createStatement();
            String sql = "select s.id, s.name, s.area from stk s, usrStk u where s.id = u.id and u.gz_flg = 1 and s.id = '" + stkId + "'";
            rs = stm.executeQuery(sql);
            
            String id, name, area;
            
            int stock2_queue_sz = ParamManager.getIntParam("GZ_STOCK2_QUEUE_SIZE", "TRADING", null);
            
            if (rs.next()) {
                id = rs.getString("id");
                name = rs.getString("name");
                area = rs.getString("area");
                stock2_queue_sz = ParamManager.getIntParam("GZ_STOCK2_QUEUE_SIZE", "TRADING", id);
                s = new Stock2(id, name, area, stock2_queue_sz);
                gzstocks.put(id, s);
                log.info("addGzStocks completed for: " + stkId);
            }
            rs.close();
            stm.close();
            con.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        log.info("StockMarket addGzStocks " + stkId + " successed!");
        return true;
    }
    
    static public boolean removeGzStocks(String stkId) {

        boolean removed = false;
        if (!gzstocks.isEmpty()) {
             Stock2 rm = gzstocks.remove(stkId);
             if (rm != null) {
                 removed = true;
             }
        }
        if (removed) {
            log.info("removeGzStocks success for: " + stkId);
            return true;
        }
        else {
            log.info("removeGzStocks failed for: " + stkId);
            return false;
        }
    }
    
    public static ConcurrentHashMap<String, Stock2> getStocks() {
        synchronized (stocks) {
            if (stocks.isEmpty()) {
                loadStocks();
            }
            return stocks;
        }
    }

    public static void setStocks(ConcurrentHashMap<String, Stock2> stocks) {
        StockMarket.stocks = stocks;
    }

    public static ConcurrentHashMap<String, Stock2> getGzstocks() {
        synchronized (gzstocks) {
            loadGzStocks();
            return gzstocks;
        }
    }

    public static void setGzstocks(ConcurrentHashMap<String, Stock2> gzstocks) {
        StockMarket.gzstocks = gzstocks;
    }

    public static ConcurrentHashMap<String, Stock2> getRecomstocks() {
        return recomstocks;
    }

    public static void setRecomstocks(ConcurrentHashMap<String, Stock2> recomstocks) {
        StockMarket.recomstocks = recomstocks;
    }

    public static int getStkNum() {
        return StkNum;
    }

    public static void setStkNum(int stkNum) {
        StkNum = stkNum;
    }

    public static int getTotInc() {
        return TotInc;
    }

    public static void setTotInc(int totInc) {
        TotInc = totInc;
    }

    public static int getTotDec() {
        return TotDec;
    }

    public static void setTotDec(int totDec) {
        TotDec = totDec;
    }

    public static int getTotEql() {
        return TotEql;
    }

    public static void setTotEql(int totEql) {
        TotEql = totEql;
    }

    public static double getAvgIncPct() {
        return AvgIncPct;
    }

    public static void setAvgIncPct(double avgIncPct) {
        AvgIncPct = avgIncPct;
    }

    public static double getAvgDecPct() {
        return AvgDecPct;
    }

    public static void setAvgDecPct(double avgDecPct) {
        AvgDecPct = avgDecPct;
    }

    public static double getTotIncDlMny() {
        return totIncDlMny;
    }

    public static void setTotIncDlMny(double totIncDlMny) {
        StockMarket.totIncDlMny = totIncDlMny;
    }

    public static double getTotDecDlMny() {
        return totDecDlMny;
    }

    public static void setTotDecDlMny(double totDecDlMny) {
        StockMarket.totDecDlMny = totDecDlMny;
    }

    public static double getTotEqlDlMny() {
        return totEqlDlMny;
    }

    public static void setTotEqlDlMny(double totEqlDlMny) {
        StockMarket.totEqlDlMny = totEqlDlMny;
    }

    public static double getDegree(Timestamp ts) {
        
    	if (ts != null)
    	{
            String PK = ts.toString().substring(0, 16);
            
            Double deg = degreesForSim.get(PK);
            
            if (deg == null)
            {
                if (degreesForSim.size() > 0)
                {
                    log.info("Not able to get degree from map at:" + PK + ",but in sim mode use 0,  market not opened?");
                    Degree = 0.0;
                }
                else
                {
                    log.info("Not able to get degree from map at:" + PK + ", not in sim Mode, return whatever value as is:" + Degree);
                }
                return Degree;
            }
            else
            {
                log.info("Got degree from map for key:" + PK + ", return value:" + Degree);
            	Degree = deg;
            }
    	}
        return Math.round(Degree * 100) / 100.0;
    }
    
    public static void clearDegreeMap()
    {
        log.info("Clear degreesForSim...");
        degreesForSim.clear();
        Degree = 0.0;
    }
    
    public static void buildDegreeMapForSim(String start_dt, String end_dt)
    {
        
        synchronized (degreesForSim)
        {
            
            if (degreesForSim.size() > 0)
            {
                log.info("degreesForSim is loaded, skip reload again.");
                return;
            }
            
            Connection con = DBManager.getConnection();
            Statement stm = null;
            ResultSet rs = null;
            int sim_days = ParamManager.getIntParam("SIM_DAYS", "SIMULATION", null);
            
            try {
                
                //start_dt is not included.
                for (int i = 1; i<= sim_days; i++)
                {
                    String sql = "select left(str_to_date('" + start_dt + "', '%Y-%m-%d') + interval " + i + " day, 10) mydt from dual " +
                                 " where str_to_date('" + start_dt + "', '%Y-%m-%d') + interval " + i + " day <= str_to_date('" + end_dt + "', '%Y-%m-%d')";
                    
                    log.info(sql);
                    
                    stm = con.createStatement();
                    rs = stm.executeQuery(sql);
                    
                    if (!rs.next()) {
                       break; 
                    }
                    
                    String mydt = rs.getString("mydt");
                    
                    rs.close();
                    stm.close();
                    log.info("calculate index for date:" + mydt.toString());
                    
                    for (int h = 9; h<=15; h++)
                    {
                        for (int m = 0; m<60; m++)
                        {
                            if ((h == 9 && m <= 30) || (h == 11 && m >= 30) || (h >11 && h < 13) || (h == 15 && m > 0))
                                continue;
                            String PK = mydt + " " + (h < 10 ? ("0" + h) : h) + ":" + (m < 10 ? ("0" + m) : m);
                            //calIndex(PK + ":00");
                            sql = "select indexval, delindex, deltapct, delamt, delmny from stockindex where indexid = 's_sh000001' and add_dt <= str_to_date('" + PK  + ":59', '%Y-%m-%d %H:%i:%s') order by id desc";
                            stm = con.createStatement();
                            
                            log.info(sql);
                            rs = stm.executeQuery(sql);
                            
                            if (rs.next())
                            {
                                ShIndex = rs.getDouble("indexval");
                                DeltaShIndex = rs.getDouble("delindex");
                                DeltaShIndexPct = rs.getDouble("deltapct");
                                ShDelAmt =  rs.getLong("delamt");
                                shDelMny = rs.getDouble("delmny");
                                
                                log.info("calculate DeltaShIndexPct:" + DeltaShIndexPct + " at timestamp:" + PK + " put into degreeForSim.");
                                degreesForSim.put(PK, DeltaShIndexPct);
                            }
                            
                            rs.close();
                            stm.close();
                        }
                    }
                }
            }
            catch(SQLException e)
            {
                e.printStackTrace();
            }
            finally {
                try {
                    con.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    log.error(e.getMessage() + " with error code:" + e.getErrorCode());
                }
            }
        }
    }

    public static boolean isJumpWater(int tailSz, double jumpPct, double stockJumpCntPct) {
    	int total = stocks.size();
    	if (total == 0) {
    		log.info("stocks is emtpy, can not check jump water for market.");
    		return false;
    	}
    	int jumpCnt = 0;
    	for(String s : stocks.keySet()) {
    		Stock2 stk = stocks.get(s);
    		if (stk.isJumpWater(tailSz, jumpPct)) {
    			jumpCnt++;
    		}
    	}
    	log.info("total stocks:" + total + " and jump water stocks:" + jumpCnt);
    	double actPct = jumpCnt * 1.0 / total;
    	log.info("Passed param [tailSz, stockJumpCntPct, jumpPct]=[" + tailSz + "," + stockJumpCntPct + "," + jumpPct + "] actPct:" + actPct + " return " + (actPct >= stockJumpCntPct ? "true" : "false"));
    	if (actPct >= stockJumpCntPct) {
    		return true;
    	}
    	return false;
    }
    
    public static boolean isGzStocksJumpWater(int tailSz, double jumpPct, double stockJumpCntPct) {
    	if (gzstocks.isEmpty()) {
    		getGzstocks();
    	}
    	int total = gzstocks.size();
    	if (total == 0) {
    		log.info("stocks is emtpy, can not check jump water for gz stocks.");
    		return false;
    	}
    	int jumpCnt = 0;
    	for(String s : gzstocks.keySet()) {
    		Stock2 stk = gzstocks.get(s);
    		if (stk.isJumpWater(tailSz, jumpPct)) {
    			jumpCnt++;
    		}
    	}
    	log.info("total Gz stocks:" + total + " and jump water stocks:" + jumpCnt);
    	double actPct = jumpCnt * 1.0 / total;
    	log.info("Passed param [tailSz, stockJumpCntPct, jumpPct]=[" + tailSz + "," + stockJumpCntPct + "," + jumpPct + "] actPct:" + actPct + " return " + (actPct >= stockJumpCntPct ? "true" : "false"));
    	if (actPct >= stockJumpCntPct) {
    		return true;
    	}
    	return false;
    }
    
    static public boolean calIndex(Object tm) {

        Connection con = DBManager.getConnection();
        Statement stm = null;
        String deadline = null;
        if (tm == null) {
        	deadline = "sysdate()";
        }
        else {
            if (tm instanceof Timestamp)
            {
        	    deadline = "str_to_date('" + tm.toString() + "', '%Y-%m-%d %H:%i:%s')";
            }
            else 
            {
                deadline = "str_to_date('" + tm + "', '%Y-%m-%d %H:%i:%s')";
            }
        }

        int catagory = -2;
        try {
            stm = con.createStatement();
            String sql = "select sum(case when cur_pri > td_opn_pri then 1 else 0 end) IncNum, " +
                         "       sum(case when cur_pri < td_opn_pri then 1 else 0 end) DecNum, " +
                         "       sum(case when cur_pri = td_opn_pri then 1 else 0 end) EqlNum, " +
                         " avg((cur_pri - td_opn_pri)/td_opn_pri) avgPct," +
                         " sum(dl_mny_num) totDlMny," +
                         " case when cur_pri > td_opn_pri then 1 " +
                         "               when cur_pri < td_opn_pri then -1 " +
                         "               when cur_pri = td_opn_pri then 0 end catagory " +
                         " from stkdat2 " +
                         " join (select max(ft_id) max_ft_id, id from stkdat2 skd where skd.dl_dt <= " + deadline + " group by id) t" +
                         "   on stkdat2.id = t.id " +
                         "  and stkdat2.ft_id = t.max_ft_id " +
                         " where td_opn_pri > 0 " +
                         "   and left(dl_dt, 10) = " + "left(" + deadline + ", 10)" +
                         " group by case when cur_pri > td_opn_pri then 1 " +
                         "               when cur_pri < td_opn_pri then -1 " +
                         "               when cur_pri = td_opn_pri then 0 end" +
                         " order by catagory asc";
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);

            while (rs.next()) {
                catagory = rs.getInt("catagory");
                if (catagory == -1)
                {
                    TotDec = rs.getInt("DecNum");
                    AvgDecPct = rs.getDouble("avgPct");
                    totDecDlMny = rs.getDouble("totDlMny");
                }
                else if (catagory == 0)
                {
                    TotEql = rs.getInt("EqlNum");
                    totEqlDlMny = rs.getDouble("totDlMny");
                }
                else if (catagory == 1)
                {
                    TotInc = rs.getInt("IncNum");
                    AvgIncPct = rs.getDouble("avgPct");
                    totIncDlMny = rs.getDouble("totDlMny");
                }
            }
            log.info("TotDec:" + TotDec);
            log.info("AvgDecPct:" + AvgDecPct);
            log.info("totDecDlMny:" + totDecDlMny);
            log.info("TotEql:" + TotEql);
            log.info("totEqlDlMny:" + totEqlDlMny);
            log.info("TotInc:" + TotInc);
            StkNum = TotDec + TotInc + TotEql;
            Degree = (TotInc * AvgIncPct + TotDec * AvgDecPct) * 100.0 / (TotInc * 0.1 + TotDec * 0.1);
            
            rs.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        finally {
            try {
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage() + " with error code:" + e.getErrorCode());
            }
            
        }
        return true;
    }
    
    public static boolean calStats()
    {
        long ct = -1;
        long pt = -1;
        
        if (cur_stats_ts != null)
        {
            ct = cur_stats_ts.getTime();
            long seconds = -1;
        
            if (pre_stats_ts != null)
            {
                pt = pre_stats_ts.getTime();
                seconds = (ct - pt)/1000;
                log.info("passed seconds:" + seconds);
            }
            
            //For simulation mode, seconds may < 0.
            if (pt == -1 || Math.abs(seconds) >= 0)
            {
                calIndex(cur_stats_ts);
                log.info("Finished stats calculation for cur_stats_ts:" + cur_stats_ts.toString());
                pre_stats_ts = cur_stats_ts;
            }
        }
        
        refreshShIndex();
        
        log.info("Now start to refresh user's gz stock stats");
        calGzStockStats();
        log.info("Finished GzStockstats calculation for frmUsr.");
        
        log.info("Now start to calculate fetch information:");
        calFetchInfor();
        log.info("Calculate fetch information completed");
        return true;
    }
   static public boolean calGzStockStats()
   {
        Connection mycon = DBManager.getConnection();
        Statement stm = null;
        
        try {
            String sql = "select distinct u.openID "
                    + "     from stk s, usrStk u "
                    + "    where s.id = u.id "
                    + "      and u.gz_flg = 1 "
                    + "      and u.suggested_by in (u.openID,'" + ParamManager.getStr2Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null) + "')";
            log.info(sql);
            stm = mycon.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            while (rs.next())
            {
                String usr = rs.getString("openID");
                String msg = getGzStockStats(usr);
                log.info("Put user:" + " gzed stock stats:" + msg);
                log.info(msg);
                gzStockStats.put(usr, msg);
            }
            rs.close();
        }
        catch (Exception e)
        {
            log.error(e.getMessage(),e);
        }
        finally {
            try {
                stm.close();
                mycon.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                log.error(e.getMessage(), e);
            }
        }
        return true;
    }
   
    static private String getGzStockStats(String frmUsr)
    {

        String msg = "";
        Map<String, String> Stocks = new HashMap<String, String> ();
        Map<String, Integer> sellmodes = new HashMap<String, Integer> ();
        DecimalFormat df2 = new DecimalFormat("##.##");
        DecimalFormat df3 = new DecimalFormat("##.###");
        Connection mycon = DBManager.getConnection();
        Statement stm = null;

        try {
            String sql = "select s.id, s.name, u.stop_trade_mode_flg "
                    + "     from stk s, usrStk u "
                    + "    where s.id = u.id "
                    + "      and u.gz_flg = 1 "
                    + "      and u.openID ='" + frmUsr + "' "
                    + "      and u.suggested_by in ('" + frmUsr + "','" + ParamManager.getStr2Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING", null) + "')";
            log.info(sql);
            stm = mycon.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            
            while (rs.next()) {
                Stocks.put(rs.getString("id"), rs.getString("name"));
                sellmodes.put(rs.getString("id"), rs.getInt("stop_trade_mode_flg"));
            }
            rs.close();
            stm.close();
            
            if (Stocks.size() > 0)
            {
            for (String stock : Stocks.keySet()) {
                double dev = 0;
                double cur_pri = 0;
                
                int stkcnt = Stocks.keySet().size();

                
                String sellMode = (sellmodes.get(stock) == 1) ? "是" : "否";

                try {
                    sql = "select avg(dev) dev from ("
                           + "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, left(dl_dt, 10) atDay "
                           + "  from stkdat2 "
                           + " where id ='" + stock + "'"
                           + "   and yt_cls_pri > 0 "
                           + "   and left(dl_dt, 10) >= left(sysdate() - interval 7 day, 10)"
                           + " group by left(dl_dt, 10)) t";
                    log.info(sql);
                    stm = mycon.createStatement();
                    rs = stm.executeQuery(sql);
                    if (rs.next()) {
                        dev = rs.getDouble("dev");
                    }
                    Stock2 stk = (Stock2)StockMarket.getStocks().get(stock);
                    Double cur_pri1 = stk.getCur_pri();
                    if (cur_pri1 != null) {
                        cur_pri = cur_pri1;
                    }
                    else {
                        log.info("cur_pri for stock:" + stock + " is null, reloading...");
                        cur_pri = -1;
                        stk.getSd().LoadData();
                        cur_pri = stk.getCur_pri();
                        log.info("regot cur_pri:" + cur_pri);
                    }
                    
                    Integer dl_stk_num = stk.getDl_stk_num();
                    Double dl_mny_num = stk.getDl_mny_num();
                    
                    Double hst_pri = stk.getMaxTd_hst_pri();
                    Double lst_pri = stk.getMinTd_lst_pri();
                    Double b1_pri = stk.getB1_pri();
                    Double b2_pri = stk.getB2_pri();
                    Double b3_pri = stk.getB3_pri();
                    Double b4_pri = stk.getB4_pri();
                    Double b5_pri = stk.getB5_pri();
                    Integer b1_num = stk.getB1_num();
                    Integer b2_num = stk.getB2_num();
                    Integer b3_num = stk.getB3_num();
                    Integer b4_num = stk.getB4_num();
                    Integer b5_num = stk.getB5_num();
                    Double s1_pri = stk.getS1_pri();
                    Double s2_pri = stk.getS2_pri();
                    Double s3_pri = stk.getS3_pri();
                    Double s4_pri = stk.getS4_pri();
                    Double s5_pri = stk.getS5_pri();
                    Integer s1_num = stk.getS1_num();
                    Integer s2_num = stk.getS2_num();
                    Integer s3_num = stk.getS3_num();
                    Integer s4_num = stk.getS4_num();
                    Integer s5_num = stk.getS5_num();
                    Double opn_pri = stk.getOpen_pri();
                    Double yt_cls_pri = stk.getYtClsPri();
                    Double dlt_pri = cur_pri - yt_cls_pri;
                    Double pct = dlt_pri / yt_cls_pri * 100;
                    
                    //max support 4 stock with detail infor.
                    if (stkcnt < 5)
                    {
                        msg += "[" + stock + "]:" + Stocks.get(stock) + " 昨收" + df2.format(yt_cls_pri) + "\n";
                        msg += "现价:" + df2.format(cur_pri) + "    今开:" + df2.format(opn_pri) + "\n";
                        msg += "涨跌:" + df2.format(dlt_pri) + "    涨幅:" + df2.format(pct) + "%\n";
                        msg += "最高:" + df2.format(hst_pri) + "    最低:" + df2.format(lst_pri) + "\n";
                        msg += "成交:" + df2.format(dl_stk_num / 1000000.0) + "万手" + "  金额:" + df2.format(dl_mny_num / 10000000) + "千万\n";
                    
                        msg += "买五:" + df2.format(b5_pri) + "   手:" + b5_num / 100 + "\n";
                        msg += "买四:" + df2.format(b4_pri) + "   手:" + b4_num / 100 + "\n";
                        msg += "买三:" + df2.format(b3_pri) + "   手:" + b3_num / 100 + "\n";
                        msg += "买二:" + df2.format(b2_pri) + "   手:" + b2_num / 100 + "\n";
                        msg += "买一:" + df2.format(b1_pri) + "   手:" + b1_num / 100 + "\n";
                        msg += "--------------------\n";
                        msg += "卖一:" + df2.format(s1_pri) + "   手:" + s1_num / 100 + "\n";
                        msg += "卖二:" + df2.format(s2_pri) + "   手:" + s2_num / 100 + "\n";
                        msg += "卖三:" + df2.format(s3_pri) + "   手:" + s3_num / 100 + "\n";
                        msg += "卖四:" + df2.format(s4_pri) + "   手:" + s4_num / 100 + "\n";
                        msg += "卖五:" + df2.format(s5_pri) + "   手:" + s5_num / 100 + "\n";
                        msg += "统计:" + " 七天dev:" + df3.format(dev) + " StopTrade: " + sellMode + "\n\n";
                    }
                    else {
                        msg += "[" + stock + "]:" + Stocks.get(stock) + "  停交:" + sellMode;
                        msg += "\n涨幅:" + df2.format(pct) + "%" + "  Dev:" + df3.format(dev) + "\n";
                        msg += "现价:" + df2.format(cur_pri) + "  金额:" + df2.format(dl_mny_num / 10000000) + "千万\n";
                        msg += "\n";
                    }
                    rs.close();
                } catch(Exception e0) {
                    log.info("No price infor for stock:" + stock + " continue...");
                    continue;
                }
            }
            }
            else {
                msg = "你没有主动或者系统推荐可买卖关注的股票.";
            }
        } catch (SQLException e) {
            log.error("DB Exception:" + e.getMessage());
        }
        finally {
            try {
                stm.close();
                mycon.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                log.error("DB Exception:" + e.getMessage());
            }
        }
    
        return msg;
        
    }
    
    static public boolean calFetchInfor()
    {
        fetch_stat = "";
        Connection con = DBManager.getConnection();
        Statement stm = null;
        String sql = "select count(*) totCnt, count(*)/case when count(distinct id) = 0 then 1 else count(distinct id) end cntPerStk "
                + "  from stkDat2 where left(dl_dt, 10) = left(sysdate(), 10) ";
        try {
            stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()){
                fetch_stat += "总共收集:" + rs.getLong("totCnt") + "条记录.\n"
                      +"平均每股收集:" + rs.getLong("cntPerStk") + "次.\n";
            }
            rs.close();
            log.info("calculate fetch stat msg:" + fetch_stat  + " for opt 5");
        } catch (Exception e) {
            e.printStackTrace();
            fetch_stat = "无数据.\n";
        }
        finally {
            try {
            stm.close();
            con.close();
            }
            catch (Exception e)
            {
                log.error("DB exception:" + e.getMessage());
                fetch_stat = "数据库异常:" + e.getMessage();
            }
        }
        
        fetch_stat += getShortDesc();
        
        return true;
    }
    
    static public String getShortDesc() {
    	DecimalFormat df = new DecimalFormat("##.##");
        return "温度:" + df.format(Degree) + "[" + StkNum + "/" + df.format((totDecDlMny +totEqlDlMny + totIncDlMny)/100000000) + "亿 "
    			+ TotInc + "/" + df.format(AvgIncPct) + "/" + df.format(totIncDlMny/100000000) + "亿涨 "
                + TotDec + "/" + df.format(AvgDecPct) + "/" + df.format(totDecDlMny/100000000) + "亿跌 "
    			+ TotEql + "/" + df.format(totEqlDlMny/100000000) + "亿平]";
    }
    
    static public String getDegreeMny() {
    	DecimalFormat df = new DecimalFormat("##.##");
        return "温度:" + df.format(Degree) + "[" + StkNum + "/" + df.format((totDecDlMny +totEqlDlMny + totIncDlMny)/100000000) + "亿 ]";
    }
    
    static public String getLongDsc() {
        
    	DecimalFormat df = new DecimalFormat("##.##");
        String index = "<table border = 1>" +
        "<tr>" +
        "<th> Stock Count</th> " +
        "<th> Total+ </th> " +
        "<th> AvgPct+ </th> " +
        "<th> TotDlMny+ </th> " +
        "<th> Total-</th> " +
        "<th> AvgPct-</th> " + 
        "<th> TotDlMny- </th> " +
        "<th> Total= </th> " +
        "<th> TotDlMny= </th> " +
        "<th> Degree </th> </tr> ";
        index += "<tr> <td>" + StkNum + "</td>" +
        "<td> " + TotInc + "</td>" +
         "<td> " + df.format(AvgIncPct*100) + "%</td>" +
         "<td> " + df.format(totIncDlMny/100000000) + "亿</td>" +
         "<td> " + TotDec + "</td>" +
         "<td> " + df.format(AvgDecPct*100) + "%</td>" +
         "<td> " + df.format(totDecDlMny/100000000) + "亿</td>" +
         "<td> " + TotEql + "</td>" +
         "<td> " + df.format(totEqlDlMny/100000000) + "亿</td>" +
         "<td> " + df.format((TotInc * AvgIncPct + TotDec * AvgDecPct) * 100.0 / (TotInc * 0.1 + TotDec * 0.1)) + " C</tr></table>";
        return index;
    }
    static public boolean isMarketTooCold(Timestamp tm) {
        return Degree < -20;
    }
    
    static public boolean hasMoreIncStock() {
        return TotInc > TotDec;
    }
    
    static double getMnyRatioIncDec() {
        if (totDecDlMny == 0) {
            //not possible ratio.
            return 10000;
        }
        return totIncDlMny / totDecDlMny;
    }
}
