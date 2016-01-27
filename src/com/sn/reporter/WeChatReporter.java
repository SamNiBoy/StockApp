package com.sn.reporter;

import org.apache.log4j.Logger;

import com.sn.work.WorkManager;
import com.sn.work.fetcher.FetchStockData;
import com.sn.work.monitor.MonitorStockData;
import com.sn.work.output.CalFetchStat;
import com.sn.work.output.GzStock;
import com.sn.work.output.ListGzStock;
import com.sn.work.output.ShutDownPC;
import com.sn.work.output.TopTenBst;
import com.sn.work.output.TopTenWst;
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

        WeChatReporter wcr = new WeChatReporter();
        wcr.setWcMsg(wcMsg);
        wcr.getResponse();
    }


    public WeChatReporter() {

        log.info("WeChatReporter initilized...");
    }

    public String printHelp() {
        resContent = "你可以发送以下代码:\n"
                + "1.获取我关注的股票.\n" + "2.获取股票数据.\n"
                + "3.停止获取股票数据.\n" + "4.报告获取数据情况.\n" 
                + "5.监控关注股票\n" + "6.停止监控关注股票\n"
                + "7.关机\n"
                + "gzxxxxxx 关注股票.\n" + " tzxxxxxx 停止关注股票.\n";

        String msg = createWCMsg();
        return msg;
    }
    public String getResponse() {
        if (content == null || content.equals("")) {
            return printHelp();
        }
        
        log.info("got input:[" + content + "], firstly let's check tasks");
//        if (!TaskManager.isTasksStarted())
//        {
//            TaskManager.startTasks();
//        }

            if (content.equals("1")) {
                ListGzStock ttb = new ListGzStock(0, 3);
                if (!WorkManager.submitWork(ttb)) {
                    resContent = "Work already scheduled, can not do it again!";
                }
                else {
                    resContent = ttb.getWorkResult();
                }
            }
            else if (content.equals("2")) {
                FetchStockData fsd = new FetchStockData(0, 30000);
                if (!WorkManager.submitWork(fsd)) {
                    resContent = "Work already scheduled, can not do it again!";
                }
                else {
                    resContent = "Started fetching stock data!";
                }
            }
            else if (content.equals("3")) {
                FetchStockData sfd = new FetchStockData(0, 0);
                WorkManager.cancelWork(sfd.getWorkName());
                resContent = "Stoped fetching stock data.";
            }
            else if (content.equals("4")) {
                CalFetchStat cfs = new CalFetchStat(0, 3);
                if (!WorkManager.submitWork(cfs)) {
                    resContent = "Work already scheduled, can not do it again!";
                }
                else {
                    resContent = cfs.getWorkResult();
                }
            }
            else if (content.equals("5")) {
                MonitorStockData sdp = new MonitorStockData(0, 35000);
                if (!WorkManager.submitWork(sdp)) {
                    resContent = "Work already scheduled, can not do it again!";
                }
                else {
                    resContent = sdp.getWorkResult();
                }
            }
            else if (content.equals("6")) {
                MonitorStockData sdp = new MonitorStockData(0, 0);
                WorkManager.cancelWork(sdp.getWorkName());
                resContent = "Stoped monitoring stock data.";
            }
            else if (content.equals("7")) {
                ShutDownPC sdp = new ShutDownPC(0, 3);
                if (!WorkManager.submitWork(sdp)) {
                    resContent = "Work already scheduled, can not do it again!";
                }
                else {
                    resContent = sdp.getWorkResult();
                }
            }
            else if (content.length() > 2 && content.substring(0,2).equalsIgnoreCase("gz")) {
                String stk = content.substring(2, 8);
                GzStock sdp = new GzStock(0, 3, stk);
                if (!WorkManager.submitWork(sdp)) {
                    resContent = "Work already scheduled, can not do it again!";
                }
                else {
                    resContent = sdp.getWorkResult();
                }
            }
            else {
                if (content.length() == 6) {
                    usrStock us = new usrStock(0, 0, this.getFromUserName(), content);
                    WorkManager.submitWork(us);
                    if (!WorkManager.submitWork(us)) {
                        resContent = "Work already scheduled, can not do it again!";
                    }
                    else {
                        resContent = us.getWorkResult();
                    }
                }
                else {
                    return printHelp();
                }
            }
            return createWCMsg();
    }
}
