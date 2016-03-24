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

public class KeepGainStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(KeepGainStockSelector.class);
    int days = 3;
    double dayPct = 0.01;
    /**
     * @param args
     */
    public boolean isGoodStock(Stock2 s, ICashAccount ac) {
        if (s.getSd().keepDaysClsPriGain(days, dayPct)) {
             log.info("returned true because keep " + days + " days gain " + dayPct);
             return true;
        }
        log.info("returned false for isGoodStock()");
        return false;
    }
	@Override
	public boolean isORCriteria() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean isMandatoryCriteria() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		if (harder) {
			if (days >= 5) {
				log.info("days can not more than 5");
			}
			else {
			    days++;
			}
			if (dayPct >= 0.03) {
				log.info("dayPct can not more than 0.03");
			}
			else {
			    dayPct += dayPct/10;
			}
		}
		else {
			if (days <= 3) {
				log.info("days can not less than 3");
			}
			else {
				days--;
			}
			if (dayPct <= 0.001) {
				log.info("dayPct can not less than 0.001");
			}
			else {
				dayPct -= dayPct/10;
			}
		}
		log.info("try " + (harder ? " harder" : " loose") + " days:" + days + " dayPct:" + dayPct);
		return true;
	}
}
