package com.sn.work.fetcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.StockRawData;
import com.sn.work.WorkManager;
import com.sn.work.itf.IWork;

public class RawStockDataQueue {

    static Logger log = Logger.getLogger(RawStockDataQueue.class);
    
    private ArrayBlockingQueue<StockRawData> datque = null;
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        int sz = 1000;
        RawStockDataQueue fsd = new RawStockDataQueue(sz);
    }

    public RawStockDataQueue(int sz)
    {
        datque = new ArrayBlockingQueue<StockRawData>(sz, false);
    }

    public ArrayBlockingQueue<StockRawData> getDatque() {
        return datque;
    }

    public void setDatque(ArrayBlockingQueue<StockRawData> datque) {
        this.datque = datque;
    }
    
}
