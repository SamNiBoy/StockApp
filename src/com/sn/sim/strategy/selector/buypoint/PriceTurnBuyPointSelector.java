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

public class PriceTurnBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(PriceTurnBuyPointSelector.class);


	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {

        if ((ac != null && !ac.hasStockInHand(stk)) || ac == null) {
            if (stk.priceUpAfterSharpedDown(10, 6)) {
                log.info("isGoodBuyPoint true as price goes up after 6/10 times down!");
                return true;
            }
            else {
                log.info("isGoodBuyPoint false as price goes up after 6/10 times down!");
            }
        } else {
            // has stock in hand;
            Double lstBuy = ac.getLstBuyPri(stk);
            Double cur_pri = stk.getCur_pri();
            Double yt_cls_pri = stk.getYtClsPri();
            if (lstBuy != null && cur_pri != null && yt_cls_pri != null) {
                if ((lstBuy - cur_pri) / yt_cls_pri > 0.05 && stk.priceUpAfterSharpedDown(10, 6)) {
                    log.info("isGoodBuyPoint Buy true as price up after 6/10 times down:" + stk.getDl_dt() + " stock:" + stk.getID() + " lstBuyPri:"
                            + lstBuy + " curPri:" + cur_pri + " yt_cls_pri:" + yt_cls_pri);
                    return true;
                }
            }
        }
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
}
