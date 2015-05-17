package com.sn.reporter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sn.db.DBManager;
import com.sn.timerTask.ReporterRefresher;

public class WeChatReporter {
    /*
     * <xml> <ToUserName> <![CDATA[gh_a9586d5aa590]]> </ToUserName>
     * 
     * <FromUserName> <![CDATA[osCWfs-ZVQZfrjRK0ml-eEpzeop0]]> </FromUserName>
     * 
     * <CreateTime> 1431827441 </CreateTime>
     * 
     * <MsgType> <![CDATA[text]]> </MsgType>
     * 
     * <Content> <![CDATA[Standard ]]> </Content>
     * 
     * <MsgId> 6149652032815793242 </MsgId>
     * 
     * </xml>
     */
    private String wcMsg;

    public String getWcMsg() {
        return wcMsg;
    }

    public void setWcMsg(String wcMsg) {
        this.wcMsg = wcMsg;
        parseMsg();
    }

    private String frmUsr;
    private String toUsr;
    private String msgTyp;
    private String content;
    private String resContent;

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        WeChatReporter wcr = new WeChatReporter();
        wcr.content = "1";
        wcr.getResponse();

    }

    public WeChatReporter() {

    }

    public String printHelp() {
        resContent = "This is help for wechat:\n" + "Input:\n"
                + "1. Get top df1.\n" + "2. Get top -df.\n"
                + "3. Get top df2.\n" + "4. Get top -df2.";
        String msg = makeMsg();
        return msg;
    }

    private boolean parseMsg() {
        if (wcMsg != null && !wcMsg.equals("")) {
            int fromuser_s = wcMsg.indexOf("<FromUserName><![CDATA[");
            int fromuser_e = wcMsg.indexOf("]]></FromUserName>");
            frmUsr = wcMsg.substring(fromuser_s + 23, fromuser_e);

            int touser_s = wcMsg.indexOf("<ToUserName><![CDATA[");
            int touser_e = wcMsg.indexOf("]]></ToUserName>");
            toUsr = wcMsg.substring(touser_s + 21, touser_e);

            int content_s = wcMsg.indexOf("<Content><![CDATA[");
            int content_e = wcMsg.indexOf("]]></Content>");
            content = wcMsg.substring(content_s + 18, content_e);

            return true;
        } else {
            return false;
        }
    }

    private String makeMsg() {

        String rspMsg = "";
        if (frmUsr != null && !frmUsr.equals("")) {

            rspMsg += "<xml>";
            rspMsg += "    <ToUserName><![CDATA[" + frmUsr + "]]></ToUserName>";
            rspMsg += "    <FromUserName><![CDATA[" + toUsr
                    + "]]></FromUserName>";
            rspMsg += "    <CreateTime>" + new Date().getTime()
                    + "</CreateTime>";
            rspMsg += "    <MsgType><![CDATA[text]]></MsgType>";
            rspMsg += "    <Content><![CDATA[" + resContent + "]]></Content>";
            rspMsg += "    <FuncFlag>0</FuncFlag>";
            rspMsg += "</xml>";
        }

        return rspMsg;
    }

    private static Thread t;

    public String getResponse() {
        if (content == null || content.equals("")) {
            return printHelp();
        }

        if (t == null) {
            t = new Thread(new Runnable() {
                public void run()
                {
                    ReporterRefresher.DoRun();
                }
            });
            t.start();
        }

        System.out.println("got input:[" + content + "]");
        /* Get top 10 df1 */
        if (content.equals("1") || content.equals("2") || content.equals("3")
                || content.equals("4")) {
            String opt = content;
            if (msgForMenu == null) {
                refreshMsgMap();
                resContent = msgForMenu.get(opt);
                if (resContent == null) {
                    System.out.println("resContent is null");
                    resContent = "";
                }
                System.out.println("after refresh for resContent is:"
                        + resContent);
            } else {
                resContent = msgForMenu.get(opt);
            }

            System.out.println("returning:" + resContent);
            return makeMsg();
        }
        return printHelp();
    }

    static Map<String, String> msgForMenu;

    static public boolean refreshMsgMap() {

        if (msgForMenu == null) {
            msgForMenu = new ConcurrentHashMap<String, String>();
        }

        // ///////////Menu 1///////////////
        String msg = "";
        Connection con = DBManager.getConnection();
        String sql = "select stk.area || df.id id, df.cur_pri_df, stk.name, std.cur_pri "
                + "  from curpri_df_vw df, stk, (select id, cur_pri"
                + "                                from stkDat "
                + "                               where not exists(select 'x' "
                + "                                                  from stkDat sd2 "
                + "                                                 where sd2.id = stkDat.id "
                + "                                                   and sd2.ft_id > stkDat.ft_id)) std "
                + " where df.id = stk.id "
                + "   and df.id = std.id "
                + "   and not exists (select 'x' from curpri_df_vw dfv where dfv.id = df.id and dfv.ft_id > df.ft_id) "
                + "  order by df.cur_pri_df desc ";
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);

            for (int i = 0; i < 10 && rs.next(); i++) {
                msg += (i + 1) + ": " + rs.getString("id") + " "
                        + rs.getString("name") + "\n";
                msg += "Current Price: " + rs.getString("cur_pri") + "\n";
                msg += "Current Price Diff: " + rs.getString("cur_pri_df")
                        + "\n";
            }
            stm.close();
            con.commit();
            System.out.println("putting msg:" + msg + " for opt 1");
            msgForMenu.put("1", msg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // //////////////////Menu
        // 2///////////////////////////////////////////////////
        msg = "";
        con = DBManager.getConnection();
        sql = "select stk.area || df.id id, df.cur_pri_df, stk.name, std.cur_pri "
                + "  from curpri_df_vw df, stk, (select id, cur_pri"
                + "                                from stkDat "
                + "                               where not exists(select 'x' "
                + "                                                  from stkDat sd2 "
                + "                                                 where sd2.id = stkDat.id "
                + "                                                   and sd2.ft_id > stkDat.ft_id)) std "
                + " where df.id = stk.id "
                + "   and df.id = std.id "
                + "   and not exists (select 'x' from curpri_df_vw dfv where dfv.id = df.id and dfv.ft_id > df.ft_id) "
                + "  order by df.cur_pri_df ";
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);

            for (int i = 0; i < 10 && rs.next(); i++) {
                msg += (i + 1) + ": " + rs.getString("id") + " "
                        + rs.getString("name") + "\n";
                msg += "Current Price: " + rs.getString("cur_pri") + "\n";
                msg += "Current Price Diff: " + rs.getString("cur_pri_df")
                        + "\n";
            }
            stm.close();
            con.commit();
            System.out.println("putting msg:" + msg + " for opt 2");
            msgForMenu.put("2", msg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // //////////////////Menu 3/////////////////////////////////////////////
        msg = "";
        sql = "select stk.area || df.id id, df.cur_pri_df2, stk.name"
                + "  from curpri_df2_vw df, stk "
                + " where df.id = stk.id "
                + "   and not exists (select 'x' from curpri_df2_vw dfv where dfv.id = df.id and dfv.ft_id > df.ft_id) "
                + "  order by df.cur_pri_df2 desc ";
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);

            String id = "";
            for (int i = 0; i < 10 && rs.next(); i++) {
                if (id.equals(rs.getString("id"))) {
                    continue;
                } else {
                    id = rs.getString("id");
                }
                msg += (i + 1) + ": " + rs.getString("id") + " "
                        + rs.getString("name") + "\n";
                msg += "Current Price Diff2: " + rs.getString("cur_pri_df2")
                        + "\n";
            }
            stm.close();
            con.commit();
            System.out.println("putting msg:" + msg + " for opt 3");
            msgForMenu.put("3", msg);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // //////////////////Menu 4/////////////////////////////////////////////
        msg = "";
        sql = "select stk.area || df.id id, df.cur_pri_df2, stk.name"
                + "  from curpri_df2_vw df, stk "
                + " where df.id = stk.id "
                + "   and not exists (select 'x' from curpri_df2_vw dfv where dfv.id = df.id and dfv.ft_id > df.ft_id) "
                + "  order by df.cur_pri_df2 ";
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);

            String id = "";
            for (int i = 0; i < 10 && rs.next(); i++) {
                if (id.equals(rs.getString("id"))) {
                    continue;
                } else {
                    id = rs.getString("id");
                }
                msg += (i + 1) + ": " + rs.getString("id") + " "
                        + rs.getString("name") + "\n";
                msg += "Current Price Diff2: " + rs.getString("cur_pri_df2")
                        + "\n";
            }
            stm.close();
            con.commit();
            System.out.println("putting msg:" + msg + " for opt 4");
            msgForMenu.put("4", msg);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}
