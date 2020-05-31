package com.sn.stock;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.suggest.selector.ClosePriceTrendStockSelector;

public class Stock2 implements Comparable<Stock2>{

    public class StockData{

        int MAX_SZ = 800;
        //Save all history data
        String stkid;
        List<Double> td_opn_pri_lst = null;
        List<Double> yt_cls_pri_lst = null;
        List<Double> td_hst_pri_lst = null;
        List<Double> td_lst_pri_lst = null;
        List<String> dt_lst = null;

        //Save today's data
        List<Integer> ft_id_lst =  null;
        List<Double> cur_pri_lst = null;
        List<Double> b1_bst_pri_lst = null;
        List<Double> s1_bst_pri_lst = null;
        List<Integer> dl_stk_num_lst =null;
        List<Double> dl_mny_num_lst = null;
        List<Integer> b1_num_lst = null;
        List<Double> b1_pri_lst = null;
        List<Integer> b2_num_lst = null;
        List<Double> b2_pri_lst = null;
        List<Integer> b3_num_lst = null;
        List<Double> b3_pri_lst = null;
        List<Integer> b4_num_lst = null;
        List<Double> b4_pri_lst = null;
        List<Integer> b5_num_lst = null;
        List<Double> b5_pri_lst = null;
        List<Integer> s1_num_lst = null;
        List<Double> s1_pri_lst = null;
        List<Integer> s2_num_lst = null;
        List<Double> s2_pri_lst = null;
        List<Integer> s3_num_lst = null;
        List<Double> s3_pri_lst = null;
        List<Integer> s4_num_lst = null;
        List<Double> s4_pri_lst = null;
        List<Integer> s5_num_lst = null;
        List<Double> s5_pri_lst = null;
        List<Timestamp> dl_dt_lst =null;
        
        boolean is_jumpping_water = false;
        int tailSz_jumpping_water = 30;
        double pct_jumpping_water = 0.08;
        
        
        public boolean keepDaysClsPriLost(int days, double threshold) {
            log.info("keepDaysClsPriLost: check if yt_cls_pri has " + days + " days keep lost with threshold value:" + threshold);
            int size = yt_cls_pri_lst.size();
            if(size < days + 1 || days <= 0) {
                return false;
            }
            
            double pre_yt_cls_pri = 0;
            double yt_cls_pri = 0;
            for(int i = 0; i < days; i++) {
            	pre_yt_cls_pri = yt_cls_pri_lst.get(size - i - 2);
                yt_cls_pri = yt_cls_pri_lst.get(size - i - 1);
                if ((pre_yt_cls_pri - yt_cls_pri) / pre_yt_cls_pri >= threshold) {
                    log.info("pre_yt_cls_pri:" + pre_yt_cls_pri + " is higher than " + yt_cls_pri + " by" + threshold);
                    continue;
                }
                else {
                        log.info("pre_cls_pri < yt_cls_pri, return false");
                        return false;
                }
            }
            return true;
        }
        
        public boolean keepDaysClsPriGain(int days, double threshold) {
            log.info("keepDaysClsPriGain: check if yt_cls_pri has " + days + " days keep gain with threshold value:" + threshold);
            int size = yt_cls_pri_lst.size();
            if(size < days + 1 || days <= 0) {
                return false;
            }
            
            double pre_yt_cls_pri = 0;
            double yt_cls_pri = 0;
            for(int i = 0; i < days; i++) {
            	pre_yt_cls_pri = yt_cls_pri_lst.get(size - i - 2);
                yt_cls_pri = yt_cls_pri_lst.get(size - i - 1);
                if (threshold <= (yt_cls_pri - pre_yt_cls_pri) / pre_yt_cls_pri) {
                    log.info("pre_yt_cls_pri:" + pre_yt_cls_pri + " is less than yt_cls_pri:" + yt_cls_pri + " by" + threshold);
                    continue;
                }
                else {
                        log.info("pre_cls_pri > yt_cls_pri, lost return false");
                        return false;
                }
            }
            return true;
        }
        
        public double getAvgYtClsPri(int days, int shiftDays) {
            log.info("getAvgYtClsPri: get avg yt_cls_pri for " + days + " days with shiftDays" + shiftDays);
            double avgPri = 0;
            int size = yt_cls_pri_lst.size();
            if (size < days + shiftDays) {
                log.info("Warning: there is no " + days + " yt_cls_pri data for stock:" + id + " using " + size + " days' data.");
                days = size - shiftDays;
            }
            
            for (int i = 0; i<days; i++) {
                avgPri += yt_cls_pri_lst.get(size - i - 1 - shiftDays);
            }
            
            avgPri = avgPri / days;
            log.info("Get avg yt_cls_pri as:" + avgPri);
            return avgPri;
        }
        
        public Double getCurPriStddev() {
            log.info("getCurPriStddev: get stddev for cur_pri.");
            double yt_cls_pri = 0;
            int size = cur_pri_lst.size();
            int size2 = cur_pri_lst.size();
            if (size <= 0 || size2 <= 0) {
                log.info("No curPri for stddev calculation.");
                return null;
            }
            
            yt_cls_pri = yt_cls_pri_lst.get(size2 - 1);
            
            List<Double> pctLst = new ArrayList<Double>();
            
            double pct = 0.0;
            double cur_pri = 0.0;
            for (int i = 0; i < size; i++) {
            	cur_pri = cur_pri_lst.get(i);
            	pct = (cur_pri - yt_cls_pri) / yt_cls_pri;
            	pctLst.add(pct);
            }
            
            double avgPct = 0.0;
            for(int i = 0; i < size; i++) {
            	avgPct += pctLst.get(i);
            }
            
            avgPct = avgPct / size;
            
            double stddev = 0.0;
            for(int i = 0; i < size; i++) {
            	stddev += Math.abs(pctLst.get(i) - avgPct);
            }
            
            stddev = stddev / size;
            log.info("Get avg (cur_pri - yt_cls_pri)/yt_cls_pri as:" + avgPct + " stddev:" + stddev);
            return stddev;
        }
        
        public boolean detQtyPlused(int periods, int ratio) {
            log.info("detQtyPlused: check if detQty plused during " + periods + " by ratio:" + ratio);
            int size = dl_stk_num_lst.size();
            if (size <= periods + 1) {
                return false;
            }
            long sumDetQty = 0;
            long curDetQty = dl_stk_num_lst.get(size - 1) - dl_stk_num_lst.get(size - 2);
            for (int i = 0; i<periods; i++) {
                sumDetQty += dl_stk_num_lst.get(size - i - 2) - dl_stk_num_lst.get(size - i - 3);
            }
            long avgPeriodDetQty = sumDetQty / periods;
            
            avgPeriodDetQty = (avgPeriodDetQty == 0) ? 1 : avgPeriodDetQty;
            
            if (curDetQty / avgPeriodDetQty > ratio) {
                log.info("detQtyPlused: curDetQty" + curDetQty + " " + curDetQty / avgPeriodDetQty + " times more than ratio:" + ratio);
                return true;
            }
            return false;
        }
        
        public boolean isVOLPlused(int period, double pct) {
            log.info("isVOLPlused: check if VOL plused for period:" + period + " with pct:" + pct);
            
            
            String sql = "select max(ft_id) max_ft_id, left(dl_dt, 10) dte from stkdat2 where id = '" + stkid + "' group by left(dl_dt, 10) order by dte desc";
            Connection con = DBManager.getConnection();
            Statement stm = null;
            ResultSet rs = null;
            boolean result = false;

            try {
            	log.info(sql);
            	stm = con.createStatement();
            	rs = stm.executeQuery(sql);
            	
            	int cnt = 0;
            	long lst_vol = 0;
            	long oth_vol = 0;
            	int bigCnt = 0;
            	
            	while (rs.next() && cnt < period) {
            		cnt++;
            		
            		String sql2 = "select dl_stk_num from stkdat2 s where s.id = '" + stkid + "' and s.ft_id = " + rs.getLong("max_ft_id");
            		Statement stm2 = con.createStatement();
            		
            		log.info(sql2);
            		ResultSet rs2 = stm2.executeQuery(sql2);
            		
            		if (!rs2.next()) {
                		rs2.close();
                		stm2.close();
                		break;
            		}
            		if (cnt == 1) {
            			lst_vol = rs2.getLong("dl_stk_num");
            			continue;
            		}
            		else {
            			oth_vol = rs2.getLong("dl_stk_num");
            		}
            		
            		rs2.close();
            		stm2.close();
            		
            		if (lst_vol > oth_vol) {
            			bigCnt++;
            		}
            	}
            	
            	log.info("Got VOL bigCnt:" + bigCnt + " in past " + period + " days");
            	
            	rs.close();
            	stm.close();
            	
            	if (cnt < period) {
            		log.info("There are only " + cnt + " days data avaialbe which is less then:" + period + " return false");
            		result = false;
            	}
            	else if (bigCnt * 1.0 / period >= pct){
            		log.info("For stock:" + stkid + ", the trading VOL for last day is " + bigCnt + " times then " + period + " days, which is bigger than pct:" + pct + ", return true");
            		result = true;
            	}
            }
            catch(Exception e) {
            	log.error(e.getMessage(), e);
            }
            finally {
            	try {
            		con.close();
            	}
            	catch(Exception e) {
            		log.error(e.getMessage(), e);
            	}
            }
            
            return result;
        }
        
        public boolean priceGoingUp(int period) {
            log.info("priceGoingUp: check if price goes up for period:" + period);
            int size = cur_pri_lst.size();
            if (size < period + 1) {
                log.info("priceGoingUp: only has " + size + " data less or equal than " + (1 + period));
                return false;
            }
            
            boolean result = true;
            for (int i=0; i< period; i++) {
                if (cur_pri_lst.get(size - 1 - i - 1) >= cur_pri_lst.get(size - 1 - i)) {
                	result = false;
                	break;
                }
            }
            
            if (result) {
            	price_trend = 1;
            }
            
            return result;
        }
        
        public boolean priceGoingDown(int period) {
            log.info("priceGoingDown: check if price goes down for period:" + period);
            int size = cur_pri_lst.size();
            if (size < period + 1) {
                log.info("priceGoingDown: only has " + size + " data less or equal than " + (1 + period));
                return false;
            }
            
            boolean result = true;
            for (int i=0; i< period; i++) {
                if (cur_pri_lst.get(size - 1 - i - 1) <= cur_pri_lst.get(size - 1 - i)) {
                	result = false;
                	break;
                }
            }
            
            if (result) {
            	price_trend = -1;
            }
            
            return result;
        }
        
        public boolean priceBreakingBoxUpEdge(int period) {
            log.info("priceBreakingBoxUpEdge: check if price breaking box up edge for period:" + period);
            int size = cur_pri_lst.size();
            if (size < period + 1) {
                log.info("priceBreakingBoxUpEdge: only has " + size + " data less or equal than " + (1 + period));
                return false;
            }
            
            boolean result = true;
            double lst_cur_pri = cur_pri_lst.get(size - 1);
            for (int i=0; i< period; i++) {
                if (cur_pri_lst.get(size - 1 - i - 1) >= lst_cur_pri) {
                	result = false;
                	break;
                }
            }
            
            return result;
        }
        
        public boolean priceBreakingBoxBtnEdge(int period) {
            log.info("priceBreakingBoxBtnEdge: check if price breaking box bottom edge for period:" + period);
            int size = cur_pri_lst.size();
            if (size < period + 1) {
                log.info("priceBreakingBoxBtnEdge: only has " + size + " data less or equal than " + (1 + period));
                return false;
            }
            
            boolean result = true;
            double lst_cur_pri = cur_pri_lst.get(size - 1);
            for (int i=0; i< period; i++) {
                if (cur_pri_lst.get(size - 1 - i - 1) <= lst_cur_pri) {
                	result = false;
                	break;
                }
            }
            
            return result;
        }
        
        public boolean priceUpAfterSharpedDown(int period) {
            log.info("priceUpAfterSharpedDown: check if price goes up after sharp down during period:" + period);
            int size = cur_pri_lst.size();
            if (size < period + 1) {
                log.info("priceUpAfterSharpedDown: only has " + size + " data less or equal than " + (1 + period));
                return false;
            }
            
            if (period <= 2) {
            	
            	boolean result = (cur_pri_lst.get(size - 1) > cur_pri_lst.get(size - 2)) || ((cur_pri_lst.get(size - 1) + cur_pri_lst.get(size - 2)) / 2.0 > (cur_pri_lst.get(size - 2) + cur_pri_lst.get(size - 3)) / 2.0);
            	log.info("check stock:" + this.stkid + " price1:" + cur_pri_lst.get(size - 3) + ", price2:" + cur_pri_lst.get(size - 2) + " price3:" + cur_pri_lst.get(size - 1) + " is up-down-up?" + result);
            	
            	return (result);
            }
            double cur_pri = cur_pri_lst.get(size -1);
            
            //make sure cur_pri is highest price during the past period.
            for (int i=0; i< period; i++) {
                if (cur_pri_lst.get(size - 1 - i - 1) > cur_pri) {
                	log.info("last price:" + cur_pri + " is not higest, return fasle.");
                	return false;
                }
            }
            
            //make sure avg(last period) > avg(2nd last period)
            double avg1 = 0.0;
            double avg2 = 0.0;
            for (int i=0; i< period; i++) {
                avg1 += cur_pri_lst.get(size - 1 - i);
                avg2 += cur_pri_lst.get(size - 1 - i - 1);
            }
            
            avg1 = avg1 / period;
            avg2 = avg2 / period;
            
            log.info(stkid + " at time:" + dl_dt_lst.get(size - 1) + " avg1:" + avg1 + " > avg2:" + avg2 + "? " +(avg1 > avg2));
            
            if (avg1 > avg2) {
                return true;
            }
            return false;
        }
        
        public boolean priceDownAfterSharpedUp(int period) {
            log.info("priceDownAfterSharpedUp: check if price goes down after sharp up during period:" + period);
            int size = cur_pri_lst.size();
            if (size < period + 1) {
                log.info("priceDownAfterSharpedUp: only has " + size + " data less or equal than " + (1 + period));
                return false;
            }
            
            if (period <= 2) {
            	
            	boolean result = (cur_pri_lst.get(size - 1) < cur_pri_lst.get(size - 2)) || ((cur_pri_lst.get(size - 1) + cur_pri_lst.get(size - 2)) / 2.0 < (cur_pri_lst.get(size - 2) + cur_pri_lst.get(size - 3)) / 2.0);
            	log.info("check stock:" + this.stkid + " price1:" + cur_pri_lst.get(size - 3) + ", price2:" + cur_pri_lst.get(size - 2) + " price3:" + cur_pri_lst.get(size - 1) + " is down-up-down?" + result);
            	
            	return (result);
            }
            
            double cur_pri = cur_pri_lst.get(size -1);
            
            //make sure cur_pri is lowest price during the past period + 1.
            for (int i=0; i< period; i++) {
                if (cur_pri_lst.get(size - 1 - i - 1) < cur_pri) {
                	log.info("last price:" + cur_pri + " is not lowest, return fasle.");
                	return false;
                }
            }
            
            //make sure avg(last period) < avg(2nd last period)
            double avg1 = 0.0;
            double avg2 = 0.0;
            for (int i=0; i< period; i++) {
                avg1 += cur_pri_lst.get(size - 1 - i);
                avg2 += cur_pri_lst.get(size - 1 - i - 1);
            }
            
            avg1 = avg1 / period;
            avg2 = avg2 / period;
            
            log.info(stkid + " at time:" + dl_dt_lst.get(size - 1) + " avg1:" + avg1 + " < avg2:" + avg2 + "? " +(avg1 < avg2));
            
            if (avg1 < avg2) {
                return true;
            }
            return false;
        }

        public String getStkid() {
            return stkid;
        }

        public void setStkid(String stkid) {
            this.stkid = stkid;
        }

        public List<Double> getTd_opn_pri_lst() {
            return td_opn_pri_lst;
        }

        public void setTd_opn_pri_lst(List<Double> tdOpnPriLst) {
            td_opn_pri_lst = tdOpnPriLst;
        }

        public List<Double> getYt_cls_pri_lst() {
            return yt_cls_pri_lst;
        }

        public void setYt_cls_pri_lst(List<Double> ytClsPriLst) {
            yt_cls_pri_lst = ytClsPriLst;
        }

        public List<Double> getTd_hst_pri_lst() {
            return td_hst_pri_lst;
        }

        public void setTd_hst_pri_lst(List<Double> tdHstPriLst) {
            td_hst_pri_lst = tdHstPriLst;
        }

        public List<Double> getTd_lst_pri_lst() {
            return td_lst_pri_lst;
        }

        public void setTd_lst_pri_lst(List<Double> tdLstPriLst) {
            td_lst_pri_lst = tdLstPriLst;
        }

        public List<String> getDt_lst() {
            return dt_lst;
        }

        public void setDt_lst(List<String> dtLst) {
            dt_lst = dtLst;
        }

        public List<Integer> getFt_id_lst() {
            return ft_id_lst;
        }

        public void setFt_id_lst(List<Integer> ftIdLst) {
            ft_id_lst = ftIdLst;
        }

        public List<Double> getCur_pri_lst() {
            return cur_pri_lst;
        }

        public void setCur_pri_lst(List<Double> curPriLst) {
            cur_pri_lst = curPriLst;
        }

        public List<Double> getB1_bst_pri_lst() {
            return b1_bst_pri_lst;
        }

        public void setB1_bst_pri_lst(List<Double> b1BstPriLst) {
            b1_bst_pri_lst = b1BstPriLst;
        }

        public List<Double> getS1_bst_pri_lst() {
            return s1_bst_pri_lst;
        }

        public void setS1_bst_pri_lst(List<Double> s1BstPriLst) {
            s1_bst_pri_lst = s1BstPriLst;
        }

        public List<Integer> getDl_stk_num_lst() {
            return dl_stk_num_lst;
        }

        public void setDl_stk_num_lst(List<Integer> dlStkNumLst) {
            dl_stk_num_lst = dlStkNumLst;
        }

        public List<Double> getDl_mny_num_lst() {
            return dl_mny_num_lst;
        }

        public void setDl_mny_num_lst(List<Double> dlMnyNumLst) {
            dl_mny_num_lst = dlMnyNumLst;
        }

        public List<Integer> getB1_num_lst() {
            return b1_num_lst;
        }
        
        public Integer getB1_num() {
            int sz = b1_num_lst.size();
            if (sz > 0) {
                return b1_num_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }

        public void setB1_num_lst(List<Integer> b1NumLst) {
            b1_num_lst = b1NumLst;
        }

        public List<Double> getB1_pri_lst() {
            return b1_pri_lst;
        }

        public Double getB1_pri() {
            int sz = b1_pri_lst.size();
            if (sz > 0) {
                return b1_pri_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }
        
        public void setB1_pri_lst(List<Double> b1PriLst) {
            b1_pri_lst = b1PriLst;
        }

        public List<Integer> getB2_num_lst() {
            return b2_num_lst;
        }

        public Integer getB2_num() {
            int sz = b2_num_lst.size();
            if (sz > 0) {
                return b2_num_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }
        
        public void setB2_num_lst(List<Integer> b2NumLst) {
            b2_num_lst = b2NumLst;
        }

        public List<Double> getB2_pri_lst() {
            return b2_pri_lst;
        }

        public Double getB2_pri() {
            int sz = b2_pri_lst.size();
            if (sz > 0) {
                return b2_pri_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }
        
        public void setB2_pri_lst(List<Double> b2PriLst) {
            b2_pri_lst = b2PriLst;
        }

        public List<Integer> getB3_num_lst() {
            return b3_num_lst;
        }
        
        public Integer getB3_num() {
            int sz = b3_num_lst.size();
            if (sz > 0) {
                return b3_num_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }

        public void setB3_num_lst(List<Integer> b3NumLst) {
            b3_num_lst = b3NumLst;
        }

        public Double getB3_pri() {
            int sz = b3_pri_lst.size();
            if (sz > 0) {
                return b3_pri_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }
        
        public List<Double> getB3_pri_lst() {
            return b3_pri_lst;
        }

        public void setB3_pri_lst(List<Double> b3PriLst) {
            b3_pri_lst = b3PriLst;
        }

        public List<Integer> getB4_num_lst() {
            return b4_num_lst;
        }

        public void setB4_num_lst(List<Integer> b4NumLst) {
            b4_num_lst = b4NumLst;
        }
        
        public Integer getB4_num() {
            int sz = b4_num_lst.size();
            if (sz > 0) {
                return b4_num_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }

        public List<Double> getB4_pri_lst() {
            return b4_pri_lst;
        }
        public Double getB4_pri() {
            int sz = b4_pri_lst.size();
            if (sz > 0) {
                return b4_pri_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }
        

        public void setB4_pri_lst(List<Double> b4PriLst) {
            b4_pri_lst = b4PriLst;
        }

        public List<Integer> getB5_num_lst() {
            return b5_num_lst;
        }

        public Integer getB5_num() {
            int sz = b5_num_lst.size();
            if (sz > 0) {
                return b5_num_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }
        public void setB5_num_lst(List<Integer> b5NumLst) {
            b5_num_lst = b5NumLst;
        }

        public List<Double> getB5_pri_lst() {
            return b5_pri_lst;
        }

        public void setB5_pri_lst(List<Double> b5PriLst) {
            b5_pri_lst = b5PriLst;
        }

        public Double getB5_pri() {
            int sz = b5_pri_lst.size();
            if (sz > 0) {
                return b5_pri_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }
        
        public List<Integer> getS1_num_lst() {
            return s1_num_lst;
        }

        public Integer getS1_num() {
            int sz = s1_num_lst.size();
            if (sz > 0) {
                return s1_num_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }
        public void setS1_num_lst(List<Integer> s1NumLst) {
            s1_num_lst = s1NumLst;
        }

        public List<Double> getS1_pri_lst() {
            return s1_pri_lst;
        }

        public Double getS1_pri() {
            int sz = s1_pri_lst.size();
            if (sz > 0) {
                return s1_pri_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }
        public void setS1_pri_lst(List<Double> s1PriLst) {
            s1_pri_lst = s1PriLst;
        }

        public List<Integer> getS2_num_lst() {
            return s2_num_lst;
        }
        public Integer getS2_num() {
            int sz = s2_num_lst.size();
            if (sz > 0) {
                return s2_num_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }

        public void setS2_num_lst(List<Integer> s2NumLst) {
            s2_num_lst = s2NumLst;
        }

        public List<Double> getS2_pri_lst() {
            return s2_pri_lst;
        }
        public Double getS2_pri() {
            int sz = s2_pri_lst.size();
            if (sz > 0) {
                return s2_pri_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }

        public void setS2_pri_lst(List<Double> s2PriLst) {
            s2_pri_lst = s2PriLst;
        }

        public List<Integer> getS3_num_lst() {
            return s3_num_lst;
        }
        public Integer gets3_num() {
            int sz = s3_num_lst.size();
            if (sz > 0) {
                return s3_num_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }

        public void setS3_num_lst(List<Integer> s3NumLst) {
            s3_num_lst = s3NumLst;
        }

        public List<Double> getS3_pri_lst() {
            return s3_pri_lst;
        }
        public Double getS3_pri() {
            int sz = s3_pri_lst.size();
            if (sz > 0) {
                return s3_pri_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }

        public void setS3_pri_lst(List<Double> s3PriLst) {
            s3_pri_lst = s3PriLst;
        }

        public List<Integer> getS4_num_lst() {
            return s4_num_lst;
        }
        public Integer getS4_num() {
            int sz = s4_num_lst.size();
            if (sz > 0) {
                return s4_num_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }

        public void setS4_num_lst(List<Integer> s4NumLst) {
            s4_num_lst = s4NumLst;
        }

        public List<Double> getS4_pri_lst() {
            return s4_pri_lst;
        }
        public Double getS4_pri() {
            int sz = s4_pri_lst.size();
            if (sz > 0) {
                return s4_pri_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }

        public void setS4_pri_lst(List<Double> s4PriLst) {
            s4_pri_lst = s4PriLst;
        }

        public List<Integer> getS5_num_lst() {
            return s5_num_lst;
        }
        public Integer getS5_num() {
            int sz = s5_num_lst.size();
            if (sz > 0) {
                return s5_num_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }

        public void setS5_num_lst(List<Integer> s5NumLst) {
            s5_num_lst = s5NumLst;
        }

        public List<Double> getS5_pri_lst() {
            return s5_pri_lst;
        }
        public Double getS5_pri() {
            int sz = s5_pri_lst.size();
            if (sz > 0) {
                return s5_pri_lst.get(sz - 1);
            }
            else {
                return null;
            }
        }

        public void setS5_pri_lst(List<Double> s5PriLst) {
            s5_pri_lst = s5PriLst;
        }

        public List<Timestamp> getDl_dt_lst() {
            return dl_dt_lst;
        }

        public void setDl_dt_lst(List<Timestamp> dlDtLst) {
            dl_dt_lst = dlDtLst;
        }
        
        public boolean isLstQtyPlused(int period) {
            int sz = dl_stk_num_lst.size();
            if (sz < period + 2) {
                log.info("dl_stk_num_lst has less data, lstQtyPlused is false.");
                return false;
            }
            long DetQty = 0;
            long maxDetQty = 0;
            
            for (int i=0; i<period; i++) {
            	DetQty = dl_stk_num_lst.get(sz - 1 - i) - dl_stk_num_lst.get(sz - 2 - i);
            	if (DetQty > maxDetQty) {
            		maxDetQty = DetQty;
            	}
            }
            
            log.info("maxDetQty is:" + maxDetQty + " for period:" + period + " with size:" + sz);
            long cnt = 0;
            for (int i = 0; i<sz - period - 1; i++) {
                long preDetQty = dl_stk_num_lst.get(sz - 1 - i - period) - dl_stk_num_lst.get(sz - 2 - i - period);
                if (preDetQty < maxDetQty) {
                    cnt++;
                }
            }
            
            double plus_pct = ParamManager.getFloatParam("VOLUME_PLUS_PCT", "TRADING", this.stkid);
            
            if (cnt * 1.0 / (sz - 1) >= plus_pct) {
                log.info("cnt is:" + cnt + " cnt/(sz-1):" + cnt * 1.0 / (sz-1) + " big than " + plus_pct + ", plused return true.");
                return true;
            }
            else {
                log.info("cnt is:" + cnt + " cnt/(sz-1):" + cnt * 1.0 / (sz-1) + " less than " + plus_pct + ", plused return false.");
                return false;
            }
        }
        
        //Check if cur price is jumping water, tailSz tells how many recent records should be check, and pct tells
        //how many percentage price decrease determine jumping water.
        public boolean isJumpWater(int tailSz, double pct) {
        	if (tailSz >= cur_pri_lst.size()) {
        		log.info("cur_pri_lst size is: " + cur_pri_lst.size() + " is small than tailSz/pct:" + tailSz + "/" + pct + " return false.");
        		is_jumpping_water = false;
        		return false;
        	}
        	int idx = cur_pri_lst.size() - 1;
        	double yt_cls_pri = this.getYtClsPri();
        	double detPri = 0;
        	int ts = tailSz;
        	while (ts > 0) {
        		if (cur_pri_lst.get(idx) < cur_pri_lst.get(idx - 1)) {
        			detPri += cur_pri_lst.get(idx) - cur_pri_lst.get(idx - 1);
        		}
        		idx--;
        		ts--;
        	}
        	
        	log.info("check jumpWater, cur_pri_lst.size: " + cur_pri_lst.size() + " TailSz:" + tailSz + " yt_cls_pri:" + yt_cls_pri
        			+ " detPri:" + detPri + ", detPri/yt_cls_pri pct:" + detPri * 1.0 / yt_cls_pri + " passed pct:" + pct);
        	if (detPri * 1.0 / yt_cls_pri <= -pct) {
        		log.info("jump water return true");
                tailSz_jumpping_water = tailSz;
                pct_jumpping_water = pct;
        		is_jumpping_water = true;
        		return true;
        	}
        	else {
        		log.info("jump water return false");
                tailSz_jumpping_water = tailSz;
                pct_jumpping_water = pct;
        		is_jumpping_water = false;
        	}
        	return false;
        }
        
        //Tells if cur price is recover from jump water, when decrease count is half less then pct then true.
        public boolean isStoppingJumpWater() {
        	if (tailSz_jumpping_water >= cur_pri_lst.size()) {
        		log.info("cur_pri_lst size is: " + cur_pri_lst.size() + " is small than tailSz_jumpping_water/pct_jumpping_water:" + tailSz_jumpping_water + "/" + pct_jumpping_water + " return false.");
        		return false;
        	}

        	if (!is_jumpping_water) {
        		log.info("did not jump water, can not stopping jumpping water either.");
        		return false;
        	}
        	int tailSz = tailSz_jumpping_water;
        	int idx = cur_pri_lst.size() - 1;
        	double yt_cls_pri = this.getYtClsPri();
        	int ts = tailSz;
        	double detPri = 0;
        	while (ts > 0) {
        		if (cur_pri_lst.get(idx) < cur_pri_lst.get(idx - 1)) {
        			detPri += cur_pri_lst.get(idx) - cur_pri_lst.get(idx - 1);
        		}
        		idx--;
        		ts--;
        	}
            
        	detPri = detPri / tailSz;
        	log.info("check isStopjumpWater, cur_pri_lst.size: " + cur_pri_lst.size() + " TailSz:" + tailSz + " yt_cls_pri:" + yt_cls_pri
        			+ " detPri:" + detPri + ", detPri/yt_cls_pri pct:" + detPri * 1.0 / yt_cls_pri + " passed pct/2:" + pct_jumpping_water/2);
        	if (detPri * 1.0 / yt_cls_pri >= pct_jumpping_water / 2) {
        		log.info("isStopjumpWater return true");
        		is_jumpping_water = false;
        		return true;
        	}
        	return false;
        }

        StockData(String stkId, int sz) {
            stkid = stkId;
            MAX_SZ = sz;
            
            td_opn_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            yt_cls_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            td_hst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            td_lst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            dt_lst = new BoundArrayList<String>(MAX_SZ);
            

            //Save today's data
            ft_id_lst = new BoundArrayList<Integer>(MAX_SZ);
            cur_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            b1_bst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s1_bst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            dl_stk_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            dl_mny_num_lst = new BoundArrayList<Double>(MAX_SZ);
            b1_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            b1_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            b2_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            b2_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            b3_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            b3_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            b4_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            b4_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            b5_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            b5_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s1_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            s1_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s2_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            s2_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s3_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            s3_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s4_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            s4_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s5_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            s5_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            dl_dt_lst = new BoundArrayList<Timestamp>(MAX_SZ);
            
            //LoadData();
            /*Connection con = DBManager.getConnection();
            try {
                Statement stm = con.createStatement();
                String sql = "select * from stkDlyInfo where id ='" + stkId + "' order by dt";
                
                log.info(sql);
                ResultSet rs = stm.executeQuery(sql);
                while(rs.next()) {
                    td_opn_pri_lst.add(rs.getDouble("td_opn_pri"));
                    yt_cls_pri_lst.add(rs.getDouble("yt_cls_pri"));
                    td_hst_pri_lst.add(rs.getDouble("td_hst_pri"));
                    td_lst_pri_lst.add(rs.getDouble("td_lst_pri"));
                    dt_lst.add(rs.getString("dt"));
                }
                rs.close();
                stm.close();
                con.close();
            }
            catch(SQLException e) {
                e.printStackTrace();
            }*/
        }
        
        StockData(String stkId, String start_dte, String end_dte, int sz) {
            stkid = stkId;
            MAX_SZ = sz;
            
            td_opn_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            yt_cls_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            td_hst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            td_lst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            dt_lst = new BoundArrayList<String>(MAX_SZ);
            

            //Save today's data
            ft_id_lst = new BoundArrayList<Integer>(MAX_SZ);
            cur_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            b1_bst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s1_bst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            dl_stk_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            dl_mny_num_lst = new BoundArrayList<Double>(MAX_SZ);
            b1_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            b1_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            b2_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            b2_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            b3_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            b3_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            b4_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            b4_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            b5_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            b5_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s1_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            s1_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s2_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            s2_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s3_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            s3_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s4_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            s4_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s5_num_lst = new BoundArrayList<Integer>(MAX_SZ);
            s5_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            dl_dt_lst = new BoundArrayList<Timestamp>(MAX_SZ);

            /*Connection con = DBManager.getConnection();
            try {
                Statement stm = con.createStatement();
                String sql = "select * from stkDlyInfo where id ='" + stkId + "' order by dt";
                
                log.info(sql);
                ResultSet rs = stm.executeQuery(sql);
                while(rs.next()) {
                    td_opn_pri_lst.add(rs.getDouble("td_opn_pri"));
                    yt_cls_pri_lst.add(rs.getDouble("yt_cls_pri"));
                    td_hst_pri_lst.add(rs.getDouble("td_hst_pri"));
                    td_lst_pri_lst.add(rs.getDouble("td_lst_pri"));
                    dt_lst.add(rs.getString("dt"));
                }
                rs.close();
                stm.close();
                con.close();
                //LoadData(start_dte, end_dte);
            }
            catch(SQLException e) {
                e.printStackTrace();
            }*/
        }
        
        public boolean LoadData() {
            int lst_ft_id = 0;
            if (!ft_id_lst.isEmpty()) {
                lst_ft_id = ft_id_lst.get(ft_id_lst.size() - 1);
            }
            Connection con = null;
            try {
            	con = DBManager.getConnection();
                Statement stm = con.createStatement();
                String sql = "select * from stkDat2 where ft_id > " + lst_ft_id +
                " and id = '" + stkid +
                "' and left(dl_dt,10) = (select left(max(dl_dt), 10) from stkDat2 d2 where d2.id = '" + stkid + "') order by dl_dt";
                
                log.info(sql);
                ResultSet rs = stm.executeQuery(sql);
                while(rs.next()) {
                    ft_id_lst.add(rs.getInt("ft_id"));
                    cur_pri_lst.add(rs.getDouble("cur_pri"));
                    b1_bst_pri_lst.add(rs.getDouble("b1_bst_pri"));
                    s1_bst_pri_lst.add(rs.getDouble("s1_bst_pri"));
                    dl_stk_num_lst.add(rs.getInt("dl_stk_num"));
                    dl_mny_num_lst.add(rs.getDouble("dl_mny_num"));
                    b1_num_lst.add(rs.getInt("b1_num"));
                    b1_pri_lst.add(rs.getDouble("b1_pri"));
                    b2_num_lst.add(rs.getInt("b2_num"));
                    b2_pri_lst.add(rs.getDouble("b2_pri"));
                    b3_num_lst.add(rs.getInt("b3_num"));
                    b3_pri_lst.add(rs.getDouble("b3_pri"));
                    b4_num_lst.add(rs.getInt("b4_num"));
                    b4_pri_lst.add(rs.getDouble("b4_pri"));
                    b5_num_lst.add(rs.getInt("b5_num"));
                    b5_pri_lst.add(rs.getDouble("b5_pri"));
                    s1_num_lst.add(rs.getInt("s1_num"));
                    s1_pri_lst.add(rs.getDouble("s1_pri"));
                    s2_num_lst.add(rs.getInt("s2_num"));
                    s2_pri_lst.add(rs.getDouble("s2_pri"));
                    s3_num_lst.add(rs.getInt("s3_num"));
                    s3_pri_lst.add(rs.getDouble("s3_pri"));
                    s4_num_lst.add(rs.getInt("s4_num"));
                    s4_pri_lst.add(rs.getDouble("s4_pri"));
                    s5_num_lst.add(rs.getInt("s5_num"));
                    s5_pri_lst.add(rs.getDouble("s5_pri"));
                    dl_dt_lst.add(rs.getTimestamp("dl_dt"));
                    td_opn_pri_lst.add(rs.getDouble("td_opn_pri"));
                    yt_cls_pri_lst.add(rs.getDouble("yt_cls_pri"));
                    td_hst_pri_lst.add(rs.getDouble("td_hst_pri"));
                    td_lst_pri_lst.add(rs.getDouble("td_lst_pri"));
                }
                rs.close();
                stm.close();
                //PrintStockData();
            }
            catch(SQLException e) {
                e.printStackTrace();
            }
    		finally {
    			try {
    				con.close();
    			} catch (SQLException e) {
    				// TODO Auto-generated catch block
    				log.error(e.getCause(), e);
    			}
    		}
            return true;
        }
        
        boolean LoadData(String stat_dte, String end_dte) {
            int lst_ft_id = 0;
            if (!ft_id_lst.isEmpty()) {
                lst_ft_id = ft_id_lst.get(ft_id_lst.size() - 1);
            }
            Connection con = null;
            try {
            	con = DBManager.getConnection();
                Statement stm = con.createStatement();
                String sql = "select * from stkDat2 where ft_id > " + lst_ft_id +
                " and id = '" + stkid +
                "' and left(dl_dt, 10) >= '" + stat_dte + "' and left(dl_dt, 10) <= '" + end_dte + "' order by dl_dt";
                
                log.info(sql);
                ResultSet rs = stm.executeQuery(sql);
                while(rs.next()) {
                    ft_id_lst.add(rs.getInt("ft_id"));
                    cur_pri_lst.add(rs.getDouble("cur_pri"));
                    b1_bst_pri_lst.add(rs.getDouble("b1_bst_pri"));
                    s1_bst_pri_lst.add(rs.getDouble("s1_bst_pri"));
                    dl_stk_num_lst.add(rs.getInt("dl_stk_num"));
                    dl_mny_num_lst.add(rs.getDouble("dl_mny_num"));
                    b1_num_lst.add(rs.getInt("b1_num"));
                    b1_pri_lst.add(rs.getDouble("b1_pri"));
                    b2_num_lst.add(rs.getInt("b2_num"));
                    b2_pri_lst.add(rs.getDouble("b2_pri"));
                    b3_num_lst.add(rs.getInt("b3_num"));
                    b3_pri_lst.add(rs.getDouble("b3_pri"));
                    b4_num_lst.add(rs.getInt("b4_num"));
                    b4_pri_lst.add(rs.getDouble("b4_pri"));
                    b5_num_lst.add(rs.getInt("b5_num"));
                    b5_pri_lst.add(rs.getDouble("b5_pri"));
                    s1_num_lst.add(rs.getInt("s1_num"));
                    s1_pri_lst.add(rs.getDouble("s1_pri"));
                    s2_num_lst.add(rs.getInt("s2_num"));
                    s2_pri_lst.add(rs.getDouble("s2_pri"));
                    s3_num_lst.add(rs.getInt("s3_num"));
                    s3_pri_lst.add(rs.getDouble("s3_pri"));
                    s4_num_lst.add(rs.getInt("s4_num"));
                    s4_pri_lst.add(rs.getDouble("s4_pri"));
                    s5_num_lst.add(rs.getInt("s5_num"));
                    s5_pri_lst.add(rs.getDouble("s5_pri"));
                    dl_dt_lst.add(rs.getTimestamp("dl_dt"));
                    td_opn_pri_lst.add(rs.getDouble("td_opn_pri"));
                    yt_cls_pri_lst.add(rs.getDouble("yt_cls_pri"));
                    td_hst_pri_lst.add(rs.getDouble("td_hst_pri"));
                    td_lst_pri_lst.add(rs.getDouble("td_lst_pri"));
                }
                rs.close();
                stm.close();
            }
            catch(SQLException e) {
                e.printStackTrace();
            }
    		finally {
    			try {
    				con.close();
    			} catch (SQLException e) {
    				// TODO Auto-generated catch block
    				log.error(e.getCause(), e);
    			}
    		}
            return true;
        }

        
        //This is for simTrade to step on rs.
        public boolean loadDataFromRs(ResultSet rs) {
            if (rs == null) {
                log.info("Can not LoadDataFromRs as rs is null");
                return false;
            }

            try {
            	
                if (dl_dt_lst.size() > 0) {
                	Timestamp ts = dl_dt_lst.get(dl_dt_lst.size() - 1);
                	String dtstr = ts.toString().substring(0, 10);
                	
                	Timestamp rsts = rs.getTimestamp("dl_dt");
                	String rsstr = rsts.toString().substring(0, 10);
                	
                	log.info("date in dl_dt_lst:" + dtstr);
                	log.info("date in rs:" + rsstr);
                	
                	if (!rsstr.equals(dtstr)) {
                		log.debug("clear all data in list as date changed");
                		clearInjectRawData();
                	}
                }
                
                    ft_id_lst.add(rs.getInt("ft_id"));
                    cur_pri_lst.add(rs.getDouble("cur_pri"));
                    b1_bst_pri_lst.add(rs.getDouble("b1_bst_pri"));
                    s1_bst_pri_lst.add(rs.getDouble("s1_bst_pri"));
                    dl_stk_num_lst.add(rs.getInt("dl_stk_num"));
                    dl_mny_num_lst.add(rs.getDouble("dl_mny_num"));
                    b1_num_lst.add(rs.getInt("b1_num"));
                    b1_pri_lst.add(rs.getDouble("b1_pri"));
                    b2_num_lst.add(rs.getInt("b2_num"));
                    b2_pri_lst.add(rs.getDouble("b2_pri"));
                    b3_num_lst.add(rs.getInt("b3_num"));
                    b3_pri_lst.add(rs.getDouble("b3_pri"));
                    b4_num_lst.add(rs.getInt("b4_num"));
                    b4_pri_lst.add(rs.getDouble("b4_pri"));
                    b5_num_lst.add(rs.getInt("b5_num"));
                    b5_pri_lst.add(rs.getDouble("b5_pri"));
                    s1_num_lst.add(rs.getInt("s1_num"));
                    s1_pri_lst.add(rs.getDouble("s1_pri"));
                    s2_num_lst.add(rs.getInt("s2_num"));
                    s2_pri_lst.add(rs.getDouble("s2_pri"));
                    s3_num_lst.add(rs.getInt("s3_num"));
                    s3_pri_lst.add(rs.getDouble("s3_pri"));
                    s4_num_lst.add(rs.getInt("s4_num"));
                    s4_pri_lst.add(rs.getDouble("s4_pri"));
                    s5_num_lst.add(rs.getInt("s5_num"));
                    s5_pri_lst.add(rs.getDouble("s5_pri"));
                    dl_dt_lst.add(rs.getTimestamp("dl_dt"));
                    td_opn_pri_lst.add(rs.getDouble("td_opn_pri"));
                    yt_cls_pri_lst.add(rs.getDouble("yt_cls_pri"));
                    td_hst_pri_lst.add(rs.getDouble("td_hst_pri"));
                    td_lst_pri_lst.add(rs.getDouble("td_lst_pri"));
            }
            catch(SQLException e) {
                e.printStackTrace();
            }
            
            log.info("Loaded data success from rs for stock:" + this.getStkid() + " with size:" + cur_pri_lst.size());
            return true;
        }
        
        
        public boolean injectRawData(RawStockData rsd) {
            if(rsd != null) {
                //ft_id_lst.add(rs.getInt("ft_id"));
                cur_pri_lst.add(rsd.cur_pri);
                b1_bst_pri_lst.add(rsd.b1_bst_pri);
                s1_bst_pri_lst.add(rsd.s1_bst_pri);
                dl_stk_num_lst.add(rsd.dl_stk_num);
                dl_mny_num_lst.add(rsd.dl_mny_num);
                b1_num_lst.add(rsd.b1_num);
                b1_pri_lst.add(rsd.b1_pri);
                b2_num_lst.add(rsd.b2_num);
                b2_pri_lst.add(rsd.b2_pri);
                b3_num_lst.add(rsd.b3_num);
                b3_pri_lst.add(rsd.b3_pri);
                b4_num_lst.add(rsd.b4_num);
                b4_pri_lst.add(rsd.b4_pri);
                b5_num_lst.add(rsd.b5_num);
                b5_pri_lst.add(rsd.b5_pri);
                s1_num_lst.add(rsd.s1_num);
                s1_pri_lst.add(rsd.s1_pri);
                s2_num_lst.add(rsd.s2_num);
                s2_pri_lst.add(rsd.s2_pri);
                s3_num_lst.add(rsd.s3_num);
                s3_pri_lst.add(rsd.s3_pri);
                s4_num_lst.add(rsd.s4_num);
                s4_pri_lst.add(rsd.s4_pri);
                s5_num_lst.add(rsd.s5_num);
                s5_pri_lst.add(rsd.s5_pri);
                dl_dt_lst.add(Timestamp.valueOf(rsd.dl_dt + " " + rsd.dl_tm));
                td_opn_pri_lst.add(rsd.td_opn_pri);
                yt_cls_pri_lst.add(rsd.yt_cls_pri);
                td_hst_pri_lst.add(rsd.td_hst_pri);
                td_lst_pri_lst.add(rsd.td_lst_pri);
            }
            else {
                return false;
            }
            return true;
        }
        
        public boolean clearInjectRawData() {
        	if (!cur_pri_lst.isEmpty()) {
                cur_pri_lst.clear();
                b1_bst_pri_lst.clear();
                s1_bst_pri_lst.clear();
                dl_stk_num_lst.clear();
                dl_mny_num_lst.clear();
                b1_num_lst.clear();
                b1_pri_lst.clear();
                b2_num_lst.clear();
                b2_pri_lst.clear();
                b3_num_lst.clear();
                b3_pri_lst.clear();
                b4_num_lst.clear();
                b4_pri_lst.clear();
                b5_num_lst.clear();
                b5_pri_lst.clear();
                s1_num_lst.clear();
                s1_pri_lst.clear();
                s2_num_lst.clear();
                s2_pri_lst.clear();
                s3_num_lst.clear();
                s3_pri_lst.clear();
                s4_num_lst.clear();
                s4_pri_lst.clear();
                s5_num_lst.clear();
                s5_pri_lst.clear();
                dl_dt_lst.clear();
                log.info("Now clearInjectedRawData success...");
                return true;
        	}
        	else {
        		log.info("Now clearInjectedRawData fail...");
        		return false;
        	}
        }
        
        void PrintStockData() {
            log.info("Total get " + dt_lst.size() + " days data.");
            for (int i = 0; i < dt_lst.size(); i++) {
                log.info("Date:" + dt_lst.get(i) +
                         " top:" + td_opn_pri_lst.get(i) +
                         " ycp:" + yt_cls_pri_lst.get(i) +
                         " thp:" + td_hst_pri_lst.get(i) +
                         " tlp:" + td_lst_pri_lst.get(i) + "\n");
            }
            log.info("Total get " + dl_dt_lst.size() + " records for today.");
            for (int i = 0; i < dl_dt_lst.size(); i++) {
                log.info(" ft_id: " + ft_id_lst.get(i) +
                         " cur_pri: " + cur_pri_lst.get(i) +
                         " b1_bst: " + b1_bst_pri_lst.get(i) +
                         " s1_bst: " + s1_bst_pri_lst.get(i) +
                         " dl_stk_num: " + dl_stk_num_lst.get(i) +
                         " dl_mny_num: " + dl_mny_num_lst.get(i) +
                         " b1_num: " + b1_num_lst.get(i) +
                         " b1_pri: " + b1_pri_lst.get(i) +
                         " b2_num: " + b2_num_lst.get(i) +
                         " b2_pri: " + b2_pri_lst.get(i) +
                         " b3_num: " + b3_num_lst.get(i) +
                         " b3_pri: " + b3_pri_lst.get(i) +
                         " b4_num: " + b4_num_lst.get(i) +
                         " b4_pri: " + b4_pri_lst.get(i) +
                         " b5_num: " + b5_num_lst.get(i) +
                         " b5_pri: " + b5_pri_lst.get(i) +
                         " s1_num: " + s1_num_lst.get(i) +
                         " s1_pri: " + s1_pri_lst.get(i) +
                         " s2_num: " + s2_num_lst.get(i) +
                         " s2_pri: " + s2_pri_lst.get(i) +
                         " s3_num: " + s3_num_lst.get(i) +
                         " s3_pri: " + s3_pri_lst.get(i) +
                         " s4_num: " + s4_num_lst.get(i) +
                         " s4_pri: " + s4_pri_lst.get(i) +
                         " s5_num: " + s5_num_lst.get(i) +
                         " s5_pri: " + s5_pri_lst.get(i) +
                         " dl_dt: " + dl_dt_lst.get(i) + "\n");
            }
        }

        //We skip top 1 biggest value as max to remove exceptional data.
		public Double getMaxCurPri() {
			// TODO Auto-generated method stub
			int sz = cur_pri_lst.size();
			double maxPri = 0;
			int idx = -1;
			if (sz > 0) {
			    for (int i = 0; i < sz; i++) {
			    	if (maxPri < cur_pri_lst.get(i)) {
			    		maxPri = cur_pri_lst.get(i);
			    		idx = i;
			    	}
			    }
			    
			    maxPri = 0;
			    for (int i = 0; i < sz; i++) {
			    	
			    	if (i == idx) {
			    		continue;
			    	}
			    	
			    	if (maxPri < cur_pri_lst.get(i)) {
			    		maxPri = cur_pri_lst.get(i);
			    	}
			    }
			    
			}
			log.info("got max pri for stock:" + id + ":" + maxPri);
			if (maxPri > 0) {
				return maxPri;
			}
			return null;
		}
		
		//We skip top 1 smallest value as min to remove exceptional data.
		public Double getMinCurPri() {
			// TODO Auto-generated method stub
			int sz = cur_pri_lst.size();
			double minPri = 100000;
			int idx = -1;
			if (sz > 0) {
			    for (int i = 0; i < sz; i++) {
			    	if (minPri > cur_pri_lst.get(i) && cur_pri_lst.get(i) > 0) {
			    		minPri = cur_pri_lst.get(i);
			    		idx = i;
			    	}
			    }
			    
			    minPri = 100000;
			    for (int i = 0; i < sz; i++) {
			    	
			    	if (i == idx) {
			    		continue;
			    	}
			    	if (minPri > cur_pri_lst.get(i) && cur_pri_lst.get(i) > 0) {
			    		minPri = cur_pri_lst.get(i);
			    	}
			    }
			    
			}
			log.info("got min pri for stock:" + id + ":" + minPri);
			if (minPri > 0) {
				return minPri;
			}
			return null;
		}
        
		public Double getMinTd_lst_pri() {
			// TODO Auto-generated method stub
			int sz = td_lst_pri_lst.size();
			double minPri = 100000;
			if (sz > 0) {
			    for (int i = 0; i < sz; i++) {
			    	if (minPri > td_lst_pri_lst.get(i) && td_lst_pri_lst.get(i) > 0) {
			    		minPri = td_lst_pri_lst.get(i);
			    	}
			    }
			}
			log.info("got min td_lst_pri for stock:" + id + ":" + minPri);
			if (minPri > 0) {
				return minPri;
			}
			return null;
		}
		public Double getMaxTd_hst_pri() {
			// TODO Auto-generated method stub
			int sz = td_hst_pri_lst.size();
			double maxPri = 0;
			if (sz > 0) {
			    for (int i = 0; i < sz; i++) {
			    	if (maxPri < td_hst_pri_lst.get(i)) {
			    		maxPri = td_hst_pri_lst.get(i);
			    	}
			    }
			}
			log.info("got max td_hst_pri for stock:" + id + ":" + maxPri);
			if (maxPri > 0) {
				return maxPri;
			}
			return null;
		}
		
		public Double getMaxYtClsPri(int Days) {
			// TODO Auto-generated method stub
			int sz = yt_cls_pri_lst.size();
			double maxPri = 0;
			
			if (sz <= Days) {
				log.info("no " + Days + " yt_cls_pri available, return null for getMaxYtClsPri().");
				return null;
			}
			if (sz > 0) {
				int cnt = 0;
			    for (int i = sz - 1; i >= 0 && cnt < Days; i--, cnt++) {
			    	if (maxPri < yt_cls_pri_lst.get(i)) {
			    		maxPri = yt_cls_pri_lst.get(i);
			    	}
			    }
			}
			log.info("got max yt_cls_pri for stock:" + id + ":" + maxPri);
			if (maxPri > 0) {
				return maxPri;
			}
			return null;
		}
		
		public Double getMinYtClsPri(int Days) {
			// TODO Auto-generated method stub
			int sz = yt_cls_pri_lst.size();
			double minPri = 100000;
			if (sz <= Days) {
				log.info("no " + Days + " yt_cls_pri available, return null for getMinYtClsPri().");
				return null;
			}
			if (sz > 0) {
				int cnt = 0;
			    for (int i = sz - 1; i >= 0 && cnt < Days; i--, cnt++) {
			    	if (minPri > yt_cls_pri_lst.get(i) && yt_cls_pri_lst.get(i) > 0) {
			    		minPri = yt_cls_pri_lst.get(i);
			    	}
			    }
			}
			log.info("got min yt_cls_pri for stock:" + id + ":" + minPri);
			if (minPri > 0) {
				return minPri;
			}
			return null;
		}

		public Double getYtClsPri() {
			// TODO Auto-generated method stub
			int sz = yt_cls_pri_lst.size();
			
			if (sz > 0) {
				log.info("yt_cls_pri for stock:" + id + " is:" +yt_cls_pri_lst.get(sz - 1));
				return yt_cls_pri_lst.get(sz - 1);
			}
			return null;
		}
        
		public Double getDl_mny_num() {
			// TODO Auto-generated method stub
			int sz = dl_mny_num_lst.size();
			
			if (sz > 0) {
				log.info("last dl_mny_num for stock:" + id + " is:" +dl_mny_num_lst.get(sz - 1));
				return dl_mny_num_lst.get(sz - 1);
			}
			return null;
		}
		public Integer getDl_stk_num() {
			// TODO Auto-generated method stub
			int sz = dl_stk_num_lst.size();
			
			if (sz > 0) {
				log.info("last dl_stk_num for stock:" + id + " is:" + dl_stk_num_lst.get(sz - 1));
				return dl_stk_num_lst.get(sz - 1);
			}
			return null;
		}
    }
    
    static Logger log = Logger.getLogger(Stock2.class);
    /**
     * @param args
     */
    String id;
    String name;
    String area;
    StockData sd;
    String suggested_by;
    String suggested_comment;
    double suggested_score = 0;
    String traded_by_selector;
    String traded_by_selector_comment;
    private int price_trend = 0;
    
    
    public String getSuggestedComment() {
        return suggested_comment;
    }

    public void setSuggestedComment(String suggested_comment) {
        this.suggested_comment = suggested_comment;
    }
    
    public double getSuggestedScore() {
        return suggested_score;
    }

    public void setSuggestedscore(double suggested_score) {
        this.suggested_score = suggested_score;
    }

    public String getTradedBySelectorComment() {
        return traded_by_selector_comment;
    }

    public void setTradedBySelectorComment(String traded_by_selector_comment) {
        this.traded_by_selector_comment = traded_by_selector_comment;
    }
    
    public String getSuggestedBy() {
        return suggested_by;
    }

    public void setSuggestedBy(String suggested_by) {
        this.suggested_by = suggested_by;
    }

    public String getTradedBySelector() {
        return traded_by_selector;
    }

    public void setTradedBySelector(String traded_by_selector) {
        this.traded_by_selector = traded_by_selector;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getArea() {
        return area;
    }

    public void setArea(String ara) {
        this.area = ara;
    }

    public StockData getSd() {
        return sd;
    }

    public void setSd(StockData sd) {
        this.sd = sd;
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Connection con = DBManager.getConnection();
        try {
            Statement stm = con.createStatement();
            String sql = "select id, name, area from stk where id = '002654'";
            
            ResultSet rs = stm.executeQuery(sql);
            List<Stock2> sl = new LinkedList<Stock2>();
            while(rs.next()) {
                Stock2 s = new Stock2(rs.getString("id"), rs.getString("name"), rs.getString("area"), 60);
                sl.add(s);
            }
            for (int i = 0; i < sl.size(); i++) {
                Stock2 s = sl.get(i);
                ClosePriceTrendStockSelector cs = new ClosePriceTrendStockSelector();
                cs.isTargetStock(s, null);
                s.printStockInfo();
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Stock2(String ids, String nm, String ara, int sz)
    {
        id = ids;
        name = nm;
        area = ara;
        sd = new StockData(id, sz);
    }
    
    public Stock2(String ids, String nm, String ara, String start_dte, String end_dte, int sz)
    {
        id = ids;
        name = nm;
        area = ara;
        sd = new StockData(id, start_dte, end_dte, sz);
    }

    @Override
    public int compareTo(Stock2 arg0) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    public boolean injectData(RawStockData rsd) {
        return sd.injectRawData(rsd);
    }
    
    public Double getCurPriStddev() {
        return sd.getCurPriStddev();
    }
    
    public Double getMaxCurPri() {
    	return sd.getMaxCurPri();
    }
    
    public Double getMinCurPri() {
    	return sd.getMinCurPri();
    }
    
    public Double getMaxYtClsPri(int days) {
    	return sd.getMaxYtClsPri(days);
    }
    
    public Double getMinYtClsPri(int days) {
    	return sd.getMinYtClsPri(days);
    }
    
    public Double getAvgYtClsPri(int days, int shiftDays) {
    	return sd.getAvgYtClsPri(days, shiftDays);
    }

    public Double getYtClsPri() {
    	return sd.getYtClsPri();
    }
    
    public boolean isJumpWater(int tailSz, double pct) {
    	if (sd.isJumpWater(tailSz, pct)) {
    		//sd.PrintStockData();
    		return true;
    	}
    	else {
    		return false;
    	}
    }
    
    //this method tells if the lasted record has dl_stk_num qty plused.
    public boolean isLstQtyPlused(int period) {
        return sd.isLstQtyPlused(period);
    }
    
    public boolean isStoppingJumpWater() {
    	if (sd.isStoppingJumpWater()) {
    		sd.PrintStockData();
    		return true;
    	}
    	else {
    		return false;
    	}
    }
    
    public boolean saveData(RawStockData rsd, Connection con) {

        if (rsd == null || con == null) {
            return false;
        }
        
        if (rsd.cur_pri <= 0) {
        	log.info("rsd.cur_pri is 0, skip insert stkdat2");
        	return false;
        }
        
        String sql = "insert into stkDat2 (ft_id,"
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
            "'" + rsd.id + "',"
                + rsd.td_opn_pri + ","
                + rsd.yt_cls_pri + ","
                + rsd.cur_pri + ","
                + rsd.td_hst_pri + ","
                + rsd.td_lst_pri + ","
                + rsd.b1_bst_pri + ","
                + rsd.s1_bst_pri + ","
                + rsd.dl_stk_num + ","
                + rsd.dl_mny_num + ","
                + rsd.b1_num + ","
                + rsd.b1_pri + ","
                + rsd.b2_num + ","
                + rsd.b2_pri + ","
                + rsd.b3_num + ","
                + rsd.b3_pri + ","
                + rsd.b4_num + ","
                + rsd.b4_pri + ","
                + rsd.b5_num + ","
                + rsd.b5_pri + ","
                + rsd.s1_num + ","
                + rsd.s1_pri + ","
                + rsd.s2_num + ","
                + rsd.s2_pri + ","
                + rsd.s3_num + ","
                + rsd.s3_pri + ","
                + rsd.s4_num + ","
                + rsd.s4_pri + ","
                + rsd.s5_num + ", "
                + rsd.s5_pri + ","
            + "str_to_date('" + rsd.dl_dt.toString() +" " + rsd.dl_tm +"', '%Y-%m-%d %H:%i:%s') from stkDat2";
        
        log.info(sql);
        try {
            Statement stm = con.createStatement();
            stm.execute(sql);
            stm.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        
        sd.injectRawData(rsd);
        log.info("saveData and injectRawData ran success.");
        
        return true;
    }
    
    public void printStockInfo() {
        log.info("========================================\n");
        log.info("Stock " + id + " data information:\n");
        log.info("========================================\n");
        log.info("ID\t|Name\t|");
        log.info(id + "\t|" + name + "\t|\n");
        sd.PrintStockData();
    }

    public Timestamp getDl_dt() {
        // TODO Auto-generated method stub
        if (!sd.dl_dt_lst.isEmpty()) {
            return sd.dl_dt_lst.get(sd.dl_dt_lst.size() - 1);
        }
        return null;
    }

    public Double getCur_pri() {
        // TODO Auto-generated method stub
    	Double cur_pri = null;
        if (!sd.cur_pri_lst.isEmpty()) {
        	cur_pri = sd.cur_pri_lst.get(sd.cur_pri_lst.size() - 1);
        	log.info("Got cur_pri:" + cur_pri + " for stock:" + id);
            return cur_pri;
        }
        else {
        	log.info("return null as cur_pri");
            return cur_pri;
        }
    }
    
    public Double getOpen_pri() {
    	Double opn_pri = null;
        if (!sd.td_opn_pri_lst.isEmpty()) {
        	opn_pri = sd.td_opn_pri_lst.get(sd.td_opn_pri_lst.size() - 1);
        	log.info("Got opn_pri:" + opn_pri + " for stock:" + id);
            return opn_pri;
        }
        return opn_pri;
    }
    
    public Double getDl_mny_num() {
        return sd.getDl_mny_num();
    }

    public Integer getDl_stk_num() {
        return sd.getDl_stk_num();
    }
    
    public Integer getB1_num() {
        return sd.getB1_num();
    }
    public Integer getB2_num() {
        return sd.getB2_num();
    }
    public Integer getB3_num() {
        return sd.getB3_num();
    }
    public Integer getB4_num() {
        return sd.getB4_num();
    }
    public Integer getB5_num() {
        return sd.getB5_num();
    }
    
    public List<Integer> getS1_num_lst() {
        return sd.getS1_num_lst();
    }
    
    public Integer getS1_num() {
        return sd.getS1_num();
    }
    public Integer getS2_num() {
        return sd.getS2_num();
    }
    public Integer getS3_num() {
        return sd.gets3_num();
    }
    public Integer getS4_num() {
        return sd.getS4_num();
    }
    public Integer getS5_num() {
        return sd.getS5_num();
    }
    
    
    public Double getB1_pri() {
        return sd.getB1_pri();
    }
    public Double getB2_pri() {
        return sd.getB2_pri();
    }
    public Double getB3_pri() {
        return sd.getB3_pri();
    }
    public Double getB4_pri() {
        return sd.getB4_pri();
    }
    public Double getB5_pri() {
        return sd.getB5_pri();
    }
    public Double getS1_pri() {
        return sd.getS1_pri();
    }
    public Double getS2_pri() {
        return sd.getS2_pri();
    }
    public Double getS3_pri() {
        return sd.getS3_pri();
    }
    public Double getS4_pri() {
        return sd.getS4_pri();
    }
    public Double getS5_pri() {
        return sd.getS5_pri();
    }
    public Double getMaxTd_hst_pri() {
        return sd.getMaxTd_hst_pri();
    }
    public Double getMinTd_lst_pri() {
        return sd.getMinTd_lst_pri();
    }
    public boolean priceDownAfterSharpedUp(int period) {
        return sd.priceDownAfterSharpedUp(period);
    }
    public boolean priceUpAfterSharpedDown(int period) {
        return sd.priceUpAfterSharpedDown(period);
    }
    
    public boolean priceGoingDown(int period) {
        return sd.priceGoingDown(period);
    }
    public boolean priceGoingUp(int period) {
        return sd.priceGoingUp(period);
    }
    
    public boolean priceBreakingBoxUpEdge(int period) {
        return sd.priceBreakingBoxUpEdge(period);
    }
    public boolean priceBreakingBoxBtnEdge(int period) {
        return sd.priceBreakingBoxBtnEdge(period);
    }
    
    public boolean isVOLPlused(int period, double pct) {
    	 return sd.isVOLPlused(period, pct);
    }
    
    public int getPriceTrend() {
    	return price_trend;
    }
    public String getID() {
        // TODO Auto-generated method stub
        return id;
    }
}
