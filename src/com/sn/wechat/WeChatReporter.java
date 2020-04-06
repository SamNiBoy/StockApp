package com.sn.wechat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.task.addmail.AddMail;
import com.sn.task.enasuggeststock.EnaSuggestStock;
import com.sn.db.DBManager;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.strategy.algorithm.param.ParamManager;
import com.sn.task.EnaUsrBuySell;
import com.sn.task.TaskManager;
import com.sn.task.usrStock;
import com.sn.task.fetcher.GzStockDataFetcher;
import com.sn.task.fetcher.StockDataFetcher;
import com.sn.task.monitor.MonitorStockData;
import com.sn.wechat.action.CalFetchStat;
import com.sn.wechat.action.GzStock;
import com.sn.wechat.action.ListGzStock;
import com.sn.wechat.action.ListSuggestStock;
import com.sn.wechat.action.ShutDownPC;
import com.sn.wechat.action.TopTenBst;
import com.sn.wechat.action.TopTenWst;
import com.sn.task.WorkManager;

public class WeChatReporter extends BaseWCReporter{

    static Logger log = Logger.getLogger(WeChatReporter.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        String wcMsg = "<xml><ToUserName><![CDATA[gh_a9586d5aa590]]></ToUserName>" +
        "<FromUserName><![CDATA[osCWfs-ZVQZfrjRK0ml-eEpzeop0]]></FromUserName>" +
        "<CreateTime>1431827441</CreateTime>" +
        "<MsgType><![CDATA[text]]></MsgType>" +
        "<Content><![CDATA[1]]></Content>" +
        "<MsgId>6149652032815793242</MsgId>" +
        "</xml>";
        
        String wcMsg2 = "<xml><ToUserName><![CDATA[gh_a9586d5aa590]]></ToUserName>" +
        "<FromUserName><![CDATA[osCWfs-ZVQZfrjRK0ml-eEpzeop0]]></FromUserName>" +
        "<CreateTime>1431827441</CreateTime>" +
        "<MsgType><![CDATA[event]]></MsgType>" +
        "<Event><![CDATA[subscribe]]></Event>" +
        "<Event_key><![CDATA[qrscene_123123]]></Event_key>" +
        "<Ticket><![CDATA[TICKET]]></Ticket>" +
        "</xml>";

        WeChatReporter wcr = new WeChatReporter();
        wcr.setWcMsg(wcMsg);
        wcr.getResponse();
    }


    public WeChatReporter() {

        log.info("WeChatReporter initilized...");
    }

    public String printHelp() {
    	if (is_admin_flg) {
        resContent = "你可以发送以下代码:\n"
                + "1.获取我关注的股票.\n" + "2.获取系统推荐股票.\n"
                + "3.启用/停止买卖.\n" + "4.启用/停止推荐.\n"
                + "5.启用/取消一键平仓.\n"
                + "6.查询交易赢利.\n"
                + "7.报告数据情况.\n"
                + "xxxxxx 关注/取消关注股票.\n"
                + "xxx@yyy.zzz添加邮箱接收买卖信息.\n";
    	}
    	else {
            resContent = "你可以发送以下代码:\n"
                    + "1.获取我关注的股票.\n"
                    + "2.获取系统推荐股票数据.\n"
                    + "3.启用/停止股票买卖.\n"
                    + "4.启用/停止推荐.\n"
                    + "5.启用/取消一键平仓.\n"
                    + "6.查询交易赢利.\n"
                    + "7.报告数据情况.\n"
                    + "xxxxxx 关注/取消关注股票.\n"
                    + "xxx@yyy.zzz添加邮箱接收买卖信息.\n";
    	}

        return resContent;
    }
    public String getResponse() {
        if (content == null || content.equals("")) {
            return printHelp();
        }
        
        log.info("got input:[" + content + "], firstly let's check tasks");

        if (msgType.equals("event")) {
        	if (content.equals("subscribe")) {
        		resContent = "欢迎关注微信:\n"
        				+ printHelp();
        	}
        }
        else {
        	// remap key for other user.
        	if (!is_admin_flg) {
        	}
            if (content.equals("1")) {
                String msg = "";
                Map<String, String> Stocks = new HashMap<String, String> ();
                Map<String, Integer> sellmodes = new HashMap<String, Integer> ();
                DecimalFormat df2 = new DecimalFormat("##.##");
                DecimalFormat df3 = new DecimalFormat("##.###");
                Connection mycon = DBManager.getConnection();
                Statement stm = null;

                try {
                    String sql = "select s.id, s.name, u.stop_trade_mode_flg "
                            + "     from stk s, usrStk u "
                            + "    where s.id = u.id "
                            + "      and u.gz_flg = 1 "
                            + "      and u.openID ='" + frmUsr + "' "
                            + "      and u.suggested_by in ('" + frmUsr + "','" + ParamManager.getStr2Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING") + "')";
                    log.info(sql);
                    stm = mycon.createStatement();
                    ResultSet rs = stm.executeQuery(sql);
                    
                    while (rs.next()) {
                        Stocks.put(rs.getString("id"), rs.getString("name"));
                        sellmodes.put(rs.getString("id"), rs.getInt("stop_trade_mode_flg"));
                    }
                    rs.close();
                    stm.close();
                    
                    if (Stocks.size() > 0)
                    {
                    for (String stock : Stocks.keySet()) {
                        double dev = 0;
                        double cur_pri = 0;
                        
                        int stkcnt = Stocks.keySet().size();

                        
                        String sellMode = (sellmodes.get(stock) == 1) ? "是" : "否";

                        try {
                            sql = "select avg(dev) dev from ("
                                   + "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, left(dl_dt, 10) atDay "
                                   + "  from stkdat2 "
                                   + " where id ='" + stock + "'"
                                   + "   and yt_cls_pri > 0 "
                                   + "   and left(dl_dt, 10) >= left(sysdate() - interval 7 day, 10)"
                                   + " group by left(dl_dt, 10)) t";
                            log.info(sql);
                            stm = mycon.createStatement();
                            rs = stm.executeQuery(sql);
                            if (rs.next()) {
                                dev = rs.getDouble("dev");
                            }
                            Stock2 stk = (Stock2)StockMarket.getStocks().get(stock);
                            Double cur_pri1 = stk.getCur_pri();
                            if (cur_pri1 != null) {
                                cur_pri = cur_pri1;
                            }
                            else {
                                log.info("cur_pri for stock:" + stock + " is null, use -1.");
                                cur_pri = -1;
                                stk.getSd().LoadData();
                            }
                            
                            Integer dl_stk_num = stk.getDl_stk_num();
                            Double dl_mny_num = stk.getDl_mny_num();
                            
                            Double hst_pri = stk.getMaxTd_hst_pri();
                            Double lst_pri = stk.getMinTd_lst_pri();
                            Double b1_pri = stk.getB1_pri();
                            Double b2_pri = stk.getB2_pri();
                            Double b3_pri = stk.getB3_pri();
                            Double b4_pri = stk.getB4_pri();
                            Double b5_pri = stk.getB5_pri();
                            Integer b1_num = stk.getB1_num();
                            Integer b2_num = stk.getB2_num();
                            Integer b3_num = stk.getB3_num();
                            Integer b4_num = stk.getB4_num();
                            Integer b5_num = stk.getB5_num();
                            Double s1_pri = stk.getS1_pri();
                            Double s2_pri = stk.getS2_pri();
                            Double s3_pri = stk.getS3_pri();
                            Double s4_pri = stk.getS4_pri();
                            Double s5_pri = stk.getS5_pri();
                            Integer s1_num = stk.getS1_num();
                            Integer s2_num = stk.getS2_num();
                            Integer s3_num = stk.getS3_num();
                            Integer s4_num = stk.getS4_num();
                            Integer s5_num = stk.getS5_num();
                            Double opn_pri = stk.getOpen_pri();
                            Double yt_cls_pri = stk.getYtClsPri();
                            Double dlt_pri = cur_pri - yt_cls_pri;
                            Double pct = dlt_pri / yt_cls_pri * 100;
                            
                            msg += "[" + stock + "]:" + Stocks.get(stock) + " 昨收" + df2.format(yt_cls_pri) + "\n";
                            msg += "现价:" + df2.format(cur_pri) + "    今开:" + df2.format(opn_pri) + "\n";
                            msg += "涨跌:" + df2.format(dlt_pri) + "    涨幅:" + df2.format(pct) + "%\n";
                            msg += "最高:" + df2.format(hst_pri) + "    最低:" + df2.format(lst_pri) + "\n";
                            msg += "成交:" + df2.format(dl_stk_num / 1000000.0) + "万手" + "  金额:" + df2.format(dl_mny_num / 10000000) + "千万\n";
                            
                            //max support 4 stock with detail infor.
                            if (stkcnt < 5)
                            {
                                msg += "买五:" + df2.format(b5_pri) + "   手:" + b5_num / 100 + "\n";
                                msg += "买四:" + df2.format(b4_pri) + "   手:" + b4_num / 100 + "\n";
                                msg += "买三:" + df2.format(b3_pri) + "   手:" + b3_num / 100 + "\n";
                                msg += "买二:" + df2.format(b2_pri) + "   手:" + b2_num / 100 + "\n";
                                msg += "买一:" + df2.format(b1_pri) + "   手:" + b1_num / 100 + "\n";
                                msg += "--------------------\n";
                                msg += "卖一:" + df2.format(s1_pri) + "   手:" + s1_num / 100 + "\n";
                                msg += "卖二:" + df2.format(s2_pri) + "   手:" + s2_num / 100 + "\n";
                                msg += "卖三:" + df2.format(s3_pri) + "   手:" + s3_num / 100 + "\n";
                                msg += "卖四:" + df2.format(s4_pri) + "   手:" + s4_num / 100 + "\n";
                                msg += "卖五:" + df2.format(s5_pri) + "   手:" + s5_num / 100 + "\n";
                                msg += "统计:" + " 七天dev:" + df3.format(dev) + " sellMode: " + sellMode + "\n\n";
                            }
                            else {
                                msg += "\n";
                            }
                            rs.close();
                        } catch(Exception e0) {
                            log.info("No price infor for stock:" + stock + " continue...");
                            continue;
                        }
                    }
                    }
                    else {
                        msg = "你没有主动或者系统推荐可买卖关注的股票.";
                    }
                } catch (SQLException e) {
                    log.error("DB Exception:" + e.getMessage());
                    resContent = "DB Exception:" + e.getMessage();
                }
                finally {
                    try {
                        stm.close();
                        mycon.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        log.error("DB Exception:" + e.getMessage());
                        resContent = "DB Exception:" + e.getMessage();
                    }
                }
                resContent = msg;
            }
            else if (content.equals("2")) {
                Statement stm = null;
                String system_suggester = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING");
                String system_trader = ParamManager.getStr2Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING");
                
                String sql = "select s.id, s.name from stk s, usrStk u where s.id = u.id and u.gz_flg = 1 and u.openID ='" + frmUsr + "' and u.suggested_by in ('" + system_suggester + "','" + system_trader + "')";
                String content = "";
                Map<String, String> Stocks = new HashMap<String, String> ();
                DecimalFormat df = new DecimalFormat("##.###");
                Connection con = DBManager.getConnection();

                try {
                    stm = con.createStatement();
                    ResultSet rs = stm.executeQuery(sql);
                    
                    while (rs.next()) {
                        Stocks.put(rs.getString("id"), rs.getString("name"));
                    }
                    rs.close();
                    
                    for (String stock : Stocks.keySet()) {
                        double dev = 0;
                        double cur_pri = 0;

                        content += stock + ":" + Stocks.get(stock) + "\n";

                        try {
                            sql = "select avg(dev) dev from ("
                                   + "select stddev((cur_pri - yt_cls_pri) / yt_cls_pri) dev, left(dl_dt, 10) atDay "
                                   + "  from stkdat2 "
                                   + " where id ='" + stock + "'"
                                   + "   and yt_cls_pri > 0 "
                                   + "   and left(dl_dt, 10) >= left(sysdate() - interval 7 day, 10)"
                                   + " group by left(dl_dt, 10))";
                            log.info(sql);
                            rs = stm.executeQuery(sql);
                            if (rs.next()) {
                                dev = rs.getDouble("dev");
                            }
                            Stock2 s =  (Stock2)StockMarket.getStocks().get(stock);
                            cur_pri =  s.getCur_pri();
                            content += "价:" + df.format(cur_pri) + " stddev:" + df.format(dev) + "\n";
                            rs.close();
                        } catch(SQLException e0) {
                            log.info("No price infor for stock:" + stock + " continue...");
                            continue;
                        }
                    }
                    resContent = content;
                } catch (SQLException e) {
                    log.error("DB Exception:" + e.getMessage());
                    resContent = "DB Exception:" + e.getMessage();
                }
                finally {
                    try {
                        stm.close();
                        con.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        log.error("DB Exception:" + e.getMessage());
                        resContent = "DB Exception:" + e.getMessage();
                    }
                }
            }
            else if (content.equals("3")) {
                Connection con = DBManager.getConnection();
                Statement stm = null;
                log.info("Now start EnaUsrBuySell work...");
                try {
                    String sql = "update usr set buy_sell_enabled = 1 - buy_sell_enabled where openID = '" + frmUsr + "'";
                    
                    stm = con.createStatement();
                    log.info("EnaUsrBuySell:" + sql);
                    stm.executeUpdate(sql);
                    stm.close();
                    
                    sql = "select buy_sell_enabled, mail from usr where openID = '" + frmUsr + "'";
                    stm = con.createStatement();
                    ResultSet rs = stm.executeQuery(sql);
                    
                    rs.next();
                    
                    long buy_sell_enabled = rs.getLong("buy_sell_enabled");
                    String mail = rs.getString("mail");
                    
                    if (buy_sell_enabled == 1 && (mail == null || mail.length() <= 0)) {
                            resContent = "已经开启买卖信息提示，请发送邮箱地址进行订阅。";
                    }
                    else if (buy_sell_enabled == 1){
                        resContent = "已经开启买卖信息提示，您的邮箱:" + mail + "将收到买卖提示信息。";
                    }
                    else {
                        resContent = "已停止买卖信息提示。";
                    }
                    
                    rs.close();
                }
                catch (Exception e)
                {
                    
                }
                finally {
                    try {
                        stm.close();
                        con.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        log.error("DB Exception:" + e.getMessage());
                        resContent = "DB Exception:" + e.getMessage();
                    }
                }
                
            }
            else if (content.equals("4")) {
                Connection con = DBManager.getConnection();
                log.info("Now start EnaUsrBuySell work...");
                Statement mainStm = null;
                
                try {
                String sql = "update usr set suggest_stock_enabled = 1 - suggest_stock_enabled where openID = '" + frmUsr + "'";
                log.info("EnaUsrBuySell:" + sql);
                mainStm = con.createStatement();
                mainStm.executeUpdate(sql);
                mainStm.close();
                
                sql = "select mail, suggest_stock_enabled from usr where openID = '" + frmUsr + "' and mail is not null";
                mainStm = con.createStatement();
                ResultSet rs = mainStm.executeQuery(sql);
                
                rs.next();
                
                long suggest_stock_enabled = rs.getLong("suggest_stock_enabled");
                String mail = rs.getString("mail");
                
                rs.close();
                
                if (suggest_stock_enabled == 1 && (mail == null || mail.length() <= 0)) {
                        resContent = "已经开启推荐股票，请发送邮箱地址进行买卖信息订阅。或输入1进行查询.";
                    }
                else if (suggest_stock_enabled == 1){
                    resContent = "已经开启推荐股票，您的邮箱:" + mail + "将收到被推荐股票买卖提示信息。";
                }
                else {
                    resContent = "已停止推荐股票。";
                }
                }
                catch (Exception e)
                {
                    log.error("DB Exception:" + e.getMessage());
                    resContent = "DB Exception:" + e.getMessage();
                }
                finally {
                    try {
                        mainStm.close();
                        con.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        log.error("DB Exception:" + e.getMessage());
                        resContent = "DB Exception:" + e.getMessage();
                    }
                }
            }
            else if (content.equals("5")) {
                
                Connection con = DBManager.getConnection(); 
                Statement stm = null;
                try {
                    stm = con.createStatement();
                    String sql = "update usrstk set stop_trade_mode_flg = 1 - stop_trade_mode_flg where openID = '" + frmUsr + "' and gz_flg = 1";
                    stm.execute(sql);
                    stm.close();
                    
                    sql = "select case when max(stop_trade_mode_flg) is null then -1 else max(stop_trade_mode_flg) end max_flg from usrstk where openID = '" + frmUsr + "' and gz_flg = 1";
                    stm = con.createStatement();
                    ResultSet rs = stm.executeQuery(sql);
                    
                    if (rs.next())
                    {
                        if (rs.getInt("max_flg") == 1)
                        {
                            resContent = "平仓模式已启用, 系统平仓中,稍后查看结果.";
                        }
                        else if(rs.getInt("max_flg") == 0){
                            resContent = "平仓模式已取消, 系统进入自动交易.";
                        }
                        else {
                            resContent = "没有关注的股票, 未能设置平仓模式.";
                        }
                    }
                    rs.close();
                }
                catch (Exception e)
                {
                    log.info("set stop_trade_mode_flg to 1 failed:" + e.getMessage());
                    resContent = "设置平仓交易失败:" + e.getMessage();
                }
                finally {
                    try {
                    stm.close();
                    con.close();
                    }
                    catch (Exception e)
                    {
                        log.error("DB exception:" + e.getMessage());
                        resContent = "数据库异常:" + e.getMessage();
                    }
                }
            }
            else if(content.equals("6"))
            {
                String msg = "实际账户:";
                Connection con = DBManager.getConnection();
                Statement stm = null;
                DecimalFormat df2 = new DecimalFormat("##.##");
                DecimalFormat df4 = new DecimalFormat("##.####");
                String simAcntPrefix = ParamManager.getStr1Param("ACNT_SIM_PREFIX", "ACCOUNT");
                
                String sql = "select count(*) cnt,"
                            + "      case when sum(used_mny) is null then 0 else sum(used_mny) end tot_used_mny,"
                            + "      case when sum(used_mny * used_mny_hrs) / sum(used_mny) is null then 0 else sum(used_mny * used_mny_hrs) / sum(used_mny) end tot_used_mny_hrs,"
                            + "      case when sum(pft_mny) is null then 0 else sum(pft_mny) end tot_pft_mny,"
                            + "      case when sum(pft_mny) - sum(commission_mny) is null then 0 else sum(pft_mny) - sum(commission_mny) end net_pft_mny,"
                            + "      case when sum(in_hand_stk_mny) is null then 0 else sum(in_hand_stk_mny) end tot_in_hand_stk_mny,"
                            + "      case when sum(amount_mny) is null then 0 else sum(amount_mny) end tot_amount_mny,"
                            + "      case when sum(commission_mny) is null then 0 else sum(commission_mny) end tot_commission_mny,"
                            + "      case when sum(used_mny) is null then 0 else (sum(pft_mny) - sum(commission_mny)) / (case when sum(used_mny) <= 0 then 1 else sum(used_mny) end) end profit_pct"
                            + "  from cashacnt ca"
                            + "  join (select sum(in_hand_qty * in_hand_stk_price) in_hand_stk_mny,"
                            + "               sum(total_amount) amount_mny,"
                            + "               sum(commission_mny) commission_mny,"
                            + "               acntid"
                            + "          from tradehdr group by acntid) th  "
                            + "    on ca.acntid = th.acntid "
                            + " where ca.acntid not like '" + simAcntPrefix + "%'";
                    log.info(sql);
                try {
                    stm = con.createStatement();
                    ResultSet rs = stm.executeQuery(sql);
                    if (rs.next()){
                        msg += "账户数:" + rs.getLong("cnt")+ "\n"
                              +"总使用金额:" + df2.format(rs.getDouble("tot_used_mny")) + "\n"
                              +"平均使用小时:" + df2.format(rs.getDouble("tot_used_mny_hrs")) + "\n"
                              +"总利润:" + df2.format(rs.getDouble("tot_pft_mny")) + "\n"
                              +"净利润:" + df2.format(rs.getDouble("net_pft_mny")) + "\n"
                              +"股票价值:" + df2.format(rs.getDouble("tot_in_hand_stk_mny")) + "\n"
                              +"总交易额:" + df2.format(rs.getDouble("tot_amount_mny")) + "\n"
                              +"佣金:" + df2.format(rs.getDouble("tot_commission_mny")) + "\n"
                              +"利润率:" + df4.format(rs.getDouble("profit_pct")) + "\n";
                    }
                    rs.close();
                    stm.close();
                    
                    msg += "\n\n";
                    msg += "模拟账户:";
                    sql = "select count(*) cnt,"
                            + "      case when sum(used_mny) is null then 0 else sum(used_mny) end tot_used_mny,"
                            + "      case when sum(used_mny * used_mny_hrs) / sum(used_mny) is null then 0 else sum(used_mny * used_mny_hrs) / sum(used_mny) end tot_used_mny_hrs,"
                            + "      case when sum(pft_mny) is null then 0 else sum(pft_mny) end tot_pft_mny,"
                            + "      case when sum(pft_mny) - sum(commission_mny) is null then 0 else sum(pft_mny) - sum(commission_mny) end net_pft_mny,"
                            + "      case when sum(in_hand_stk_mny) is null then 0 else sum(in_hand_stk_mny) end tot_in_hand_stk_mny,"
                            + "      case when sum(amount_mny) is null then 0 else sum(amount_mny) end tot_amount_mny,"
                            + "      case when sum(commission_mny) is null then 0 else sum(commission_mny) end tot_commission_mny,"
                            + "      case when sum(used_mny) is null then 0 else (sum(pft_mny) - sum(commission_mny)) / (case when sum(used_mny) <= 0 then 1 else sum(used_mny) end) end profit_pct"
                            + "  from cashacnt ca"
                            + "  join (select sum(in_hand_qty * in_hand_stk_price) in_hand_stk_mny,"
                            + "               sum(total_amount) amount_mny,"
                            + "               sum(commission_mny) commission_mny,"
                            + "               acntid"
                            + "          from tradehdr group by acntid) th  "
                            + "    on ca.acntid = th.acntid "
                            + " where ca.acntid like '" + simAcntPrefix + "%'";
                    log.info(sql);
                    stm = con.createStatement();
                    rs = stm.executeQuery(sql);
                    if (rs.next()){
                        msg += "账户数:" + rs.getLong("cnt")+ "\n"
                                +"总使用金额:" + df2.format(rs.getDouble("tot_used_mny")) + "\n"
                                +"平均使用小时:" + df2.format(rs.getDouble("tot_used_mny_hrs")) + "\n"
                                +"总利润:" + df2.format(rs.getDouble("tot_pft_mny")) + "\n"
                                +"净利润:" + df2.format(rs.getDouble("net_pft_mny")) + "\n"
                                +"股票价值:" + df2.format(rs.getDouble("tot_in_hand_stk_mny")) + "\n"
                                +"总交易额:" + df2.format(rs.getDouble("tot_amount_mny")) + "\n"
                                +"佣金:" + df2.format(rs.getDouble("tot_commission_mny")) + "\n"
                                +"利润率:" + df4.format(rs.getDouble("profit_pct")) + "\n";
                    }
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    msg = "无数据.\n";
                }
                finally {
                    try {
                    stm.close();
                    con.close();
                    }
                    catch (Exception e)
                    {
                        log.error("DB exception:" + e.getMessage());
                        resContent = "数据库异常:" + e.getMessage();
                    }
                }
                resContent = msg;
            }
            else if(content.equals("7"))
            {
                String msg = "";
                Connection con = DBManager.getConnection();
                Statement stm = null;
                String sql = "select count(*) totCnt, count(*)/case when count(distinct id) = 0 then 1 else count(distinct id) end cntPerStk "
                        + "  from stkDat2 where left(dl_dt, 10) = left(sysdate(), 10) ";
                try {
                    stm = con.createStatement();
                    ResultSet rs = stm.executeQuery(sql);
                    if (rs.next()){
                        msg += "总共收集:" + rs.getLong("totCnt") + "条记录.\n"
                              +"平均每股收集:" + rs.getLong("cntPerStk") + "次.\n";
                    }
                    rs.close();
                    log.info("calculate fetch stat msg:" + msg + " for opt 5");
                } catch (Exception e) {
                    e.printStackTrace();
                    msg = "无数据.\n";
                }
                finally {
                    try {
                    stm.close();
                    con.close();
                    }
                    catch (Exception e)
                    {
                        log.error("DB exception:" + e.getMessage());
                        resContent = "数据库异常:" + e.getMessage();
                    }
                }
                resContent = msg + StockMarket.getShortDesc();
            }
            /*else if (content.equals("6")) {
                ShutDownPC sdp = new ShutDownPC(0, 3);
                if (!WorkManager.submitWork(sdp)) {
                    resContent = "ShutDownPC already scheduled, can not do it again!";
                }
                else {
                    resContent = sdp.getWorkResult();
                }
            }*/
            else if (content.length() == 6) {
                String stockID = content;

                String msg = "";
                Connection con = DBManager.getConnection();
                Statement stm = null;
                String sql = "select gz_flg, suggested_by from usrStk where id = '" + stockID + "' and openID = '" + frmUsr + "'";
                try {
                    
                    String system_suggest_role = ParamManager.getStr1Param("SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT", "TRADING");
                    stm = con.createStatement();
                    ResultSet rs = null;
                    rs = stm.executeQuery(sql);

                    if (rs.next()) {
                        long gz_flg = rs.getLong("gz_flg");
                        String suggested_by = rs.getString("suggested_by");
                        rs.close();
                        stm.close();
                        if (gz_flg == 1 && suggested_by.equals(system_suggest_role))
                        {
                            sql = "update usrStk set suggested_by = '" + frmUsr + "', suggested_by_selector = '" + frmUsr + "', suggested_comment = 'User manual suggest', mod_dt = sysdate() where id = '" + stockID + "' and openID = '" + frmUsr + "'";
                        }
                        else {
                            sql = "update usrStk set gz_flg = 1 - gz_flg, suggested_by = '" + frmUsr + "', suggested_by_selector = '" + frmUsr + "', suggested_comment = 'User manual suggest', mod_dt = sysdate() where id = '" + stockID + "' and openID = '" + frmUsr + "'";
                        }
                        stm = con.createStatement();
                        log.info(sql);
                        stm.execute(sql);
                        if (gz_flg == 1 && !suggested_by.equals(system_suggest_role)) {
                            msg = "成功取消关注:" + stockID;
                            StockMarket.removeGzStocks(stockID);
                        }
                        else {
                            msg = "成功关注:" + stockID;
                            StockMarket.addGzStocks(stockID);
                        }
                    }
                    else {
                        rs.close();
                        stm.close();
                        stm = con.createStatement();
                        sql = "select 'x' from stk where id = '" + stockID + "'";
                        log.info(sql);
                        rs = stm.executeQuery(sql);
                        if (!rs.next()) {
                            msg = "不存在该股票代码:" + stockID;
                            rs.close();
                        }
                        else {
                            rs.close();
                            stm.close();
                            stm = con.createStatement();
                            sql = "insert into usrStk values ('" + frmUsr + "','" + stockID + "',1,0,'" + frmUsr + "','" + frmUsr +"','User manual suggested',sysdate(), sysdate())";
                            log.info(sql);
                            stm.execute(sql);
                            msg = "成功添加关注:" + stockID;
                            StockMarket.addGzStocks(stockID);
                        }
                    }
                    resContent = msg;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    try {
                        stm.close();
                        con.close();
                    } catch (SQLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        log.error(e.getMessage() + " with error: " + e.getErrorCode());
                    }
                }
            
            }
            else if (content.indexOf("@") > 0) {
                    AddMail us = new AddMail(0, 0, this.getFromUserName(), content);
                    if (!WorkManager.submitWork(us)) {
                        resContent = "AddMail already scheduled, can not do it again!";
                    }
                    else {
                        resContent = us.getWorkResult();
                    }
            }
            else {
            	resContent = printHelp();
            }
        }
            return createWCMsg();
    }
}
