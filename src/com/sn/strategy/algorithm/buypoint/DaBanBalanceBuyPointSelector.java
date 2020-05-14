package com.sn.strategy.algorithm.buypoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.STConstants;
import com.sn.strategy.TradeStrategyImp;
import com.sn.strategy.algorithm.IBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.DefaultBuyPointSelector;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.sellmode.SellModeWatchDog;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trader.StockTrader;

public class DaBanBalanceBuyPointSelector implements IBuyPointSelector {

	static Logger log = Logger.getLogger(DaBanBalanceBuyPointSelector.class);
	
    private String selector_name = "DaBanBalanceBuyPointSelector";
    private String selector_comment = "";

	private boolean sim_mode = false;
    
	public DaBanBalanceBuyPointSelector(boolean sm)
	{
	    sim_mode = sm;
	}
    
	@Override
	public boolean isGoodBuyPoint(Stock2 stk, ICashAccount ac) {
		log.info("DaBanBalanceBuyPointSelector should never buy.");
		return false;
	}
	
	@Override
	public int getBuyQty(Stock2 s, ICashAccount ac) {
	    return 0;
	}

    public boolean isSimMode() {
        // TODO Auto-generated method stub
        return sim_mode;
    }
    

}
