package com.sn.reporter;

import org.apache.log4j.Logger;

import com.sn.work.WorkManager;
import com.sn.work.fetcher.GzStockDataFetcher;
import com.sn.work.fetcher.StockDataFetcher;
import com.sn.work.monitor.MonitorStockData;
import com.sn.work.output.CalFetchStat;
import com.sn.work.output.GzStock;
import com.sn.work.output.ListGzStock;
import com.sn.work.output.ListSuggestStock;
import com.sn.work.output.ShutDownPC;
import com.sn.work.output.TopTenBst;
import com.sn.work.output.TopTenWst;
import com.sn.work.task.AddMail;
import com.sn.work.task.EnaSuggestStock;
import com.sn.work.task.EnaUsrBuySell;
import com.sn.work.task.TaskManager;
import com.sn.work.task.usrStock;

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
                + "5.报告数据情况.\n"
                + "xxxxxx 关注/取消关注股票.\n"
                + "xxx@yyy.zzz添加邮箱接收买卖信息.\n";
    	}
    	else {
            resContent = "你可以发送以下代码:\n"
                    + "1.获取我关注的股票.\n"
                    + "2.获取系统推荐股票数据.\n"
                    + "3.启用/停止股票买卖.\n"
                    + "4.启用/停止推荐.\n"
                    + "5.报告数据情况.\n"
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
        if (!TaskManager.isTasksStarted())
        {
            log.info("starting tasks");
            TaskManager.startTasks();
        }

            log.info("after starting tasks, content:" + content + ", length:" + content.length());
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
                ListGzStock ttb = new ListGzStock(0, 3, this.getFromUserName());
                if (!WorkManager.submitWork(ttb)) {
                    resContent = "正在获取关注的股票详细信息,请稍后再试.";
                }
                else {
                    resContent = ttb.getWorkResult();
                }
            }
            else if (content.equals("2")) {
            	ListSuggestStock ttb = new ListSuggestStock(0, 3, this.getFromUserName());
                if (!WorkManager.submitWork(ttb)) {
                    resContent = "ListSuggestStock already scheduled, can not do it again!";
                }
                else {
                    resContent = ttb.getWorkResult();
                }
            }
            else if (content.equals("3")) {
            	EnaUsrBuySell us = new EnaUsrBuySell(0, 0, this.getFromUserName());
                if (!WorkManager.submitWork(us)) {
                    resContent = "EnaUsrBuySell already scheduled, can not do it again!";
                }
                else {
                    resContent = us.getWorkResult();
                }
            }
            else if (content.equals("4")) {
            	EnaSuggestStock us = new EnaSuggestStock(0, 0, this.getFromUserName());
                if (!WorkManager.submitWork(us)) {
                    resContent = "EnaSuggestStock already scheduled, can not do it again!";
                }
                else {
                    resContent = us.getWorkResult();
                }
            }
            else if (content.equals("5")) {
                CalFetchStat cfs = new CalFetchStat(0, 3);
                if (!WorkManager.submitWork(cfs)) {
                    resContent = "CalFetchStat already scheduled, can not do it again!";
                }
                else {
                    resContent = cfs.getWorkResult();
                }
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
                String stk = content;
                GzStock sdp = new GzStock(0, 3, this.getFromUserName(), stk);
                if (!WorkManager.submitWork(sdp)) {
                    resContent = "正在处理关注的股票,请稍后再试.";
                }
                else {
                    resContent = sdp.getWorkResult();
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
