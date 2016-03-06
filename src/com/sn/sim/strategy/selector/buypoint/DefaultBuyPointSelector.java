package com.sn.sim.strategy.selector.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.selector.stock.DefaultStockSelector;
import com.sn.stock.Stock;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;

public class DefaultBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(DefaultBuyPointSelector.class);

	/**
	 * @param args
	 */
	public boolean isGoodBuyPoint(Stock2 s, ICashAccount ac) {
		int periods = 5;
		int ratio = 10;
		int downTimes = 3;

		if (StockMarket.isMarketTooCold(s.getDl_dt()) && !StockMarket.hasMoreIncStock()) {
			log.info("returned false because market is too cool.");
			return false;
		}

		if (ac != null) {
			boolean hasStockInHand = ac.hasStockInHand(s);
			double inhandPri = ac.getInHandStockCostPrice(s);
			// If stock price goes down then previous bought, bug again.
			if (hasStockInHand && (inhandPri - s.getCur_pri()) / inhandPri >= 0.03) {
				log.info("buy more as price goes down after bought.");
				return true;
			}
		}
		
		if (s.getSd().getAvgYtClsPri(3) < s.getCur_pri()) {
			log.info("Past 3 days close pri lower then cur pri:" + s.getCur_pri() + " good to buy, return true.");
			return true;
		}

		log.info("returned false for isGoodBuyPoint");
		return false;
	}
}
