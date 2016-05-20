package com.sn.trade.strategy.imp;

import org.apache.log4j.Logger;

public class STConstants {

    static Logger log = Logger.getLogger(STConstants.class);
    
    //Default cash account attributes.
    public static final double DFT_INIT_MNY = 20000.0;
    public static final int DFT_SPLIT = 4;
    public static final double DFT_MAX_USE_PCT = 0.8;
    public static final String ACNT_TRADE_PREFIX = "ACNT";
    public static final String ACNT_SIM_PREFIX = "SIM";
    
    //class vars.
    public static final int MAX_TRADE_TIMES_BUY_OR_SELL_PER_STOCK = 3;
	public static final int MAX_TRADE_TIMES_PER_STOCK = 10;
	public static final int MAX_TRADE_TIMES_PER_DAY = 40;
	public static final int BUY_MORE_THEN_SELL_CNT = 2;
	public static final String openID = "osCWfs-ZVQZfrjRK0ml-eEpzeop0";
	
	public static final String SUGGESTED_BY_FOR_USER = "osCWfs-ZVQZfrjRK0ml-eEpzeop0";
	public static final String SUGGESTED_BY_FOR_SYSTEMGRANTED = "SYSTEMGRANTED";
	public static final String SUGGESTED_BY_FOR_SYSTEM = "SYSTEM";
	public static final String SUGGESTED_BY_FOR_SYSTEMUPDATE = "SYSTEMUPDATE";
	
	
	public static final int MAX_LOST_TIME_BEFORE_EXIT_TRADE = 2;
	public static final int MAX_DAYS_WITHOUT_TRADE_BEFORE_EXIT_TRADE = 7;
	
	public static final double MAX_LOST_PCT_FOR_SELL_MODE = -0.06;
	public static final double MAX_GAIN_PCT_FOR_DISABLE_SELL_MODE = 0.04;
    
	public static final double BASE_TRADE_THRESH = 0.03;
	
	public static final double TOP_INCSTOP_BASE_TRADE_THRESH = 0.06;
}
