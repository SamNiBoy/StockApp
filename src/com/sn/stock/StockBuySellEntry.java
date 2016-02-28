package com.sn.stock;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.work.fetcher.FetchStockData;

public class StockBuySellEntry{

    static Logger log = Logger.getLogger(Stock.class);
    /**
     * @param args
     */
    public String id;
    public String name;
    public boolean is_buy_point;
    public double price;
    public String timestamp;
    
    public static void main(String[] args) {}
    
    public StockBuySellEntry(String ids, String nm, boolean ibp, String tm)
    {
        id = ids;
        name = nm;
        is_buy_point = ibp;
        timestamp = tm;
    }

    public void printStockInfo() {
        log.info("========================================\n");
        log.info("Stock " + id + " data buy/sell information:\n");
        log.info("========================================\n");
        log.info("ID\t|Name\t|IS_BUY_POINT\t|");
        log.info(id + "\t|" + name + "\t|" + ((is_buy_point)? "TRUE" : "FALSE") + "\t|\n");
    }
}
