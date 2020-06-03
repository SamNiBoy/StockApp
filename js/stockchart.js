var refresh_flg = true;
function switchRefresh() {
        	//alert('clicked refres button');
        	refresh_flg = !refresh_flg;
        	
        	if (refresh_flg) {
        	    $("#refreshBtn").text('停止刷新');
        	}
        	else {
        		$("#refreshBtn").text('开始刷新');
        	}
        }
        
        function addToTrade() {
        	//alert(' has selected');
        	var has_success=false;
        	$("#suggestedStocks tbody>tr").each(function(){
        		if ($(this).hasClass('selected'))
        		{
        			//alert($(this).attr('id') + ' has selected');
        			
        			var stkid = $(this).attr('id');
        			
                    $.ajax({
                        type:"GET",
                        url:"/StockApp/GetIndex",
                         data:{
                             type: "putStockToTrading",
                             id: stkid
                         },
                        success:function (result) {
                        	 
                        	 if (result == "success")
                        	 {
                        		 //alert('put stock ' + $(this).attr('id') + ' into trade success');
                        	 }
                        	 
                        	 has_success = true;
                        	 listSuggestedStocks();
                        	 listTradingStocks();
                        },
                        error:function (err) {
                            //alert("系统错误-TRADERECORD.jsp-ajax");
                        }
                    });
        			
        		}
            });
        }
        
        function addNewToTrade() {
        	//alert(' has selected');
        	var has_success=false;
        			//alert($(this).attr('id') + ' has selected');
        			
        	var stkid = $("#stockToAdd").val();
        	if (stkid !== "") {
                    $.ajax({
                        type:"GET",
                        url:"/StockApp/GetIndex",
                         data:{
                             type: "putStockToTrading",
                             id: stkid
                         },
                        success:function (result) {
                        	 
                        	 if (result == "success")
                        	 {
                        		 //alert('put stock ' + $(this).attr('id') + ' into trade success');
                        	 }
                        	 
                        	 has_success = true;
                        	 listSuggestedStocks();
                        	 listTradingStocks();
                        },
                        error:function (err) {
                            //alert("系统错误-TRADERECORD.jsp-ajax");
                        }
                    });
        	   }
        }
        
        function removeFromTrade() {
        	
        	var has_success=false;
        	$("#tradingStocks tbody>tr").each(function(){
        		if ($(this).hasClass('selected'))
        		{
        			//alert($(this).attr('id') + ' has selected');
        			
        			var stkid = $(this).attr('id');
        			
                    $.ajax({
                        type:"GET",
                        url:"/StockApp/GetIndex",
                         data:{
                             type: "putStockToSuggest",
                             id: stkid
                         },
                        success:function (result) {
                        	 
                        	 if (result == "success")
                        	 {
                        		 //alert('put stock ' + $(this).attr('id') + ' into suggest pool success');
                        	 }
                        	 
                        	 has_success = true;
                        	 listSuggestedStocks();
                        	 listTradingStocks();
                        },
                        error:function (err) {
                        }
                    });
        			
        		}
            });
        }
        
function drawIndex1(subtitle, pct)
{
    	var idx1_opt = {
    	    		title : {
    	        text: '上证指数涨跌幅',
    	        subtext:subtitle,
    	        backgroundColor: '#ABABAB',
    	        subtextStyle: myTextStyle
    	    },
    	        tooltip : {
    	            formatter: "{a} <br/>{c}{b}"
    	        },
    	        toolbox: {
    	            show: true,
    	            feature: {
    	                restore: {show: false},
    	                saveAsImage: {show: true}
    	            }
    	        },
    	        series : [
    	            {
    	                name: '上证指数',
    	                type: 'gauge',
    	                z: 3,
    	                min: -10,
    	                max: 10,
    	         
    	                splitNumber: 20,
    	                radius: '50%',
    	                axisLine: {            // 坐标轴线
    	                    lineStyle: {       // 属性lineStyle控制线条样式
    	                        width: 10
    	                    }
    	                },
    	                axisTick: {            // 坐标轴小标记
    	                    length: 15,        // 属性length控制线长
    	                    lineStyle: {       // 属性lineStyle控制线条样式
    	                        color: 'auto'
    	                    }
    	                },
    	                splitLine: {           // 分隔线
    	                    length: 20,         // 属性length控制线长
    	                    lineStyle: {       // 属性lineStyle（详见lineStyle）控制线条样式
    	                        color: 'auto'
    	                    }
    	                },
    	                title : {
    	                    textStyle: {       // 其余属性默认使用全局文本样式，详见TEXTSTYLE
    	                        fontWeight: 'bolder',
    	                        fontSize: 20,
    	                        fontStyle: 'italic',
    	                        color:"#7FFFD4"
    	                    }
    	                },
    	                detail : {
    	                    textStyle: {       // 其余属性默认使用全局文本样式，详见TEXTSTYLE
    	                        fontWeight: 'bolder',
    	                        fontSize: 36
    	                    }
    	                },
    	                data:[{value: pct, name: '\n\n  \n '}]
    	            }
    	        ]
    	    };
    	    idx1.setOption(idx1_opt, true);
    	    return idx1_opt;
}
 
    	 
        function drawIndex2(degree)
        {
    	    var idx2_opt = {
    	    		title : {
    	        text: '股票实时温度',
    	        subtext:'',
    	        backgroundColor: '#ABABAB',
    	        subtextStyle: myTextStyle
    	    },
    	        tooltip : {
    	            formatter: "{a} <br/>{c}{b}"
    	        },
    	        toolbox: {
    	            show: true,
    	            feature: {
    	                restore: {show: true},
    	                saveAsImage: {show: true}
    	            }
    	        },
    	        series : [
    	            {
    	                name: '上证指数',
    	                type: 'gauge',
    	                z: 3,
    	                min: -100,
    	                max: 100,
    	         
    	                splitNumber: 20,
    	                radius: '50%',
    	                axisLine: {            // 坐标轴线
    	                    lineStyle: {       // 属性lineStyle控制线条样式
    	                        width: 10
    	                        
    	                    }
    	                },
    	                axisTick: {            // 坐标轴小标记
    	                    length: 15,        // 属性length控制线长
    	                    lineStyle: {       // 属性lineStyle控制线条样式
    	                        color: 'auto'
    	                    }
    	                },
    	                splitLine: {           // 分隔线
    	                    length: 20,         // 属性length控制线长
    	                    lineStyle: {       // 属性lineStyle（详见lineStyle）控制线条样式
    	                        color: 'auto'
    	                    }
    	                },
    	                title : {
    	                    textStyle: {       // 其余属性默认使用全局文本样式，详见TEXTSTYLE
    	                        fontWeight: 'bolder',
    	                        fontSize: 20,
    	                        fontStyle: 'italic',
    	                        color:"#7FFFD4"
    	                    }
    	                },
    	                detail : {
    	                    textStyle: {       // 其余属性默认使用全局文本样式，详见TEXTSTYLE
    	                        fontWeight: 'bolder',
    	                        fontSize: 36
    	                    }
    	                },
    	                data:[{value: deg, name: '\n\n  \n'}]
    	            }
    	        ]
    	    };
    	 
    	    idx2.setOption(idx2_opt);
    	    return idx2_opt;
        }
    	    
    	 function drawIndex3(xdata, ydata)
    	 {
    	   var idx3_opt = {
    	title : {
    	    text: '上证指数运行轨迹',
    	    subtext: '',
    	    backgroundColor: '#ABABAB',
    	    subtextStyle: myTextStyle
    	},
    	tooltip : {
    	    trigger: 'axis'
    	},
    	legend: {
    	    data:['涨跌值']
    	},
    	toolbox: {
    	    show : true,
    	    feature : {
    	        dataView : {show: true, readOnly: false},
    	        magicType : {show: true, type: ['line', 'bar']},
    	        restore : {show: true},
    	        saveAsImage : {show: true}
    	    }
    	},
    	calculable : true,
    	color :['#22B101'],
    	xAxis : [
    	    {
    	        type : 'category',
    	        data : xdata
    	    }
    	],
    	yAxis : [
    	    {
    	        type : 'value'
    	    }
    	],
    	series : [
    	    {
    	        name:'涨跌值',
    	        type:'bar',
    	        barWidth: 30,
    	        itemStyle:{
    	            normal:{
    	    	color: function(params) {
    	        if (params.value>0)
    	        	return "#c23531";
    	        else 
    	        	return "#4BABDE";
    	        }
    	            }
    	        },
    	        data:ydata,
    	        markPoint : {
    	            data : [
    	                {type : 'max', name: '最大值'},
    	                {type : 'min', name: '最小值'}
    	            ]
    	        },
    	        markLine : {
    	            data : [
    	                {type : 'average', name: '平均值'}
    	            ]
    	        }
    	    }
    	]
    	};
    	    idx3.setOption(idx3_opt, true);
    	    return idx3_opt;
   }
    	    


    	 function drawIndex4(IncCnt, DecCnt, EqlCnt)
    	 {
    	    var idx4_opt = {
    	    	    title : {
    	    	        text: '涨跌家数分布',
    	    	        subtext: '',
    	    	        textStyle: {
    	    	        	color: '#CD0000'
    	    	        },
    	    	        subtextStyle: myTextStyle,
    	    	        x:'center'
    	    	    },
    	    	    tooltip : {
    	    	        trigger: 'item',
    	    	        formatter: "{a} <br/>{b} : {c} ({d}%)"
    	    	    },
    	    	    legend: {
    	    	        orient: 'vertical',
    	    	        left: 'left',
    	    	        data: ['上涨家数','下跌家数','持平家数']
    	    	    },
    	    	    toolbox: {
    	                show : true,
    	                feature : {
    	                    dataView : {show: true, readOnly: false},
    	                    magicType : {show: true, type: ['line', 'bar']},
    	                    restore : {show: true},
    	                    saveAsImage : {show: true},
    	                    dataZoom: {
    	                    	show: true},
    	                    mark: {show: false}
    	                }
    	            },
    	    	    series : [
    	    	        {
    	    	            name: '涨跌家数分布',
    	    	            type: 'pie',
    	    	            radius : '55%',
    	    	            center: ['50%', '60%'],
    	    	            label: {
    	    	        	        normal: {
    	    	        	          show: true,
    	    	        	          formatter: '{b} : {c} ({d}%)'    	        	        	
    	    	        	        }
    	    	                   },
    	    	            data:[
    	    	                {value:IncCnt, name:'上涨家数'},
    	    	                {value:DecCnt, name:'下跌家数'},
    	    	                {value:EqlCnt, name:'持平家数'}
    	    	            ],
    	    	            itemStyle: {
    	    	                emphasis: {
    	    	                    shadowBlur: 10,
    	    	                    shadowOffsetX: 0,
    	    	                    shadowColor: 'rgba(0, 0, 0, 0.5)'
    	    	                }
    	    	            }
    	    	        }
    	    	    ]
    	    	};
    	    
    	    idx4.setOption(idx4_opt);
    	    return idx4_opt;
    	 }
    	    
function drawIndexCharts() {
	
	if (refresh_flg == false)
		return;
	
	console.log("this is for debug message.");
	    
    $.ajax({
        type:"GET",
        url:"/StockApp/GetIndex",
         data:{
             type: "INDEX"
         },
        success:function (result) {

        	 var vars = result.split("#");

     	    idx1_opt.title.subtext = vars[0];
        	idx1_opt.series[0].data[0].value = vars[1];
     	    idx1.setOption(idx1_opt, true);

        },
        error:function (err) {
            //alert("系统错误-loginPage.jsp-ajax");
        }
    });
    
    
    $.ajax({
        type:"GET",
        url:"/StockApp/GetIndex",
         data:{
             type: "DEGREE"
         },
        success:function (result) {

        	idx2_opt.series[0].data[0].value = result;
     	    idx2.setOption(idx2_opt, true);

        },
        error:function (err) {
            //alert("系统错误-idx2_opt-ajax");
        }
    });
    
    
    $.ajax({
        type:"GET",
        url:"/StockApp/GetIndex",
         data:{
             type: "DELTALST"
         },
        success:function (result) {

        	 var vars = result.split("#");

        	 idx3_opt.xAxis[0].data = eval(vars[0]);
        	 idx3_opt.series[0].data = eval(vars[1]);
     	    idx3.setOption(idx3_opt, true);

        },
        error:function (err) {
            //alert("系统错误-loginPage.jsp-ajax");
        }
    });
    
    $.ajax({
        type:"GET",
        url:"/StockApp/GetIndex",
         data:{
             type: "CNT"
         },
        success:function (result) {

        	 var vars = result.split("#");
        	 idx4_opt.series[0].data[0].value = eval(vars[0]);
        	 idx4_opt.series[0].data[1].value = eval(vars[1]);
        	 idx4_opt.series[0].data[2].value = eval(vars[2]);
     	    idx4.setOption(idx4_opt, true);

        },
        error:function (err) {
            //alert("系统错误-idx4_opt.jsp-ajax");
        }
    });
    
}

function drawSimResultChart(xdata, y1data, y2data, y3data)
{
   var SimResult_opt = {
        title : {
        text: '收益曲线',
        subtext: '',
        backgroundColor: '#ABABAB',
        subtextStyle: myTextStyle
        },
        tooltip : {
        trigger: 'axis'
        },
        legend: {
        data:['模拟收益曲线']
        },
        toolbox: {
        show : true,
        feature : {
            dataView : {show: true, readOnly: false},
            magicType : {show: true, type: ['line', 'bar']},
            restore : {show: true},
            saveAsImage : {show: true}
        }
        },
        calculable : true,
        color :['#22B101'],
        xAxis : [
        {
            type : 'category',
            data : xdata
        }
        ],
        yAxis : [
        {
            type : 'value'
        }
        ],
        series : [
        	    {
                name:'使用金额(十万)',
                type:'line',
                symbolSize: 6,
                itemStyle:{
                    normal:{
        	    	color: "#cc0031"
                    }
                },
                lineStyle: {
                	color: "#cc0031",
        	    	    width: 3
                },
                data:y2data
             },
            {
            name:'净利润(千)',
            type:'line',
            symbolSize: 6,
            itemStyle:{
                normal:{
        	    color: "#00cc31"
                }
            },
            lineStyle: {
            	color: "#00cc31",
          	    width: 3
            },
            data:y1data,
            markPoint : {
                data : [
                    {type : 'max', name: '最大值'},
                    {type : 'min', name: '最小值'}
                ]
            }
            },
            {
            name:'佣金(千)',
            type:'line',
            symbolSize: 6,
            itemStyle:{
                normal:{
            color: "#1234bb"
            }
            },
            lineStyle: {
            	color: "#1234bb",
          	    width: 3
            },
            data:y3data
            }
        ]
    };
    SimResult.setOption(SimResult_opt, true);
    return SimResult_opt;
}

function drawSimResult() {
    $.ajax({
        type:"GET",
        url:"/StockApp/GetIndex",
         data:{
             type: "SIMRESULT"
         },
        success:function (result) {

        	 var vars = result.split("#");

        	 SimResult_opt.xAxis[0].data = eval(vars[0]);
        	 SimResult_opt.series[1].data = eval(vars[1]);
        	 SimResult_opt.series[0].data = eval(vars[2]);
        	 SimResult_opt.series[2].data = eval(vars[3]);
        	 
        	 if(SimResult_opt.xAxis[0].data.length > 0) {
        		 $("#SimResult").show();
        	     SimResult.setOption(SimResult_opt, true);
        	 }
        	 else {
        		 $("#SimResult").hide();
        	 }
        },
        error:function (err) {
            //alert("系统错误-loginPage.jsp-ajax");
        }
    });
}



function drawTradeSummary() {
	
	if (refresh_flg == false)
		return;
	
    $.ajax({
        type:"GET",
        url:"/StockApp/GetIndex",
         data:{
             type: "TRADESUMMARY"
         },
        success:function (result) {

             $("#tradeSummary").replaceWith(result);
             $("#tradeSummary tbody>tr:odd").addClass("odd");
             $("#tradeSummary tbody>tr:even").addClass("even");
             $("#tradeSummary thead").addClass("thead");
             
             $(function() {
                 $('tr.parent').click(function() {
                 	$(this).toggleClass("selected")
                 	.siblings('.child_' + this.id).toggleClass("childselected");
                 });
             });
             
        	 listSuggestedStocks();
        	 listTradingStocks();
        },
        error:function (err) {
            //alert("系统错误-TRADERECORD.jsp-ajax");
        }
    });
}

var got_response_flg = true;

function drawTradeRecords() {
	if (refresh_flg == false)
		return;
	
	if (got_response_flg == false) {
		return;
	}
	else {
		got_response_flg = false;
	}
    $.ajax({
        type:"GET",
        url:"/StockApp/GetIndex",
         data:{
             type: "TRADERECORD"
         },
        success:function (result) {

             $("#tradeRecord").replaceWith(result);
             $("#tradeRecord tbody>tr:odd").addClass("odd");
             $("#tradeRecord tbody>tr:even").addClass("even");
             $("#tradeRecord thead").addClass("thead");
             
             $(function() {
             	$("#myfilter").keyup(function() {
             		$('#detail tbody tr').hide().filter(":contains('" + ($(this).val()) + "')").show();
             	}).keyup();
             });
             got_response_flg = true;
        },
        error:function (err) {
            //alert("系统错误-TRADERECORD.jsp-ajax");
        }
    });
}



function listSuggestedStocks() {
	if (refresh_flg == false)
		return;
	
    $.ajax({
        type:"GET",
        url:"/StockApp/GetIndex",
         data:{
             type: "listSuggestStocks"
         },
        success:function (result) {

             $("#suggestedStocks").replaceWith(result);
             $("#suggestedStocks tbody>tr:odd").addClass("odd");
             $("#suggestedStocks tbody>tr:even").addClass("even");
             $("#suggestedStocks thead").addClass("thead");
             
             $("#suggestedStocks tbody>tr").click(function (){
            	 $(this).toggleClass('selected')
            	 .siblings().removeClass('selected');
             });
        },
        error:function (err) {
            //alert("系统错误-TRADERECORD.jsp-ajax");
        }
    });
}

function listTradingStocks() {
	if (refresh_flg == false)
		return;
	
    $.ajax({
        type:"GET",
        url:"/StockApp/GetIndex",
         data:{
             type: "listTradingStocks"
         },
        success:function (result) {

             $("#tradingStocks").replaceWith(result);
             $("#tradingStocks tbody>tr:odd").addClass("odd");
             $("#tradingStocks tbody>tr:even").addClass("even");
             $("#tradingStocks thead").addClass("thead");         
             
             $("#tradingStocks tbody>tr").click(function (){
            	 $(this).toggleClass('selected')
            	 .siblings().removeClass('selected');
             });
        },
        error:function (err) {
            //alert("系统错误-TRADERECORD.jsp-ajax");
        }
    });
}

function listTopNMnyStocks() {
	
	if (refresh_flg == false)
		return;
	var fordate = $("#fordate").val();
	var topn = $("#topn").val();
	
	if (topn === "") {
		topn = 5;
	}
        $.ajax({
            type:"GET",
            url:"/StockApp/GetIndex",
             data:{
                 type: "listTopNMnyStocks",
                 fordate: fordate,
                 topn: topn
             },
            success:function (result) {
        
                 $("#topNmnystocks").replaceWith(result);
                 $("#topNmnystocks tbody>tr:odd").addClass("odd");
                 $("#topNmnystocks tbody>tr:even").addClass("even");
                 $("#topNmnystocks thead").addClass("thead");
                 
                 $("#topNmnystocks tbody>tr").click(function (){
                	 $(this).toggleClass('selected')
                	 .siblings().removeClass('selected');
                 });
            },
            error:function (err) {
                //alert("系统错误-TRADERECORD.jsp-ajax");
            }
        });
}

function buy() {
	//alert(' has selected');
	var has_success=false;
	$("#topNmnystocks tbody>tr").each(function(){
		if ($(this).hasClass('selected'))
		{
			//alert($(this).attr('id') + ' has selected');
			
			var stkid_timestamp = $(this).attr('id');
			var stkid = stkid_timestamp.substring(0, stkid_timestamp.indexOf("_"));
			
			var ret = confirm("购买:" + stkid + "?");
			
			if (ret == true) {
	            $.ajax({
                type:"GET",
                url:"/StockApp/GetIndex",
                 data:{
                     type: "buyStock",
                     id: stkid
                 },
                success:function (result) {
                	 
                	 if (result == "buysuccess")
                	 {
                		 alert('成功下单 ' + stkid);
                	 }
                	 else {
                		 alert('下单 ' + stkid + ' 失败!');
                	 }
                	 has_success = true;
                },
                error:function (err) {
                    //alert("系统错误-TRADERECORD.jsp-ajax");
                }
            });
			}
			
		    
//			$.confirm({
//				title: '购买确认',
//		        content:  '确认买' + stkid + "?",
//		        useBootstrap: false,
//		        width:
//			    buttons: {
//		            ok: {
//		                text: '买',
//		                theme: 'modern',
//		                btnClass: 'btn btn-primary btn-block example-pc-1',
//		                action: function() {
//	                        $.ajax({
//	                            type:"GET",
//	                            url:"/StockApp/GetIndex",
//	                             data:{
//	                                 type: "buyStock",
//	                                 id: stkid
//	                             },
//	                            success:function (result) {
//	                            	 
//	                            	 if (result == "buysuccess")
//	                            	 {
//	                            		    $.alert({
//	                            		        title: '结果确认!',
//	                            		        content: '买 stock' + stkid + '成功!',
//	                            		    });
//	                            	 }
//	                            	 has_success = true;
//	                            },
//	                            error:function (err) {
//	                                //alert("系统错误-TRADERECORD.jsp-ajax");
//	                            }
//	                        });
//		                }
//		            },
//		            cancel: {
//		                text: '取消',
//		                btnClass: 'btn-primary'
//		            }
//		        }
//			});
		}
    });
}
