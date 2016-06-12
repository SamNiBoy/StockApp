package com.sn.trade.strategy.imp;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.sn.cashAcnt.CashAcnt;
import com.sn.cashAcnt.CashAcntManger;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.trade.strategy.ITradeStrategy;
import com.sn.trade.strategy.selector.buypoint.DefaultBuyPointSelector;
import com.sn.trade.strategy.selector.buypoint.IBuyPointSelector;
import com.sn.trade.strategy.selector.buypoint.IncStopBuyPointSelector;
import com.sn.trade.strategy.selector.buypoint.MacdBuyPointSelector;
import com.sn.trade.strategy.selector.buypoint.QtyBuyPointSelector;
import com.sn.trade.strategy.selector.sellpoint.DefaultSellPointSelector;
import com.sn.trade.strategy.selector.sellpoint.ISellPointSelector;
import com.sn.trade.strategy.selector.sellpoint.IncStopSellPointSelector;
import com.sn.trade.strategy.selector.sellpoint.MacdSellPointSelector;
import com.sn.trade.strategy.selector.sellpoint.QtySellPointSelector;
import com.sn.trade.strategy.selector.stock.DefaultStockSelector;
import com.sn.trade.strategy.selector.stock.IStockSelector;

public class TradeStrategyGenerator {

    static public Set<ITradeStrategy> generatorStrategies() throws Exception {
        Set<ITradeStrategy> res = new HashSet<ITradeStrategy>();
        
        IBuyPointSelector bs = new QtyBuyPointSelector();
        ISellPointSelector ses = new QtySellPointSelector();
//        IBuyPointSelector bs = new MacdBuyPointSelector();
//        ISellPointSelector ses = new MacdSellPointSelector();
//        IBuyPointSelector bs = new DefaultBuyPointSelector();
//        ISellPointSelector ses = new DefaultSellPointSelector();
        ICashAccount ca = null;
        //CashAcntManger.getDftAcnt();
        //ca.initAccount();
        ITradeStrategy its = new TradeStrategyImp(bs, ses, ca);
        
        res.add(its);
        
//        IBuyPointSelector bs2 = new IncStopBuyPointSelector();
//        ISellPointSelector ses2 = new IncStopSellPointSelector();
//        ITradeStrategy its2 = new TradeStrategyImp(bs2, ses2, ca);
        
//        res.add(its2);
        return res;
    }
    
    static public Set<ITradeStrategy> generatorDefaultStrategies() {
        
        Set<ITradeStrategy> ss = new HashSet<ITradeStrategy> ();
        IBuyPointSelector bs = new QtyBuyPointSelector();
        ISellPointSelector ses = new QtySellPointSelector();
        ICashAccount ca = null;
        ITradeStrategy its = new TradeStrategyImp(bs, ses, ca);
        ss.add(its);
        
//        IBuyPointSelector bs2 = new IncStopBuyPointSelector();
//        ISellPointSelector ses2 = new IncStopSellPointSelector();
//        ITradeStrategy its2 = new TradeStrategyImp(bs2, ses2, ca);
        
//        ss.add(its2);
        return ss;
    }
}
