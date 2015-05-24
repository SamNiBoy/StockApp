package com.sn.work.task;

import org.apache.log4j.Logger;

import com.sn.work.WorkManager;

public class TaskManager {

    static Logger log = Logger.getLogger(CalStkDDF.class);
    
    private static boolean tskStarted = false;
    
    private static CalStkDDF tsk1 = null;
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        startTasks();
    }
    
    public static boolean startTasks()
    {
        if (!tskStarted)
        {
            log.info("Starting tasks...");
            tsk1 = new CalStkDDF(0, 65000);
            WorkManager.submitWork(tsk1);
            tskStarted = true;
            return true;
        }
        else {
            log.info("Tasks already started...");
        }
        return false;
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
            WorkManager.cancelWork(tsk1.getWorkName());
            tskStarted = false;
        }
        return true;
    }
    
    public static boolean isTasksStarted()
    {
        return tskStarted;
    }

}
