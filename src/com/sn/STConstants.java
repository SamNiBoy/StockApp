package com.sn;

import org.apache.log4j.Logger;

public class STConstants {

    static Logger log = Logger.getLogger(STConstants.class);
    
    //Default cash account attributes.
    public static final double DFT_INIT_MNY = 50000.0;
    public static final double DFT_MAX_USE_PCT = 0.8;
    public static final double DFT_MAX_MNY_PER_TRADE = 10000;
    public static final String ACNT_SIM_PREFIX = "SIM";
    public static final double COMMISSION_RATE = 0.0014;
    
    //class vars.
    public static final int MAX_TRADE_TIMES_BUY_OR_SELL_PER_STOCK = 20;
	public static final int MAX_TRADE_TIMES_PER_STOCK = 50;
	public static final int BUY_SELL_MAX_DIFF_CNT = 2;
	public static final String openID = "osCWfs-ZVQZfrjRK0ml-eEpzeop0";
    
	public static final int MAX_MINUTES_ALLOWED_TO_KEEP_BALANCE = 30;
	public static final int HOUR_TO_KEEP_BALANCE = 14;
	public static final int MINUTE_TO_KEEP_BALANCE = 55;
	public static final double STOP_BREAK_BALANCE_IF_CURPRI_REACHED_PCT = 0.8;
	public static final int STOP_TRADE_IF_LOST_MORE_THAN_GAIN_TIMES = 2;
	
	public static final String SUGGESTED_BY_FOR_USER = "osCWfs-ZVQZfrjRK0ml-eEpzeop0";
	public static final String SUGGESTED_BY_FOR_SYSTEMGRANTED = "SYSTEM_GRANTED_TRADE";
	public static final String SUGGESTED_BY_FOR_SYSTEM = "SYSTEM";
	public static final String SUGGESTED_BY_FOR_SYSTEMUPDATE = "SYSTEMUPDATE";
	
	
	public static final int MAX_LOST_TIME_BEFORE_EXIT_TRADE = 2;
	public static final int MAX_DAYS_WITHOUT_TRADE_BEFORE_EXIT_TRADE = 7;
	
	public static final double MAX_LOST_PCT_FOR_SELL_MODE = -0.06;
	public static final double MAX_GAIN_PCT_FOR_DISABLE_SELL_MODE = 0.06;
    
	//Simulation parameters:
    public static final int SIM_DAYS = 1;
    public static final int SIM_THREADS_COUNT = 1;
    public static final int SIM_STOCK_COUNT_FOR_EACH_THREAD = 250;
    
    //Archive & Purge.
    public static final int ARCHIVE_DAYS_OLD = 1;
    public static final int PURGE_DAYS_OLD = 5;
}
