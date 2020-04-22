package com.sn.srvlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sn.stock.StockMarket;

import java.io.IOException;
import java.io.PrintWriter;


@WebServlet("/GetIndex")
public class GetIndex extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    	doGet(request, response);
    }
 
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
 
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
        out.flush();
        out.close();
    }

}
