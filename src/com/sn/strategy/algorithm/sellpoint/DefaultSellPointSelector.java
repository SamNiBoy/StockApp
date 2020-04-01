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
import com.sn.stock.Stock2;

public class DefaultSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(DefaultSellPointSelector.class);

    private boolean sim_mode;
    
    
    public DefaultSellPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock2 s, ICashAccount ac) {
		int periods = 5;
		int upTimes = 4;

		if (ac != null) {
			boolean hasStockInHand = ac.hasStockInHand(s);
			//double inhandPri = ac.getInHandStockCostPrice(s);
			double lstbuypri = ac.getLstBuyPri(s);

			if (hasStockInHand && ((s.getCur_pri() - lstbuypri) / lstbuypri > 0.02 || (s.getCur_pri() - lstbuypri) / lstbuypri < -0.01)) {
				if (s.getCur_pri() - lstbuypri > 0) {
				log.info("good sell point because prices goes higher, sell it.");
				}
				else if (s.getCur_pri() - lstbuypri < 0) {
					log.info("good sell point because prices goes downer, sell it.");
			    }
				return true;
			}
			else if (!hasStockInHand) {
				log.info("No stock inhand, can not sell, return false as sellpoint.");
				return false;
			}
		}

		log.info("returned false");
		return false;
	}

	@Override
	public int getSellQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
		return 100;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
