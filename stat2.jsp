<%@ page language="java" contentType="image/png;charset=UTF-8" 
import="java.awt.image.*" 
import="java.awt.*" 
import="javax.imageio.*"
import="javax.servlet.*"
import="com.sn.draw.StockStat.*"
%>

<%
response.reset(); 
response.setContentType("image/png"); 
BufferedImage image = null;
DrwStockStat dss = new DrwStockStat();

image = dss.Draw();

ServletOutputStream sos = response.getOutputStream();
ImageIO.write(image, "PNG", sos);
sos.close();
%>