package com.sn.sim.strategy.selector.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;

public class MacdBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(MacdBuyPointSelector.class);


    private boolean sim_mode;
    
    
    MacdBuyPointSelector(boolean sm)
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
		if (macd.DIF > macd.DEF && macd.DEF < -0.001 && macd.DIF - macd.DEF < 0.004) {
			log.info("MACD good, buy it.");
			return true;
		}

		log.info("MACD returned false for buy.");
		return false;
	}


	@Override
	public int getBuyQty(Stock2 s, ICashAccount ac) {
        // TODO Auto-generated method stub
        double useableMny = 0;
        int buyMnt = 0;
        int maxMnt = 0;
        
        if (ac != null) {
            useableMny = ac.getMaxAvaMny();
            maxMnt = (int)(useableMny/s.getCur_pri()) / 100 * 100;
            
            if (maxMnt >= 400) {
                buyMnt = maxMnt / 2;
                buyMnt = buyMnt - buyMnt % 100;
            }
            else {
                buyMnt = maxMnt;
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
