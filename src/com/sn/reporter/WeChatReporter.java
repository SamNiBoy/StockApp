package com.sn.reporter;

import org.apache.log4j.Logger;

import com.sn.work.WorkManager;
import com.sn.work.fetcher.FetchStockData;
import com.sn.work.output.CalFetchStat;
import com.sn.work.output.ShutDownPC;
import com.sn.work.output.TopTenBst;
import com.sn.work.output.TopTenWst;

public class WeChatReporter extends BaseWCReporter{

    static Logger log = Logger.getLogger(WeChatReporter.class);

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
        String msg = createWCMsg();
        return msg;
    }


    public String getResponse() {
        if (content == null || content.equals("")) {
            return printHelp();
        }

           log.info("got input:[" + content + "]");

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
                FetchStockData fsd = new FetchStockData(0, 60);
                WorkManager.submitWork(fsd);
                resContent = "Started fetching stock data!";
            }
            else if (content.equals("4")) {
                FetchStockData sfd = new FetchStockData(0, 0);
                WorkManager.cancelWork(sfd.getWorkName());
                resContent = "Stoped fetching stock data.";
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
            return createWCMsg();
    }
}
