package com.sn.sim.strategy.imp;

import org.apache.log4j.Logger;

public class STConstants {

    static Logger log = Logger.getLogger(STConstants.class);
    
    //class vars.
	public static final int MAX_TRADE_TIMES_PER_STOCK = 10;
	public static final int MAX_TRADE_TIMES_PER_DAY = 40;
	public static final int BUY_MORE_THEN_SELL_CNT = 2;
	public static final String openID = "osCWfs-ZVQZfrjRK0ml-eEpzeop0";
	
	public static final String SUGGESTED_BY_FOR_USER = "osCWfs-ZVQZfrjRK0ml-eEpzeop0";
	public static final String SUGGESTED_BY_FOR_SYSTEMGRANTED = "SYSTEMGRANTED";
	public static final String SUGGESTED_BY_FOR_SYSTEM = "SYSTEM";
	public static final String SUGGESTED_BY_FOR_SYSTEMUPDATE = "SYSTEMUPDATE";
	
	
	public static final int MAX_LOST_TIME_BEFORE_EXIT_TRADE = 2;
	public static final int MAX_DAYS_WITHOUT_TRADE_BEFORE_EXIT_TRADE = 3;
    
}
