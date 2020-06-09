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
import com.sn.task.suggest.SuggestStock;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class PriceStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(PriceStockSelector.class);
    double HighestPrice = ParamManager.getFloatParam("MAX_PRICE_FOR_SUGGEST", "SUGGESTER", null);
    double LowestPrice = ParamManager.getFloatParam("MIN_PRICE_FOR_SUGGEST", "SUGGESTER", null);
    private String on_dte = "";
    private String suggest_by = "PriceStockSelector";
    
    public PriceStockSelector (String s) {
    	on_dte = s;
    }
    
    /**
     * @param args
     */
    public boolean isTargetStock(Stock2 s, ICashAccount ac) {
    	
    	// skip startup stock which starts with '300'
    	
    	if (s.getID().startsWith("300")) {
    		log.info("skip startup stock:" + s.getID());
    		return false;
    	}
    	
    	if (s.getName().indexOf("ST") >= 0) {
    		log.info("skip ST stock:" + s.getID());
    		return false;
    	}
    	
//    	if (SuggestStock.calculateStockTrend(s.getID()) <= 0)
//    	{
//    		log.info("skip stock:" + s.getID() + "/" + s.getName() + " as trend is going down.");
//    		return false;
//    	}
    	
    	Double curpri = s.getCur_pri();
    	
    	if (curpri == null) {
    		s.getSd().LoadData(on_dte, on_dte);
    	}
    	
    	curpri = s.getCur_pri();
    	
        if (curpri != null && curpri <= HighestPrice && curpri >= LowestPrice) {
                    log.info("returned true because price is <= " + HighestPrice + " and >= LowestPrice:" + LowestPrice);
                    s.setSuggestedBy(this.suggest_by);
                    s.setSuggestedComment("Price is in range[" + LowestPrice + "," + HighestPrice + "]");
                    s.setSuggestedscore(0);
                    return true;
        }
        log.info("stock:" + s.getID() + " returned false for PriceStockSelector with price:" + curpri + " not in range[" + LowestPrice + "," + HighestPrice + "]");
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
