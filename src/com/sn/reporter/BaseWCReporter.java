package com.sn.reporter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.wechat.msg.itf.IWCMsg;

public abstract class BaseWCReporter implements IWCMsg {
    static Logger log = Logger.getLogger(BaseWCReporter.class);
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
    String frmUsr;
    String toUsr;
    String content;
    String msgId;
    long crtTime;

    String resContent;
    private String wcMsg;

    public void setWcMsg(String wcMsg) {
        this.wcMsg = wcMsg;
        parseMsg();
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

            int msgId_s = wcMsg.indexOf("<MsgId>");
            int msgId_e = wcMsg.indexOf("</MsgId>");
            msgId = wcMsg.substring(msgId_s + 7, msgId_e).trim();

            int crtTime_s = wcMsg.indexOf("<CreateTime>");
            int crtTime_e = wcMsg.indexOf("</CreateTime>");
            String ct = wcMsg.substring(crtTime_s + 12, crtTime_e).trim();
            crtTime = Long.valueOf(ct);

            chkAndCrtUsr(frmUsr, false);
            chkAndCrtUsr(toUsr, true);
            crtRcvMsg();
            return true;
        } else {
            return false;
        }
    }

    public String createWCMsg() {
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

    private boolean chkAndCrtUsr(String usr, boolean hst_flg) {
        Connection con = DBManager.getConnection();
        try {
            Statement stm = con.createStatement();
            String sql;
            sql = "select 'x' from usr where openID = '" + usr + "'";
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) {
                log.info("User:" + frmUsr + " already being added!");
            } else {
                sql = "insert into usr values ('" + usr + "',"
                        + (hst_flg == true ? 1 : 0) + "," + "sysdate)";
                stm.executeUpdate(sql);
                log.info("User:" + frmUsr + " added as "
                        + (hst_flg == true ? 1 : 0));
                return true;
            }
            rs.close();
            stm.close();
            con.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("chkAndCrtUsr errored:" + e.getMessage());
        }
        return false;
    }

    private void crtRcvMsg() {
        Connection con = DBManager.getConnection();
        try {
            Statement stm = con.createStatement();
            String sql;
            /*
             * msgId number not null, frmUsrID varchar2(100 byte) not null,
             * toUsrID varchar2(100 byte) not null, crtTime number not null,
             * mstType varchar2(20 byte) not null, content varchar2(1000 byte)
             * not null,
             */
            sql = "insert into msg values ('" + msgId + "', '" + frmUsr
                    + "', '" + toUsr + "', " + crtTime + ", 'text', '"
                    + content + "')";
            log.info("Creating msg:" + sql);
            int crted = stm.executeUpdate(sql);
            if (crted == 1) {
                log.info("Msg from user:" + frmUsr + " already being added!");
            } else {
                log.info("Msg from user:" + frmUsr + " NOT being added!");
            }
            stm.close();
            con.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("crtRcvMsg errored:" + e.getMessage());
        }
    }

    @Override
    public String getContent() {
        // TODO Auto-generated method stub
        return content;
    }

    @Override
    public String getCreateTime() {
        // TODO Auto-generated method stub
        return "";
    }

    @Override
    public String getFromUserName() {
        // TODO Auto-generated method stub
        return frmUsr;
    }

    @Override
    public String getMsgId() {
        // TODO Auto-generated method stub
        return "";
    }

    @Override
    public String getMsgType() {
        // TODO Auto-generated method stub
        return "text";
    }

    @Override
    public String getToUserName() {
        // TODO Auto-generated method stub
        return toUsr;
    }
}
