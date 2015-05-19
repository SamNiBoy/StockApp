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

import com.sn.work.itf.IWork;

/* CycleWork Manager.
 */
public class WorkManager {

    static int CW_THREAD_NUMBER = 3;
    static ScheduledExecutorService exec = Executors
            .newScheduledThreadPool(CW_THREAD_NUMBER);

    static Map<String, ScheduledFuture<?>> SFM = new HashMap<String, ScheduledFuture<?>>();

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    public static boolean submitWork(IWork work) {
        if (work.isCycleWork()) {
            if (SFM.get(work.getWorkName()) != null) {
                
                System.out.println("worker:" + work.getWorkName()
                        + " already scheduled, skipp rescheduling.");
                return false;
            }

            System.out.println("worker:" + work.getWorkName()
                    + " scheduled, with initdelay:" + work.getInitDelay()
                    + "delayBeforeNxt:" + work.getDelayBeforeNxt()
                    + " timeUnit:" + work.getTimeUnit());

            ScheduledFuture<?> sf = exec.scheduleWithFixedDelay(work, work.getInitDelay(),
                    work.getDelayBeforeNxt(), work.getTimeUnit());

            SFM.put(work.getWorkName(), sf);
        }
        else
        {
            System.out.println("Non cycle worker:" + work.getWorkName()
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
    	
    	System.out.println("cancelling work:" + name);
    	if (sf != null)
    	{
    		if(sf.cancel(true))
    		{
    			SFM.remove(name);
    			System.out.println("work is cancalled:" + name);
    			return true;
    		}
    	}
    	return false;
    }
    
    

}
