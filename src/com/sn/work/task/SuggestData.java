package com.sn.work.task;

import org.apache.log4j.Logger;
import com.sn.stock.Stock;

public class SuggestData {
    public Stock s;
    public int trade_mode_id;
    public boolean moved_to_trade;
    public SuggestData(Stock s1, int tmi) {
        // TODO Auto-generated constructor stub
        s = s1;
        trade_mode_id = tmi;
        moved_to_trade = false;
    }
}
