package com.sn.work.task;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

import com.sn.work.itf.IWork;
import com.sn.sim.SimTrader;
import com.sn.work.WorkManager;
import com.sn.work.fetcher.GzStockDataFetcher;
import com.sn.work.fetcher.StockDataFetcher;

/* Below section needs to be added into web.xml file in tomcat:
<listener>  
<listener-class>com.sn.work.task.TaskManager</listener-class>  
</listener>
*/
public class TaskManager implements ServletContextListener{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static Logger log = Logger.getLogger(TaskManager.class);
    
    private volatile static boolean tskStarted = false;
    
    private static Map<String, IWork> tsks = new ConcurrentHashMap<String, IWork>();
    
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }
    
    public static boolean startTasks()
    {
        if (!tskStarted)
        {
            log.info("Starting tasks...");
            tskStarted = true;
            StockDataFetcher.start();
            GzStockDataFetcher.start();
        	SuggestStock.start();
        	SellModeWatchDog.start();
            SimTrader.start();
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

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// TODO Auto-generated method stub

        log.info("Auto start tasks begin...");
        startTasks();
        log.info("Auto start tasks ended!");
    
	}

}
