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
import com.sn.wechat.msg.itf.IWCMsg;
import com.sn.work.CalFetchStat;
import com.sn.work.FetchStockData;
import com.sn.work.ShutDownPC;
import com.sn.work.TopTenBst;
import com.sn.work.TopTenWst;
import com.sn.work.WorkManager;

public abstract class BaseWCReporter implements IWCMsg {
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

            return true;
        } else {
            return false;
        }
    }
    
	public String createWCMsg(){
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
