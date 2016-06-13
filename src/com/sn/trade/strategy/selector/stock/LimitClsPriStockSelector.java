package com.sn.trade.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.trade.strategy.imp.TradeStrategyImp;

public class LimitClsPriStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(LimitClsPriStockSelector.class);
    int longPrd = 20;
    int midPrd = 10;
    int shortPrd = 5;
    double ALLOW_INC_THRESH_VALUE = 0.05;
    double MIN_PCT_YT_PRI = 0.8;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
    	double shtAvgPri = s.getAvgYtClsPri(shortPrd, 0);
    	double midAvgPri = s.getAvgYtClsPri(midPrd, 0);
    	double longAvgPri = s.getAvgYtClsPri(longPrd, 0);
    	Double yt_lst_pri = s.getLst_pri(0);
    	Double yt_hst_pri = s.getHst_pri(0);
    	Double yt_opn_pri = s.getOpen_pri(0);
    	Double yt_cls_pri = s.getCls_pri(0);
    	
    	//If the stock price sharply increased by ALLOW_INC_THRESH_VALUE, not suggest this stock.
    	if (shtAvgPri > midAvgPri && midAvgPri > longAvgPri && (shtAvgPri - longAvgPri) / longAvgPri < ALLOW_INC_THRESH_VALUE) {
    		log.info("stock: " + s.getID() + " shtAvgPri:" + shtAvgPri
    		        + " midAvgPri:" + midAvgPri + " longAvgPri:" + longAvgPri
    		        + ", ALLOW_INC_THRESH_VALUE:" + ALLOW_INC_THRESH_VALUE
    		        + ", MIN_PCT_YT_PRI:" + MIN_PCT_YT_PRI);
    		
    		double pct = 0;
    		if (yt_lst_pri != null &&
                yt_hst_pri != null &&
                yt_opn_pri != null &&
                yt_cls_pri != null) {
    		    pct = (yt_cls_pri - yt_opn_pri) / (yt_hst_pri - yt_lst_pri);
    		}
    		
    		log.info("Is yt_cls_pri:" + yt_cls_pri == null ? "null" : yt_cls_pri
    		        + ", yt_opn_pri: " + yt_opn_pri == null ? "null" : yt_opn_pri
    		        + ", yt_lst_pri: " + yt_lst_pri == null ? "null" : yt_lst_pri
    		        + ", yt_hst_pri: " + yt_hst_pri == null ? "null" : yt_hst_pri
    		        + " and up cross midAvgPri: " + midAvgPri + "pct:" + pct);
    		
    		if (yt_lst_pri != null &&
    		    yt_hst_pri != null &&
    		    yt_opn_pri != null &&
    		    yt_cls_pri != null &&
    		    yt_cls_pri > yt_opn_pri &&
    		    midAvgPri > yt_lst_pri &&
    		    yt_cls_pri > midAvgPri &&
    		    pct >= MIN_PCT_YT_PRI) {
    		    return true;
    		}
    	}
    	log.info("stock: " + s.getID() + " shtAvgPri:" + shtAvgPri + " midAvgPri:" + midAvgPri + " longAvgPri:" + longAvgPri + ", ALLOW_INC_THRESH_VALUE: " + ALLOW_INC_THRESH_VALUE + ", return false");
        return false;
    }
	@Override
	public boolean isORCriteria() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isMandatoryCriteria() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		if (!harder) {
		    ALLOW_INC_THRESH_VALUE += 0.02;
		    if (ALLOW_INC_THRESH_VALUE > 0.1) {
		        ALLOW_INC_THRESH_VALUE = 0.1;
		    }
            MIN_PCT_YT_PRI -= 0.02;
            if (MIN_PCT_YT_PRI < 0.5) {
                MIN_PCT_YT_PRI = 0.5;
            }
		}
		else {
	          ALLOW_INC_THRESH_VALUE -= 0.02;
	          if (ALLOW_INC_THRESH_VALUE < 0.01) {
	              ALLOW_INC_THRESH_VALUE = 0.01;
	          }
	          MIN_PCT_YT_PRI += 0.02;
	          if (MIN_PCT_YT_PRI > 0.9) {
	              MIN_PCT_YT_PRI = 0.9;
	          }
		}
		log.info("try harder:" + harder + ", ALLOW_INC_THRESH_VALUE:" + ALLOW_INC_THRESH_VALUE);
		return true;
	}
}
