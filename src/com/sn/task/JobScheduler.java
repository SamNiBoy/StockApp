package com.sn.task;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.sn.db.DBManager;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import com.sn.task.calstkstats.CalStkStats;
import com.sn.task.calstkstats.CalStkTopnVOL;
import com.sn.task.calstkstats.GetStockAvgPri;
import com.sn.task.ga.StockParamSearch;
import com.sn.task.fetcher.StockDataFetcher;
import com.sn.task.fetcher.GzStockDataFetcher;
import com.sn.task.suggest.SuggestStock;
import com.sn.simulation.SimResultCollector;
import com.sn.simulation.SimTrader;
import com.sn.task.fetcher.StockDataConsumer;
import com.sn.task.fetcher.GzStockDataConsumer;

/*

字段   允许值   允许的特殊字符   
秒    0-59    , - * /   
分    0-59    , - * /   
小时    0-23    , - * /   
日期    1-31    , - * ? / L W C   
月份    1-12 或者 JAN-DEC    , - * /   
星期    1-7 或者 SUN-SAT    , - * ? / L C #   
年（可选）    留空, 1970-2099    , - * /   


表达式   意义   
"0 0 12 * * ?"    每天中午12点触发   
"0 15 10 ? * *"    每天上午10:15触发   
"0 15 10 * * ?"    每天上午10:15触发   
"0 15 10 * * ? *"    每天上午10:15触发   
"0 15 10 * * ? 2005"    2005年的每天上午10:15触发   
"0 * 14 * * ?"    在每天下午2点到下午2:59期间的每1分钟触发   
"0 0/5 14 * * ?"    在每天下午2点到下午2:55期间的每5分钟触发    
"0 0/5 14,18 * * ?"    在每天下午2点到2:55期间和下午6点到6:55期间的每5分钟触发    
"0 0-5 14 * * ?"    在每天下午2点到下午2:05期间的每1分钟触发   
"0 10,44 14 ? 3 WED"    每年三月的星期三的下午2:10和2:44触发   
"0 15 10 ? * MON-FRI"    周一至周五的上午10:15触发   
"0 15 10 15 * ?"    每月15日上午10:15触发   
"0 15 10 L * ?"    每月最后一日的上午10:15触发   
"0 15 10 ? * 6L"    每月的最后一个星期五上午10:15触发     
"0 15 10 ? * 6L 2002-2005"    2002年至2005年的每月的最后一个星期五上午10:15触发   
"0 15 10 ? * 6#3"    每月的第三个星期五上午10:15触发    

特殊字符   意义   
*    表示所有值；   
?    表示未说明的值，即不关心它为何值；   
-    表示一个指定的范围；   
,    表示附加一个可能值；   
/    符号前表示开始时间，符号后表示每次递增的值；   
L("last")    ("last") "L" 用在day-of-month字段意思是 "这个月最后一天"；用在 day-of-week字段, 它简单意思是 "7" or "SAT"。 如果在day-of-week字段里和数字联合使用，它的意思就是 "这个月的最后一个星期几" – 例如： "6L" means "这个月的最后一个星期五". 当我们用“L”时，不指明一个列表值或者范围是很重要的，不然的话，我们会得到一些意想不到的结果。   
W("weekday")    只能用在day-of-month字段。用来描叙最接近指定天的工作日（周一到周五）。例如：在day-of-month字段用“15W”指“最接近这个月第15天的工作日”，即如果这个月第15天是周六，那么触发器将会在这个月第14天即周五触发；如果这个月第15天是周日，那么触发器将会在这个月第16天即周一触发；如果这个月第15天是周二，那么就在触发器这天触发。注意一点：这个用法只会在当前月计算值，不会越过当前月。“W”字符仅能在day-of-month指明一天，不能是一个范围或列表。也可以用“LW”来指定这个月的最后一个工作日。    
#    只能用在day-of-week字段。用来指定这个月的第几个周几。例：在day-of-week字段用"6#3"指这个月第3个周五（6指周五，3指第3个）。如果指定的日期不存在，触发器就不会触发。    
C 指和calendar联系后计算过的值。例：在day-of-month 字段用“5C”指在这个月第5天或之后包括calendar的第一天；在day-of-week字段用“1C”指在这周日或之后包括calendar的第一天 
*/
/* CycleWork Manager.
 */
public class JobScheduler {
    
    static SchedulerFactory sf = new StdSchedulerFactory();
    static Scheduler sched = null;
    
    public static boolean job_started = false;
    
    {
        PropertyConfigurator.configure("D:/StockApp/StockApp/WEB-INF/conf/log4j.properties");
    }
    

    static Logger log = Logger.getLogger(WorkManager.class);
    /**
     * @param args
     * @throws SchedulerException 
     */
    public static void main(String[] args) throws SchedulerException {
        // TODO Auto-generated method stub

    	JobScheduler example = new JobScheduler();
        example.submitAllTasks();
    }

    public static boolean submitAllTasks() throws SchedulerException {
    	
    	if (sched == null)
    	{
    	    sched = sf.getScheduler();
    	}
    	
    	String houseKeepingBeforeMarketOpen = "0 20 9 ? * MON-FRI";
        String marketRunningTime = "0 0/30 9-15 ? * MON-FRI";
        String gzStockFetcherRunningTime = "0 0/1 9-15 ? * MON-FRI";
        //String stockParamTrainningTime = "0 35 11 ? * MON-FRI";
        //String stockParamTrainningTime = "0 50 16 ? * MON-SUN";
        //String stockSuggestTime = "0 0 12,16 ? * MON-FRI";
        String stockSuggestTime = "0 0 16 ? * MON-FRI";
        //String stockSimTime = "0 30 15 ? * MON-FIR";
        String stockSimTime = "0 34 21 ? * MON-SUN";
        String stockSimResCollectorTime = "*/10 * * ? * MON-SUN";
        String getStockAvgPriTime = "0 45 19 ? * MON-SUN";
        
       // JobDetail job_StockParamSearch = newJob(StockParamSearch.class).withIdentity("StockParamSearch", "StockApp").build();

        //CronTrigger trigger = newTrigger().withIdentity("CalStkStats", "StockApp").withSchedule(cronSchedule("0 25/5 9-15 ? * MON-FRI")).build();
//        CronTrigger trigger_StockParamSearch = newTrigger().withIdentity("StockParamSearch", "StockApp").withSchedule(cronSchedule(stockParamTrainningTime)).build();
//        
//        Date ft = sched.scheduleJob(job_StockParamSearch, trigger_StockParamSearch);
//        log.info(job_StockParamSearch.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
//                 + trigger_StockParamSearch.getCronExpression());
        
        
        JobDetail job_SuggestStock = newJob(SuggestStock.class).withIdentity("SuggestStock", "StockApp").build();
        CronTrigger trigger_SuggestStock = newTrigger().withIdentity("SuggestStock", "StockApp").withSchedule(cronSchedule(stockSuggestTime)).build();
        
        Date ft = sched.scheduleJob(job_SuggestStock, trigger_SuggestStock);
        log.info(job_SuggestStock.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
                 + trigger_SuggestStock.getCronExpression());
        
        JobDetail job_ResetDataBeforeMarketOpen = newJob(ResetDataBeforeMarketOpen.class).withIdentity("ResetDataBeforeMarketOpen", "StockApp").build();
        CronTrigger trigger_ResetDataBeforeMarketOpen = newTrigger().withIdentity("ResetDataBeforeMarketOpen", "StockApp").withSchedule(cronSchedule(houseKeepingBeforeMarketOpen)).build();
        
        ft = sched.scheduleJob(job_ResetDataBeforeMarketOpen, trigger_ResetDataBeforeMarketOpen);
        log.info(job_ResetDataBeforeMarketOpen.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
                 + trigger_ResetDataBeforeMarketOpen.getCronExpression());
        
        JobDetail job_SimCalStkStats = newJob(CalStkStats.class).withIdentity("SimCalStkStats", "StockApp").build();
        CronTrigger trigger_SimCalStkStats = newTrigger().withIdentity("SimCalStkStats", "StockApp").withSchedule(cronSchedule(stockSimTime)).build();
        
        ft = sched.scheduleJob(job_SimCalStkStats, trigger_SimCalStkStats);
        log.info(job_SimCalStkStats.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
                 + trigger_SimCalStkStats.getCronExpression());
        
        JobDetail job_SimTrader = newJob(SimTrader.class).withIdentity("SimTrader", "StockApp").build();
        CronTrigger trigger_SimTrader = newTrigger().withIdentity("SimTrader", "StockApp").withSchedule(cronSchedule(stockSimTime)).build();
        
        ft = sched.scheduleJob(job_SimTrader, trigger_SimTrader);
        log.info(job_SimTrader.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
                 + trigger_SimTrader.getCronExpression());
        
        JobDetail job_SimResultCollector = newJob(SimResultCollector.class).withIdentity("SimResultCollector", "StockApp").build();
        CronTrigger trigger_SimResultCollector = newTrigger().withIdentity("SimResultCollector", "StockApp").withSchedule(cronSchedule(stockSimResCollectorTime)).build();
        
        ft = sched.scheduleJob(job_SimResultCollector, trigger_SimResultCollector);
        log.info(job_SimResultCollector.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
                 + trigger_SimResultCollector.getCronExpression());
        
        JobDetail job_StockDataConsumer = newJob(StockDataConsumer.class).withIdentity("StockDataConsumer", "StockApp").build();

        //CronTrigger trigger = newTrigger().withIdentity("CalStkStats", "StockApp").withSchedule(cronSchedule("0 25/5 9-15 ? * MON-FRI")).build();
        CronTrigger trigger_StockDataConsumer = newTrigger().withIdentity("StockDataConsumer", "StockApp").withSchedule(cronSchedule(marketRunningTime)).build();
        
        ft = sched.scheduleJob(job_StockDataConsumer, trigger_StockDataConsumer);
        log.info(job_StockDataConsumer.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
                 + trigger_StockDataConsumer.getCronExpression());
        
        
        JobDetail job_StockDataFetcher = newJob(StockDataFetcher.class).withIdentity("StockDataFetcher", "StockApp").build();

        //CronTrigger trigger = newTrigger().withIdentity("CalStkStats", "StockApp").withSchedule(cronSchedule("0 25/5 9-15 ? * MON-FRI")).build();
        CronTrigger trigger_StockDataFetcher = newTrigger().withIdentity("StockDataFetcher", "StockApp").withSchedule(cronSchedule(marketRunningTime)).build();
        
        ft = sched.scheduleJob(job_StockDataFetcher, trigger_StockDataFetcher);
        log.info(job_StockDataFetcher.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
                 + trigger_StockDataFetcher.getCronExpression());
        
        
        JobDetail job_GzStockDataConsumer = newJob(GzStockDataConsumer.class).withIdentity("GzStockDataConsumer", "StockApp").build();

        //CronTrigger trigger = newTrigger().withIdentity("CalStkStats", "StockApp").withSchedule(cronSchedule("0 25/5 9-15 ? * MON-FRI")).build();
        CronTrigger trigger_GzStockDataConsumer = newTrigger().withIdentity("GzStockDataConsumer", "StockApp").withSchedule(cronSchedule(gzStockFetcherRunningTime)).build();
        
        ft = sched.scheduleJob(job_GzStockDataConsumer, trigger_GzStockDataConsumer);
        log.info(job_GzStockDataConsumer.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
                 + trigger_GzStockDataConsumer.getCronExpression());
        
        
        JobDetail job_GzStockDataFetcher = newJob(GzStockDataFetcher.class).withIdentity("GzStockDataFetcher", "StockApp").build();

        //CronTrigger trigger = newTrigger().withIdentity("CalStkStats", "StockApp").withSchedule(cronSchedule("0 25/5 9-15 ? * MON-FRI")).build();
        CronTrigger trigger_GzStockDataFetcher = newTrigger().withIdentity("GzStockDataFetcher", "StockApp").withSchedule(cronSchedule(marketRunningTime)).build();
        
        ft = sched.scheduleJob(job_GzStockDataFetcher, trigger_GzStockDataFetcher);
        log.info(job_GzStockDataFetcher.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
                 + trigger_GzStockDataFetcher.getCronExpression());

        
        
        JobDetail job_CalStkStats = newJob(CalStkStats.class).withIdentity("CalStkStats", "StockApp").build();
        CronTrigger trigger_CalStkStats = newTrigger().withIdentity("CalStkStats", "StockApp").withSchedule(cronSchedule(marketRunningTime)).build();
        
        ft = sched.scheduleJob(job_CalStkStats, trigger_CalStkStats);
        log.info(job_CalStkStats.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
                 + trigger_CalStkStats.getCronExpression());
        
        
        JobDetail job_GetStockAvgPri = newJob(GetStockAvgPri.class).withIdentity("GetStockAvgPri", "StockApp").build();
        CronTrigger trigger_GetStockAvgPri = newTrigger().withIdentity("GetStockAvgPri", "StockApp").withSchedule(cronSchedule(getStockAvgPriTime)).build();
        
        ft = sched.scheduleJob(job_GetStockAvgPri, trigger_GetStockAvgPri);
        log.info(job_GetStockAvgPri.getKey() + " has been scheduled to run at: " + ft + " and repeat based on expression: "
                 + trigger_GetStockAvgPri.getCronExpression());
        
        sched.start();
        
        job_started = true;

        log.info("------- Now job scheduler going to exit, goodby all jobs... -------------");
        try {
          // wait 65 seconds to show job
          //Thread.sleep(650L * 1000L);
          // executing...
        } catch (Exception e) {
          //
        }
        
        //log.info("------- Shutting Down ---------------------");

        //sched.shutdown(true);

        //log.info("------- Shutdown Complete -----------------");

        SchedulerMetaData metaData = sched.getMetaData();
        log.info("Executed " + metaData.getNumberOfJobsExecuted() + " jobs.");
        
        return true;
    }
    
    public static boolean isJobScheduled() {
    	return job_started;
    }
    public static boolean stopJobs() {
    	log.info("Now shutdown job scheduler...");
    	try {
    	    sched.shutdown();
    	    job_started = false;
    	    return true;
    	}
    	catch (Exception e) {
    		log.error(e.getMessage(), e);
    	}
    	return false;
    }
}
