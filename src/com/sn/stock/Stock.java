package com.sn.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.work.fetcher.FetchStockData;

public class Stock implements Comparable<Stock>{

    static Logger log = Logger.getLogger(Stock.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Stock s = new Stock("600863", "abc", 1);

    }
    private String ID;
    private String Name;
    private String Area;
    private String Py;
    private String Bu;
    private double pct; // the pct of cur_pri to td_opn_pri
    private double prePct; //pct of previous days.
    private long incPriCnt;
    private long desPriCnt;
    private long detQty;
    private double cur_pri;
    private long cur_qty;
    private long gz_flg;
    private long keepLostDays;
    
    private List<Integer> rk = new ArrayList<Integer>();
    
    public long getKeepLostDays() {
        return keepLostDays;
    }

    public void setKeepLostDays(long keepLostDays) {
        this.keepLostDays = keepLostDays;
    }

    public double getPrePct() {
        return prePct;
    }

    public void setPrePct(double prePct) {
        this.prePct = prePct;
    }
    
    public long getGz_flg() {
        return gz_flg;
    }

    public void setGz_flg(long gzFlg) {
        gz_flg = gzFlg;
    }

    public long getIncPriCnt() {
        return incPriCnt;
    }

    public void setIncPriCnt(long incPriCnt) {
        this.incPriCnt = incPriCnt;
    }

    public long getDesPriCnt() {
        return desPriCnt;
    }

    public void setDesPriCnt(long desPriCnt) {
        this.desPriCnt = desPriCnt;
    }

    public long getDetQty() {
        return detQty;
    }

    public void setDetQty(long detQty) {
        this.detQty = detQty;
    }

    public double getCur_pri() {
        return cur_pri;
    }

    public void setCur_pri(double cur_pri) {
        this.cur_pri = cur_pri;
    }

    public long getCur_qty() {
        return cur_qty;
    }

    public void setCur_qty(long cur_qty) {
        this.cur_qty = cur_qty;
    }
    
    public Stock(String id, String nm, long gzflg)
    {
        ID = id;
        Name = nm;
        gz_flg = gzflg;
    }
    
    public String dsc()
    {
        DecimalFormat df = new DecimalFormat("#0.##");
        String dsc = "";
        dsc += "ID:" + ID + "\n";
        dsc += "Name:" + Name + "\n";
        return dsc;
        
    }
    
    public void setRk(int r) {
        rk.add(r);
    }
    
    public int getRk() {
        
        int r;
        if (rk.size() <= 0) {
            r = -1;
        }
        
        r = rk.get(rk.size() - 1);
        return r;
    }
    
    public double getAvgRkSpeed() {
        if (rk.size() <= 0) {
            return 0;
        }
        
        int pre_rk = 0, detRk = 0;
        for (int i = 0; i < rk.size(); i++) {
            if (pre_rk == 0) {
                pre_rk = rk.get(i);
                continue;
            }
            else {
                detRk += rk.get(i) - pre_rk;
            }
        }
        
        double avgRkSpeed = detRk*1.0 / rk.size();
        log.info("got avgRkSpeed as:" + avgRkSpeed);
        
        return avgRkSpeed;
    }
    
    public String getArea() {
        return Area;
    }

    public void setArea(String area) {
        Area = area;
    }

    public String getPy() {
        return Py;
    }

    public void setPy(String py) {
        Py = py;
    }

    public String getBu() {
        return Bu;
    }

    public void setBu(String bu) {
        Bu = bu;
    }

    public double getPct() {
        return pct;
    }

    public void setPct(double pct) {
        this.pct = pct;
    }

    public void setID(String iD) {
        ID = iD;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getName()
    {
        return Name;
    }
    
    public String getID()
    {
        return ID;
    }
    
    @Override
    public int compareTo(Stock o) {
        // TODO Auto-generated method stub
        return pct - o.pct > 0 ? -1 : 1;
    }
    
    public String getTableRow() {
        String row = "";
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        row = "<tr> <td> " + nf.format(pct*100) + "</td>" +
        "<td> " + nf.format(prePct*100) + "</td>" +
        "<td> " + getRk() + "</td>" +
        "<td> " + nf.format(getAvgRkSpeed()) + "</td>" +
        "<td> " + keepLostDays + "</td>" +
        "<td> " + ID + "</td> " +
        "<td> " + Name + "</td>" +
        "<td> " + incPriCnt + "</td>" +
        "<td> " + desPriCnt + "</td>" +
        "<td> " + detQty + "</td>" +
        "<td> " + cur_pri + "</td> " +
        "<td> " + cur_qty + "</td> </tr>";
        
        return row;
    }

}
