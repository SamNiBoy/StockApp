package com.sn.srvlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.sn.simulation.SimTrader;
import com.sn.stock.StockMarket;

import java.io.IOException;
import java.io.PrintWriter;


@WebServlet("/GetIndex")
public class GetIndex extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	static Logger log = Logger.getLogger(GetIndex.class);

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    	doGet(request, response);
    }
 
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
 
    	response.setCharacterEncoding("UTF-8");
    	
        PrintWriter out=response.getWriter();
        
        String typ = request.getParameter("type");
        
        if ("INDEX".equals(typ))
        {
        	out.write(StockMarket.getSHIndexLngDsc() + "#" +StockMarket.getSHIndexDeltaPct());
        }
        else if ("DEGREE".equals(typ))
        {
        	out.write("" + StockMarket.getDegree(null));
        }
        else if ("DELTALST".equals(typ))
        {
        	out.write(StockMarket.getDeltaTSLst() + "#" +StockMarket.getDeltaShIdxLst());
        }
        else if ("CNT".equals(typ))
        {
        	out.write(StockMarket.getTotInc() + "#" +StockMarket.getTotDec()+ "#" + StockMarket.getTotEql());
        }
        else if ("TRADERECORD".equals(typ))
        {
        	out.write(TradeRecord.getTradeRecordsAsTableString());
        }
        else if ("TRADESUMMARY".equals(typ))
        {
        	out.write(TradeRecord.getTradeSummaryAsTableString());
        }
        else if ("listSuggestStocks".equals(typ))
        {
        	out.write(TradeRecord.getSuggestStocksAsTableString());
        }
        else if ("listTradingStocks".equals(typ))
        {
        	out.write(TradeRecord.getTradingStocksAsTableString());
        }
        else if ("putStockToTrading".equals(typ))
        {
        	String stkid = request.getParameter("id");
        	if (TradeRecord.putStockIntoTrade(stkid)) {
        		out.write("success");
        	}
        }
        else if ("putStockToSuggest".equals(typ))
        {
        	String stkid = request.getParameter("id");
        	if (TradeRecord.putStockIntoSuggest(stkid)) {
        		out.write("success");
        	}
        }
        else if ("SIMRESULT".equals(typ))
        {
        	String str1 = StockMarket.getSimTSLst();
        	String str2 = StockMarket.getSimNetPFitLst();
        	String str3 = StockMarket.getSimUsedMnyLst();
        	String str4 = StockMarket.getSimCommMnyLst();
        	String finalStr = str1 + "#" + str2 + "#" + str3 + "#" + str4;
        	log.info(finalStr);
        	out.write(finalStr);
        	
        }
        else if ("listTopNMnyStocks".equals(typ))
        {
        	String fordate = request.getParameter("fordate");
        	int topn = Integer.valueOf(request.getParameter("topn"));
        	out.write(TradeRecord.getTopNMnyStocksAsTableString(fordate, topn));
        }
        else if ("buyStock".equals(typ))
        {
        	String id = request.getParameter("id");
        	
        	log.info("from web buy Stock:" + id);
        	out.write(TradeRecord.buyStock(id));
        }
        out.flush();
        out.close();
    }

}
