<%@ page language="java" contentType="text/html; charset=utf-8"
import="java.io.*,java.util.*,java.sql.*,javax.servlet.http.*,com.sn.db.DBManager,com.sn.srvlet.TradeRecord,com.sn.stock.StockMarket,java.text.DecimalFormat,org.apache.log4j.Logger,org.apache.log4j.PropertyConfigurator"
	pageEncoding="utf-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>股市监控</title>
<!-- 引入ECharts文件 -->
<script type="text/javascript" src="./js/echarts.min.js" ></script>
<script type="text/javascript" src="./js/jquery-3.5.0.min.js" ></script>
<script type="text/javascript" src="./js/stockchart.js" ></script>

<link rel="stylesheet" type="text/css" href="css/common.css" />
 
<script type="text/javascript">
var myTextStyle = {
 color: "#BB00AA",//文字颜色
 fontStyle: "normal",//italic斜体  oblique倾斜
 fontWeight: "normal",//文字粗细bold   bolder   lighter  100 | 200 | 300 | 400...
 fontFamily: "sans-serif",//字体系列
 fontSize: 12 //字体大小
};

<jsp:useBean id="sm" scope="application" class="com.sn.stock.StockMarket" />
 
</script>
</head>
<body>
	<center>
		<h1>股票实时监控</h1>
		<div id="index" style="width: 100%;height:400px; border: solid 1px #0000AA; margin: 2px; background-color: white;">
		     <div id="idx1" style="width: 15%;height:100%; margin: 2px; background-color: white;"></div>
		     <div id="idx2" style="width: 15%;height:100%; margin: 2px; background-color: white;"></div>
		     <div id="idx4" style="width: 25%;height:100%; margin: 2px; background-color: white;"></div>
		     <div id="idx3" style="width: 45%;height:100%; margin: 2px; background-color: white;"></div>
		</div>
		
		<div id="SimResult" style="width: 100%;height:400px; border: solid 1px #0000AA; margin: 2px; background-color: white;"></div>
		
		<h1>交易汇总</h1>
		<div id="tradeSummary" ></div>
		
		<h1>交易明细</h1>
		<div class="filter" ">
		<span style="width:1190px;text-align:right;">过滤:</span>
		<input id="myfilter" style="width:150px;height:30px;"></input>
		<button id="refreshBtn" onclick="switchRefresh()" style="width:150px;height:40px;">停止刷新</button>
		</div>
		<div id="tradeRecord" ></div>
	
		<div class="wrap">
	    <panel id="left">推荐的股票
	       <div id="suggestedStocks"></div>
	    </panel>
	    <panel id="middle">
	        <button class="actinbtn" id="addToTrade" onclick="addToTrade()" style="width:150px;height:60px;">添加到交易池-></button>
	        <button class="actinbtn" id="removeFromTrade" onclick="removeFromTrade()" style="width:150px;height:60px;">移动到推荐池<-</button>
	    </panel>
	    <panel id="right">交易中的股票
	    		<div id="tradingStocks"></div>
	    </panel>
	    </div>
	
		<script type="text/javascript">

        var subtitle = <%=sm.getSHIndexLngDsc()%>;
        var pct = <%=sm.getSHIndexDeltaPct()%>;
        var idx1 = echarts.init(document.getElementById('idx1'));
		var idx1_opt = drawIndex1(subtitle, pct);
		
		var deg = <%=sm.getDegree(null)%>;
 	    var idx2 = echarts.init(document.getElementById('idx2'));
		var idx2_opt = drawIndex2(deg);
		
	    var xdata = eval(<%=sm.getDeltaTSLst()%>);
	    var ydata = eval(<%=sm.getDeltaShIdxLst()%>);
	    var idx3 = echarts.init(document.getElementById('idx3'));
	    var idx3_opt = drawIndex3(xdata, ydata);
	    
	    var IncCnt = <%=sm.getTotInc()%>;
	    var DecCnt = <%=sm.getTotDec()%>;
	    var EqlCnt = <%=sm.getTotEql()%>;
	    var idx4 = echarts.init(document.getElementById('idx4'));
	    var idx4_opt = drawIndex4(IncCnt, DecCnt, EqlCnt);
	    //fresh above sub charts every 30 seconds.
	    setInterval(drawIndexCharts, 30000);
	    
	    
	    var xdata = eval(<%=sm.getSimTSLst()%>);
	    var y1data = eval(<%=sm.getSimNetPFitLst()%>);
	    var y2data = eval(<%=sm.getSimUsedMnyLst()%>);
	    var y3data = eval(<%=sm.getSimCommMnyLst()%>);
	    var SimResult = echarts.init(document.getElementById('SimResult'));
	    var SimResult_opt = drawSimResultChart(xdata, y1data, y2data, y3data);

	    setInterval(drawSimResult, 10000);
	    
	    setInterval(drawTradeSummary, 30000);
	    setInterval(drawTradeRecords, 60000);
	    
	    listSuggestedStocks();
	    listTradingStocks();
		
        </script>
	</center>
</body>
</html>