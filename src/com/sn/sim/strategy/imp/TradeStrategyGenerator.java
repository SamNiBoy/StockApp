package com.sn.sim.strategy.imp;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.sn.cashAcnt.CashAcnt;
import com.sn.cashAcnt.CashAcntManger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.ITradeStrategy;
import com.sn.sim.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.sim.strategy.selector.buypoint.IBuyPointSelector;
import com.sn.sim.strategy.selector.sellpoint.DefaultSellPointSelector;
import com.sn.sim.strategy.selector.sellpoint.ISellPointSelector;
import com.sn.sim.strategy.selector.stock.DefaultStockSelector;
import com.sn.sim.strategy.selector.stock.IStockSelector;
import com.sn.stock.Stock;

public class TradeStrategyGenerator {

    static public List<ITradeStrategy> generatorStrategies() throws Exception {
        List<ITradeStrategy> res = new ArrayList<ITradeStrategy>();
        
        IStockSelector ss = new DefaultStockSelector();
        IBuyPointSelector bs = new DefaultBuyPointSelector();
        ISellPointSelector ses = new DefaultSellPointSelector();
        ICashAccount ca = null;
        //CashAcntManger.getDftAcnt();
        //ca.initAccount();
        ITradeStrategy its = new TradeStrategyImp(ss, bs, ses, ca);
        
        res.add(its);
        
        return res;
    }
}
