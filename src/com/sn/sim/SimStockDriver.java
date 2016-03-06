package com.sn.sim;

import java.sql.Connection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import com.sn.cashAcnt.CashAcnt;
import com.sn.cashAcnt.CashAcntManger;
import com.sn.db.DBManager;
import com.sn.mail.reporter.MailSenderType;
import com.sn.mail.reporter.MailSenderFactory;
import com.sn.mail.reporter.SimpleMailSender;
import com.sn.mail.reporter.StockObserver;
import com.sn.mail.reporter.StockObserverable;
import com.sn.reporter.WCMsgSender;
import com.sn.stock.Stock;
import com.sn.stock.Stock2;
import com.sn.stock.StockMarket;
import com.sn.stock.Stock.Follower;

public class SimStockDriver {

    static Logger log = Logger.getLogger(SimStockDriver.class);

    Connection con = null;

    ArrayList<String> stk_list = new ArrayList<String>();
    
    String start_dt = ""; // format 'yyyy-mm-dd'
    String end_dt = "";
    Statement SimStm = null;
    ResultSet DtRs = null;
    
    private boolean is_sim_on_today = false;
    
    public ConcurrentHashMap<String, Stock2> simstocks = null;
    
    boolean addStkToSim(String stkId) {
        if (stkId != null && !stkId.equals("")) {
            stk_list.add(stkId);
        }
        else {
            return false;
        }
        return true;
    }
    
    public void removeStkToSim() {
        stk_list.clear();
    }
    
    boolean setStartEndSimDt(String s, String e) {
        start_dt = s;
        end_dt = e;
        //SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-DD");
        log.info("end_dt string:" + end_dt);
        if (con == null) {
            con = DBManager.getConnection();
        }

        String sql = "select 'x' from dual where to_char(sysdate, 'yyyy-mm-dd') = '" + end_dt + "'";
        try {
            Statement stm = con.createStatement();
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) {
                log.info("Sim on today's data, set is_sim_on_today to true");
                is_sim_on_today = true;
            }
            else {
                is_sim_on_today = false;
                log.info("Sim not cover today's data, set is_sim_on_today to false");
            }
            rs.close();
            stm.close();
        }
        catch (SQLException ee) {
            ee.printStackTrace();
        }
        return true;
    }
    
    public SimStockDriver() {
    }

    static public void main(String[] args) {
        SimStockDriver ssd = new SimStockDriver();
        ssd.loadStocks();
    }

    public boolean loadStocks() {

        Statement stm = null;
        ResultSet rs = null;
        
        simstocks = new ConcurrentHashMap<String, Stock2>();
        Stock2 s = null;
        int cnt = 0;
        
        if (con == null) {
            con = DBManager.getConnection();
        }
        try {
            if (stk_list.isEmpty()) {
                stm = con.createStatement();
                String sql = "select id, name, gz_flg from stk order by id";
                rs = stm.executeQuery(sql);
                
                String id, name;
                
                while (rs.next()) {
                    id = rs.getString("id");
                    name = rs.getString("name");
                    s = new Stock2(id, name, rs.getLong("gz_flg"));
                    simstocks.put(id, s);
                    cnt++;
                    log.info("LoadStocks completed:" + cnt * 1.0 / 2811);
                }
                rs.close();
                stm.close();
            }
            else {
                for (String stkId : stk_list) {
                    stm = con.createStatement();
                    String sql = "select id, name, gz_flg from stk where id ='" + stkId + "'";
                    rs = stm.executeQuery(sql);
                    
                    String id, name;
                    
                    if (rs.next()) {
                        id = rs.getString("id");
                        name = rs.getString("name");
                        s = new Stock2(id, name, rs.getLong("gz_flg"));
                        simstocks.put(id, s);
                        cnt++;
                        log.info("LoadStocks completed:" + cnt * 1.0 / stk_list.size());
                    }
                    rs.close();
                    stm.close();
                }
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        log.info("SimStockDriver loadStock successed!");
        return true;
    
    }
    
    boolean initData() {
        if (is_sim_on_today) {
            log.info("is_sim_on_today is true, initWithTodayData will take care of initData!");
            return true;
        }
        if (start_dt.equals("") || end_dt.equals("")) {
            log.info("start date or end date is not specificed, can not initData!");
            return false;
        }
        
        if (con == null) {
            con = DBManager.getConnection();
        }
        
        String idClause = "";
        boolean addedFlg = false;
        
        if (!stk_list.isEmpty()) {
            idClause = " and id in ('";
            for (String stkId : stk_list) {
                if (addedFlg) {
                    idClause += ",'";
                }
                idClause += stkId + "'";
                addedFlg = true;
            }
            idClause += ")";
        }
        
        log.info("got idClause: " + idClause);
        String sql = "select * from arc_stkdat2 where to_char(dl_dt, 'yyyy-mm-dd') >= '" + start_dt + 
        "'"+ " and to_char(dl_dt, 'yyyy-mm-dd') <= '" + end_dt + "'" + idClause + " order by id, ft_id";
        try {
            SimStm = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            DtRs = SimStm.executeQuery(sql);
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        
        if (DtRs != null) {
            log.info("successfully loaded DtRs in initData()");
            return true;
        }
        else {
            log.info("unsuccessfully loaded DtRs in initData()");
            return false;
        }
    }
    
    boolean initWithTodayData(String stkId) {
        if (start_dt.equals("") || end_dt.equals("")) {
            log.info("start date or end date is not specificed, can not initWithTodayData!");
            return false;
        }
        else if(!is_sim_on_today) {
            log.info("is_sim_on_today is false, can not run initWithTodayData.");
            return false;
        }
        
        if (con == null) {
            con = DBManager.getConnection();
        }
        
        String idClause = " and id = '" + stkId + "'";
        
        String sql = "select * from arc_stkdat2 where to_char(dl_dt, 'yyyy-mm-dd') >= '" + start_dt + 
        "'"+ " and to_char(dl_dt, 'yyyy-mm-dd') <= '" + end_dt + "'" + idClause + " order by id, ft_id";
        log.info(sql);
        try {
            if (DtRs != null) {
                DtRs.close();
                SimStm.close();
            }
            SimStm = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            DtRs = SimStm.executeQuery(sql);
        }
        catch(SQLException e) {
            e.printStackTrace();
        }
        
        if (DtRs != null) {
            log.info("successfully loaded DtRs in initWithTodayData()");
            return true;
        }
        else {
            log.info("unsuccessfully loaded DtRs in initWithTodayData()");
            return false;
        }
    }
    
    Map<String, Integer> pointer = new HashMap<String, Integer>();
    
    boolean step() {
        if (DtRs == null && !is_sim_on_today) {
            log.info("DtRs is null, can not step");
            return false;
        }
        try {
            for (String stkId : stk_list) {
                
                //Every time we need to reload data from db when is sim on today's data.
                if (is_sim_on_today) {
                    log.info("Now loading DtRs for is_sim_on_today is true");
                    initWithTodayData(stkId);
                }
                int pt = 0;
                DtRs.first();
                
                if (pointer.containsKey(stkId)) {
                    pt = pointer.get(stkId);
                }
                
                if (!DtRs.next()) {
                    log.info("There is no data to simTrader...");
                    return false;
                }
                
                String sid = DtRs.getString("id");
                int ft_id = DtRs.getInt("ft_id");
                
                while (!sid.equals(stkId) || (pt > 0 && ft_id <= pt)) {
                    
                    if (!DtRs.next()) {
                        log.info("No more row in DtRs, finished step()!");
                        return false;
                    }
                    
                    sid = DtRs.getString("id");
                    ft_id = DtRs.getInt("ft_id");
                }
                
                Stock2 s = simstocks.get(stkId);
                if (s != null) {
                    log.info("Now, loading DtRs for stock:" + s.getID());
                    s.getSd().loadDataFromRs(DtRs);
                    //StockMarket.calIndex(s.getDl_dt());
                }

                pointer.put(stkId, ft_id);
            }
            log.info("end step with true!");
            return true;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        log.info("end step with false!");
        return false;
    }
    
    boolean finishStep() {
        log.info("start finishStep");
        try {
            DtRs.close();
            con.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        log.info("end finishStep");
        return true;
    }
    
    boolean startOver() {
        log.info("start startOver again");
        try {
            pointer.clear();
            DtRs.first();
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        log.info("end startOver again.");
        return true;
    }
}