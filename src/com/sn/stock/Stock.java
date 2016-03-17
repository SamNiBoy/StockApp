package com.sn.stock;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
        Connection con = DBManager.getConnection();
        try {
            Statement stm = con.createStatement();
            String sql = "select id, name, gz_flg from stk";
            
            ResultSet rs = stm.executeQuery(sql);
            List<Stock> sl = new LinkedList<Stock>();
            while(rs.next()) {
                Stock s = new Stock(rs.getString("id"), rs.getString("name"), 1);
                s.constructFollowers();
                sl.add(s);
            }
            for (int i = 0; i < sl.size(); i++) {
                Stock s = sl.get(i);
                s.printFollowers();
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
    }
    private String ID;
    private String Name;
    private String Area;
    private String Py;
    private String Bu;
    private double pct; // the pct of cur_pri to td_opn_pri
    private double pctToCls; // the pct of cur_pri to yt_cls_pri
    private double prePct; //pct of previous days.
    private long incPriCnt;
    private long desPriCnt;
    private long detQty;
    public long pre_detQty = 0; //store pre 5 mintes det_qty.
    private boolean pre_detQty_plused = false; //true if detQty increased sharply.
    private double cur_pri;
    private double dlmnynum;
    private Timestamp dl_dt;
    private long cur_qty;
    private long gz_flg;
    private long keepLostDays;
    private List<Integer> rk = new ArrayList<Integer>();
    
    public boolean isGoodCandidateForBuy() {
        if ((getCur_pri() <= 50 && getPrePct() < -0.05) ||
             getGz_flg() > 0 ||
             (getCur_pri() <= 50 && getKeepLostDays() > 0 && getPrePct() < -0.01) ||
             isPre_detQty_plused()) {
            return true;
        }
        else {
            return false;
        }
    }
    
    public boolean isGoodCandidateForSell() {
        if ((getCur_pri() <= 50 && getPrePct() < 0.5) ||
             (getCur_pri() <= 50 && getKeepLostDays() > 0 && getPrePct() < -0.01) ||
             isPre_detQty_plused()) {
            return true;
        }
        else {
            return false;
        }
    }
    
    public Timestamp getDl_dt() {
        return dl_dt;
    }

    public void setDl_dt(Timestamp dl_dt) {
        this.dl_dt = dl_dt;
    }

    public double getDlmnynum() {
        return dlmnynum;
    }

    public void setDlmnynum(double dlmnynum) {
        this.dlmnynum = dlmnynum;
    }

    public double getPctToCls() {
        return pctToCls;
    }

    public void setPctToCls(double pctToCls) {
        this.pctToCls = pctToCls;
    }

    public long getPre_detQty() {
        long temp = pre_detQty;
        log.info("pre_detQty :" + pre_detQty + " swap with detQty:" + detQty);
        pre_detQty = detQty;
        return temp;
    }

    public void setPre_detQty(long preDetQty) {
        pre_detQty = preDetQty;
    }

    public boolean isPre_detQty_plused() {
        return pre_detQty_plused;
    }

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
        
        if (pre_detQty == 0) {
            pre_detQty = detQty;
        }
        
        if (detQty > 20 * pre_detQty) {
            pre_detQty_plused =true;
        }
        else {
            pre_detQty_plused = false;
        }
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
        dsc += "cur_pri:" + cur_pri + "\n";
        dsc += "keepLostDays:" + keepLostDays + "\n";
        dsc += "gz_flg:" + gz_flg + "\n";
        dsc += "incPriCnt:" + incPriCnt + "\n";
        dsc += "desPriCnt:" + desPriCnt + "\n";
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
    
    public long getAvgRkSpeed() {
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
                pre_rk = rk.get(i);
            }
        }
        
        long avgRkSpeed = detRk / rk.size();
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
        DecimalFormat df = new DecimalFormat("##.##");
        df.setMaximumFractionDigits(2);
        row = "<tr> <td> " + df.format(pctToCls*100) + "</td>" +
        "<td> " + df.format(pct*100) + "</td>" +
        "<td> " + df.format(prePct*100) + "</td>" +
        "<td> " + keepLostDays + "</td>" +
        "<td> " + getRk() + "</td>" +
        "<td> " + getAvgRkSpeed() + "/" + rk.size() + "</td>" +
        "<td> " + ID + "</td> " +
        "<td> " + Name + "</td>" +
        "<td> " + incPriCnt + "</td>" +
        "<td> " + desPriCnt + "</td>" +
        "<td> " +  df.format(this.pre_detQty == 0 ? 0 : detQty/this.getPre_detQty()) + "</td>" +
        "<td> " + cur_pri + "</td> " +
        "<td> " + cur_qty/100 + "手</td> " +
        "<td> " + df.format(dlmnynum / 100000000) + "亿</td> </tr>";
        
        return row;
    }
    
    //static data
    private Map<String, List<Follower>> followers = null;
    
    public Map<String, List<Follower>> getFollowers() {
        return followers;
    }

    public void setFollowers(Map<String, List<Follower>> followers) {
        this.followers = followers;
    }
    
    private List<Double> cur_pri_lst = new ArrayList<Double>();
    private List<Long> dl_stk_num_lst = new ArrayList<Long>();
    private List<Double> dl_mny_num_lst = new ArrayList<Double>();
    private List<Double> pctToCls_lst = new ArrayList<Double>();
    private List<Timestamp> dl_dt_lst = new ArrayList<Timestamp>();
    
    public boolean refreshData(ResultSet stkDat2set) {
        ResultSet rs = stkDat2set;
        try {
            cur_pri = rs.getDouble("cur_pri");
            cur_qty = rs.getLong("dl_stk_num");
            pct = (cur_pri - rs.getDouble("td_opn_pri"))/ rs.getDouble("td_opn_pri");
            pctToCls = (cur_pri - rs.getDouble("yt_cls_pri"))/ rs.getDouble("yt_cls_pri");
            dlmnynum = rs.getDouble("dl_mny_num");
            dl_dt = rs.getTimestamp("dl_dt");
            cur_pri_lst.add(cur_pri);
            dl_stk_num_lst.add(cur_qty);
            dl_mny_num_lst.add(dlmnynum);
            pctToCls_lst.add(pctToCls);
            dl_dt_lst.add(dl_dt);
            return true;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public class Follower{
        Follower(String identifier, int fc) {
            id = identifier;
            followCnt =fc;
        }
        public String id;
        public int followCnt;
    }
    public void constructFollowers()
    {
        Connection con = DBManager.getConnection();
        followers = new HashMap<String, List<Follower>>();
        String sql = "";
        Statement stm = null, stm2 = null;
        ResultSet rs = null, rs2 = null;
        String dt = "";
        int pctRk = 0;

        try {
            log.info("construct followers for: " + ID + " name:" + Name);
            stm = con.createStatement();
            sql = "select s2.dt, cast((s1.yt_cls_pri - s2.yt_cls_pri) / s2.yt_cls_pri / 0.01 as int) pctRk"
                + "  from stkDlyInfo s1, stkDlyInfo s2 "
                + " where s1.id = s2.id "
                + "   and to_char(to_date(s1.dt, 'yyyy-mm-dd hh:mi:ss') - 1, 'yyy-mm-dd') = to_char(to_date(s2.dt, 'yyyy-mm-dd hh:mi:ss'), 'yyy-mm-dd')"
                + "   and s1.id = '" + ID + "'"
                + "   and to_char(to_date(s1.dt, 'yyyy-mm-dd hh:mi:ss'), 'yyyy-mm-dd') >= to_char(sysdate - 10, 'yyyy-mm-dd')"
                + "   and abs (cast((s1.yt_cls_pri - s2.yt_cls_pri) / s2.yt_cls_pri / 0.01 as int)) > 7 "
                + "  order by dt ";

            log.info(sql);
            rs = stm.executeQuery(sql);
            while (rs.next()) {

                dt = rs.getString("dt");
                pctRk = rs.getInt("pctRk");

                if (pctRk >= 0) {
                    sql = "select s2.id, s2.dt "
                       + "  from stkDlyInfo s1, stkDlyInfo s2 "
                       + " where s1.id = s2.id "
                       + "   and to_char(to_date(s1.dt, 'yyyy-mm-dd hh:mi:ss') - 1, 'yyy-mm-dd') = to_char(to_date(s2.dt, 'yyyy-mm-dd hh:mi:ss'), 'yyy-mm-dd')"
                       + "   and s2.dt = '" + dt + "'"
                       + "   and cast((s1.yt_cls_pri - s2.yt_cls_pri) / s2.yt_cls_pri / 0.01 as int) >= " + pctRk;
                }
                else if (pctRk < 0) {
                    sql = "select s2.id, s2.dt "
                        + "  from stkDlyInfo s1, stkDlyInfo s2 "
                        + " where s1.id = s2.id "
                        + "   and to_char(to_date(s1.dt, 'yyyy-mm-dd hh:mi:ss') - 1, 'yyy-mm-dd') = to_char(to_date(s2.dt, 'yyyy-mm-dd hh:mi:ss'), 'yyy-mm-dd')"
                        + "   and s2.dt = '" + dt + "'"
                        + "   and cast((s1.yt_cls_pri - s2.yt_cls_pri) / s2.yt_cls_pri / 0.01 as int) <= " + pctRk;
                }
                else {
                    continue;
                }

                log.info(sql);

                stm2 = con.createStatement();
                rs2 = stm2.executeQuery(sql);

                while (rs2.next()) {
                    boolean existFlg = false;
                    List<Follower> fs = followers.get(String.valueOf(pctRk));
                    
                    //log.info("got follower:" + rs2.getString("id"));
                    
                    if (fs != null) {
                        for (int i = 0; i < fs.size(); i++) {
                            Follower f = fs.get(i);
                            if (f.id.equals(rs2.getString("id"))) {
                                f.followCnt++;
                                existFlg = true;
                                break;
                            }
                        }
                    }
                    
                    if (!existFlg) {
                        if (fs == null) {
                            fs = new LinkedList<Follower>();
                            followers.put(String.valueOf(pctRk), fs);
                        }
                        
                        fs.add(new Follower(rs2.getString("id"), 1));
                    }
                }
                rs2.close();
                stm2.close();
            }
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        finally {
            try{
                rs.close();
                stm.close();
                con.close();
            }
            catch (SQLException e2) {
                e2.printStackTrace();
            }
        }
        //printFollowers();
    }
    
    public void printFollowers()
    {
        log.info("Now print followers for " + ID);
        if (followers != null) {
            for(String key : followers.keySet()) {
                log.info("Print key:" + key);
                List<Follower> fs = followers.get(key);
                if (fs != null) {
                    for(int i = 0; i < fs.size(); i++) {
                        Follower f = fs.get(i);
                        if (f.followCnt > 2)
                        log.info(f.id + "|" + f.followCnt);
                    }
                    
                }
            }
        }
        log.info("Print followers end...");
    }
    
    public double getFollowersAvgCurPri(int pctRk) {
        List<Follower> fs = followers.get(String.valueOf(pctRk));
        if (fs == null) {
            return 0.0;
        }
        String ids = "";
        double avgPct = 0.0;
        for (int i = 0; i < fs.size(); i++) {
            Follower f = fs.get(i);
            if (f.followCnt > 2 && !f.id.equals(ID)) {
                if (ids.length() > 0) {
                    ids += ",";
                }
                ids += "'" + f.id + "'";
            }
        }
        if (ids.length() <= 0) {
            log.info("No followers more than 2 for stock:" + ID + " skip cal avgCurPri for followers...");
            return 0.0;
        }
        try {
            Connection con = DBManager.getConnection();
            Statement stm = con.createStatement();
            String sql = "select avg((cur_pri - yt_cls_pri) / yt_cls_pri) avgPct from stkdat2 " +
                         //"  where to_char(dl_dt, 'yyyy-mm-dd') = '2016-02-02'"//to_char(sysdate, 'yyyy-mm-dd') "
                         "  where to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') "
                       + "  and id in (" + ids + ")"
                       + "  and not exists (select 'x' from stkdat2 sd " +
                            "                where sd.id = stkdat2.id " +
                            //"                  and to_char(sd.dl_dt, 'yyyy-mm-dd') = '2016-02-02' "//to_char(sysdate, 'yyyy-mm-dd') "
                            "                  and to_char(sd.dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') "
                          + "                  and sd.ft_id > stkdat2.ft_id)";
            log.info(sql);
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) {
                avgPct = rs.getDouble("avgPct");
                log.info("Got avgPct for followers: " + avgPct);
            }
            rs.close();
            stm.close();
            con.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return avgPct;
    }
    
    public double getCurPct() {
        double curPct = 0.0;
        Connection con = null;
        Statement stm = null;
        ResultSet rs = null;
        try {
            con = DBManager.getConnection();
            stm = con.createStatement();
            String sql = "select (cur_pri - yt_cls_pri) / yt_cls_pri pct from stkdat2 " +
                         //"  where to_char(dl_dt, 'yyyy-mm-dd') = '2016-02-02'"//to_char(sysdate, 'yyyy-mm-dd') "
                         "  where to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') "
                       + "  and id = '" + ID + "'"
                       + "  and not exists (select 'x' from stkdat2 sd " +
                            "                where sd.id = stkdat2.id " +
                           // "                  and to_char(sd.dl_dt, 'yyyy-mm-dd') = '2016-02-02' "//to_char(sysdate, 'yyyy-mm-dd') "
                            "                  and to_char(sd.dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') "
                          + "                  and sd.ft_id > stkdat2.ft_id)";
            log.info(sql);
            rs = stm.executeQuery(sql);
            if (rs.next()) {
                curPct = rs.getDouble("pct");
                log.info("Got curPct : " + curPct);
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            try {
                rs.close();
                stm.close();
                con.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return curPct;
    }
    

}
