package com.sn.reporter;

import org.apache.log4j.Logger;

import com.sn.work.WorkManager;
import com.sn.work.fetcher.FetchStockData;
import com.sn.work.fetcher.StockDataFetcher;
import com.sn.work.monitor.MonitorStockData;
import com.sn.work.output.CalFetchStat;
import com.sn.work.output.GzStock;
import com.sn.work.output.ListGzStock;
import com.sn.work.output.ShutDownPC;
import com.sn.work.output.TopTenBst;
import com.sn.work.output.TopTenWst;
import com.sn.work.task.AddMail;
import com.sn.work.task.EnaSuggestStock;
import com.sn.work.task.EnaUsrBuySell;
import com.sn.work.task.GzStockDataFetcher;
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
        "<Content><![CDATA[0]]></Content>" +
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
        wcr.setWcMsg(wcMsg2);
        wcr.getResponse();
    }


    public WeChatReporter() {

        log.info("WeChatReporter initilized...");
    }

    public String printHelp() {
    	if (is_admin_flg) {
        resContent = "你可以发送以下代码:\n"
                + "1.获取关注/推荐的股票.\n" + "2.获取股票数据.\n"
                + "3.停止获取股票数据.\n" + "4.报告数据情况.\n" 
                + "5.启用/停止关注股票买卖.\n" + "6.启用/停止推荐股票.\n"
                + "7.关机.\n"
                + "xxxxxx 关注/取消关注股票.\n"
                + "xxx@yyy.zzz添加邮箱接收买卖信息.\n";
    	}
    	else {
            resContent = "你可以发送以下代码:\n"
                    + "1.获取关注/推荐的股票.\n"
                    + "2.启用/停止关注股票买卖.\n"
                    + "3.启用/停止推荐股票.\n"
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
            TaskManager.startTasks();
        }

        if (msgType.equals("event")) {
        	if (content.equals("subscribe")) {
        		resContent = "欢迎关注微信:\n"
        				+ printHelp();
        	}
        }
        else {
        	// remap key for other user.
        	if (!is_admin_flg) {
        		if (content.equals("2")) {
        			content = "5";
        		}
        		else if (content.equals("3")) {
        			content = "6";
        		}
        		else if (!content.equals("1") && content.length() == 1) {
        			content = "0";
        		}
        	}
            if (content.equals("1")) {
                ListGzStock ttb = new ListGzStock(0, 3, this.getFromUserName());
                if (!WorkManager.submitWork(ttb)) {
                    resContent = "ListGzStock already scheduled, can not do it again!";
                }
                else {
                    resContent = ttb.getWorkResult();
                }
            }
            else if (content.equals("2")) {
                StockDataFetcher.start();
                resContent = StockDataFetcher.getResMsg();
            }
            else if (content.equals("3")) {
                StockDataFetcher.stop();
                resContent = StockDataFetcher.getResMsg();
            }
            else if (content.equals("4")) {
                CalFetchStat cfs = new CalFetchStat(0, 3);
                if (!WorkManager.submitWork(cfs)) {
                    resContent = "CalFetchStat already scheduled, can not do it again!";
                }
                else {
                    resContent = cfs.getWorkResult();
                }
            }
            else if (content.equals("5")) {
            	EnaUsrBuySell us = new EnaUsrBuySell(0, 0, this.getFromUserName());
                if (!WorkManager.submitWork(us)) {
                    resContent = "EnaUsrBuySell already scheduled, can not do it again!";
                }
                else {
                    resContent = us.getWorkResult();
                }
            }
            else if (content.equals("6")) {
            	EnaSuggestStock us = new EnaSuggestStock(0, 0, this.getFromUserName());
                if (!WorkManager.submitWork(us)) {
                    resContent = "EnaSuggestStock already scheduled, can not do it again!";
                }
                else {
                    resContent = us.getWorkResult();
                }
            }
            else if (content.equals("7")) {
                ShutDownPC sdp = new ShutDownPC(0, 3);
                if (!WorkManager.submitWork(sdp)) {
                    resContent = "ShutDownPC already scheduled, can not do it again!";
                }
                else {
                    resContent = sdp.getWorkResult();
                }
            }
            else if (content.length() == 6 && !content.contains("@")) {
                String stk = content;
                GzStock sdp = new GzStock(0, 3, this.getFromUserName(), stk);
                if (!WorkManager.submitWork(sdp)) {
                    resContent = "GzStock already scheduled, can not do it again!";
                }
                else {
                    resContent = sdp.getWorkResult();
                }
            }
            else if (content.contains("@")) {
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
