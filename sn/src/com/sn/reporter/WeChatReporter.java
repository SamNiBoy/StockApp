package com.sn.reporter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import com.sn.db.DBManager;
import com.sn.timerTask.ReporterRefresher;
import com.sn.timerTask.StkFetcher;
import com.sn.work.CalFetchStat;
import com.sn.work.FetchStockData;
import com.sn.work.ShutDownPC;
import com.sn.work.TopTenBst;
import com.sn.work.TopTenWst;
import com.sn.work.WorkManager;

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
                + "3. Start fetch.\n" + "4. Stop fetch.\n"
                + "5. Basic report\n" + "6. GJ\n";
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

    public String getResponse() {
        if (content == null || content.equals("")) {
            return printHelp();
        }

        System.out.println("got input:[" + content + "]");
        /* Get top 10 df1 */
            
            if (content.equals("1")) {
                TopTenBst ttb = new TopTenBst(0, 3);
                WorkManager.submitWork(ttb);
                resContent = ttb.getWorkResult();
            }
            else if (content.equals("2")) {
                TopTenWst ttw = new TopTenWst(0, 3);
                WorkManager.submitWork(ttw);
                resContent = ttw.getWorkResult();
            }
            else if (content.equals("3")) {
                FetchStockData fsd = new FetchStockData(0, 3);
                WorkManager.submitWork(fsd);
                resContent = fsd.getWorkResult();
            }
            else if (content.equals("4")) {
                FetchStockData.stopFetch = false;
                resContent = "Stoped fetching stokc data.";
            }
            else if (content.equals("5")) {
                CalFetchStat cfs = new CalFetchStat(0, 3);
                WorkManager.submitWork(cfs);
                resContent = cfs.getWorkResult();
            }
            else if (content.equals("6")) {
                ShutDownPC sdp = new ShutDownPC(0, 3);
                WorkManager.submitWork(sdp);
                resContent = sdp.getWorkResult();
            }
            else {
                return printHelp();
            }
            
            return makeMsg();
        
    }
}
