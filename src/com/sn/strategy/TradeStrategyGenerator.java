package com.sn.strategy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
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
import com.sn.strategy.algorithm.buypoint.BottomHammerBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.DaBanBalanceBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.DaBanBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.DefaultBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.MacdBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.PriceBoxBrkUpBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.PricePlusBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.PriceTurnBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.QtyBuyPointSelector;
import com.sn.strategy.algorithm.buypoint.TopNMnyBuyPointSelector;
import com.sn.strategy.algorithm.sellpoint.BalanceSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.BottomHammerSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.DaBanBalanceSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.DaBanSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.DefaultSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.MacdSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.PriceBoxBrkBtnSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.PricePlusSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.PriceTurnSellPointSelector;
import com.sn.strategy.algorithm.sellpoint.QtySellPointSelector;
import com.sn.strategy.algorithm.sellpoint.TopNMnySellPointSelector;
import com.sn.task.suggest.selector.DefaultStockSelector;

public class TradeStrategyGenerator {

    static public ITradeStrategy generatorStrategyyyy(boolean sim_mode) {
        
        List<IBuyPointSelector> buyPoints = new LinkedList<IBuyPointSelector>();
        List<ISellPointSelector> sellPoints = new LinkedList<ISellPointSelector>();
        
        IBuyPointSelector bs0 = new BottomHammerBuyPointSelector(sim_mode);
        ISellPointSelector ses0 = new BottomHammerSellPointSelector(sim_mode);
        
        //IBuyPointSelector bs = new PricePlusBuyPointSelector(sim_mode);
        //ISellPointSelector ses = new PricePlusSellPointSelector(sim_mode);
        
        
//        IBuyPointSelector bs = new MacdBuyPointSelector();
//        ISellPointSelector ses = new MacdSellPointSelector();
//        IBuyPointSelector bs = new PriceTurnBuyPointSelector();
//        ISellPointSelector ses = new PriceTurnSellPointSelector();
//        IBuyPointSelector bs = new DefaultBuyPointSelector();
//        ISellPointSelector ses = new DefaultSellPointSelector();
        //ICashAccount ca = null;
        //CashAcntManger.getDftAcnt();
        //ca.initAccount();
//        IBuyPointSelector bbs = new BalanceBuyPointSelector(sim_mode);
//        ISellPointSelector bes = new BalanceSellPointSelector(sim_mode);
        
        
        buyPoints.add(bs0);
        //buyPoints.add(bs);
//        buyPoints.add(bbs);
        
        sellPoints.add(ses0);
//        sellPoints.add(bes);
        
        ITradeStrategy bts = new TradeStrategyImp(buyPoints, sellPoints , null, "QtyTradeStrategy", sim_mode);
        
        return bts;
    }
    
    static public ITradeStrategy generatorStrategyxx(boolean sim_mode) {
        
        List<IBuyPointSelector> buyPoints = new LinkedList<IBuyPointSelector>();
        List<ISellPointSelector> sellPoints = new LinkedList<ISellPointSelector>();
        
        IBuyPointSelector bs0 = new PriceBoxBrkUpBuyPointSelector(sim_mode);
        ISellPointSelector ses0 = new PriceBoxBrkBtnSellPointSelector(sim_mode);
        
        //IBuyPointSelector bs = new PricePlusBuyPointSelector(sim_mode);
        //ISellPointSelector ses = new PricePlusSellPointSelector(sim_mode);
        
        
//        IBuyPointSelector bs = new MacdBuyPointSelector();
//        ISellPointSelector ses = new MacdSellPointSelector();
//        IBuyPointSelector bs = new PriceTurnBuyPointSelector();
//        ISellPointSelector ses = new PriceTurnSellPointSelector();
//        IBuyPointSelector bs = new DefaultBuyPointSelector();
//        ISellPointSelector ses = new DefaultSellPointSelector();
        //ICashAccount ca = null;
        //CashAcntManger.getDftAcnt();
        //ca.initAccount();
        //IBuyPointSelector bbs = new BalanceBuyPointSelector(sim_mode);
        //ISellPointSelector bes = new BalanceSellPointSelector(sim_mode);
        
        
        buyPoints.add(bs0);
        //buyPoints.add(bs);
        //buyPoints.add(bbs);
        
        sellPoints.add(ses0);
        //sellPoints.add(bes);
        
        ITradeStrategy bts = new TradeStrategyImp(buyPoints, sellPoints , null, "PriceBoxBrkTradeStrategy", sim_mode);
        
        return bts;
    }
    
    static public ITradeStrategy generatorStrategy(boolean sim_mode) {
        
        List<IBuyPointSelector> buyPoints = new LinkedList<IBuyPointSelector>();
        List<ISellPointSelector> sellPoints = new LinkedList<ISellPointSelector>();
        
        IBuyPointSelector bs0 = new QtyBuyPointSelector(sim_mode);
        ISellPointSelector ses0 = new QtySellPointSelector(sim_mode);
        
        //IBuyPointSelector bs = new PricePlusBuyPointSelector(sim_mode);
        //ISellPointSelector ses = new PricePlusSellPointSelector(sim_mode);
        
        
//        IBuyPointSelector bs = new MacdBuyPointSelector();
//        ISellPointSelector ses = new MacdSellPointSelector();
//        IBuyPointSelector bs = new PriceTurnBuyPointSelector();
//        ISellPointSelector ses = new PriceTurnSellPointSelector();
//        IBuyPointSelector bs = new DefaultBuyPointSelector();
//        ISellPointSelector ses = new DefaultSellPointSelector();
        //ICashAccount ca = null;
        //CashAcntManger.getDftAcnt();
        //ca.initAccount();
        IBuyPointSelector bbs = new BalanceBuyPointSelector(sim_mode);
        ISellPointSelector bes = new BalanceSellPointSelector(sim_mode);
        
        
        buyPoints.add(bs0);
        //buyPoints.add(bs);
        buyPoints.add(bbs);
        
        sellPoints.add(ses0);
        sellPoints.add(bes);
        
        ITradeStrategy bts = new TradeStrategyImp(buyPoints, sellPoints , null, "QtyTradeStrategy", sim_mode);
        
        return bts;
    }
    
    static public ITradeStrategy generatorStrategy44(boolean sim_mode) {
        
        List<IBuyPointSelector> buyPoints = new LinkedList<IBuyPointSelector>();
        List<ISellPointSelector> sellPoints = new LinkedList<ISellPointSelector>();
        
        IBuyPointSelector bs0 = new TopNMnyBuyPointSelector(sim_mode);
        ISellPointSelector ses0 = new TopNMnySellPointSelector(sim_mode);
        
        //IBuyPointSelector bs = new PricePlusBuyPointSelector(sim_mode);
        //ISellPointSelector ses = new PricePlusSellPointSelector(sim_mode);
        
        
//        IBuyPointSelector bs = new MacdBuyPointSelector();
//        ISellPointSelector ses = new MacdSellPointSelector();
//        IBuyPointSelector bs = new PriceTurnBuyPointSelector();
//        ISellPointSelector ses = new PriceTurnSellPointSelector();
//        IBuyPointSelector bs = new DefaultBuyPointSelector();
//        ISellPointSelector ses = new DefaultSellPointSelector();
        //ICashAccount ca = null;
        //CashAcntManger.getDftAcnt();
        //ca.initAccount();
        IBuyPointSelector bbs = new BalanceBuyPointSelector(sim_mode);
        ISellPointSelector bes = new BalanceSellPointSelector(sim_mode);
        
        
        buyPoints.add(bs0);
        //buyPoints.add(bs);
        buyPoints.add(bbs);
        
        sellPoints.add(ses0);
        sellPoints.add(bes);
        
        ITradeStrategy bts = new TradeStrategyImp(buyPoints, sellPoints , null, "TopNMnyTradeStrategy", sim_mode);
        
        return bts;
    }
    static public ITradeStrategy generatorStrategy1(boolean sim_mode) {
        
        List<IBuyPointSelector> buyPoints = new LinkedList<IBuyPointSelector>();
        List<ISellPointSelector> sellPoints = new LinkedList<ISellPointSelector>();
        
        //IBuyPointSelector bs = new QtyBuyPointSelector(sim_mode);
        //ISellPointSelector ses = new QtySellPointSelector(sim_mode);
        IBuyPointSelector bs = new MacdBuyPointSelector(true);
        ISellPointSelector ses = new MacdSellPointSelector(true);
//        IBuyPointSelector bs = new PriceTurnBuyPointSelector();
//        ISellPointSelector ses = new PriceTurnSellPointSelector();
//        IBuyPointSelector bs = new DefaultBuyPointSelector();
//        ISellPointSelector ses = new DefaultSellPointSelector();
        //ICashAccount ca = null;
        //CashAcntManger.getDftAcnt();
        //ca.initAccount();
        IBuyPointSelector bbs = new BalanceBuyPointSelector(sim_mode);
        ISellPointSelector bes = new BalanceSellPointSelector(sim_mode);
        
        
        buyPoints.add(bs);
        buyPoints.add(bbs);
        
        sellPoints.add(ses);
        sellPoints.add(bes);
        
        ITradeStrategy bts = new TradeStrategyImp(buyPoints, sellPoints , null, "MacdTradeStrategy", sim_mode);
        
        return bts;
    }
    static public ITradeStrategy generatorStrategy2(boolean sim_mode) {
        
        List<IBuyPointSelector> buyPoints = new LinkedList<IBuyPointSelector>();
        List<ISellPointSelector> sellPoints = new LinkedList<ISellPointSelector>();
        
        //IBuyPointSelector bs = new QtyBuyPointSelector(sim_mode);
        IBuyPointSelector bs = new DaBanBuyPointSelector(sim_mode);
        ISellPointSelector ses = new DaBanSellPointSelector(sim_mode);
//        IBuyPointSelector bs = new MacdBuyPointSelector();
//        ISellPointSelector ses = new MacdSellPointSelector();
//        IBuyPointSelector bs = new PriceTurnBuyPointSelector();
//        ISellPointSelector ses = new PriceTurnSellPointSelector();
//        IBuyPointSelector bs = new DefaultBuyPointSelector();
//        ISellPointSelector ses = new DefaultSellPointSelector();
        //ICashAccount ca = null;
        //CashAcntManger.getDftAcnt();
        //ca.initAccount();
        IBuyPointSelector bbs = new DaBanBalanceBuyPointSelector(sim_mode);
        ISellPointSelector bes = new DaBanBalanceSellPointSelector(sim_mode);
        
        buyPoints.add(bs);
        buyPoints.add(bbs);
        
        sellPoints.add(ses);
        sellPoints.add(bes);
        
        ITradeStrategy bts = new TradeStrategyImp(buyPoints, sellPoints , null, "DaBanTradeStrategy", sim_mode);
        
        return bts;
    }
}
