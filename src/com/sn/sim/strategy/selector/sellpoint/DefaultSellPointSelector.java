package com.sn.sim.strategy.selector.sellpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import com.sn.db.DBManager;
import com.sn.stock.Stock;
import com.sn.stock.Stock2;

public class DefaultSellPointSelector implements ISellPointSelector {

    /**
     * @param args
     */
    public boolean isGoodSellPoint(Stock2 s) {
        double cur_pri, pre_cur_pri;
        if (s.getSd().getCur_pri_lst().size() <= 1) {
            return false;
        }
        cur_pri = s.getSd().getCur_pri_lst().get(s.getSd().getCur_pri_lst().size() - 1);
        pre_cur_pri = s.getSd().getCur_pri_lst().get(s.getSd().getCur_pri_lst().size() - 2);
        if (pre_cur_pri > cur_pri) {
            return true;
        }
        return false;
    }
}
