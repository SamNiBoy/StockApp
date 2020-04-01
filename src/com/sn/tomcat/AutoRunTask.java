package com.sn.tomcat;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.sn.work.WorkManager;
import com.sn.work.task.TaskManager;

public class AutoRunTask implements ServletContextListener{

    static Logger log = Logger.getLogger(WorkManager.class);
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    //Tomcat startup.
    public void contextInitialized(ServletContextEvent arg0){
        if(!TaskManager.isTasksStarted())
        {
            log.info("Tomcat startsup, run TaskManager startTasks()");
            TaskManager.startTasks();
            log.info("Tomcat startsup, run TaskManager ended.");
        }
    }
    //tomcat close.
    public void contextDestroyed(ServletContextEvent arg0){
        log.info("Tomcat destroyed, run TaskManager stopTasks()");
        TaskManager.stopTasks();
        log.info("Tomcat destroyed, run TaskManager stopTasks ended.");
    }
}