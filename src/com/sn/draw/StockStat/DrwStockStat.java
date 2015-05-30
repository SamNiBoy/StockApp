package com.sn.draw.StockStat;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Comparator;

import org.apache.log4j.Logger;

import com.sn.draw.itf.Draw;
import com.sn.stock.Stock;
import com.sn.work.WorkManager;
import com.sn.work.fetcher.FetchStockData;
import com.sn.work.task.EvaStocks;

public class DrwStockStat implements Draw{

    private static int width = 640;
    private static int height = 480;
    static Logger log = Logger.getLogger(DrwStockStat.class);
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        DrwStockStat ss = new DrwStockStat();
        ss.Draw();
    }

    public BufferedImage Draw() {

        BufferedImage image = null;
        synchronized (EvaStocks.stkLst){
            if (EvaStocks.stkLst.size() <= 0) {
                EvaStocks evs = new EvaStocks(0, 0, true);
                evs.run();
            }
            width = EvaStocks.stkLst.size();
            height = width / 2;
            image = new BufferedImage(width + 100, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics g = image.getGraphics();
            g.setColor(Color.WHITE);
            Collections.sort(EvaStocks.stkLst, new SortByIncPct());
            g.fillRect(0, 0, width + 100, height);
            g.drawLine(50, 10, 50, 480);
            g.drawLine(50, 240, width + 50, 240);
            g.drawString(this.getXLabel(), width/2, 260 );
            g.drawString(this.getYLabel(), 50, 240 );
            for (int i = 0; i<EvaStocks.stkLst.size(); i++) {
                Stock s = EvaStocks.stkLst.get(i);
                double incPct = s.map.get("incPct");
                log.info("DrwStockStat, Stock:" + s.getID() + " Increased:" + incPct);
                if (incPct > 0)
                {
                    g.setColor(Color.RED);
                }
                else
                {
                    g.setColor(Color.GREEN);
                }
                g.drawLine(50 + (i+1), 240, 50 + (i+1), 240 - (int)( incPct * 240));
            }
        }
        //g.setFont(new Font("方正粗宋简体", Font.PLAIN, 25));
        //g.drawString("JSP Web图表的绘制", 45, 145);
        
        return image;
    }

    @Override
    public int getHeight() {
        // TODO Auto-generated method stub
        return height;
    }

    @Override
    public String getTitle() {
        // TODO Auto-generated method stub
        return "Stock Statistics";
    }

    @Override
    public int getWidth() {
        // TODO Auto-generated method stub
        return width;
    }

    @Override
    public String getXLabel() {
        // TODO Auto-generated method stub
        return "Stocks";
    }

    @Override
    public String getYLabel() {
        // TODO Auto-generated method stub
        return "Percentage";
    }
    
    class SortByIncPct implements Comparator<Stock> {

        @Override
        public int compare(Stock arg0, Stock arg1) {
            // TODO Auto-generated method stub
            Stock s0 = arg0;
            Stock s1 = arg1;
            if (s0.map.get("incPct") < s1.map.get("incPct")) {
                log.info("s0 ID:" + s0.getID() + " < s1 ID:" + s1.getID());
                return -1;
            } else {
                log.info("s0 ID:" + s0.getID() + " > s1 ID:" + s1.getID());
                return 1;
            }
        }
    }

}
