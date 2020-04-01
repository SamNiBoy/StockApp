package com.sn.strategy;

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
import com.sn.strategy.ITradeStrategy;
import com.sn.strategy.algorithm.IBuyPointSelector;
import com.sn.strategy.algorithm.ISellPointSelector;
import com.sn.task.IStockSelector;
import com.sn.strategy.algorithm.buypoint.BalanceBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.DefaultBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.MacdBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.PriceTurnBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.QtyBuyPointSelector;
import com.sn.strategy.algorithm.sellpoint.BalanceSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.DefaultSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.MacdSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.PriceTurnSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.QtySellPointSelector;
import com.sn.task.suggest.selector.DefaultStockSelector;

public class TradeStrategyGenerator {

    static public List<ITradeStrategy> generatorStrategies(boolean sim_mode) throws Exception {
        List<ITradeStrategy> res = new ArrayList<ITradeStrategy>();
        
        IBuyPointSelector bs = new QtyBuyPointSelector(sim_mode);
        ISellPointSelector ses = new QtySellPointSelector(sim_mode);
//        IBuyPointSelector bs = new MacdBuyPointSelector();
//        ISellPointSelector ses = new MacdSellPointSelector();
//        IBuyPointSelector bs = new PriceTurnBuyPointSelector();
//        ISellPointSelector ses = new PriceTurnSellPointSelector();
//        IBuyPointSelector bs = new DefaultBuyPointSelector();
//        ISellPointSelector ses = new DefaultSellPointSelector();
        //ICashAccount ca = null;
        //CashAcntManger.getDftAcnt();
        //ca.initAccount();
        ITradeStrategy its = new TradeStrategyImp(bs, ses, null, "QtyBuySellTradeStrategy", sim_mode);
        
        res.add(its);
        
        IBuyPointSelector bbs = new BalanceBuyPointSelector(sim_mode);
        ISellPointSelector bes = new BalanceSellPointSelector(sim_mode);
        
        ITradeStrategy bts = new TradeStrategyImp(bbs, bes, null, "BalanceBuySellTradeStrategy", sim_mode);
        
        res.add(bts);
        
        return res;
    }
    
    static public ITradeStrategy generatorDefaultStrategy(boolean sim_mode) {
        
        IBuyPointSelector bs = new QtyBuyPointSelector(sim_mode);
        ISellPointSelector ses = new QtySellPointSelector(sim_mode);
        ICashAccount ca = null;
        ITradeStrategy its = new TradeStrategyImp(bs, ses, ca, "QtyBuySellTradeStrategy", sim_mode);
        
        return its;
    }
}
