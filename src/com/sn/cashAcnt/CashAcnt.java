package com.sn.cashAcnt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.mail.reporter.StockObserverable;
import com.sn.stock.Stock;

public class CashAcnt implements ICashAccount{

    static Logger log = Logger.getLogger(CashAcnt.class);
    private String actId;
    private double initMny;
    private double usedMny;
    private double pftMny;
    private int splitNum;
    private double maxUsePct;
    private boolean dftAcnt;
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }
    public CashAcnt(String id) {
        Connection con = DBManager.getConnection();
        String sql = "select * from CashAcnt where acntId = '" + id + "'";
        
        log.info("create cashAcnt info:" + id);
        log.info(sql);
        try {
        Statement stm = con.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        
        if (rs.next()) {
            actId = id;
            initMny = rs.getDouble("init_mny");
            usedMny = rs.getDouble("used_mny");
            pftMny = rs.getDouble("pft_mny");
            splitNum = rs.getInt("split_num");
            dftAcnt = rs.getInt("dft_acnt_flg") > 0;
            maxUsePct = rs.getDouble("max_useable_pct");
            log.info("actId:" + actId + " initMny:" + initMny + " usedMny:" + usedMny + " calMny:" + pftMny
                    + " splitNum:" + splitNum + " max_useable_pct:" + maxUsePct
                    + " dftAcnt:" + dftAcnt);
        }
        rs.close();
        con.close();
        con = null;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public double getMaxAvaMny() {
        
        double useableMny = initMny * maxUsePct;
        if ((useableMny - usedMny) > initMny / splitNum) {
            return initMny / splitNum;
        }
        else {
            return useableMny - usedMny;
        }
    }
    public String getActId() {
        return actId;
    }
    public void setActId(String actId) {
        this.actId = actId;
    }
    public double getInitMny() {
        return initMny;
    }
    public void setInitMny(double initMny) {
        this.initMny = initMny;
    }
    public double getUsedMny() {
        return usedMny;
    }
    public void setUsedMny(double usedMny) {
        this.usedMny = usedMny;
    }
    public double getPftMny() {
        return pftMny;
    }
    public void setPftMny(double calMny) {
        this.pftMny = calMny;
    }
    public int getSplitNum() {
        return splitNum;
    }
    public void setSplitNum(int splitNum) {
        this.splitNum = splitNum;
    }
    public boolean isDftAcnt() {
        return dftAcnt;
    }
    public void setDftAcnt(boolean dftAcnt) {
        this.dftAcnt = dftAcnt;
    }
    
    public int getSellableAmt(String stkId, String sellDt) {
        Connection con = DBManager.getConnection();
        String sql = "select case when sum(b.amount) is null then 0 else sum(b.amount) end SellableAmt from TradeDtl b " +
                "      where b.stkId = '" + stkId + "'" +
                "        and acntId = '" + actId + "'" +
                "        and b.buy_flg = 1 " +
                "        and to_char(dl_dt, 'yyyy-mm-dd') < '" + sellDt + "' " +
                "      order by b.seqnum";
        ResultSet rs = null;
        
        int sellableAmt = 0;
        int soldAmt = 0;
        
        try {
            Statement stm = con.createStatement();
            log.info(sql);
            rs = stm.executeQuery(sql);
            if (rs.next()) {
                sellableAmt = rs.getInt("SellableAmt");
            }
            
            rs.close();
            stm.close();
            
            if (sellableAmt > 0) {
                stm = con.createStatement();
                sql = "select case when sum(b.amount) is null then 0 else sum(b.amount) end SoldAmt from TradeDtl b " +
                "      where b.stkId = '" + stkId + "'" +
                "        and acntId = '" + actId + "'" +
                "        and b.buy_flg = 0 " +
                "      order by b.seqnum";
                
                log.info(sql);
                rs = stm.executeQuery(sql);
                if (rs.next()) {
                    soldAmt = rs.getInt("SoldAmt");
                }
                rs.close();
                stm.close();
            }
            con.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        
        log.info("sellable/Sold Amt for :" + stkId + " is (" + sellableAmt + "/" + soldAmt + ")");
        return sellableAmt - soldAmt;
    }
    
    public int getUnSellableAmt(String stkId, String sellDt) {
        Connection con = DBManager.getConnection();
        String sql = "select case when sum(b.amount) is null then 0 else sum(b.amount) end unSellableAmt from TradeDtl b " +
                "      where b.stkId = '" + stkId + "'" +
                "        and b.acntId = '" + actId + "'" +
                "        and b.buy_flg = 1 " +
                "        and to_char(b.dl_dt, 'yyyy-mm-dd') = '" + sellDt + "' " +
                "      order by b.seqnum";
        ResultSet rs = null;
        
        int unSellableAmt = 0;
        
        try {
            Statement stm = con.createStatement();
            log.info(sql);
            rs = stm.executeQuery(sql);
            if (rs.next()) {
                unSellableAmt = rs.getInt("unSellableAmt");
            }
            rs.close();
            stm.close();
            con.close();
            con = null;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        log.info("unsellable Amt for :" + stkId + " is (" + unSellableAmt +")");
        return unSellableAmt;
    }
    
    public boolean calProfit(String ForDt) {
        Connection con = DBManager.getConnection();
        
        String sql = "select stkId from TradeHdr h where h.acntId = '" + actId + "'";
        
        ResultSet rs = null;
        pftMny = 0;
        double subpftMny = 0;
        try {
            Statement stm = con.createStatement();
            rs = stm.executeQuery(sql);
            Map<String, Stock> stks = StockObserverable.stocks;
            while (rs.next()) {
                String stkId = rs.getString("stkId");
                Stock s = stks.get(stkId);
                
                int inHandMnt = getSellableAmt(stkId, ForDt) + getUnSellableAmt(stkId, ForDt);
                
                log.info("in hand amt:" + inHandMnt + " price:" + s.getCur_pri() + " with cost:" + usedMny);
                subpftMny = inHandMnt * s.getCur_pri();
                
                sql = "update TradeHdr set pft_mny = " + subpftMny
                + ", pft_price =" + s.getCur_pri()
                + ", in_hand_qty = " + inHandMnt + " where acntId ='" + actId + "' and stkId ='" + stkId + "'";
                Statement stm2 = con.createStatement();
                log.info(sql);
                stm2.execute(sql);
                stm2.close();
            }
            
            rs.close();
            stm.close();
            
            sql = "select sum(pft_mny) tot_pft_mny from TradeHdr h where acntId = '" + actId + "'";
            
            stm = con.createStatement();
            rs = stm.executeQuery(sql);
            
            double tot_pft_mny = 0.0;
            if (rs.next()) {
                tot_pft_mny = rs.getDouble("tot_pft_mny");
                sql = "update CashAcnt set pft_mny = " + tot_pft_mny + " where acntId = '" + actId + "'";
                Statement stm2 = con.createStatement();
                
                pftMny = tot_pft_mny;
                
                log.info(sql);
                
                stm2.execute(sql);
                stm2.close();
            }
            con.commit();
            rs.close();
            stm.close();
            con.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
            log.info("calProfit returned with exception:" + e.getMessage());
            return false;
        }
        return true;
    }
    
    public String reportAcntProfitWeb() {
        String msg = "Account: " + this.actId + " profit report<br/>";
        msg += "<table border = 1>" +
        "<tr>" +
        "<th> Cash Account</th> " +
        "<th> Init Money </th> " +
        "<th> Used Money </th> " +
        "<th> Split Number </th> " +
        "<th> MaxUse Pct</th> " +
        "<th> Default Account</th> " +
        "<th> Account Profit</th> " +
        "<th> Report Date</th> </tr> ";
        
        
        String dt = "";  
        SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");  
        Date date = new Date();  
        dt = f.format(date);
        
        
        msg += "<tr> <td>" + actId + "</td>" +
               "<td> " + initMny + "</td>" +
               "<td> " + usedMny + "</td>" +
               "<td> " + splitNum + "</td>" +
               "<td> " + maxUsePct + "</td>" +
               "<td> " + (dftAcnt ? "yes" : "No") + "</td>" +
               "<td> " + pftMny + "</td>" +
               "<td> " + dt + "</td> </tr> </table>";
        
        String detailTran = "Detail Transactions <br/>";
        
        detailTran += "<table border = 1>" +
        "<tr>" +
        "<th> Cash Account</th> " +
        "<th> Stock Id </th> " +
        "<th> Sequence Number </th> " +
        "<th> Price </th> " +
        "<th> Amount </th> " +
        "<th> Buy/Sell </th> " +
        "<th> Transaction Date</th> </tr> ";
        
        Connection con = DBManager.getConnection();
        String sql = "select stkId," +
        		"           seqnum," +
        		"           round(price, 2) price," +
        		"           amount," +
        		"           buy_flg," +
        		"           to_char(dl_dt, 'hh:mi:ss yyyy-mm-dd') dl_dt" +
        		" from TradeDtl d " +
                     " where d.acntId ='" + actId + "' order by d.stkId, d.seqnum ";
        
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            while (rs.next()) {
                detailTran += "<tr> <td>" + actId + "</td>" +
                "<td> " + rs.getString("stkId") + "</td>" +
                "<td> " + rs.getInt("seqnum") + "</td>" +
                "<td> " + rs.getDouble("price") + "</td>" +
                "<td> " + rs.getInt("amount") + "</td>" +
                "<td> " + (rs.getInt("buy_flg") > 0 ? "B" : "S") + "</td>" +
                "<td> " + rs.getString("dl_dt") + "</td></tr>";
            }
            
            detailTran += "</table>";
            rs.close();
            stm.close();
            con.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        
        String totMsg = msg + detailTran;
        log.info("got profit information:" + totMsg);
        
        return totMsg;
        
    }
    
    public void printAcntInfo() {
        DecimalFormat df = new DecimalFormat("##.##");
        String pftPct = df.format((pftMny - usedMny) / usedMny * 100);
        String profit = df.format(pftMny - usedMny);
        log.info("##################################################################################################");
        log.info("|AccountId\t|InitMny\t|UsedMny\t|PftMny\t\t|SplitNum\t|MaxUsePct\t|DftAcnt\t|PP\t|Profit|");
        log.info("|" + actId + "\t|" + initMny + "\t|" + usedMny + "\t\t|" + pftMny + "\t\t|" + splitNum + "\t\t|" + maxUsePct + "\t\t|" + dftAcnt + "\t\t|" + pftPct + "%\t|" + profit);
        log.info("##################################################################################################");
    }
    
    @Override
    public void printTradeInfo() {
        // TODO Auto-generated method stub
        Connection con = DBManager.getConnection();
        Statement stm = null;
        String sql = null;
        try {
            stm = con.createStatement();
            sql = "select acntId," +
            		"     stkId," +
            		"    round(pft_mny, 2) pft_mny, " +
            		"    in_hand_qty, " +
            		"    round(pft_price, 2) pft_price, " +
            		"    to_char(add_dt, 'yyyy-mm-dd hh24:mi:ss') add_dt " +
            		"from TradeHdr where acntId = '" + actId + "' order by stkid ";
            //log.info(sql);
            ResultSet rs = stm.executeQuery(sql);
            log.info("=======================================================================================");
            log.info("|AccountID\t|StockID\t|Pft_mny\t|InHandQty\t|PftPrice\t|TranDt|");
            while (rs.next()) {
                log.info("|" + rs.getString("acntId") + "\t|" +
                        rs.getString("stkId") + "\t\t|" +
                        rs.getString("pft_mny") + "\t\t|" +
                        rs.getInt("in_hand_qty") + "\t\t|" +
                        rs.getDouble("pft_price") + "\t\t|" +
                        rs.getString("add_dt") + "|");
                Statement stmdtl = con.createStatement();
                String sqldtl = "select stkid, seqnum, price, amount, to_char(dl_dt, 'yyyy-mm-dd hh24:mi:ss') dl_dt, buy_flg " +
                		        "  from tradedtl where stkid ='" + rs.getString("stkId") + "'";
                //log.info(sql);
                
                ResultSet rsdtl = stmdtl.executeQuery(sqldtl);
                log.info("\tStockID\tSeqnum\tPrice\tAmount\tBuy/Sell\tTranDt");
                while (rsdtl.next()) {
                    log.info("\t" + rsdtl.getString("stkid") + "\t" +
                             rsdtl.getInt("seqnum") + "\t" +
                             rsdtl.getDouble("price") + "\t" +
                             rsdtl.getInt("amount") + "\t" +
                             (rsdtl.getInt("buy_flg") > 0 ? "B":"S") + "\t\t" +
                             rsdtl.getString("dl_dt") + "\t");
                }
                rsdtl.close();
                stmdtl.close();
            }
            log.info("=======================================================================================");
            rs.close();
            stm.close();
            con.close();
        }
        catch(SQLException e){
            e.printStackTrace();
        }
    }
}
