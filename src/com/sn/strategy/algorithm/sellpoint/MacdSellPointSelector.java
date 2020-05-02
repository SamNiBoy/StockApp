package com.sn.strategy.algorithm.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.strategy.algorithm.ISellPointSelector;
import com.sn.strategy.algorithm.buypoint.DefaultBuyPointSelector;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.stock.Stock2;
import com.sn.stock.indicator.MACD;

public class MacdSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(MacdSellPointSelector.class);

    private boolean sim_mode;
    private String selector_name = "MacdSellPointSelector";
    
    
    public MacdSellPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock2 stk, ICashAccount ac) {

		int s = 6, l = 30, m = 5;

		MACD macd = new MACD(s, l, m, stk);

		if (macd.DIF == null || macd.DEF == null || macd.MACD == null) {
			log.info("MACD is not calculatable, returned false");
			return false;
		}
		log.info("MacdSellPointSelector:" + stk.getID() + "/" + stk.getName() + " at time:" + stk.getDl_dt() + ", DIF:" + macd.DIF + ", DEF:" + macd.DEF + ", (macd.DIF - macd.DEF) < 0 && macd.DIF * macd.DEF < 0 && Math.abs(macd.DIF) > 0.001 ? " + ((macd.DIF - macd.DEF) < 0 && macd.DIF * macd.DEF < 0 && Math.abs(macd.DIF) > 0.001));
		if ((macd.DIF - macd.DEF) < 0 && macd.DIF * macd.DEF < 0 && Math.abs(macd.DIF) > 0.001) {
            stk.setTradedBySelector(this.selector_name);
            stk.setTradedBySelectorComment("DIF:" + macd.DIF + ", DEF:" + macd.DEF + ", (macd.DIF - macd.DEF) < 0 && macd.DIF * macd.DEF < 0 && Math.abs(macd.DIF) > 0.001? " + ((macd.DIF - macd.DEF) < 0 && macd.DIF * macd.DEF < 0 && Math.abs(macd.DIF) > 0.001));
			return true;
		}
		return false;
	}

	@Override
	public int getSellQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
        int sellMnt = 0;
        
        if (ac != null) {
            int sellableAmt = (int) (ac.getMaxMnyForTrade() / s.getCur_pri());
            sellMnt =  sellableAmt - sellableAmt % 100;
            
            if (!sim_mode)
            {
                  int tradeLocal = ParamManager.getIntParam("TRADING_AT_LOCAL", "TRADING", null);
                  if (tradeLocal == 1)
                  {
                      log.info("Trade at local sellable amount is zero, user max mny per trade to get the qty:" + sellMnt);
                  }
                  else
                  {
                       sellableAmt = ac.getSellableAmt(s.getID(), null);
                       if (sellMnt > sellableAmt)
                       {
                            log.info("Tradex sellable amount:" + sellableAmt + " less than calculated amt:" + sellMnt + " use sellabeAmt.");
                            sellMnt = sellableAmt;
                       }
                  }
             }
             log.info("getSellQty, sellableAmt:" + sellableAmt + " sellMnt:" + sellMnt);
        }
        else {
        	if (s.getCur_pri() <= 10) {
        		sellMnt = 200;
        	}
        	else {
        		sellMnt = 100;
        	}
        	log.info("getSellQty, cur_pri:" + s.getCur_pri() + " sellMnt:" + sellMnt);
        }
		return sellMnt;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
