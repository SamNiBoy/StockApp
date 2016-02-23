package com.sn.mail.reporter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

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

public class StockObserverable extends Observable {

    static Logger log = Logger.getLogger(StockObserverable.class);

    static Connection con = DBManager.getConnection();
    public String subject;
    public String content;
    private boolean hasSentMail = false;
    private boolean needSentMail = false;
    static public ConcurrentHashMap<String, Stock> stocks = null;

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public boolean hasSentMail() {
        return hasSentMail;
    }

    public StockObserverable() {
        this.addObserver(StockObserver.globalObs);
    }

    static public void main(String[] args) {
        StockObserverable ppo = new StockObserverable();
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
        //stockPlused = getDetQtyPlused();
        String returnStr = "";  
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
        Date date = new Date();  
        returnStr = f.format(date);
        subject = content = "";
        subject = "News " + returnStr;
        content = index + gzSummary + "<br/>" + otherStockSummary + "<br/>" + fmsg + "<br/>";
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
                   "<th> PTC</th> " +
                   "<th> PTO</th> " +
                   "<th> P4C</th> " +
                   "<th> KDL</th> " +
                   "<th> PRK </th> " +
                   "<th> RKS</th> " +
                   "<th> ID </th> " +
                   "<th> Name </th> " +
                   "<th> IncCnt</th> " +
                   "<th> DscCnt</th> " +
                   "<th> DQR</th> " +
                   "<th> Price</th> " +
                   "<th> Qty</th> " +
                   "<th> Mny </th> </tr> ";
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
                    sql = "select cur_pri, td_opn_pri, dl_stk_num, yt_cls_pri, dl_mny_num from stkdat2 where id ='"
                            + stock
                            + "' and to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate , 'yyyy-mm-dd') "
                           // + "' and to_char(dl_dt, 'yyyy-mm-dd') >= '2016-02-05'"
                            + "  and dl_dt >= sysdate - (5*1.0)/(24*60.0) "
                            + "  and td_opn_pri > 0 order by ft_id ";
                    
                    log.info(sql);
                    
                    rs = stm.executeQuery(sql);
                    
                    double pre_cur_pri = 0, cur_pri = 0;
                    long pre_qty = 0, cur_qty = 0;
                    double pct = 0, pctToCls = 0;
                    double totMny = 0.0;
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
                        pctToCls = (cur_pri - rs.getDouble("yt_cls_pri"))/ rs.getDouble("yt_cls_pri");

                        totMny = rs.getDouble("dl_mny_num");

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
                        stk.setPctToCls(pctToCls);
                        stk.setDlmnynum(totMny);
                        stk.setPrePct(prePct);
                        stk.setKeepLostDays(keepDaysLost);
                        stk.setGz_flg((gz_flg ? 1 : 0));

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
                if (p.isGoodCandidateForBuy()) {
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
                   "<th> Pct</th> " +
                   "<th> Rate </th> " +
                   "<th> DetQty </th> " +
                   "<th> Pre_DetQty </th></tr> ";
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        List<Stock> ls = new ArrayList<Stock>();
        for (String stk : stocks.keySet()) {
            ls.add(stocks.get(stk));
        }
        
//        Collections.sort(ls, new Comparator<Stock>() {
//            @Override
//            public int compare(Stock l, Stock r) {
//                // TODO Auto-generated method stub
//                double lr = l.getDetQty() * 1.0 / l.pre_detQty;
//                double rr = r.getDetQty() * 1.0 / r.pre_detQty;
//                if ( lr > rr)
//                {
//                    return -1;
//                }
//                else if (lr == rr ){
//                    return 0;
//                }
//                else {
//                    return 1;
//                }
//            }
//        });
        for (Stock s : ls) {
               if (s.isPre_detQty_plused()) {
                   StockPlused += "<tr> <td>" + s.getID() + "</td>" +
                        "<td> " + s.getName() + "</td>" +
                        "<td> " + nf.format(s.getCur_pri()) + "</td>" +
                        "<td> " + nf.format(s.getPct() * 100) + "%</td>" +
                        "<td> " + nf.format(s.pre_detQty == 0 ? 0.0 : s.getDetQty() / s.pre_detQty) + "</td>" +
                        "<td> " + s.getDetQty() + "</td>" +
                        "<td> " + s.getPre_detQty() + "</td> </tr>";
                   needSentMail = true;
               }
        }
        ls = null;
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
        double totIncDlMny = 0.0;
        double totDecDlMny = 0.0;
        double totEqlDlMny = 0.0;
        int catagory = -2;
        index += "<table border = 1>" +
                   "<tr>" +
                   "<th> Stock Count</th> " +
                   "<th> Total+ </th> " +
                   "<th> AvgPct+ </th> " +
                   "<th> TotDlMny+ </th> " +
                   "<th> Total-</th> " +
                   "<th> AvgPct-</th> " + 
                   "<th> TotDlMny- </th> " +
                   "<th> Total= </th> " +
                   "<th> TotDlMny= </th> " +
                   "<th> Degree </th> </tr> ";
        try {
            stm = con.createStatement();
            String sql = "select count(case when cur_pri > td_opn_pri then 1 else 0 end) IncNum, " +
                         "       count(case when cur_pri < td_opn_pri then 1 else 0 end) DecNum, " +
                         "       count(case when cur_pri = td_opn_pri then 1 else 0 end) EqlNum, " +
            		     " avg((cur_pri - td_opn_pri)/td_opn_pri) avgPct," +
            		     " sum(dl_mny_num) totDlMny," +
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
                    totDecDlMny = rs.getDouble("totDlMny");
                }
                else if (catagory == 0)
                {
                    TotEql = rs.getInt("EqlNum");
                    totEqlDlMny = rs.getDouble("totDlMny");
                }
                else if (catagory == 1)
                {
                    TotInc = rs.getInt("IncNum");
                    AvgIncPct = rs.getDouble("avgPct");
                    totIncDlMny = rs.getDouble("totDlMny");
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
                     "<td> " + nf.format(totIncDlMny/100000000) + "亿</td>" +
                     "<td> " + TotDec + "</td>" +
                     "<td> " + nf.format(AvgDecPct*100) + "%</td>" +
                     "<td> " + nf.format(totDecDlMny/100000000) + "亿</td>" +
                     "<td> " + TotEql + "</td>" +
                     "<td> " + nf.format(totEqlDlMny/100000000) + "亿</td>" +
                     "<td> " + nf.format((TotInc * AvgIncPct + TotDec * AvgDecPct) * 100.0 / (TotInc * 0.1 + TotDec * 0.1)) + " C</tr></table>";
        }
        log.info("got index msg:" + index);
        return index;
    }
    
    static public boolean loadStocks() {

        Statement stm = null;
        ResultSet rs = null;
        
        stocks = new ConcurrentHashMap<String, Stock>();
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
                s.setCur_pri(7.8);
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