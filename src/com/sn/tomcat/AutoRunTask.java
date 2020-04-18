package com.sn.tomcat;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.quartz.SchedulerException;

import com.sn.db.DBManager;
import com.sn.task.JobScheduler;


public class AutoRunTask implements ServletContextListener{

    static Logger log = Logger.getLogger(AutoRunTask.class);
    
    static {
    	DBManager.initLog4j();
    	DBManager.initDataSource();
    }
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    //Tomcat startup.
    public void contextInitialized(ServletContextEvent arg0){
        if(!JobScheduler.isJobScheduled())
        {
            log.info("Tomcat startsup, run TaskManager startTasks()");
            try {
				JobScheduler.submitAllTasks();
			} catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error(e.getMessage(), e);
			}
            log.info("Tomcat startsup, run TaskManager ended.");
        }
    }
    //tomcat close.
    public void contextDestroyed(ServletContextEvent arg0){
        log.info("Tomcat destroyed, run TaskManager stopTasks()");
        JobScheduler.stopJobs();
        log.info("Tomcat destroyed, run TaskManager stopTasks ended.");
    }
}