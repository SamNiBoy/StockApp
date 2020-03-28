package com.sn.sim.strategy.selector.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.work.task.SellModeWatchDog;

public class PriceTurnSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(PriceTurnSellPointSelector.class);

	private double BASE_TRADE_THRESH = 0.03;
	Map<String, Boolean> preSellMode = new HashMap<String, Boolean>();
    
    private boolean sim_mode;
    
    
    public PriceTurnSellPointSelector(boolean sm)
    {
        sim_mode = sm;
    }
    
	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock2 stk, ICashAccount ac) {

        if ((ac != null && !ac.hasStockInHand(stk)) || ac == null) {
            if (stk.priceDownAfterSharpedUp(10, 6)) {
                log.info("isGoodBuyPoint true as price goes down after 6/10 times up!");
                return true;
            }
            else {
                log.info("isGoodBuyPoint false as price goes down after 6/10 times up!");
            }
        } else {
            // has stock in hand;
            Double lstBuy = ac.getLstBuyPri(stk);
            Double cur_pri = stk.getCur_pri();
            Double yt_cls_pri = stk.getYtClsPri();
            if (lstBuy != null && cur_pri != null && yt_cls_pri != null) {
                if ((cur_pri - lstBuy) / yt_cls_pri > 0.05 && stk.priceDownAfterSharpedUp(10, 6)) {
                    log.info("isGoodBuyPoint Buy true as price down after 6/10 times up:" + stk.getDl_dt() + " stock:" + stk.getID() + " lstBuyPri:"
                            + lstBuy + " curPri:" + cur_pri + " yt_cls_pri:" + yt_cls_pri);
                    return true;
                }
            }
        }
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
