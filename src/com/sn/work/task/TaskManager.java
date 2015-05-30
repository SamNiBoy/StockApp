package com.sn.work.task;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.sn.work.itf.IWork;
import com.sn.work.WorkManager;

public class TaskManager {

    static Logger log = Logger.getLogger(CalStkDDF.class);
    
    private static boolean tskStarted = false;
    
    private static Map<String, IWork> tsks = new ConcurrentHashMap<String, IWork>();
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        startEvaStocks();
    }
    
    public static boolean startTasks()
    {
        if (!tskStarted)
        {
            log.info("Starting tasks...");
            tskStarted = true;
            startCalStkDDF();
            startEvaStocks();
            return true;
        }
        else {
            log.info("Tasks already started...");
        }
        return false;
    }
    
    private static boolean startCalStkDDF()
    {
        CalStkDDF csd = new CalStkDDF(0, 0);
        WorkManager.submitWork(csd);
        tsks.put(csd.getWorkName(), csd);
        return true;
    }
    
    private static boolean startEvaStocks()
    {
        EvaStocks evs = new EvaStocks(0, 0, false);
        WorkManager.submitWork(evs);
        tsks.put(evs.getWorkName(), evs);
        return true;
    }
    
    public static boolean stopTasks()
    {
        if (tskStarted == false)
        {
            log.info("Tasks not started, can not stop...");
            return false;
        }
        else {
            log.info("Tasks is in stopping...");
            Set<Map.Entry<String, IWork>> allSet=tsks.entrySet();
            
            Iterator<Map.Entry<String, IWork>> iter=allSet.iterator();
            while(iter.hasNext()){
                Map.Entry<String, IWork> tsk=iter.next();
                System.out.println(tsk.getKey());
                WorkManager.cancelWork(tsk.getValue().getWorkName());
                tsks.remove(tsk.getValue().getWorkName());
            }
            tskStarted = false;
        }
        return true;
    }
    
    public static boolean isTasksStarted()
    {
        return tskStarted;
    }

}
