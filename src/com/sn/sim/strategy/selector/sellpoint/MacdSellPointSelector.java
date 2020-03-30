package com.sn.sim.strategy.selector.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.selector.ISellPointSelector;
import com.sn.sim.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.stock.Stock2;
import com.sn.stock.indicator.MACD;

public class MacdSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(MacdSellPointSelector.class);

    private boolean sim_mode;
    
    
    public MacdSellPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock2 stk, ICashAccount ac) {

		int s = 2, l = 6, m = 4;

		MACD macd = new MACD(s, l, m, stk);

		if (macd.DIF == null || macd.DEF == null || macd.MACD == null) {
			log.info("MACD is not calculatable, returned false");
			return false;
		}
		if (macd.DIF > macd.DEF && macd.DEF > 0.01) {
			log.info("MACD good, sell it.");
			return true;
		}

		log.info("MACD returned false");
		return false;
	}

	@Override
	public int getSellQty(Stock2 s, ICashAccount ac) {
        // TODO Auto-generated method stub
        int sellMnt = 0;
        
        if (ac != null) {
            String dt = s.getDl_dt().toString().substring(0, 10);
            int sellableAmt = ac.getSellableAmt(s.getID(), dt);
            
            if (sellableAmt >= 400) {
                sellMnt = sellableAmt / 2;
                sellMnt = sellMnt - sellMnt % 100;
            }
            else {
                sellMnt = sellableAmt;
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
