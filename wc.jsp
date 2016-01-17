<%@ page language="java" import="java.io.*,java.util.*" pageEncoding="UTF-8"%>
    <%
               String path = request.getContextPath();
    %>

<%@ page language="java" contentType="image/png;charset=GB2312" 
import="java.awt.image.*" 
import="java.awt.*" 
import="javax.imageio.*"
import="javax.servlet.*"
%>

<%
response.reset(); 
response.setContentType("image/png"); 
int width = 640, height = 480; 
BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); 
Graphics g = image.getGraphics(); 
g.setColor(Color.WHITE); 
g.fillRect(0, 0, width, height); 
g.setColor(Color.RED); 
for (int i = 1; i <= 11; i++) 
{
  g.drawLine(10, 10, 90, i * 10); 

} 
g.drawLine(160, 10, 110, 110); 
g.drawLine(160, 10, 210, 110); 
g.drawLine(110, 110, 210, 110);
g.drawLine(230, 10, 230, 110);
g.drawLine(230, 10, 330, 10);
g.drawLine(330, 10, 330, 110);
g.drawLine(230, 110, 330, 110);
g.setFont(new Font("方正粗宋简体", Font.PLAIN, 25));
g.drawString("JSP Web图表的绘制", 45, 145);

ServletOutputStream sos = response.getOutputStream();
ImageIO.write(image, "PNG", sos);
sos.close();
%>