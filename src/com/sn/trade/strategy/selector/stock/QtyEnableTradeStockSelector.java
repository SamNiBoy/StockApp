package com.sn.trade.strategy.selector.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.imp.TradeStrategyImp;

public class QtyEnableTradeStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(QtyEnableTradeStockSelector.class);
    double tradeThresh = STConstants.BASE_TRADE_THRESH;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock stk, ICashAccount ac) {
        Double maxPri = stk.getMaxDlyClsPri();
        Double minPri = stk.getMinDlyClsPri();
        Double yt_cls_pri = stk.getYtClsPri();
        Double cur_pri = stk.getCur_pri();

        if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {

            double maxPct = (maxPri - minPri) / yt_cls_pri;
            double curPct =(cur_pri - minPri) / yt_cls_pri;

            boolean dlQtyPlused = stk.isDlyDlQtyPlused();
            
            log.info("maxPct:" + maxPct + ", tradeThresh:" + tradeThresh + ", curPct:" + curPct + ", dlQtyPlused:" + dlQtyPlused);
            
            if (maxPct >= tradeThresh && curPct < maxPct * 1.0 / 10.0 && dlQtyPlused && stk.isLstDlyClsPriTurnaround(true)) {
                log.info("QtyEnableTradeStockSelector true says Check Buy:" + stk.getDl_dt() + " stock:" + stk.getID()
                        + " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " curPri:" + cur_pri);
                return true;
            }
            else {
                log.info("QtyEnableTradeStockSelector false Check Buy:" + stk.getDl_dt() + " stock:" + stk.getID()
                        + " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " curPri:" + cur_pri + " tradeThresh:" + tradeThresh);
            }
        } else {
            log.info("QtyEnableTradeStockSelector says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
        }
        return false;
    }
    
	@Override
	public boolean isORCriteria() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isMandatoryCriteria() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		if (harder) {
		    tradeThresh += 0.02;
		    if (tradeThresh > 0.1) {
		        tradeThresh = 0.1;
		    }
		}
		else {
		    tradeThresh -= 0.02;
	          if (tradeThresh < 0.01) {
	              tradeThresh = 0.01;
	          }
		}
		log.info("try harder:" + harder + ", tradeThresh:" + tradeThresh);
		return true;
	}
}
