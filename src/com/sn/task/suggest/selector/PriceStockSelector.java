package com.sn.task.suggest.selector;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.strategy.TradeStrategyImp;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.IStockSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class PriceStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(PriceStockSelector.class);
    double HighestPrice = ParamManager.getFloatParam("MAX_PRICE_FOR_SUGGEST", "SUGGESTER", null);
    double LowestPrice = ParamManager.getFloatParam("MIN_PRICE_FOR_SUGGEST", "SUGGESTER", null);
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	
    	Double curpri = s.getCur_pri();
    	
    	if (curpri == null) {
    		s.getSd().LoadData();
    	}
    	
    	curpri = s.getCur_pri();
    	
        if (curpri != null && curpri <= HighestPrice && curpri >= LowestPrice) {
                    log.info("returned true because price is <= " + HighestPrice + " and >= LowestPrice:" + LowestPrice);
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
		return false;
	}
}
