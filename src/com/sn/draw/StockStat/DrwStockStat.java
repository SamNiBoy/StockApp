package com.sn.draw.StockStat;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;

import org.apache.log4j.Logger;

import com.sn.draw.itf.Draw;
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
//        synchronized (EvaStocks.stkLst){
//            if (EvaStocks.stkLst.size() <= 0) {
//                EvaStocks evs = new EvaStocks(0, 0, true);
//                evs.run();
//            }
//            width = EvaStocks.stkLst.size();
//            height = width / 2;
//            image = new BufferedImage(width + 100, height,
//                    BufferedImage.TYPE_INT_RGB);
//            Graphics g = image.getGraphics();
//            g.setColor(Color.WHITE);
//            Collections.sort(EvaStocks.stkLst, new SortByIncPct());
//            g.fillRect(0, 0, width + 100, height);
//            g.drawLine(50, 10, 50, height);
//            g.drawLine(50, height/2, width + 50, height/2);
//            int incCnt = 0, dscCnt = 0, equCnt = 0;
//            double incPctSum = 0, dscPctSum = 0;
//            for (int i = 0; i<EvaStocks.stkLst.size(); i++) {
//                Stock s = EvaStocks.stkLst.get(i);
//                double incPct = s.map.get("incPct");
//                log.info("DrwStockStat, Stock:" + s.getID() + " Increased:" + incPct);
//                if (incPct > 0)
//                {
//                    incCnt++;
//                    incPctSum += incPct;
//                    g.setColor(Color.RED);
//                    log.info("incCnt stoks Number:"+incCnt + "set Red color.");
//                }
//                else if (incPct < 0)
//                {
//                    dscCnt++;
//                    dscPctSum+=incPct;
//                    g.setColor(Color.GREEN);
//                    log.info("dscCnt stoks Number:"+dscCnt + "set Green color.");
//                }
//                else {
//                    equCnt++;
//                    log.info("eqaCnt stoks Number:"+equCnt + "skip drawing.");
//                    continue;
//                }
//                int gd = (height/2 - 10);
//                g.drawLine(50 + (i+1), height/2, 50 + (i+1), gd - (int)( incPct * gd));
//            }
//            double incp1 = EvaStocks.stkLst.get(0).map.get("incPct");
//            double incp2 = EvaStocks.stkLst.get(EvaStocks.stkLst.size()-1).map.get("incPct");
//            log.info("incPct Range is:[" + incp1 + ", " + incp2+ "]");
//            g.drawString(this.getXLabel(), width/2, height/2 + 20 );
//            g.drawString(this.getYLabel(), 50, height/2 );
//            DecimalFormat df = new DecimalFormat("##.##");
//            
//            g.setFont(new Font("方正粗宋简体", Font.PLAIN, 25));
//            String title = "IncCnt:" + incCnt + " avg:" + df.format(incPctSum/incCnt) + ", dscCnt:" + dscCnt + " avg:" + df.format(dscPctSum/dscCnt) + " equal:" + equCnt;
//            g.drawString(title, width/2, 50 );
//        }
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
    

}
