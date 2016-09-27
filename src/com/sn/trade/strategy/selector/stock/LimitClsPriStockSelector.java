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
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.imp.TradeStrategyImp;

public class LimitClsPriStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(LimitClsPriStockSelector.class);
    int longPrd = 14;
    int midPrd = 7;
    int shortPrd = 3;
    double ALLOW_INC_THRESH_VALUE = 0.05;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock s, ICashAccount ac) {
    	double shtAvgPri = s.getAvgYtClsPri(shortPrd, 0);
    	double midAvgPri = s.getAvgYtClsPri(midPrd, 0);
    	double longAvgPri = s.getAvgYtClsPri(longPrd, 0);
    	
    	//If the stock price sharply increased by ALLOW_INC_THRESH_VALUE, not suggest this stock.
    	if (shtAvgPri > midAvgPri && midAvgPri > longAvgPri && (shtAvgPri - longAvgPri) / longAvgPri < ALLOW_INC_THRESH_VALUE) {
            log.info("stock: " + s.getID() + " shtAvgPri:" + shtAvgPri
                    + " midAvgPri:" + midAvgPri + " longAvgPri:" + longAvgPri
                    + ", ALLOW_INC_THRESH_VALUE:" + ALLOW_INC_THRESH_VALUE);
            return true;
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
		}
		else {
	          ALLOW_INC_THRESH_VALUE -= 0.02;
	          if (ALLOW_INC_THRESH_VALUE < 0.01) {
	              ALLOW_INC_THRESH_VALUE = 0.01;
	          }
		}
		log.info("try harder:" + harder + ", ALLOW_INC_THRESH_VALUE:" + ALLOW_INC_THRESH_VALUE);
		return true;
	}

    @Override
    public Integer getTradeModeId() {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
	public boolean shouldStockExitTrade(String s) {
		// TODO Auto-generated method stub
		return false;
	}
}
