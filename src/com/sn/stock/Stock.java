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
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.selector.stock.ClosePriceTrendStockSelector;

public class Stock implements Comparable<Stock>{

    public class StockData{

    	static public final int BIG_SZ = 270;  // store 4.5 hours data.
    	static public final int SMALL_SZ = 60; // store 1 hour data.
    	static public final int DLY_RCD_SZ = 14; // store 2 weeks data for daily record.
    	static public final int SECONDS_PER_FETCH = 60;
        int MAX_SZ = 800;
        //Save all history data
        String stkid;
        List<Double> dly_td_opn_pri_lst = null;
        List<Double> dly_td_cls_pri_lst = null;
        List<Double> dly_yt_cls_pri_lst = null;
        List<Double> dly_td_hst_pri_lst = null;
        List<Double> dly_td_lst_pri_lst = null;
        List<Integer> dly_dl_stk_num_lst =null;
        List<Double> dly_dl_mny_num_lst = null;
        List<String> dly_dt_lst = null;
        
        //Save today's data
        List<Integer> ft_id_lst =  null;
        List<Double> cur_pri_lst = null;
        List<Double> b1_bst_pri_lst = null;
        List<Double> s1_bst_pri_lst = null;
        List<Double> td_opn_pri_lst = null;
        List<Double> yt_cls_pri_lst = null;
        List<Double> td_hst_pri_lst = null;
        List<Double> td_lst_pri_lst = null;
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
        int tailSz_jumpping_water = 100;
        double pct_jumpping_water = 0.8;
        
        
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
        
        public boolean priceUpAfterSharpedDown(int periods, int downTimes) {
            log.info("priceUpAfterSharpedDown: check if price goes up after " + downTimes + " times down during period:" + periods);
            int size = cur_pri_lst.size();
            if (size <= periods) {
                log.info("priceUpAfterSharpedDown: only has " + size + " data less or equal than " +periods);
                return false;
            }
            double cur_pri = cur_pri_lst.get(size -1);
            double pre_cur_pri = cur_pri_lst.get(size - 2);
            if (cur_pri < pre_cur_pri) {
                return false;
            }
            
            int downTimeCnt = 0;
            for (int i=0; i<periods; i++) {
                if (cur_pri_lst.get(size - 1 - i - 1) < cur_pri_lst.get(size - 1 - i - 2)) {
                    downTimeCnt++;
                }
            }
            
            log.info("Check if actual down times:" + downTimeCnt + " is >= parameter:" + downTimes);
            if (downTimeCnt >= downTimes) {
                return true;
            }
            return false;
        }
        
        public boolean priceDownAfterSharpedUp(int periods, int upTimes) {
            log.info("priceUpAfterSharpedDown: check if price goes down after " + upTimes + " times up during period:" + periods);
            int size = cur_pri_lst.size();
            if (size <= periods ) {
                log.info("priceDownAfterSharpedUp: only has " + size + " data less or equal than " +periods);
                return false;
            }
            double cur_pri = cur_pri_lst.get(size -1);
            double pre_cur_pri = cur_pri_lst.get(size - 2);
            if (cur_pri > pre_cur_pri) {
                return false;
            }
            
            int upTimeCnt = 0;
            for (int i=0; i<periods - 1; i++) {
                if (cur_pri_lst.get(size - 1 - i - 1) > cur_pri_lst.get(size - 1 - i - 2)) {
                    upTimeCnt++;
                }
            }
            
            log.info("Check if actual up times:" + upTimeCnt + " is >= parameter:" + upTimes);
            if (upTimeCnt >= upTimes) {
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

        public List<Double> getDly_td_opn_pri_lst() {
            return dly_td_opn_pri_lst;
        }
        
        public List<Double> getDly_td_cls_pri_lst() {
            return dly_td_cls_pri_lst;
        }

        public void setDly_td_opn_pri_lst(List<Double> dly_td_opn_pri_lst) {
            this.dly_td_opn_pri_lst = dly_td_opn_pri_lst;
        }
        
        public void setDly_td_cls_pri_lst(List<Double> dly_td_cls_pri_lst) {
            this.dly_td_cls_pri_lst = dly_td_cls_pri_lst;
        }

        public List<Double> getDly_yt_cls_pri_lst() {
            return dly_yt_cls_pri_lst;
        }

        public void setDly_yt_cls_pri_lst(List<Double> dly_yt_cls_pri_lst) {
            this.dly_yt_cls_pri_lst = dly_yt_cls_pri_lst;
        }

        public List<Double> getDly_td_hst_pri_lst() {
            return dly_td_hst_pri_lst;
        }

        public void setDly_td_hst_pri_lst(List<Double> dly_td_hst_pri_lst) {
            this.dly_td_hst_pri_lst = dly_td_hst_pri_lst;
        }

        public List<Double> getDly_td_lst_pri_lst() {
            return dly_td_lst_pri_lst;
        }

        public void setDly_td_lst_pri_lst(List<Double> dly_td_lst_pri_lst) {
            this.dly_td_lst_pri_lst = dly_td_lst_pri_lst;
        }

        public List<Integer> getDly_dl_stk_num_lst() {
            return dly_dl_stk_num_lst;
        }

        public void setDly_dl_stk_num_lst(List<Integer> dly_dl_stk_num_lst) {
            this.dly_dl_stk_num_lst = dly_dl_stk_num_lst;
        }

        public List<Double> getDly_dl_mny_num_lst() {
            return dly_dl_mny_num_lst;
        }

        public void setDly_dl_mny_num_lst(List<Double> dly_dl_mny_num_lst) {
            this.dly_dl_mny_num_lst = dly_dl_mny_num_lst;
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

        public List<String> getDly_dt_lst() {
            return dly_dt_lst;
        }

        public void setDly_dt_lst(List<String> dtLst) {
            dly_dt_lst = dtLst;
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

        public void setB1_num_lst(List<Integer> b1NumLst) {
            b1_num_lst = b1NumLst;
        }

        public List<Double> getB1_pri_lst() {
            return b1_pri_lst;
        }

        public void setB1_pri_lst(List<Double> b1PriLst) {
            b1_pri_lst = b1PriLst;
        }

        public List<Integer> getB2_num_lst() {
            return b2_num_lst;
        }

        public void setB2_num_lst(List<Integer> b2NumLst) {
            b2_num_lst = b2NumLst;
        }

        public List<Double> getB2_pri_lst() {
            return b2_pri_lst;
        }

        public void setB2_pri_lst(List<Double> b2PriLst) {
            b2_pri_lst = b2PriLst;
        }

        public List<Integer> getB3_num_lst() {
            return b3_num_lst;
        }

        public void setB3_num_lst(List<Integer> b3NumLst) {
            b3_num_lst = b3NumLst;
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

        public List<Double> getB4_pri_lst() {
            return b4_pri_lst;
        }

        public void setB4_pri_lst(List<Double> b4PriLst) {
            b4_pri_lst = b4PriLst;
        }

        public List<Integer> getB5_num_lst() {
            return b5_num_lst;
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

        public List<Integer> getS1_num_lst() {
            return s1_num_lst;
        }

        public void setS1_num_lst(List<Integer> s1NumLst) {
            s1_num_lst = s1NumLst;
        }

        public List<Double> getS1_pri_lst() {
            return s1_pri_lst;
        }

        public void setS1_pri_lst(List<Double> s1PriLst) {
            s1_pri_lst = s1PriLst;
        }

        public List<Integer> getS2_num_lst() {
            return s2_num_lst;
        }

        public void setS2_num_lst(List<Integer> s2NumLst) {
            s2_num_lst = s2NumLst;
        }

        public List<Double> getS2_pri_lst() {
            return s2_pri_lst;
        }

        public void setS2_pri_lst(List<Double> s2PriLst) {
            s2_pri_lst = s2PriLst;
        }

        public List<Integer> getS3_num_lst() {
            return s3_num_lst;
        }

        public void setS3_num_lst(List<Integer> s3NumLst) {
            s3_num_lst = s3NumLst;
        }

        public List<Double> getS3_pri_lst() {
            return s3_pri_lst;
        }

        public void setS3_pri_lst(List<Double> s3PriLst) {
            s3_pri_lst = s3PriLst;
        }

        public List<Integer> getS4_num_lst() {
            return s4_num_lst;
        }

        public void setS4_num_lst(List<Integer> s4NumLst) {
            s4_num_lst = s4NumLst;
        }

        public List<Double> getS4_pri_lst() {
            return s4_pri_lst;
        }

        public void setS4_pri_lst(List<Double> s4PriLst) {
            s4_pri_lst = s4PriLst;
        }

        public List<Integer> getS5_num_lst() {
            return s5_num_lst;
        }

        public void setS5_num_lst(List<Integer> s5NumLst) {
            s5_num_lst = s5NumLst;
        }

        public List<Double> getS5_pri_lst() {
            return s5_pri_lst;
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
        
        public boolean isLstQtyPlused() {
            int sz = dl_stk_num_lst.size();
            if (sz <= 10) {
                log.info("dl_stk_num_lst has less data, lstQtyPlused is false.");
                return false;
            }
            long lstDetQty = dl_stk_num_lst.get(sz - 1) - dl_stk_num_lst.get(sz - 2);
            log.info("lstDetQty is:" + lstDetQty + " size:" + sz);
            long cnt = 0;
            for (int i = 1; i<= sz -2; i++) {
                long preDetQty = dl_stk_num_lst.get(sz - 1 - i) - dl_stk_num_lst.get(sz - 2 - i);
                if (preDetQty < lstDetQty) {
                    cnt++;
                }
            }
            double thresh_pct = calThreashValueForQtyPluse();
            if (cnt * 1.0 / (sz - 1) >= thresh_pct) {
                log.info("cnt is:" + cnt + " cnt/(sz-1):" + cnt * 1.0 / (sz-1) + " big than " + thresh_pct + ", plused return true.");
                return true;
            }
            else {
                // We shift ahead one record as we introduced logic of isLstPriTurnaround in case previous record has qty plused.
                lstDetQty = dl_stk_num_lst.get(sz - 2) - dl_stk_num_lst.get(sz - 3);
                log.info("lstDetQty2 is:" + lstDetQty + " size:" + (sz - 1));
                cnt = 0;
                for (int i = 2; i<= sz -3; i++) {
                    long preDetQty = dl_stk_num_lst.get(sz - 1 - i) - dl_stk_num_lst.get(sz - 2 - i);
                    if (preDetQty < lstDetQty) {
                        cnt++;
                    }
                }
                if (cnt * 1.0 / (sz - 2) >= thresh_pct) {
                    log.info("cnt is:" + cnt + " cnt/(sz-2):" + cnt * 1.0 / (sz-2) + " big than " + thresh_pct + ", plused return true.");
                    return true;
                }
                else {
                    log.info("cnt is:" + cnt + " cnt/(sz-1):" + cnt * 1.0 / (sz-1) + " less than " + thresh_pct + ", plused return false.");
                    return false;
                }
            }
        }
        
        public boolean isLstPriTurnaround(boolean inc_flg) {
            int sz = cur_pri_lst.size();
            if (sz <= 10) {
                log.info("cur_pri_lst has less data, isLstPriTurnaround is false.");
                return false;
            }
            double lstPri = cur_pri_lst.get(sz - 1);
            double prePri = cur_pri_lst.get(sz - 2);
            double lstDetPri = (inc_flg ? (lstPri - prePri) : (prePri - lstPri));
            log.info("lstDetPri is:" + lstDetPri + " size:" + sz + " inc_flg:" + inc_flg);
            if (lstDetPri >= 0.05) {
                log.info("lst price is:" + lstPri + ", pre price is:" + prePri + (inc_flg ? "big" : "small") + " than 0.05, isLstPriTurnaround return true.");
                return true;
            }
            else {
                log.info("lst price is:" + lstPri + ", pre price is:" + prePri + (inc_flg ? " not big" : " not small") + " than 0.05, isLstPriTurnaround return false.");
                return false;
            }
        }
        
        public boolean isLstDlyClsPriTurnaround(boolean inc_flg) {
            int sz = dly_td_cls_pri_lst.size();
            if (sz <= 10) {
                log.info("dly_td_cls_pri_lst has less data, isLstDlyClsPriTurnaround is false.");
                return false;
            }
            double lstPri = dly_td_cls_pri_lst.get(sz - 1);
            double prePri = dly_td_cls_pri_lst.get(sz - 2);
            double lstDetPri = (inc_flg ? (lstPri - prePri) : (prePri - lstPri));
            log.info("lstDetPri is:" + lstDetPri + " size:" + sz + " inc_flg:" + inc_flg);
            if (lstDetPri / prePri >= 0.01) {
                log.info("lst price is:" + lstPri + ", pre price is:" + prePri + (inc_flg ? "big" : "small") + " than 0.01, isLstDlyClsPriTurnaround return true.");
                return true;
            }
            else {
                log.info("lst price is:" + lstPri + ", pre price is:" + prePri + (inc_flg ? " not big" : " not small") + " than 0.01, isLstDlyClsPriTurnaround return false.");
                return false;
            }
        }
        
        private double calThreashValueForQtyPluse() {
        	double base = STConstants.QTY_PLUSED_BASE_PCT;
        	double final_val = base;
        	Boolean sell_mode = StockMarket.getStockSellMode(id);
        	if (sell_mode != null && sell_mode) {
        		final_val = base;
        	}
        	else {
            	try {
            		Connection con = DBManager.getConnection();
            		Statement stm = con.createStatement();
            		String sql = "select avg(dev) dev from ("
            				   + "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, to_char(dl_dt, 'yyyy-mm-dd') atDay "
            				   + "  from stkdat2 "
            				   + " where id ='" + id + "'"
            				   + "   and to_char(dl_dt, 'yyyy-mm-dd') >= to_char(sysdate - " + STConstants.DEV_CALCULATE_DAYS + ", 'yyyy-mm-dd')"
            				   + " group by to_char(dl_dt, 'yyyy-mm-dd'))";
            		log.info(sql);
            		ResultSet rs = stm.executeQuery(sql);
            		if (rs.next()) {
            			 double dev = rs.getDouble("dev");
            			 log.info("calThreashValueForQtyPluse get dev:" + dev);
            			if (dev > 0.01 && dev <= 0.1) {
            				final_val = (1 - STConstants.QTY_PLUSED_BASE_PCT) * (dev - 0.01) / (0.1 - 0.01) + STConstants.BASE_TRADE_THRESH;
            			}
            		}
            		rs.close();
            		stm.close();
            		con.close();
            	}
            	catch(Exception e) {
            		log.info(e.getMessage());
            	}
        	}
        	log.info("calThreashValueForQtyPluse returned final_val:" + final_val);
        	return final_val;
        }
        
        public boolean isDlyDlQtyPlused() {
            int sz = dly_dl_stk_num_lst.size();
            if (sz <= 10) {
                log.info("dly_dl_stk_num_lst has less data, isDlyDlQtyPlused is false.");
                return false;
            }
            long lstQty = dly_dl_stk_num_lst.get(sz - 1);
            log.info("isDlyDlQtyPlused is:" + lstQty + " size:" + sz);
            long cnt = 0;
            for (int i = 1; i<= sz -2; i++) {
                long preQty = dly_dl_stk_num_lst.get(sz - 1 - i);
                if (preQty < lstQty) {
                    cnt++;
                }
            }
            if (cnt * 1.0 / (sz - 1) >= STConstants.DLY_DL_QTY_PLUSED_PCT) {
                log.info("isDlyDlQtyPlused cnt is:" + cnt + " cnt/(sz-1):" + cnt * 1.0 / (sz-1) + " big than " + STConstants.DLY_DL_QTY_PLUSED_PCT + ", plused return true.");
                return true;
            }
            else {
                log.info("isDlyDlQtyPlused cnt is:" + cnt + " cnt/(sz-1):" + cnt * 1.0 / (sz-1) + " less than " + STConstants.DLY_DL_QTY_PLUSED_PCT + ", plused return false.");
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
        	while (tailSz > 0) {
        		if (cur_pri_lst.get(idx) < cur_pri_lst.get(idx - 1)) {
        			detPri += cur_pri_lst.get(idx) - cur_pri_lst.get(idx - 1);
        		}
        		idx--;
        		ts--;
        	}
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
            
            dly_td_opn_pri_lst = new BoundArrayList<Double>(DLY_RCD_SZ);
            dly_td_cls_pri_lst = new BoundArrayList<Double>(DLY_RCD_SZ);
            dly_yt_cls_pri_lst = new BoundArrayList<Double>(DLY_RCD_SZ);
            dly_td_hst_pri_lst = new BoundArrayList<Double>(DLY_RCD_SZ);
            dly_td_lst_pri_lst = new BoundArrayList<Double>(DLY_RCD_SZ);
            dly_dl_stk_num_lst = new BoundArrayList<Integer>(DLY_RCD_SZ);
            dly_dl_mny_num_lst = new BoundArrayList<Double>(DLY_RCD_SZ);
            dly_dt_lst = new BoundArrayList<String>(DLY_RCD_SZ);
            

            //Save today's data
            ft_id_lst = new BoundArrayList<Integer>(MAX_SZ);
            cur_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            b1_bst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s1_bst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            td_opn_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            yt_cls_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            td_hst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            td_lst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
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
            
            Connection con = DBManager.getConnection();
            try {
            	double pre_yt_cls_pri = -1;
                Statement stm = con.createStatement();
                String sql = "select * from stkDlyInfo where id ='" + stkId + "' order by dt";
                
                log.info(sql);
                ResultSet rs = stm.executeQuery(sql);
                while(rs.next()) {
                	if (pre_yt_cls_pri != -1 && Math.abs(pre_yt_cls_pri - rs.getDouble("yt_cls_pri")) / Math.min(pre_yt_cls_pri, rs.getDouble("yt_cls_pri"))  > 1) {
                		log.info("1.pre_yt_cls_pri:" + pre_yt_cls_pri + ", rs.yt_cls_pri:" + rs.getDouble("yt_cls_pri") + " has 100% diff, clear data and reset.");
                		dly_td_opn_pri_lst.clear();
                		dly_td_cls_pri_lst.clear();
                		dly_yt_cls_pri_lst.clear();
                		dly_td_hst_pri_lst.clear();
                		dly_td_lst_pri_lst.clear();
                		dly_dl_stk_num_lst.clear();
                		dly_dl_mny_num_lst.clear();
                		dly_dt_lst.clear();
                	}
                	pre_yt_cls_pri = rs.getDouble("yt_cls_pri");
                    dly_td_opn_pri_lst.add(rs.getDouble("td_opn_pri"));
                    dly_td_cls_pri_lst.add(rs.getDouble("td_cls_pri"));
                    dly_yt_cls_pri_lst.add(rs.getDouble("yt_cls_pri"));
                    dly_td_hst_pri_lst.add(rs.getDouble("td_hst_pri"));
                    dly_td_lst_pri_lst.add(rs.getDouble("td_lst_pri"));
                    dly_dl_stk_num_lst.add(rs.getInt("dl_stk_num"));
                    dly_dl_mny_num_lst.add(rs.getDouble("dl_mny_num"));
                    dly_dt_lst.add(rs.getString("dt"));
                }
                rs.close();
                stm.close();
                con.close();
                LoadData();
            }
            catch(SQLException e) {
                e.printStackTrace();
            }
        }
        
        StockData(String stkId, String start_dte, String end_dte, int sz) {
            stkid = stkId;
            MAX_SZ = sz;
            
            dly_td_opn_pri_lst = new BoundArrayList<Double>(DLY_RCD_SZ);
            dly_td_cls_pri_lst = new BoundArrayList<Double>(DLY_RCD_SZ);
            dly_yt_cls_pri_lst = new BoundArrayList<Double>(DLY_RCD_SZ);
            dly_td_hst_pri_lst = new BoundArrayList<Double>(DLY_RCD_SZ);
            dly_td_lst_pri_lst = new BoundArrayList<Double>(DLY_RCD_SZ);
            dly_dl_stk_num_lst = new BoundArrayList<Integer>(DLY_RCD_SZ);
            dly_dl_mny_num_lst = new BoundArrayList<Double>(DLY_RCD_SZ);
            dly_dt_lst = new BoundArrayList<String>(DLY_RCD_SZ);
            

            //Save today's data
            ft_id_lst = new BoundArrayList<Integer>(MAX_SZ);
            cur_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            b1_bst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            s1_bst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            td_opn_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            yt_cls_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            td_hst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
            td_lst_pri_lst = new BoundArrayList<Double>(MAX_SZ);
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

            Connection con = DBManager.getConnection();
            try {
            	double pre_yt_cls_pri = -1;
                Statement stm = con.createStatement();
                String sql = "select * from stkDlyInfo where id ='" + stkId + "' order by dt";
                
                log.info(sql);
                ResultSet rs = stm.executeQuery(sql);
                while(rs.next()) {
                	if (pre_yt_cls_pri != -1 && Math.abs(pre_yt_cls_pri - rs.getDouble("yt_cls_pri")) /  Math.min(pre_yt_cls_pri, rs.getDouble("yt_cls_pri")) > 1) {
                		log.info("pre_yt_cls_pri:" + pre_yt_cls_pri + ", rs.yt_cls_pri:" + rs.getDouble("yt_cls_pri") + " has 100% diff, clear data and reset.");
                		dly_td_opn_pri_lst.clear();
                		dly_td_cls_pri_lst.clear();
                		dly_yt_cls_pri_lst.clear();
                		dly_td_hst_pri_lst.clear();
                		dly_td_lst_pri_lst.clear();
                		dly_dl_stk_num_lst.clear();
                		dly_dl_mny_num_lst.clear();
                		dly_dt_lst.clear();
                	}
                	pre_yt_cls_pri = rs.getDouble("yt_cls_pri");
                    dly_td_opn_pri_lst.add(rs.getDouble("td_opn_pri"));
                    dly_td_cls_pri_lst.add(rs.getDouble("td_cls_pri"));
                    dly_yt_cls_pri_lst.add(rs.getDouble("yt_cls_pri"));
                    dly_td_hst_pri_lst.add(rs.getDouble("td_hst_pri"));
                    dly_td_lst_pri_lst.add(rs.getDouble("td_lst_pri"));
                    dly_dl_stk_num_lst.add(rs.getInt("dl_stk_num"));
                    dly_dl_mny_num_lst.add(rs.getDouble("dl_mny_num"));
                    dly_dt_lst.add(rs.getString("dt"));
                }
                rs.close();
                stm.close();
                con.close();
                //LoadData(start_dte, end_dte);
            }
            catch(SQLException e) {
                e.printStackTrace();
            }
        }
        
        boolean LoadData() {
            int lst_ft_id = 0;
            if (!ft_id_lst.isEmpty()) {
                lst_ft_id = ft_id_lst.get(ft_id_lst.size() - 1);
            }
            Connection con = DBManager.getConnection();
            try {
                Statement stm = con.createStatement();
                String sql = "select * from stkDat2 where ft_id > " + lst_ft_id +
                " and id = '" + stkid +
                "' and to_char(dl_dt,'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') order by dl_dt";
                
                log.info(sql);
                ResultSet rs = stm.executeQuery(sql);
                while(rs.next()) {
                    ft_id_lst.add(rs.getInt("ft_id"));
                    cur_pri_lst.add(rs.getDouble("cur_pri"));
                    b1_bst_pri_lst.add(rs.getDouble("b1_bst_pri"));
                    s1_bst_pri_lst.add(rs.getDouble("s1_bst_pri"));
                    td_opn_pri_lst.add(rs.getDouble("td_opn_pri"));
                    yt_cls_pri_lst.add(rs.getDouble("yt_cls_pri"));
                    td_hst_pri_lst.add(rs.getDouble("td_hst_pri"));
                    td_lst_pri_lst.add(rs.getDouble("td_lst_pri"));
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
                }
                rs.close();
                stm.close();
                con.close();
                //PrintStockData();
            }
            catch(SQLException e) {
                e.printStackTrace();
            }
            return true;
        }
        
        boolean LoadData(String stat_dte, String end_dte) {
            int lst_ft_id = 0;
            if (!ft_id_lst.isEmpty()) {
                lst_ft_id = ft_id_lst.get(ft_id_lst.size() - 1);
            }
            Connection con = DBManager.getConnection();
            try {
                Statement stm = con.createStatement();
                String sql = "select * from stkDat2 where ft_id > " + lst_ft_id +
                " and id = '" + stkid +
                "' and to_char(dl_dt,'yyyy-mm-dd') >= '" + stat_dte + "' and to_char(dl_dt,'yyyy-mm-dd') <= '" + end_dte + "' order by dl_dt";
                
                log.info(sql);
                ResultSet rs = stm.executeQuery(sql);
                while(rs.next()) {
                    ft_id_lst.add(rs.getInt("ft_id"));
                    cur_pri_lst.add(rs.getDouble("cur_pri"));
                    b1_bst_pri_lst.add(rs.getDouble("b1_bst_pri"));
                    s1_bst_pri_lst.add(rs.getDouble("s1_bst_pri"));
                    td_opn_pri_lst.add(rs.getDouble("td_opn_pri"));
                    yt_cls_pri_lst.add(rs.getDouble("yt_cls_pri"));
                    td_hst_pri_lst.add(rs.getDouble("td_hst_pri"));
                    td_lst_pri_lst.add(rs.getDouble("td_lst_pri"));
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
                }
                rs.close();
                stm.close();
                con.close();
                //PrintStockData();
            }
            catch(SQLException e) {
                e.printStackTrace();
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
                    ft_id_lst.add(rs.getInt("ft_id"));
                    cur_pri_lst.add(rs.getDouble("cur_pri"));
                    b1_bst_pri_lst.add(rs.getDouble("b1_bst_pri"));
                    s1_bst_pri_lst.add(rs.getDouble("s1_bst_pri"));
                    td_opn_pri_lst.add(rs.getDouble("td_opn_pri"));
                    yt_cls_pri_lst.add(rs.getDouble("yt_cls_pri"));
                    td_hst_pri_lst.add(rs.getDouble("td_hst_pri"));
                    td_lst_pri_lst.add(rs.getDouble("td_lst_pri"));
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
                td_opn_pri_lst.add(rsd.td_opn_pri);
                yt_cls_pri_lst.add(rsd.yt_cls_pri);
                td_hst_pri_lst.add(rsd.td_hst_pri);
                td_lst_pri_lst.add(rsd.td_lst_pri);
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
                td_opn_pri_lst.clear();
                yt_cls_pri_lst.clear();
                td_hst_pri_lst.clear();
                td_lst_pri_lst.clear();
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
            log.info("Total get " + dly_dt_lst.size() + " days data.");
            for (int i = 0; i < dly_dt_lst.size(); i++) {
                log.info("Date:" + dly_dt_lst.get(i) +
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

        public Double getMaxDlyClsPri() {
            // TODO Auto-generated method stub
            int sz = dly_yt_cls_pri_lst.size();
            double maxPri = 0;
            if (sz > 0) {
                for (int i = 0; i < sz; i++) {
                    if (maxPri < dly_yt_cls_pri_lst.get(i)) {
                        maxPri = dly_yt_cls_pri_lst.get(i);
                    }
                }
            }
            log.info("got max dly_yt_cls_pri_lst pri for stock:" + id + ":" + maxPri);
            if (maxPri > 0) {
                return maxPri;
            }
            return null;
        }
        
        public Double getMinDlyClsPri() {
            // TODO Auto-generated method stub
            int sz = dly_yt_cls_pri_lst.size();
            double minPri = 100000;
            if (sz > 0) {
                for (int i = 0; i < sz; i++) {
                    if (minPri > dly_yt_cls_pri_lst.get(i) && dly_yt_cls_pri_lst.get(i) > 0) {
                        minPri = dly_yt_cls_pri_lst.get(i);
                    }
                }
            }
            log.info("got min dly_yt_cls_pri_lst pri for stock:" + id + ":" + minPri);
            if (minPri > 0) {
                return minPri;
            }
            return null;
        }
        
        public Double getMaxDlyOpnPri() {
            // TODO Auto-generated method stub
            int sz = dly_td_opn_pri_lst.size();
            double maxPri = 0;
            if (sz > 0) {
                for (int i = 0; i < sz; i++) {
                    if (maxPri < dly_td_opn_pri_lst.get(i)) {
                        maxPri = dly_td_opn_pri_lst.get(i);
                    }
                }
            }
            log.info("got max dly_td_opn_pri_lst pri for stock:" + id + ":" + maxPri);
            if (maxPri > 0) {
                return maxPri;
            }
            return null;
        }
        
        public Double getMinDlyOpnPri() {
            // TODO Auto-generated method stub
            int sz = dly_td_opn_pri_lst.size();
            double minPri = 100000;
            if (sz > 0) {
                for (int i = 0; i < sz; i++) {
                    if (minPri > dly_td_opn_pri_lst.get(i) && dly_td_opn_pri_lst.get(i) > 0) {
                        minPri = dly_td_opn_pri_lst.get(i);
                    }
                }
            }
            log.info("got min dly_td_opn_pri_lst pri for stock:" + id + ":" + minPri);
            if (minPri > 0) {
                return minPri;
            }
            return null;
        }
        
        public Double getMaxDlyTdClsPri() {
            // TODO Auto-generated method stub
            int sz = dly_td_cls_pri_lst.size();
            double maxPri = 0;
            if (sz > 0) {
                for (int i = 0; i < sz; i++) {
                    if (maxPri < dly_td_cls_pri_lst.get(i)) {
                        maxPri = dly_td_cls_pri_lst.get(i);
                    }
                }
            }
            log.info("got max dly_td_cls_pri_lst pri for stock:" + id + ":" + maxPri);
            if (maxPri > 0) {
                return maxPri;
            }
            return null;
        }
        
        public Double getMinDlyTdClsPri() {
            // TODO Auto-generated method stub
            int sz = dly_td_cls_pri_lst.size();
            double minPri = 100000;
            if (sz > 0) {
                for (int i = 0; i < sz; i++) {
                    if (minPri > dly_td_cls_pri_lst.get(i) && dly_td_cls_pri_lst.get(i) > 0) {
                        minPri = dly_td_cls_pri_lst.get(i);
                    }
                }
            }
            log.info("got min dly_td_cls_pri_lst pri for stock:" + id + ":" + minPri);
            if (minPri > 0) {
                return minPri;
            }
            return null;
        }
        
		public Double getMaxCurPri() {
			// TODO Auto-generated method stub
			int sz = cur_pri_lst.size();
			double maxPri = 0;
			if (sz > 0) {
			    for (int i = 0; i < sz; i++) {
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
		
		public Double getMinCurPri() {
			// TODO Auto-generated method stub
			int sz = cur_pri_lst.size();
			double minPri = 100000;
			if (sz > 0) {
			    for (int i = 0; i < sz; i++) {
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
    }
    
    static Logger log = Logger.getLogger(Stock.class);
    /**
     * @param args
     */
    String id;
    String name;
    StockData sd;
    
    
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
            String sql = "select id, name from stk where id = '002654'";
            
            ResultSet rs = stm.executeQuery(sql);
            List<Stock> sl = new LinkedList<Stock>();
            while(rs.next()) {
                Stock s = new Stock(rs.getString("id"), rs.getString("name"), StockData.SMALL_SZ);
                sl.add(s);
            }
            for (int i = 0; i < sl.size(); i++) {
                Stock s = sl.get(i);
                ClosePriceTrendStockSelector cs = new ClosePriceTrendStockSelector();
                cs.isTargetStock(s, null);
                s.printStockInfo();
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Stock(String ids, String nm, int sz)
    {
        id = ids;
        name = nm;
        sd = new StockData(id, sz);
    }
    
    public Stock(String ids, String nm, String start_dte, String end_dte, int sz)
    {
        id = ids;
        name = nm;
        sd = new StockData(id, start_dte, end_dte, sz);
    }

    @Override
    public int compareTo(Stock arg0) {
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
    
    public Double getMaxDlyClsPri() {
        return sd.getMaxDlyClsPri();
    }
    
    public Double getMinDlyClsPri() {
        return sd.getMinDlyClsPri();
    }
    
    public Double getMaxDlyOpnPri() {
        return sd.getMaxDlyOpnPri();
    }
    
    public Double getMinDlyOpnPri() {
        return sd.getMinDlyOpnPri();
    }
    
    public Double getMaxDlyTdClsPri() {
        return sd.getMaxDlyTdClsPri();
    }
    
    public Double getMinDlyTdClsPri() {
        return sd.getMinDlyTdClsPri();
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
    public boolean isLstQtyPlused() {
        return sd.isLstQtyPlused();
    }
    
    public boolean isLstPriTurnaround(boolean inc_flg) {
        return sd.isLstPriTurnaround(inc_flg);
    }
    
    public boolean isLstDlyClsPriTurnaround(boolean inc_flg) {
        return sd.isLstDlyClsPriTurnaround(inc_flg);
    }
    
    //this method tells if the lasted record has dly_dl_stk_num qty plused.
    public boolean isDlyDlQtyPlused() {
        return sd.isDlyDlQtyPlused();
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
            + " values (SEQ_STKDAT_PK.nextval," +
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
            + "to_date('" + rsd.dl_dt.toString() +" " + rsd.dl_tm +"', 'yyyy-mm-dd hh24:mi:ss'))";
        
        //log.info(sql);
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
        if (!sd.cur_pri_lst.isEmpty()) {
        	opn_pri = sd.td_opn_pri_lst.get(sd.td_opn_pri_lst.size() - 1);
        	log.info("Got opn_pri:" + opn_pri + " for stock:" + id);
            return opn_pri;
        }
        return opn_pri;
    }

    public String getID() {
        // TODO Auto-generated method stub
        return id;
    }
    
    //forDay = 0 means yesterday's open price.
    public Double getOpen_pri(int forDay) {
        Double opn_pri = null;
        if (!sd.dly_td_opn_pri_lst.isEmpty()) {
            opn_pri = sd.dly_td_opn_pri_lst.get(sd.dly_td_opn_pri_lst.size() - forDay - 1);
            log.info("Got opn_pri:" + opn_pri + " for stock:" + id + " for day:" + forDay);
            return opn_pri;
        }
        return opn_pri;
    }
    //forDay = 0 means yesterday's open price.
    public Double getTdCls_pri(int forDay) {
        Double td_cls_pri = null;
        if (!sd.dly_td_cls_pri_lst.isEmpty()) {
        	td_cls_pri = sd.dly_td_cls_pri_lst.get(sd.dly_td_cls_pri_lst.size() - forDay - 1);
            log.info("Got cls_pri:" + td_cls_pri + " for stock:" + id + " for day:" + forDay);
            return td_cls_pri;
        }
        return td_cls_pri;
    }
    //forDay = 0 means yesterday's cls price.
    public Double getCls_pri(int forDay) {
        Double cls_pri = null;
        if (!sd.dly_yt_cls_pri_lst.isEmpty()) {
            cls_pri = sd.dly_yt_cls_pri_lst.get(sd.dly_yt_cls_pri_lst.size() - forDay - 1);
            log.info("Got cls_pri:" + cls_pri + " for stock:" + id + " for day:" + forDay);
            return cls_pri;
        }
        return cls_pri;
    }
    //forDay = 0 means yesterday's highest price.
    public Double getHst_pri(int forDay) {
        Double hst_pri = null;
        if (!sd.dly_td_hst_pri_lst.isEmpty()) {
            hst_pri = sd.dly_td_hst_pri_lst.get(sd.dly_td_hst_pri_lst.size() - forDay - 1);
            log.info("Got hst_pri:" + hst_pri + " for stock:" + id + " for day:" + forDay);
            return hst_pri;
        }
        return hst_pri;
    }
    //forDay = 0 means yesterday's lowest price.
    public Double getLst_pri(int forDay) {
        Double lst_pri = null;
        if (!sd.dly_td_lst_pri_lst.isEmpty()) {
            lst_pri = sd.dly_td_lst_pri_lst.get(sd.dly_td_lst_pri_lst.size() - forDay - 1);
            log.info("Got lst_pri:" + lst_pri + " for stock:" + id + " for day:" + forDay);
            return lst_pri;
        }
        return lst_pri;
    }
    //forDay = 0 means yesterday's deal qty.
    public Integer getDl_stk_num(int forDay) {
        Integer dl_stk_num = null;
        if (!sd.dly_dl_stk_num_lst.isEmpty()) {
            dl_stk_num = sd.dly_dl_stk_num_lst.get(sd.dly_dl_stk_num_lst.size() - forDay - 1);
            log.info("Got dl_stk_num:" + dl_stk_num + " for stock:" + id + " for day:" + forDay);
            return dl_stk_num;
        }
        return dl_stk_num;
    }
    //forDay = 0 means yesterday's deal money.
    public Double getDl_mny_num(int forDay) {
        Double dl_stk_mny = null;
        if (!sd.dly_dl_mny_num_lst.isEmpty()) {
            dl_stk_mny = sd.dly_dl_mny_num_lst.get(sd.dly_dl_mny_num_lst.size() - forDay - 1);
            log.info("Got dl_stk_mny:" + dl_stk_mny + " for stock:" + id + " for day:" + forDay);
            return dl_stk_mny;
        }
        return dl_stk_mny;
    }
}
