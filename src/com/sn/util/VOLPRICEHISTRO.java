package com.sn.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.stock.Stock2;
import com.sn.stock.StockBuySellEntry;

public class VOLPRICEHISTRO {
	static Logger log = Logger.getLogger(VOLPRICEHISTRO.class);
	public static  Map<String, ArrayList<VOLPRICEHISTRO>> vph = new ConcurrentHashMap<String, ArrayList<VOLPRICEHISTRO>>();
	public double min_pri;
	public double max_pri;
	public int vol;
	public double vol_pct;
	public int parts;
	public String stkid;
	public int days;
	public VOLPRICEHISTRO(double minpri, double maxpri, int v, double vp, int p, String id, int dys) {
		min_pri = minpri;
		max_pri = maxpri;
		vol = v;
		vol_pct = vp;
		parts = p;
		stkid = id;
		days = dys;
	}
	

	public void printVals() {
		log.info("VOL Histro for Stock:" + stkid + " in days:" + days);
		log.info("[min_pri, max_pri] = [" + min_pri + ", " + max_pri + "] == >[vol, vol_pct] = [" + vol + "," + vol_pct +"]");
	}
}

class VOLPRICEHISTROComparator implements Comparator<VOLPRICEHISTRO> {
    public int compare(VOLPRICEHISTRO a, VOLPRICEHISTRO b) {
        if(a.vol_pct < b.vol_pct){
            return 1;
        }else if(a.vol_pct == b.vol_pct){
            return 0;
        }else{
            return -1;
        }
    }
}
