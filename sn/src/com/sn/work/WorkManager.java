package com.sn.work;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/* CycleWork Manager.
 */
public class WorkManager {

    static int CW_THREAD_NUMBER = 3;
    static ScheduledExecutorService exec = Executors
            .newScheduledThreadPool(CW_THREAD_NUMBER);

    /*
     * workerSet stores the name of cycle work.
     */
    static Set<String> workerSet = new HashSet<String>();

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    public static boolean submitWork(IWork work) {
        if (work.isCycleWork()) {
            if (workerSet.contains(work.getWorkName())) {
                
                System.out.println("worker:" + work.getWorkName()
                        + " already scheduled, skipp rescheduling.");
                return false;
            }

            System.out.println("worker:" + work.getWorkName()
                    + " scheduled, with initdelay:" + work.getInitDelay()
                    + "delayBeforeNxt:" + work.getDelayBeforeNxt()
                    + " timeUnit:" + work.getTimeUnit());

            exec.scheduleWithFixedDelay(work, work.getInitDelay(),
                    work.getDelayBeforeNxt(), work.getTimeUnit());

            workerSet.add(work.getWorkName());
        }
        else
        {
            System.out.println("Non cycle worker:" + work.getWorkName()
                    + " scheduled, with initdelay:" + work.getInitDelay()
                    + "delayBeforeNxt:" + work.getDelayBeforeNxt()
                    + " timeUnit:" + work.getTimeUnit());
            exec.schedule(work, work.getInitDelay(), work.getTimeUnit());
        }

        return true;
    }

    static void shutdownWorks() {
        exec.shutdown();
        workerSet.clear();
    }

}
