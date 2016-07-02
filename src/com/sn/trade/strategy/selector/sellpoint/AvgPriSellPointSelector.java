package com.sn.trade.strategy.selector.sellpoint;

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
import com.sn.stock.Stock;
import com.sn.stock.StockMarket;
import com.sn.stock.indicator.MACD;
import com.sn.trade.strategy.imp.STConstants;
import com.sn.trade.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.trade.strategy.selector.stock.AvgPriSellModeSelector;
import com.sn.work.task.SellModeWatchDog;

public class AvgPriSellPointSelector implements ISellPointSelector {

	static Logger log = Logger.getLogger(AvgPriSellPointSelector.class);

	/**
	 * @param args
	 */
	public boolean isGoodSellPoint(Stock stk, ICashAccount ac) {
	    AvgPriSellModeSelector selector = new AvgPriSellModeSelector();
	    if (selector.isTargetStock(stk, ac)) {
	        log.info("AvgPriSellModeSelector selector matched for stock:" + stk.getID() + " sell stock!");
	        return true;
	    }
	    else {
	        log.info("AvgPriSellModeSelector selector NOT matched for stock:" + stk.getID() + " NOT sell stock!");
	        return false;
	    }
	}
	

	@Override
	public int getSellQty(Stock s, ICashAccount ac) {
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

    @Override
    public boolean matchTradeModeId(Stock s) {
        // TODO Auto-generated method stub
        Integer trade_mode_id = null;
        try {
            Connection con = DBManager.getConnection();
            Statement stm = con.createStatement();
            String sql = "select trade_mode_id "
                       + "  from usrStk "
                       + " where id ='" + s.getID() + "'";
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) {
                trade_mode_id = rs.getInt("trade_mode_id");
                log.info("trade_mode_id for stock:" + s.getID() + " is:" + trade_mode_id + " expected:" + STConstants.TRADE_MODE_ID_AVGPRI);
            }
            else {
            }
            rs.close();
            stm.close();
            con.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        
        if (trade_mode_id != null && trade_mode_id == STConstants.TRADE_MODE_ID_AVGPRI) {
            log.info("trade mode matched, continue");
            return true;
        }
        log.info("trade mode does not matched, continue");
        return false;
    }
}
