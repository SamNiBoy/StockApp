package com.sn.basic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;

public class LoadStk {

    static Logger log = Logger.getLogger(LoadStk.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        DBManager dbm;
        Connection con = DBManager.getConnection();
        Statement stm = null;
        String sql;

        try {
            con.setAutoCommit(false);
            stm = con.createStatement();

            FileReader fr = new FileReader(".\\scripts\\stockcodes.txt");
            BufferedReader br = new BufferedReader(fr);

            String s = br.readLine();
            while (s != null) {
                log.info(s);
                sql = CreateStk(s);
                stm.execute(sql);
                s = br.readLine();
            }
            con.commit();
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                con.rollback();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        finally {
            try {
                stm.close();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    static String CreateStk(String stk)
    {
        if (stk.length() <= 0)
            return "";

        String values[] = stk.split(",");

        String sql = "insert into stk (id, area, name, py, bu) values('"
                + values[1] + "', '"
                + values[0] + "', '"
                + values[2] + "', '"
                + "py" + "', '"
                + values[3] + "')";

        log.info(sql);
        return sql;
    }

}
