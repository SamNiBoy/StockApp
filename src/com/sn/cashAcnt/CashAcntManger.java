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
import com.sn.mail.StockObserverable;

public class CashAcntManger {

    static Logger log = Logger.getLogger(CashAcntManger.class);
    
    
    
    static private List<CashAcnt> acnts = new ArrayList<CashAcnt>();
    
    static public List<CashAcnt> getAllAcnts() {
        if (acnts.isEmpty()) {
            loadAllAcnts();
        }
        return acnts;
    }
    
    /*
           private String actId;
           private double initMny;
           private double usedMny;
           private double usedMny_Hrs;
           private double pftMny;
           private double maxMnyPerTrade;
           private double maxUsePct;
     */
    static public boolean crtAcnt(String actId,
                                  double initMny,
                                  double usedMny,
                                  double usedMny_hrs,
                                  double pftMny,
                                  double maxMnyPerTrade,
                                  double maxUsePct) {

        Connection con = DBManager.getConnection();
        String sql = "delete from CashAcnt where acntId = '" + actId + "'";

        log.info("start creating account, remove it first " + actId);
        log.info(sql);
        try {
        Statement stm = con.createStatement();
        stm.execute(sql);
        stm.close();
        
        stm = con.createStatement();
        sql = "insert into cashacnt values(" +
        		"'" + actId + "',"
        		+ initMny + ","
        		+ usedMny + ","
        		+ usedMny_hrs + ","
        		+ pftMny + ","
        		+ maxMnyPerTrade + ","
        		+ maxUsePct + ","
        		+ " sysdate())";
        log.info(sql);
        stm.execute(sql);
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
