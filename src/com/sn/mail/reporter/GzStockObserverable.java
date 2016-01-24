package com.sn.mail.reporter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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

    public String subject;
    public String content;
    private boolean hasSentMail = false;
    public String getSubject() {
        return subject;
    }
    public String getContent() {
        return content;
    }
    public boolean hasSentMail()
    {
        return hasSentMail;
    }
    public GzStockObserverable()
    {
        this.addObserver(StockPriceObserver.globalObs);
    }

    static public void main(String[] args) {
        GzStockObserverable ppo = new GzStockObserverable();
        ppo.update();
    }

    public void update() {
        log.info("GzStockObserverable update...");
        Connection con = DBManager.getConnection();
        Statement stm = null;
        try {
            stm = con.createStatement();
            String sql = "select id from stk where gz_flg = 1";
            ResultSet rs = stm.executeQuery(sql);
            List<String> gzStocks = new ArrayList<String>();

            while (rs.next()) {
                gzStocks.add(rs.getString("id"));
            }
            rs.close();
            subject = content = "";
            long incPriCnt = 0, eqlPriCnt = 0;
            long desPriCnt = 0;
            long detQty = 0, qtyCnt = 0;
            for (String stock : gzStocks) {
                sql = "select cur_pri, dl_stk_num from stkdat2 where id ='"
                        + stock
                        + "' and to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') order by ft_id";
                log.info(sql);
                System.out.println(sql);
                rs = stm.executeQuery(sql);
                try {
                    double pre_cur_pri = 0, cur_pri;
                    double pre_qty = 0, cur_qty;
                    incPriCnt = eqlPriCnt = desPriCnt = 0;
                    detQty = qtyCnt = 0;
                    while (rs.next()) {
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

                        if (pre_qty != 0) {
                            if (cur_qty > pre_qty) {
                                detQty += cur_qty - pre_qty;
                            }
                            qtyCnt++;
                        }
                        pre_qty = cur_qty;
                    }
                    if (qtyCnt > 0) {
                        detQty = detQty / qtyCnt;
                    }
                    
                    double d0 = eqlPriCnt*1.0 /(incPriCnt + desPriCnt + eqlPriCnt);
                    double d1 = Math.abs(incPriCnt - desPriCnt)*1.0 / Math.abs(Math.max(incPriCnt, desPriCnt));
                    log.info("d0:" + d0 + "\nd1:" +d1);
                    System.out.println("d0:" + d0 + "\nd1:" +d1);
                    
                    if (incPriCnt + eqlPriCnt + desPriCnt > 0 &&
                            d0 < 0.3 && d1 > 0.1) {
                        if (subject.length() > 0) {
                            subject += ",";
                        }
                        subject += stock;
                        content += "\nStock:" + stock + "\n上涨次数：" + incPriCnt + " 下跌次数：" + desPriCnt + " 平衡次数 ：" + eqlPriCnt + "\n";
                        content += "平均成交量增幅:" + detQty + "\n";
                    }
                } catch (SQLException e1) {

                    e1.printStackTrace();
                }
            }
            if (content.length() > 0) {
                this.setChanged();
                this.notifyObservers(this);
                hasSentMail = true;
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("GzStockObserverable update errored:" + e.getMessage());
        }
    }

}