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

public class GzStockObserverable extends Observable {

    static Logger log = Logger.getLogger(GzStockObserverable.class);

    static Connection con = DBManager.getConnection();
    public String subject;
    public String content;
    private boolean hasSentMail = false;
    private boolean needSentMail = false;

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
        String gzSummary = "", otherStockSummary = "", index = "";
        needSentMail = false;
        gzSummary = checkStatusForStock(true, 0.0);
        otherStockSummary = checkStatusForStock(false, 0.01);
        index = getIndex();
        subject = content = "";
        subject = "News";
        content = gzSummary + "<br/>" + otherStockSummary + index;
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
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(2);
            Map<String, String> rowMap = new HashMap<String, String>();
            List<pair> sl = new ArrayList<pair>();
            
            for (String stock : gzStocks.keySet()) {
                try {
                    sql = "select cur_pri, td_opn_pri, dl_stk_num from stkdat2 where id ='"
                            + stock
                            + "' and to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate , 'yyyy-mm-dd') "
                            //+ "' and to_char(dl_dt, 'yyyy-mm-dd') >= '2015-05-29'"
                            + "  and dl_dt >= sysdate - (5*1.0)/(24*60.0) "
                            + "  and td_opn_pri > 0 order by ft_id ";
                    
                    log.info(sql);
                    
                    rs = stm.executeQuery(sql);
                    
                    double pre_cur_pri = 0, cur_pri = 0;
                    double pre_qty = 0, cur_qty = 0;
                    double pct = 2;
                    boolean hasStkInfo = false;
                    
                    incPriCnt = eqlPriCnt = desPriCnt = 0;
                    detQty = qtyCnt = 0;
                    while (rs.next()) {
                        hasStkInfo = true;
                        cur_pri = rs.getDouble("cur_pri");
                        cur_qty = rs.getDouble("dl_stk_num");
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

                    if (hasStkInfo && incPriCnt + eqlPriCnt + desPriCnt > 0) {
                        String row = "";
                        log.info("pri pct is:" + pct + " against:" + pctRt);
                        if (Math.abs(pct) >= pctRt) {
                            needSentMail = true;
                            row = "<tr> <td> " + nf.format(pct*100) + "</td>" +
                                  "<td> " + stock + "</td> " +
                                  "<td> " + gzStocks.get(stock) + "</td>" +
                                  "<td> " + incPriCnt + "</td>" +
                                  "<td> " + desPriCnt + "</td>" +
                                  "<td> " + detQty + "</td>" +
                                  "<td> " + cur_pri + "</td> " +
                                  "<td> " + cur_qty + "</td> </tr>";
                            rowMap.put(stock, row);
                         
                            sl.add(new pair(stock, pct));
                        }
                    }
                } catch (SQLException e1) {
                    log.info("No stkdat2 infor for stock " + stock + "continue...");
                    continue;
                }
            }
            
            Collections.sort(sl);
            
            for (pair p : sl) {
                summary += rowMap.get(p.ID);
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
    
    private String getIndex() {
        Statement stm = null;
        String index = "";
        int StkNum = 0;
        int TotInc = 0;
        int TotDec = 0;
        int TotEql = 0;
        double AvgIncPct = 0.0;
        double AvgDecPct = 0.0;
        int catagory = -2;
        index += "<table border = 1>" +
                   "<tr>" +
                   "<th> Stock Number</th> " +
                   "<th> Total+ </th> " +
                   "<th> AvgPct+ </th> " +
                   "<th> Total-</th> " +
                   "<th> AvgPct-</th> " + 
                   "<th> Total= </th> </tr> ";
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
                     "<td> " + nf.format(AvgIncPct) + "</td>" +
                     "<td> " + TotDec + "</td>" +
                     "<td> " + nf.format(AvgDecPct) + "</td>" +
                     "<td> " + TotEql + "</td></tr></table>";
        }
        log.info("got index msg:" + index);
        return index;
    }
    
    class pair implements Comparable<pair>{
        public String ID;
        public double pct;
        
        public pair(String idd, double pctt)
        {
            ID = idd;
            pct = pctt;
        }
        
        @Override
        public int compareTo(pair o) {
            // TODO Auto-generated method stub
            return pct - o.pct > 0 ? -1 : 1;
        }
        
    }

}