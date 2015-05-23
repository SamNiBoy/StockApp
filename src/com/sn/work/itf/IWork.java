package com.sn.work.itf;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.db.DBManager;

public interface IWork extends Runnable{

    /**
     * @param args
     */

    public String getWorkName();

    public String getWorkResult();

    public long getInitDelay();
    public long getDelayBeforeNxt();

    public TimeUnit getTimeUnit();

    public boolean isCycleWork();
}
