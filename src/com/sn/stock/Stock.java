package com.sn.stock;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.work.fetcher.FetchStockData;

public class Stock {

    static Logger log = Logger.getLogger(Stock.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        Stock s = new Stock("600863", 5, 5);

    }
    private String ID;
    private String Name;
    private String Area;
    private String Py;
    private String Bu;
    private int gapType;
    private int Days;
    
    public Stock(String id, int gaptyp, int ds)
    {
        ID = id;
        gapType = gaptyp;
        Days = ds;
        map = new HashMap<String, Double>();
        commonCreate();
    }
    /* This map stores:
     * Inc cnt,
     * Dsc cnt,
     * continue Inc cnt,
     * diff2 increase flag
     * continue Dsc cnt,
     * diff2 decrease flag,
     * qty sharp increase,
     * inc/des pct so far
     */
    public Map<String, Double> map;
    
    private void commonCreate()
    {
        log.info("Start loading data for stock:" + ID);

        Connection con = DBManager.getConnection();

        try {
            Statement stm = con.createStatement();
            String sql = "select * from stk where id ='" + ID + "'";
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()){
                Name = rs.getString("name");
                Py = rs.getString("py");
                Bu = rs.getString("bu");
            }
            rs.close();
            sql = "select sum(decode(sign(cur_pri_df),1,1,0)) incCnt," +
                  "       sum(decode(sign(cur_pri_df),-1,1,0)) dscCnt" +
                  "  from stkddf" +
                  " where id ='" + ID + 
                  "' and gap =" + gapType +
                  "  and dl_dt > (select max(dl_dt) from stkddf where id ='" + ID + "') - " +Days;
            log.info(sql);
            rs = stm.executeQuery(sql);
            if (rs.next())
            {
                log.info("Total incCnt:" + rs.getLong("incCnt") + " Total dscCnt:" + rs.getLong("dscCnt"));
                map.put("incCnt", Double.valueOf(rs.getLong("incCnt")));
                map.put("dscCnt", Double.valueOf(rs.getLong("dscCnt")));
            }
            rs.close();
            
            sql = "select * " +
            "  from stkddf" +
            " where id ='" + ID + 
            "' and gap =" + gapType +
            "  and dl_dt > (select max(dl_dt) from stkddf where id ='" + ID + "') - " +Days +
            " order by ft_id desc";
            
            log.info(sql);
            rs = stm.executeQuery(sql);
            
            boolean fstRcd = true;
            int ctnInc = 0;
            int ctnDsc = 0;
            double pdf = 0;
            while (rs.next())
            {
                pdf = rs.getDouble("cur_pri_df");
                if (fstRcd)
                {
                    if (pdf > 0) {
                        ctnInc = 1;
                        ctnDsc = 0;
                    }
                    else if (pdf < 0)
                    {
                        ctnInc = 0;
                        ctnDsc = 1;
                    }
                }
                else if (pdf > 0)
                {
                    ctnInc++;
                }
                else if (pdf < 0)
                {
                    ctnDsc++;
                }
            }
            log.info("Total continue increase ctnInc:" + ctnInc + " Total continue desrease ctnDsc:" + ctnDsc);
            map.put("ctnInc", Double.valueOf(ctnInc));
            map.put("ctnDsc", Double.valueOf(ctnDsc));
            rs.close();
            
            sql = "select sum(cur_pri_df) / fop.avg_td_opn_pri incPct" +
            "  from stkddf, (select avg(td_opn_pri) avg_td_opn_pri from stkdat2 where id ='" + ID +
            "'  and dl_dt > (select max(dl_dt) from stkdat2 where id = '" + ID + "') - " + Days + ") fop" +
            " where stkddf.id = '" + ID + 
            "' and gap =" + gapType +
            "  and dl_dt > (select max(dl_dt) from stkddf where id ='" + ID + "') - " +Days +
            " group by fop.avg_td_opn_pri";
            
            log.info(sql);
            rs = stm.executeQuery(sql);
            if (rs.next())
            {
                log.info("Total incPct:" + rs.getDouble("incPct"));
                map.put("incPct", Double.valueOf(rs.getDouble("incPct")));
            }
            rs.close();
            
            sql = "select max(dl_stk_num_df) / avg(dl_stk_num_df) qtyRatio" +
            "  from stkddf" +
            " where stkddf.id = '" + ID + 
            "' and gap =" + gapType +
            "  and dl_dt > (select max(dl_dt) from stkddf where id ='" + ID + "') - " +Days;
            
            log.info(sql);
            rs = stm.executeQuery(sql);
            if (rs.next())
            {
                log.info("Total dl_stk_num_df(max/avg) ratio:" + rs.getDouble("qtyRatio"));
                map.put("qtyRatio", Double.valueOf(rs.getDouble("qtyRatio")));
            }
            rs.close();
            con.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            log.error("Stock.CommonCreate errored:" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public String dsc()
    {
        String dsc = "";
        dsc += "ID:" + ID + "\n";
        dsc += "Name:" + Name + "\n";
        dsc += "incCnt:" + map.get("incCnt") + "\n";
        dsc += "dscCnt:" + map.get("dscCnt") + "\n";
        dsc += "ctnInc:" + map.get("ctnInc") + "\n";
        dsc += "ctnDsc:" + map.get("ctnDsc") + "\n";
        dsc += "incPct:" + map.get("incPct") + "\n";
        dsc += "qtyRatio:" + map.get("qtyRatio") + "\n";
        
        return dsc;
        
    }

}
