package com.sn.trade.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.trade.strategy.imp.TradeStrategyImp;

public class PriceStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(PriceStockSelector.class);
    double HighestPrice = 40;
    double LowestPrice = 10;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
        Double curPri = s.getCur_pri();
        if (curPri != null && curPri <= HighestPrice && curPri >= LowestPrice) {
             log.info("returned true because price " + curPri + " is in [" + LowestPrice + "," + HighestPrice +"]");
             return true;
        }
        log.info("returned false for PriceStockSelector");
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
		log.info("Try " + (harder ? "Harder" : "Loose") + " criteria for HighestPrice " + HighestPrice);
		if (harder) {
			if (HighestPrice < 5) {
				log.info("HighestPrice can not less than 5");
				return false;
			}
			else {
			    HighestPrice = HighestPrice * 0.9;
			}
		}
		else {
			if (HighestPrice > 50) {
				log.info("HighestPrice can not more than 50");
				return false;
			}
			else {
			    HighestPrice = HighestPrice * 1.1;
			}
		}
		log.info("HighestPrice is now: " + HighestPrice);
		return false;
	}
}
