package com.sn.strategy.algorithm.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.strategy.algorithm.IBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.DefaultBuyPointSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;

public class MacdBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(MacdBuyPointSelector.class);


    private boolean sim_mode;
    private String selector_name = "MacdBuyPointSelector";
    
    
    public MacdBuyPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {

		int s = 2, l = 6, m = 4;

		MACD macd = new MACD(s, l, m, stk);

		if (macd.DIF == null || macd.DEF == null || macd.MACD == null) {
			log.info("MACD is not calculatable, returned false");
			return false;
		}
		
		log.info("MacdBuyPointSelector:" + stk.getID() + "/" + stk.getName() + " at time:" + stk.getDl_dt() + ", DIF:" + macd.DIF + ", DEF:" + macd.DEF + ", (macd.DIF - macd.DEF) > 0 && macd.DIF * macd.DEF < 0 ? " + ((macd.DIF - macd.DEF) > 0 && macd.DIF * macd.DEF < 0 && Math.abs(macd.DIF) > 0.001));
		if ((macd.DIF - macd.DEF) > 0 && macd.DIF * macd.DEF < 0 && Math.abs(macd.DIF) > 0.001) {
            stk.setTradedBySelector(this.selector_name);
            stk.setTradedBySelectorComment("DIF:" + macd.DIF + ", DEF:" + macd.DEF + ", (macd.DIF - macd.DEF) > 0 && macd.DIF * macd.DEF < 0 && Math.abs(macd.DIF) > 0.001? " + ((macd.DIF - macd.DEF) > 0 && macd.DIF * macd.DEF < 0 && Math.abs(macd.DIF) > 0.001));
			return true;
		}
		return false;
	}


	@Override
	public int getBuyQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        double useableMny = 0;
        int buyMnt = 0;
        int maxMnt = 0;
        
        if (ac != null) {
            useableMny = ac.getMaxMnyForTrade();
            maxMnt = (int)(useableMny/s.getCur_pri()) / 100 * 100;
            
           	buyMnt = maxMnt;
            
           	if (!sim_mode)
           	{
           	    int sellableAmt = ac.getSellableAmt(s.getID(), null);
                if (buyMnt > sellableAmt)
                {
                    log.info("Tradex sellable amount:" + sellableAmt + " less than calculated amt:" + buyMnt + " use sellabeAmt.");
                    buyMnt = sellableAmt;
                }
           	}
            log.info("getBuyQty, useableMny:" + useableMny + " buyMnt:" + buyMnt + " maxMnt:" + maxMnt);
        }
        else {
        	if (s.getCur_pri() <= 10) {
        		buyMnt = 200;
        	}
        	else {
        		buyMnt = 100;
        	}
        	log.info("getBuyQty, cur_pri:" + s.getCur_pri() + " buyMnt:" + buyMnt);
        }
		return buyMnt;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
