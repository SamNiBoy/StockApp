package com.sn.timerTask;

import java.util.Timer;
import java.util.TimerTask;

import com.sn.reporter.WeChatReporter;

public class ReporterRefresher extends TimerTask {

    @Override
    public void run() {
        // TODO Auto-generated method stub
        WeChatReporter.refreshMsgMap();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Timer timer = new Timer();  
        timer.schedule(new ReporterRefresher(), 5000, 1000 * 60 * 5); 
        
    }
    public static void DoRun()
    {
        Timer timer = new Timer();  
        timer.schedule(new ReporterRefresher(), 5000, 1000 * 60 * 5); 
    }

}
