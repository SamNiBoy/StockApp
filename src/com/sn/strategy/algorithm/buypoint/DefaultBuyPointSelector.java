package com.sn.strategy.algorithm.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.task.suggest.selector.DefaultStockSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.IBuyPointSelector;

public class DefaultBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(DefaultBuyPointSelector.class);
    private boolean sim_mode;
    
    
    DefaultBuyPointSelector(boolean sm)
    {
        sim_mode = sm;
    }

	/**
	 * @param args
	 */
	public boolean isGoodBuyPoint(Stock2 s, ICashAccount ac) {
		int periods = 5;
		int ratio = 10;
		int downTimes = 3;

		if (StockMarket.isMarketTooCold(s.getDl_dt())) {
			log.info("returned false because market is too cool.");
			return false;
		}

		if (ac != null) {
			boolean hasStockInHand = ac.hasStockInHand(s);
			Double lstbuypri = ac.getLstBuyPri(s);
			// If stock price goes down then previous bought, bug again.
			if (hasStockInHand && (lstbuypri - s.getCur_pri()) / lstbuypri >= 0.02) {
				log.info("buy more as price goes down after bought.");
				return true;
			}
			else if (!hasStockInHand && (s.getOpen_pri() - s.getCur_pri()) / lstbuypri >= 0.02) {
				log.info("buy more as price goes down after open pri.");
				return true;
			}
		}
		
		if (s.getSd().getAvgYtClsPri(3, 0) > s.getCur_pri()) {
			log.info("Past 3 days close pri lower then cur pri:" + s.getCur_pri() + " good to buy, return true.");
			return true;
		}

		log.info("returned false for isGoodBuyPoint");
		return false;
	}

	@Override
	public int getBuyQty(Stock2 s, ICashAccount ac) {
		// TODO Auto-generated method stub
		return 100;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
}
