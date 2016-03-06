package com.sn.sim.strategy.selector.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.stock.Stock;
import com.sn.stock.Stock2;

public class DefaultSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(DefaultSellPointSelector.class);

	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock2 s, ICashAccount ac) {
		int periods = 5;
		int upTimes = 4;

		if (ac != null) {
			boolean hasStockInHand = ac.hasStockInHand(s);
			double inhandPri = ac.getInHandStockCostPrice(s);

			if (hasStockInHand && (s.getCur_pri() - inhandPri) / inhandPri > 0.03) {
				log.info("returned true");
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
}
