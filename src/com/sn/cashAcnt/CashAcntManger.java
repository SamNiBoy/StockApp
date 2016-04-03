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
        if (rs == null) {
            acnts.clear();
            crtDftAcnt();
            return getDftAcnt();
        }
        return rs;
    }
    static public boolean crtDftAcnt() {

        Connection con = DBManager.getConnection();
        String sql = "select * from CashAcnt where dft_acnt_flg = 1";
        String id = "";

        log.info("start creating default account, check if we already have default account...");
        try {
        Statement stm = con.createStatement();
        ResultSet rs = stm.executeQuery(sql);
        
        if (rs.next()) {
            rs.close();
            stm.close();
            con.close();
            return false;
        }
        sql = "insert into cashacnt values('testCashAct001',20000,0,0,4,0.5,1,sysdate)";
        stm.execute(sql);
        con.commit();
        rs.close();
        con.close();
        con = null;
        log.info("successfully created defalt account testCaseAct001...");
        return true;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        log.info("end creating default account.");
        return false;
    }
    /*
           private String actId;
           private double initMny;
           private double usedMny;
           private double pftMny;
           private int splitNum;
           private double maxUsePct;
           private boolean dftAcnt;
     */
    static public boolean crtAcnt(String actId,
                                  double initMny,
                                  double usedMny,
                                  double pftMny,
                                  int splitNum,
                                  double maxUsePct,
                                  boolean dftAcnt) {

        Connection con = DBManager.getConnection();
        String sql = "delete from CashAcnt where acntId = '" + actId + "'";

        log.info("start creating account, remove it first " + actId);
        log.info(sql);
        try {
        Statement stm = con.createStatement();
        stm.execute(sql);
        stm.close();
        con.commit();
        
        stm = con.createStatement();
        sql = "insert into cashacnt values(" +
        		"'" + actId + "',"
        		+ initMny + ","
        		+ usedMny + ","
        		+ pftMny + ","
        		+ splitNum + ","
        		+ maxUsePct + ","
        		+ (dftAcnt ? 1 : 0) +
        		",sysdate)";
        log.info(sql);
        stm.execute(sql);
        con.commit();
        con.close();
        log.info("successfully created account " + actId);
        return true;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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
    
    static public ICashAccount loadAcnt(String acntId) {

        Connection con = DBManager.getConnection();
        String sql = "select * from CashAcnt where acntId = '" + acntId + "'";
        String id = "";
        ICashAccount a = null;
        Statement stm = null;
        ResultSet rs = null;

        log.info("start loading cashAccount information for " + acntId);
        try {
            stm = con.createStatement();
            rs = stm.executeQuery(sql);
            
            while (rs.next()) {
                id = rs.getString("acntId");
                a = new CashAcnt(id);
            }
            rs.close();
            stm.close();
            con.close();
        }
        catch (SQLException e) {
            e.printStackTrace();
            try {
				rs.close();
	            stm.close();
	            con.close();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

        }
        log.info("Successed loading cashAccount information for " + acntId);
        return a;
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        loadAllAcnts();
    }

}
