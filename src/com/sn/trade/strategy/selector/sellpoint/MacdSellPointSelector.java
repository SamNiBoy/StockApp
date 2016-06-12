package com.sn.trade.strategy.selector.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.indicator.MACD;
import com.sn.trade.strategy.selector.buypoint.DefaultBuyPointSelector;

public class MacdSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(MacdSellPointSelector.class);

	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock stk, ICashAccount ac) {

		int s = 12, l = 26, m = 9;

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
	public int getSellQty(Stock s, ICashAccount ac) {
		// TODO Auto-generated method stub
		return 100;
	}
}
