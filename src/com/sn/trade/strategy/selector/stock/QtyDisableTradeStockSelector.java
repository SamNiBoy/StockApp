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

public class QtyDisableTradeStockSelector implements IStockSelector {

    static Logger log = Logger.getLogger(QtyDisableTradeStockSelector.class);
    double tradeThresh = 0.3;
    double THRESH_PCT = 0.9;
    /**
     * @param args
     */
    public boolean isTargetStock(Stock stk, ICashAccount ac) {
        Double maxPri = stk.getMaxDlyTdClsPri();
        Double minPri = stk.getMinDlyTdClsPri();
        Double yt_cls_pri = stk.getTdCls_pri(0);
        Double cur_pri = stk.getCur_pri();

        if (maxPri != null && minPri != null && yt_cls_pri != null && cur_pri != null) {

            double maxPct = (maxPri - minPri) / yt_cls_pri;
            double clsPct =(yt_cls_pri - minPri) / yt_cls_pri;

            boolean dlQtyPlused = stk.isDlyDlQtyPlused();
            
            log.info("Is maxPct:" + maxPct + ">= tradeThresh:" + tradeThresh
            		+ " and clsPct:" + clsPct + ">= maxPct:" + maxPct + " * THRESH_PCT:" + THRESH_PCT
            		+ " and dlQtyPlused:" + dlQtyPlused);
            
            if (maxPct >= tradeThresh && clsPct >= maxPct * THRESH_PCT && dlQtyPlused && stk.isLstDlyClsPriTurnaround(false)) {
                log.info("QtyDisableTradeStockSelector true Check DisableTrade:" + stk.getDl_dt() + " stock:" + stk.getID()
                        + " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " clsPct:" + clsPct);
                return true;
            }
            else {
                log.info("QtyDisableTradeStockSelector false Check DisableTrade:" + stk.getDl_dt() + " stock:" + stk.getID()
                        + " maxPri:" + maxPri + " minPri:" + minPri + " maxPct:" + maxPct + " clsPct:" + clsPct + " tradeThresh:" + tradeThresh);
            }
        } else {
            log.info("QtyDisableTradeStockSelector says either maxPri, minPri, yt_cls_pri or cur_pri is null, return false");
        }
        return false;
    }
    
	@Override
	public boolean isORCriteria() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean isMandatoryCriteria() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean adjustCriteria(boolean harder) {
		// TODO Auto-generated method stub
		log.info("try harder:" + harder + ", tradeThresh:" + tradeThresh + ", THRESH_PCT:" + THRESH_PCT);
		return true;
	}

    @Override
    public Integer getTradeModeId() {
        // TODO Auto-generated method stub
        return null;
    }
}
