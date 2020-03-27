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

public class StockBuySellEntry{

    static Logger log = Logger.getLogger(StockBuySellEntry.class);
    /**
     * @param args
     */
    public String id;
    public String name;
    public boolean is_buy_point;
    public double price;
    public int quantity;
    public Timestamp dl_dt;
    
    public static void main(String[] args) {}
    
    public StockBuySellEntry(String ids, String nm, double curpri, int qty, boolean ibp, Timestamp tm)
    {
        id = ids;
        name = nm;
        price = curpri;
        quantity = qty;
        is_buy_point = ibp;
        dl_dt = tm;
    }

    public void printStockInfo() {
        log.info("========================================\n");
        log.info("Stock " + id + " data buy/sell information:\n");
        log.info("========================================\n");
        log.info("ID\t|Name\t|IS_BUY_POINT\t|Price\t|Quantity\t|Time\t|");
        log.info(id + "|" + name + "\t|" + ((is_buy_point)? "TRUE" : "FALSE") + "\t|" + price + "\t|" + quantity + "\t|" + dl_dt.toString() + "\t|");
    }
}
