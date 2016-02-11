package com.sn.mail.reporter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import com.sn.db.DBManager;
import com.sn.mail.reporter.MailSenderType;
import com.sn.mail.reporter.MailSenderFactory;
import com.sn.mail.reporter.SimpleMailSender;
import com.sn.reporter.WCMsgSender;
import com.sn.stock.Stock;
import com.sn.stock.Stock.Follower;

public class GzStockObserverable extends Observable {

    static Logger log = Logger.getLogger(GzStockObserverable.class);

    static Connection con = DBManager.getConnection();
    public String subject;
    public String content;
    private boolean hasSentMail = false;
    private boolean needSentMail = false;
    static private Map<String, Stock> stocks = null;

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public boolean hasSentMail() {
        return hasSentMail;
    }

    public GzStockObserverable() {
        this.addObserver(StockPriceObserver.globalObs);
    }

    static public void main(String[] args) {
        GzStockObserverable ppo = new GzStockObserverable();
        ppo.update();
    }

    public void update() {
        String gzSummary = "", otherStockSummary = "", index = "", fmsg = "", stockPlused = "";
        needSentMail = false;
        if (stocks == null && !loadStocks()) {
            log.info("loadStocks failed, no mail can be sent");
            return;
        }
        gzSummary = checkStatusForStock(true, 0.0);
        otherStockSummary = checkStatusForStock(false, 0.01);
        index = getIndex();
        fmsg = getFollowers();
        stockPlused = getDetQtyPlused();
        subject = content = "";
        subject = "News";
        content = index + gzSummary + "<br/>" + otherStockSummary + "<br/>" + fmsg + "<br/>" + stockPlused;
        if (needSentMail) {
            this.setChanged();
            this.notifyObservers(this);
            hasSentMail = true;
        }
    }
    
    private String checkStatusForStock(boolean gz_flg, double pctRt)
    {

        log.info("GzStockObserverable calculate gz_flg:" + gz_flg + " pctRt:" + pctRt);
        Statement stm = null;
        String summary = "";
        if (gz_flg) {
            summary = "关注股票(pctRt:" + pctRt + ") 如下：<br/>";
        }
        else {
            summary = "非关注股票(pctRt:" + pctRt + ") 如下：<br/>";
        }
        summary += "<table border = 1>" +
                   "<tr>" +
                   "<th> Pct</th> " +
                   "<th> PrePct</th> " +
                   "<th> RK </th> " +
                   "<th> RankSpeed</th> " +
                   "<th> KeepDaysLost</th> " +
                   "<th> ID </th> " +
                   "<th> Name </th> " +
                   "<th> incCnt</th> " +
                   "<th> dscCnt</th> " +
                   "<th> detQty</th> " +
                   "<th> cur_pri</th> " +
                   "<th> qty </th> </tr> ";
        try {
            stm = con.createStatement();
            String sql = "select id, name from stk where gz_flg = " + (gz_flg ? "1" : "0");
            ResultSet rs = stm.executeQuery(sql);
            Map<String, String> gzStocks = new HashMap<String, String>();

            while (rs.next()) {
                gzStocks.put(rs.getString("id"), rs.getString("name"));
            }
            
            rs.close();
            
            long incPriCnt = 0, eqlPriCnt = 0;
            long desPriCnt = 0;
            long detQty = 0, qtyCnt = 0;

            Map<String, String> rowMap = new HashMap<String, String>();
            List<Stock> sl = new ArrayList<Stock>();
            
            for (String stock : gzStocks.keySet()) {
                try {
                    sql = "select cur_pri, td_opn_pri, dl_stk_num from stkdat2 where id ='"
                            + stock
                            + "' and to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate , 'yyyy-mm-dd') "
                           // + "' and to_char(dl_dt, 'yyyy-mm-dd') >= '2016-02-05'"
                            + "  and dl_dt >= sysdate - (5*1.0)/(24*60.0) "
                            + "  and td_opn_pri > 0 order by ft_id ";
                    
                    log.info(sql);
                    
                    rs = stm.executeQuery(sql);
                    
                    double pre_cur_pri = 0, cur_pri = 0;
                    long pre_qty = 0, cur_qty = 0;
                    double pct = 2;
                    boolean hasStkInfo = false;
                    
                    incPriCnt = eqlPriCnt = desPriCnt = 0;
                    detQty = qtyCnt = 0;
                    while (rs.next()) {
                        hasStkInfo = true;
                        cur_pri = rs.getDouble("cur_pri");
                        cur_qty = rs.getLong("dl_stk_num");
                        if (pre_cur_pri != 0) {
                            if (cur_pri > pre_cur_pri) {
                                incPriCnt++;
                            } else if (cur_pri == pre_cur_pri) {
                                eqlPriCnt++;
                            } else {
                                desPriCnt++;
                            }
                        }
                        pre_cur_pri = cur_pri;
                        
                        pct = (cur_pri - rs.getDouble("td_opn_pri"))/ rs.getDouble("td_opn_pri");

                        if (pre_qty != 0) {
                            if (cur_qty > pre_qty) {
                                detQty += cur_qty - pre_qty;
                            }
                            else {
                                log.info("what's this");
                            }
                            qtyCnt++;
                        }
                        pre_qty = cur_qty;
                    }
                    if (qtyCnt > 0) {
                        detQty = detQty / qtyCnt;
                    }
                    
                    rs.close();
                    int rowCnt = 0, daysToTrack = 4;
                    sql = "select * from ("
                        + "select id, to_char(dl_dt, 'yyyy-mm-dd') dt, max(yt_cls_pri) yt_cls_pri "
                        + "from stkdat2 "
                        + "where id = '" + stock + "' "
                        + "group by to_char(dl_dt, 'yyyy-mm-dd'), id "
                        + "order by dt desc) "
                        + "where rownum <= " + daysToTrack;

                    log.info(sql);
                    
                    double pre_ys_cls_pri = -1, prePct = 0.0;
                    long keepDaysLost = 1;
                    rs = stm.executeQuery(sql);
                    while (rs.next()) {
                        rowCnt++;
                        if (pre_ys_cls_pri == -1) {
                            pre_ys_cls_pri = rs.getDouble("yt_cls_pri");
                            continue;
                        }
                        else {
                            prePct += (pre_ys_cls_pri - rs.getDouble("yt_cls_pri")) / rs.getDouble("yt_cls_pri");
                            if (pre_ys_cls_pri > rs.getDouble("yt_cls_pri")) {
                                keepDaysLost = 0;
                            }
                            pre_ys_cls_pri = rs.getDouble("yt_cls_pri");
                        }
                    }
                    
                    if (rowCnt > 0) {
                        prePct = prePct / rowCnt;
                    }
                    
                    log.info("Past " + daysToTrack + " avg pct:" + prePct + " keepLostDays:" + keepDaysLost);
                    

                    if (hasStkInfo && incPriCnt + eqlPriCnt + desPriCnt > 0) {

                        log.info("pri pct is:" + pct + " against:" + pctRt);

                        Stock stk = stocks.get(stock);
                        stk.setIncPriCnt(incPriCnt);
                        stk.setDesPriCnt(desPriCnt);
                        stk.setDetQty(detQty);
                        stk.setCur_pri(cur_pri);
                        stk.setCur_qty(cur_qty);
                        stk.setPct(pct);
                        stk.setPrePct(prePct);
                        stk.setKeepLostDays(keepDaysLost);

                        if (Math.abs(pct) >= pctRt) {
                            needSentMail = true;
                            sl.add(stk);
                        }
                    }
                } catch (SQLException e1) {
                    log.info("No stkdat2 infor for stock " + stock + "continue...");
                    continue;
                }
            }
            
            Collections.sort(sl);
            
            int rk = 1;
            for (Stock p : sl) {
                p.setRk(rk);
                rk++;
                // only return stock with price <= 20
                if ((p.getCur_pri() <= 50 && p.getPrePct() < -0.05) ||
                     p.getGz_flg() > 0 ||
                     (p.getCur_pri() <= 50 && p.getKeepLostDays() > 0 && p.getPrePct() < -0.01)) {
                    summary += p.getTableRow();
                }
            }
            summary += "</table>";
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("GzStockObserverable update errored:" + e.getMessage());
        }
        log.info("GzStockObserverable got summary:" + summary);
        return summary;
    }
    
    private String getFollowers() {

        String fmsg = "同增股票<br/>";
        fmsg += "<table border = 1>" +
                   "<tr>" +
                   "<th> Stock</th> " +
                   "<th> Name</th> " +
                   "<th> CurPri</th> " +
                   "<th> CurPct</th> " +
                   "<th> Followers+ </th> " +
                   "<th> FollowersAvgPct+ </th> " +
                   "<th> Followers- </th> " +
                   "<th> FollowersAvgPct- </th> </tr> ";
        for (String stk : stocks.keySet()) {
            Stock s = stocks.get(stk);
            String cp = ""; // for FollowerCnt+
            double cpavgpct = 0.0;
            String cm = ""; // for FollowerCnt-
            double cmavgpct = 0.0;
            Map<String, List<Follower>> fs = s.getFollowers();
            for (String pct : fs.keySet()) {
                List<Follower> f1 = fs.get(pct);

                cm = cp = "";
                for (int j = 0; j < f1.size(); j++) {
                    Follower f = f1.get(j);
                    if (f.followCnt > 2 && !f.id.equals(s.getID())) {
                        if (pct.startsWith("-")) {
                            if (cm.length() == 0) {
                                cm = "[" + pct + "]";
                            }
                            cm += f.id + "(" + f.followCnt + ")|";
                        }
                        else {
                            if (cp.length() == 0) {
                                cp = "[" + pct + "]";
                            }
                            cp += f.id + "(" + f.followCnt + ")|";
                        }
                    }
                }
                
                if (pct.startsWith("-") && cm.length() > 0) {
                    cmavgpct = s.getFollowersAvgCurPri(Integer.valueOf(pct));
                }
                else if (cp.length() > 0) {
                    cpavgpct = s.getFollowersAvgCurPri(Integer.valueOf(pct));
                }
            }
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(2);
            if (cm.length() > 0 || cp.length() > 0)
            {
                fmsg += "<tr> <td>" + s.getID() + "</td>" +
                        "<td> " + s.getName() + "</td>" +
                        "<td> " + nf.format(s.getCur_pri()) + "</td>" +
                        "<td> " + nf.format(s.getCurPct()*100) + "%</td>" +
                        "<td> " + cp + "</td>" +
                         "<td> " + nf.format(cpavgpct*100) + "%</td>" +
                         "<td> " + cm + "</td>" +
                         "<td> " + nf.format(cmavgpct*100) + "%</td></tr>";
                needSentMail = true;
            }
        }
        fmsg += "</table>";

        log.info("got follower msg:" + fmsg);
        return fmsg;
    
    }
    
    private String getDetQtyPlused() {
        String StockPlused = "猛成交量股票<br/>";
        StockPlused += "<table border = 1>" +
                   "<tr>" +
                   "<th> Stock</th> " +
                   "<th> Name</th> " +
                   "<th> CurPri</th> " +
                   "<th> DetQty </th> " +
                   "<th> Pre_DetQty </th> </tr> ";
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        for (String stk : stocks.keySet()) {
               Stock s = stocks.get(stk);
               if (s.isPre_detQty_plused()) {
                   StockPlused += "<tr> <td>" + s.getID() + "</td>" +
                        "<td> " + s.getName() + "</td>" +
                        "<td> " + nf.format(s.getCur_pri()) + "</td>" +
                        "<td> " + s.getDetQty() + "</td>" +
                        "<td> " + s.getPre_detQty() + "</td></tr>";
                   needSentMail = true;
               }
        }
        StockPlused += "</table>";

        log.info("got StockPlused msg:" + StockPlused);
        return StockPlused;
    }
    
    private String getIndex() {
        Statement stm = null;
        String index = "股市行情<br/>";
        int StkNum = 0;
        int TotInc = 0;
        int TotDec = 0;
        int TotEql = 0;
        double AvgIncPct = 0.0;
        double AvgDecPct = 0.0;
        int catagory = -2;
        index += "<table border = 1>" +
                   "<tr>" +
                   "<th> Stock Count</th> " +
                   "<th> Total+ </th> " +
                   "<th> AvgPct+ </th> " +
                   "<th> Total-</th> " +
                   "<th> AvgPct-</th> " + 
                   "<th> Total= </th> " +
                   "<th> Degree </th> </tr> ";
        try {
            stm = con.createStatement();
            String sql = "select count(case when cur_pri > td_opn_pri then 1 else 0 end) IncNum, " +
                         "       count(case when cur_pri < td_opn_pri then 1 else 0 end) DecNum, " +
                         "       count(case when cur_pri = td_opn_pri then 1 else 0 end) EqlNum, " +
            		     " avg((cur_pri - td_opn_pri)/td_opn_pri) avgPct," +
                         " case when cur_pri > td_opn_pri then 1 " +
                         "               when cur_pri < td_opn_pri then -1 " +
                         "               when cur_pri = td_opn_pri then 0 end catagory " +
            		     " from stkdat2 " +
            		     " where td_opn_pri > 0 " +
            		     "   and not exists (select 'x' from stkdat2 skd where skd.id = stkdat2.id and ft_id > stkdat2.ft_id)" +
            		     " group by case when cur_pri > td_opn_pri then 1 " +
            		     "               when cur_pri < td_opn_pri then -1 " +
            		     "               when cur_pri = td_opn_pri then 0 end" +
            		     " order by catagory asc";
            ResultSet rs = stm.executeQuery(sql);

            while (rs.next()) {
                catagory = rs.getInt("catagory");
                if (catagory == -1)
                {
                    TotDec = rs.getInt("DecNum");
                    AvgDecPct = rs.getDouble("avgPct");
                }
                else if (catagory == 0)
                {
                    TotEql = rs.getInt("EqlNum");
                }
                else if (catagory == 1)
                {
                    TotInc = rs.getInt("IncNum");
                    AvgIncPct = rs.getDouble("avgPct");
                }
            }
            rs.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        StkNum = TotDec + TotInc + TotEql;
        
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        if (catagory != -2)
        {
            index += "<tr> <td>" + StkNum + "</td>" +
                    "<td> " + TotInc + "</td>" +
                     "<td> " + nf.format(AvgIncPct*100) + "%</td>" +
                     "<td> " + TotDec + "</td>" +
                     "<td> " + nf.format(AvgDecPct*100) + "%</td>" +
                     "<td> " + TotEql + "</td>" +
                     "<td> " + nf.format((TotInc * AvgIncPct + TotDec * AvgDecPct) * 100.0 / (TotInc * 0.1 + TotDec * 0.1)) + " C</tr></table>";
        }
        log.info("got index msg:" + index);
        return index;
    }
    
    private boolean loadStocks() {

        Statement stm = null;
        ResultSet rs = null;
        
        stocks = new HashMap<String, Stock>();
        Stock s = null;
        int Total = 0, cnt = 0;
        try {
            stm = con.createStatement();
            String sql = "select id, name, gz_flg from stk order by id";
            rs = stm.executeQuery(sql);
            
            String id, name;
            
            while (rs.next()) {
                id = rs.getString("id");
                name = rs.getString("name");
                s = new Stock(id, name, rs.getLong("gz_flg"));
                s.constructFollowers();
                stocks.put(id, s);
                cnt++;
                log.info("LoadStocks completed:" + cnt * 1.0 / 2811);
            }
            rs.close();
            stm.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        log.info("GzStockObserverable loadStock successed!");
        return true;
    
    }
}