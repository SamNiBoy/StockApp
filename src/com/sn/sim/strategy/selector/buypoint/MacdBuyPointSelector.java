package com.sn.sim.strategy.selector.buypoint;

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
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;

public class MacdBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(MacdBuyPointSelector.class);


	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {

		int s = 12, l = 26, m = 9;

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
}
