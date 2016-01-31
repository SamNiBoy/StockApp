package com.sn.mail.reporter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
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
        String gzSummary = "", otherStockSummary = "";
        needSentMail = false;
        gzSummary = checkStatusForStock(true, 0.02);
        otherStockSummary = checkStatusForStock(false, 0.02);
        subject = content = "";
        subject = "股票实时信息播报";
        content = gzSummary + "\n" + otherStockSummary;
        if (needSentMail) {
            this.setChanged();
            this.notifyObservers(this);
            hasSentMail = true;
        }
    }
    
    private String checkStatusForStock(boolean gz_flg, double pctRt)
    {

        log.info("GzStockObserverable calculate gz_flg:" + gz_flg + " eqlRt:" + pctRt);
        Statement stm = null;
        String summary = "";
        if (gz_flg) {
            summary = "关注股票(pctRt:" + pctRt + ") 如下：\n";
        }
        else {
            summary = "非关注股票(pctRt:" + pctRt + ") 如下：\n";
        }
        try {
            stm = con.createStatement();
            String sql = "select id, name from stk where gz_flg = " + (gz_flg ? "1" : "gz_flg");
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
            nf.setMaximumFractionDigits(3);
            
            for (String stock : gzStocks.keySet()) {
                // Always give id for gzed stock.
                if (gz_flg) {
                    summary += "[" + stock + ":" + gzStocks.get(stock) + "\n";
                }
                
                try {
                    sql = "select cur_pri, td_opn_pri, dl_stk_num from stkdat2 where id ='"
                            + stock
                            + "' and to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate , 'yyyy-mm-dd') "
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
                        log.info("pri pct is:" + pct + " against:" + pctRt);
                        if (Math.abs(pct) >= pctRt) {
                            if (!gz_flg) {
                                summary += stock + ":" + gzStocks.get(stock) + "\n";
                            }
                            needSentMail = true;
                            summary += "涨:" + incPriCnt + " 跌:" + desPriCnt
                                    + " 平:" + eqlPriCnt + " 幅:[" + nf.format(pct*100) + "/" + detQty + "] 价:" + cur_pri + " 量:" + cur_qty + "]\n";
                        }
                        else if (gz_flg){
                            summary += "]";
                        }
                    }
                    else if (!hasStkInfo && gz_flg) {
                        summary += "]";
                    }
                } catch (SQLException e1) {
                    log.info("No stkdat2 infor for stock " + stock + "continue...");
                    continue;
                }
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("GzStockObserverable update errored:" + e.getMessage());
        }
        log.info("GzStockObserverable got summary:" + summary);
        return summary;
    }

}