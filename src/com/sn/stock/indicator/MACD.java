package com.sn.stock.indicator;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.Stock2;

public class MACD {

	static Logger log = Logger.getLogger(MACD.class);
	// short ,long, mid periods
	int s, l, m;
	Stock2 stk = null;

	public Double DIF, DEF, MACD;

	public MACD(int sht, int lng, int mid, Stock2 stk1) {
		s = sht;
		l = lng;
		m = mid;
		stk = stk1;
		DIF = DEF = MACD = null;
		calMACD();
	}

	private boolean calMACD() {
		int sz = stk.getSd().getCur_pri_lst().size();
		List<Double> prilst = stk.getSd().getCur_pri_lst();
		int sz2 = stk.getSd().getYt_cls_pri_lst().size();
		double yt_cls_pri = stk.getSd().getYt_cls_pri_lst().get(sz2 - 1);
		if (sz < l + m || yt_cls_pri <= 0) {
			log.info("No MACD calculated success because of less data!");
			return false;
		}

		List<Double> difLst = new ArrayList<Double>(m);
		for (int k = 0; k < m; k++) {
			double avgShtPri = 0;
			for (int i = 0; i < s; i++) {
				avgShtPri += prilst.get(sz - i - 1 - k) / yt_cls_pri;
			}

			avgShtPri /= s;

			double avgLngPri = 0;
			for (int i = 0; i < l; i++) {
				avgLngPri += prilst.get(sz - i - 1 - k) / yt_cls_pri;
			}

			avgLngPri /= l;

			double dif = avgShtPri - avgLngPri;
			difLst.add(dif);
		}

		double def = 0;
		for (int j = 0; j < difLst.size(); j++) {
			def += difLst.get(j);
		}

		if (difLst.size() > 0) {
			DIF = difLst.get(0);
			DEF = def / difLst.size();
			MACD = 2 * (DEF - DIF);
			log.info("Got MACD: dif[" + DIF + "] DEF[" + DEF + "] MACD[" + MACD + "] for stock:" + stk.getID());
			return true;
		}
		else
		{
			log.info("should not possible, No MACD calculated success because of less data!");
			return false;
		}
	}

}
