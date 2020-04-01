package com.sn.task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.task.IWork;


/* CycleWork Manager.
 */
public class WorkManager {

    static int CW_THREAD_NUMBER = 10;
    static ScheduledExecutorService exec = Executors
            .newScheduledThreadPool(CW_THREAD_NUMBER);

    static Map<String, ScheduledFuture<?>> SFM = new ConcurrentHashMap<String, ScheduledFuture<?>>();

    static Logger log = Logger.getLogger(WorkManager.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    public static boolean submitWork(IWork work) {
        cleanupDoneWork();
        if (work.isCycleWork()) {
            if (SFM.get(work.getWorkName()) != null) {

                log.info("worker:" + work.getWorkName()
                        + " already scheduled, skipp rescheduling.");
                return false;
            }

            log.info("worker:" + work.getWorkName()
                    + " scheduled, with initdelay:" + work.getInitDelay()
                    + "delayBeforeNxt:" + work.getDelayBeforeNxt()
                    + " timeUnit:" + work.getTimeUnit());

            ScheduledFuture<?> sf = exec.scheduleWithFixedDelay(work, work.getInitDelay(),
                    work.getDelayBeforeNxt(), work.getTimeUnit());

            SFM.put(work.getWorkName(), sf);
        }
        else
        {
            if (SFM.get(work.getWorkName()) != null) {

                log.info("Non cycle worker:" + work.getWorkName()
                        + " already scheduled, skipp rescheduling.");
                return false;
            }
            log.info("Non cycle worker:" + work.getWorkName()
                    + " scheduled, with initdelay:" + work.getInitDelay()
                    + "delayBeforeNxt:" + work.getDelayBeforeNxt()
                    + " timeUnit:" + work.getTimeUnit());
            ScheduledFuture<?> sf = exec.schedule(work, work.getInitDelay(), work.getTimeUnit());
            SFM.put(work.getWorkName(), sf);
        }

        return true;
    }
    
    private static void cleanupDoneWork() {
        log.info("Now let's cleanup finsihed work from SFM...");
        for (String workName : SFM.keySet()) {
            ScheduledFuture<?> sf = SFM.get(workName);
            if (sf.isDone()) {
                log.info("Work:" + workName + " is finished! removed it from SFM...");
                SFM.remove(workName);
            }
        }
        log.info("Now finished cleanup finished work!");
    }
    
    public static void shutdownWorksAfterFinish() throws InterruptedException {
        log.info("Now let's check if finsihed all worker before shutdown thead pool...");
        for (String workName : SFM.keySet()) {
            ScheduledFuture<?> sf = SFM.get(workName);
            while (!sf.isDone()) {
                log.info("Work:" + workName + " is not finished! wait for 1 second");
                Thread.currentThread().sleep(1000);
            }
        }
        log.info("Now finished all works, shutdow pool");
        shutdownWorks();
    }
    
    public static void waitUntilWorkIsDone(String workName) throws InterruptedException {
        log.info("Now wait until work" + workName + " is done...");
        ScheduledFuture<?> sf = SFM.get(workName);
        while (sf != null && !sf.isDone()) {
            log.info("Work:" + workName + " is not finished! wait for 1 second");
            Thread.currentThread().sleep(1000);
        }
        log.info("Now finished work:" + workName + "return");
    }

    public static void shutdownWorks() {
        exec.shutdown();
        SFM.clear();
    }

    public static boolean cancelWork(String name)
    {
        ScheduledFuture<?> sf = SFM.get(name);

        log.info("cancelling work:" + name);
        if (sf != null)
        {
            if(sf.cancel(true))
            {
                SFM.remove(name);
                log.info("work is cancalled:" + name);
                return true;
            }
        }
        return false;
    }
    
    public static boolean canSubmitWork(String workName) {
        cleanupDoneWork();
        ScheduledFuture<?> sf = SFM.get(workName);

        log.info("check if work:" + workName + " can be submited!");
        if (sf != null)
        {
            return false;
        }
        return true;
    }



}
