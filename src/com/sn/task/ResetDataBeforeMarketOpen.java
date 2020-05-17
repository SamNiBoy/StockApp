package com.sn.task;

import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import com.sn.simulation.SimTrader;
import com.sn.stock.StockMarket;
import com.sn.strategy.TradeStrategyImp;

@DisallowConcurrentExecution
public class ResetDataBeforeMarketOpen implements Job {

    static Logger log = Logger.getLogger(ResetDataBeforeMarketOpen.class);

    /**
     * @param args
     * @throws JobExecutionException 
     */
    public static void main(String[] args) throws JobExecutionException {
        // TODO Auto-generated method stub
    	ResetDataBeforeMarketOpen fsd = new ResetDataBeforeMarketOpen();
        fsd.execute(null);
    }

    public ResetDataBeforeMarketOpen() {
    }
    
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        // TODO Auto-generated method stub
        log.info("Running ResetDataBeforeMarketOpen task begin");
        SimTrader.resetTest(true);
        StockMarket.clearDegreeMap();
        StockMarket.clearSimData();
        StockMarket.clearGzStocks();
        TradeStrategyImp.clearMaps();
        TradeStrategyImp.loadBuySellRecord();
        log.info("Running ResetDataBeforeMarketOpen task end");
    }
}
