package com.sn.sim.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.imp.TradeStrategyImp;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class AvgClsPriStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(AvgClsPriStockSelector.class);
    int longPrd = 21;
    int midPrd = 14;
    int shortPrd = 7;
    /**
     * @param args
     */
    public boolean isGoodStock(Stock2 s, ICashAccount ac) {
    	double yt_shtAvgPri = s.getAvgYtClsPri(shortPrd, shortPrd / 2);
    	double yt_midAvgPri = s.getAvgYtClsPri(midPrd, shortPrd / 2);
    	double yt_longAvgPri = s.getAvgYtClsPri(longPrd, shortPrd / 2);
    	double shtAvgPri = s.getAvgYtClsPri(shortPrd, 0);
    	double midAvgPri = s.getAvgYtClsPri(midPrd, 0);
    	double longAvgPri = s.getAvgYtClsPri(longPrd, 0);
    	if (shtAvgPri > midAvgPri && midAvgPri > longAvgPri && yt_shtAvgPri < yt_midAvgPri && yt_midAvgPri < yt_longAvgPri) {
    		log.info("stock: " + s.getID() + " shtAvgPri:" + shtAvgPri + " midAvgPri:" + midAvgPri + " longAvgPri:" + longAvgPri + " all goes increase, and with yts...");
    		log.info("stock: " + s.getID() + " yt_shtAvgPri:" + yt_shtAvgPri + " yt_midAvgPri:" + yt_midAvgPri + " yt_longAvgPri:" + yt_longAvgPri + " all goes desrease, return true");
    		return true;
    	}
    	log.info("stock: " + s.getID() + " shtAvgPri:" + shtAvgPri + " midAvgPri:" + midAvgPri + " longAvgPri:" + longAvgPri + " all goes descrease, return false");
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
		if (harder) {
			longPrd++;
			midPrd++;
			shortPrd++;
			if (longPrd >= 60) {
				longPrd = 60;
			}
			if (midPrd >= 30) {
				midPrd = 30;
			}
			if (shortPrd >= 20) {
				shortPrd = 20;
			}
		}
		else {
			longPrd--;
			midPrd--;
			shortPrd--;
			if (longPrd <= 7) {
				longPrd = 7;
			}
			if (midPrd <= 3) {
				midPrd = 3;
			}
			if (shortPrd <= 2) {
				shortPrd = 2;
			}
		}
		return true;
	}
}
