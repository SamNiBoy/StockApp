package com.sn.sim.strategy.imp;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.cashAcnt.CashAcnt;
import com.sn.cashAcnt.ICashAccount;
import com.sn.db.DBManager;
import com.sn.sim.strategy.ITradeStrategy;
import com.sn.sim.strategy.selector.buypoint.IBuyPointSelector;
import com.sn.sim.strategy.selector.sellpoint.ISellPointSelector;
import com.sn.sim.strategy.selector.stock.IStockSelector;
import com.sn.stock.Stock2;

public class TradeStrategyImp implements ITradeStrategy {

    static Logger log = Logger.getLogger(TradeStrategyImp.class);
    
    IStockSelector stock_selector = null;
    IBuyPointSelector buypoint_selector = null;
    ISellPointSelector sellpoint_selector = null;
    
    ICashAccount cash_account = null;
    
    
    public IStockSelector getStock_selector() {
        return stock_selector;
    }

    public void setStock_selector(IStockSelector stockSelector) {
        stock_selector = stockSelector;
    }

    public IBuyPointSelector getBuypoint_selector() {
        return buypoint_selector;
    }

    public void setBuypoint_selector(IBuyPointSelector buypointSelector) {
        buypoint_selector = buypointSelector;
    }

    public ISellPointSelector getSellpoint_selector() {
        return sellpoint_selector;
    }

    public void setSellpoint_selector(ISellPointSelector sellpointSelector) {
        sellpoint_selector = sellpointSelector;
    }

    public ICashAccount getCash_account() {
        return cash_account;
    }

    public void setCash_account(ICashAccount cashAccount) {
        cash_account = cashAccount;
    }

    /**
     * @param args
     */
    public boolean isGoodStockToSelect(Stock2 s) {
        return stock_selector.isTargetStock(s, cash_account);
    }

    public boolean isGoodPointtoBuy(Stock2 s) {
        return buypoint_selector.isGoodBuyPoint(s, cash_account);
    }

    public boolean isGoodPointtoSell(Stock2 s) {
        return sellpoint_selector.isGoodSellPoint(s, cash_account);
    }
    
    public TradeStrategyImp(IStockSelector ss,
                            IBuyPointSelector bs,
                            ISellPointSelector ses,
                            ICashAccount ca) throws Exception {
        if (ss == null || bs == null || ses == null) {
            throw new Exception("Can not create trade strategy with null selector.");
        }
        stock_selector = ss;
        buypoint_selector = bs;
        sellpoint_selector = ses;
        cash_account = ca;
    }

    @Override
    public boolean buyStock(Stock2 s) {
    	int buyMnt = buypoint_selector.getBuyQty(s, cash_account);
        double occupiedMny = buyMnt * s.getCur_pri();
        
        //cash_account.printAcntInfo();
        //s.printStockInfo();
        
        log.info("trying to buy amount:" + buyMnt + " with using Mny:" + occupiedMny);
        
        if (buyMnt < 100) {
            log.info(" No enough mony to by stock: " + s.getName());
            return false;
        }
        
        log.info("now start to bug stock " + s.getName()
                + " price:" + s.getCur_pri()
                + " with money: " + cash_account.getMaxAvaMny()
                + " buy mount:" + buyMnt);

        Connection con = DBManager.getConnection();
        String sql = "select case when max(d.seqnum) is null then -1 else max(d.seqnum) end maxseq from TradeHdr h " +
                "       join TradeDtl d " +
                "         on h.stkId = d.stkId " +
                "        and h.acntId = d.acntId " +
                "      where h.stkId = '" + s.getID()+ "'" +
                "        and h.acntId = '" + cash_account.getActId() + "'";
        int seqnum = 0;
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) {
                if (rs.getInt("maxseq") < 0) {
                    sql = "insert into TradeHdr values('" + cash_account.getActId() + "','"
                    + s.getID() + "',"
                    + s.getCur_pri()*buyMnt + ","
                    + buyMnt + ","
                    + s.getCur_pri() + ",to_date('" + s.getDl_dt().toLocaleString() + "','yyyy-mm-dd hh24:mi:ss'))";
                    log.info(sql);
                    Statement stm2 = con.createStatement();
                    stm2.execute(sql);
                    stm2.close();
                    stm2 = null;
                }
                seqnum = rs.getInt("maxseq") + 1;
            }
            stm.close();
            stm = con.createStatement();
            sql = "insert into TradeDtl values('"
                + cash_account.getActId() + "','"
                + s.getID() + "',"
                + seqnum + ","
                + s.getCur_pri() + ", "
                + buyMnt
                + ", to_date('" + s.getDl_dt().toLocaleString() + "','yyyy-mm-dd hh24:mi:ss'), 1)";
            log.info(sql);
            stm.execute(sql);
            
            //now sync used money
            double usedMny = cash_account.getUsedMny();
            usedMny += occupiedMny;
            
            cash_account.setUsedMny(usedMny);
            
            stm.close();
            stm = con.createStatement();
            sql = "update CashAcnt set used_mny = " + usedMny + " where acntId = '" + cash_account.getActId() + "'";
            stm.execute(sql);
            con.commit();
            con.close();
            return true;
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean sellStock(Stock2 s) {

        //cash_account.printAcntInfo();
        //s.printStockInfo();
        
        log.info("now start to sell stock " + s.getName()
                + " price:" + s.getCur_pri()
                + " against CashAcount: " + cash_account.getActId());

        int sellableAmt = sellpoint_selector.getSellQty(s, cash_account);
        
        if (sellableAmt <= 0) {
            return false;
        }

        double relasedMny = sellableAmt * s.getCur_pri();
        Connection con = DBManager.getConnection();
        int seqnum = 0;
        try {
            String sql = "select max(d.seqnum) maxseq " +
            "       from TradeDtl d " +
            "      where d.stkId = '" + s.getID()+ "'" +
            "        and d.acntId = '" + cash_account.getActId() + "'";
    
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            if (!rs.next()) {
                seqnum = 0;
            }
            else {
                seqnum = rs.getInt("maxseq") + 1;
            }
            rs.close();
            stm.close();
            stm = con.createStatement();
            sql = "insert into TradeDtl values( '"
                + cash_account.getActId() + "','"
                + s.getID() + "',"
                + seqnum + ","
                + s.getCur_pri() + ", "
                + sellableAmt
                + ", to_date('" + s.getDl_dt().toLocaleString() + "', 'yyyy-mm-dd hh24:mi:ss'), 0)";
            log.info(sql);
            stm.execute(sql);
            
            //now sync used money
            double usedMny = cash_account.getUsedMny();
            usedMny -= relasedMny;
            cash_account.setUsedMny(usedMny);
            
            stm.close();
            stm = con.createStatement();
            sql = "update CashAcnt set used_mny = used_mny - " + relasedMny + " where acntId = '" + cash_account.getActId() + "'";
            log.info(sql);
            stm.execute(sql);
            con.commit();
            con.close();
            return true;
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        return false;
    
    }

    @Override
    public boolean calProfit(String ForDt, Map<String, Stock2>stockSet) {
        // TODO Auto-generated method stub
        return cash_account.calProfit(ForDt, stockSet);
    }

    @Override
    public boolean reportTradeStat() {
        // TODO Auto-generated method stub
        cash_account.printAcntInfo();
        cash_account.printTradeInfo();
        return false;
    }

    @Override
    public ICashAccount getCashAccount() {
        // TODO Auto-generated method stub
        return cash_account;
    }

    @Override
    public boolean initAccount() {
        // TODO Auto-generated method stub
        return cash_account.initAccount();
    }

    @Override
    public void setCashAccount(ICashAccount ca) {
        // TODO Auto-generated method stub
        setCash_account(ca);
    }
}
