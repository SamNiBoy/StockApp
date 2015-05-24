package com.sn.work;

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

import com.sn.work.itf.IWork;

/* CycleWork Manager.
 */
public class WorkManager {

    static int CW_THREAD_NUMBER = 10;
    static ScheduledExecutorService exec = Executors
            .newScheduledThreadPool(CW_THREAD_NUMBER);

    static Map<String, ScheduledFuture<?>> SFM = new HashMap<String, ScheduledFuture<?>>();

    static Logger log = Logger.getLogger(WorkManager.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    public static boolean submitWork(IWork work) {
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
            log.info("Non cycle worker:" + work.getWorkName()
                    + " scheduled, with initdelay:" + work.getInitDelay()
                    + "delayBeforeNxt:" + work.getDelayBeforeNxt()
                    + " timeUnit:" + work.getTimeUnit());
            ScheduledFuture<?> sf = exec.schedule(work, work.getInitDelay(), work.getTimeUnit());
            SFM.put(work.getWorkName(), sf);
        }

        return true;
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



}
