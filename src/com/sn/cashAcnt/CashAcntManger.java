package com.sn.cashAcnt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.mail.reporter.StockObserverable;

public class CashAcntManger {

    static Logger log = Logger.getLogger(CashAcntManger.class);
    
    static private List<CashAcnt> acnts = new ArrayList<CashAcnt>();
    
    static public List<CashAcnt> getAllAcnts() {
        if (acnts.isEmpty()) {
            loadAllAcnts();
        }
        return acnts;
    }
    
    static public CashAcnt getDftAcnt() {
        CashAcnt rs = null;
        if (acnts.isEmpty()) {
            loadAllAcnts();
        }
        for (CashAcnt s : acnts) {
            if (s.isDftAcnt()) {
                rs = s;
                break;
            }
        }
        return rs;
    }
    static public boolean loadAllAcnts() {

        Connection con = DBManager.getConnection();
        String sql = "select * from CashAcnt";
        String id = "";

        log.info("start loading cashAccount information...");
        try {
        Statement stm = con.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        
        while (rs.next()) {
            id = rs.getString("acntId");
            CashAcnt a = new CashAcnt(id);
            acnts.add(a);
        }
        rs.close();
        con.close();
        con = null;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        log.info("end loading cashAccount information, last acnt:" + id);
        if (!id.equals("")) {
            return true;
        }
        else {
            return true;
        }
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        loadAllAcnts();
    }

}
