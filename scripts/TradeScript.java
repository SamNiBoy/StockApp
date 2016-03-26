Do
//Call Plugin.Sys.SetCLB("S600503")

//Rem nextPoint
stockMsg = Plugin.Sys.GetCLB()
TracePrint stockMsg
If stockMsg <> "" Then 
Call Plugin.Sys.SetCLB("")
Call trade(stockMsg)
Else 
Call keepWindowAlive()
End If
Delay 2000
//Goto nextPoint
Loop

Sub trade(stockMsg)
	bs = left(stockMsg, 1)
	stk = right(stockMsg, 6)
	If (bs = "B") Then 
	    Hwnd = Plugin.Window.Find("TdxW_MainFrame_Class", 0)
	    If (Hwnd = 1) Then 
	        Call Lib.API.运行程序("C:\new_gfzq_v6_fhzx\TdxW.exe")
	        Hwnd = Lib.API.查找窗口句柄(0, "广发证券至强版v7.511 - [定制版面-广发首页]")
	    ElseIf (Hwnd <> 0) Then
            Call Lib.API.激活窗口并置前(Hwnd)
            Plugin.Window.Max Hwnd
            
            Delay 100
            KeyPress 13, 1
            TracePrint "买" & stk

            For i = 1 To len(stk)
            	ch = mid(stk, i, 1)
            	KeyPress ch, 1
            	Delay 100
            Next

            //enter
            KeyPress 13, 1

            TracePrint "now press 21 to buy"
            For i = 1 To len("21")
            	ch = mid("21", i, 1)
            	KeyPress ch, 1
            	Delay 100
            Next
            
            KeyPress 13, 1
            
            TracePrint "now buy 100 for " & stk
            For i = 1 To len("100")
            	ch = mid("100", i, 1)
            	KeyPress ch, 1
            	Delay 100
            Next
            
            //two 'enter' to confirm, and 2 'Esc' to close possible msgbox
            KeyPress 13, 1
            Delay 100
            KeyPress 13, 1
            Delay 100
            KeyPress 27, 1
            Delay 100
            KeyPress 27, 1
            Delay 1000
            Plugin.Window.Min Hwnd
        End if
	End If
	
	TracePrint "Now Sell" & bs
	If (bs = "S") Then 
	    Hwnd = Plugin.Window.Find("TdxW_MainFrame_Class", 0)
	    If (Hwnd = 1) Then 
	        Call Lib.API.运行程序("C:\new_gfzq_v6_fhzx\TdxW.exe")
	        Hwnd = Lib.API.查找窗口句柄(0, "广发证券至强版v7.511 - [定制版面-广发首页]")
	    ElseIf (Hwnd <> 0) Then
            Call Lib.API.激活窗口并置前(Hwnd)
            Plugin.Window.Max Hwnd
            
            Delay 100
            KeyPress 13, 1
            TracePrint "卖" & stk
            For i = 1 To len(stk)
            	ch = mid(stk, i, 1)
            	KeyPress ch, 1
            	Delay 100
            Next

            KeyPress 13, 1

            TracePrint "now press 23 for S"
            For i = 1 To len("23")
            	ch = mid("23", i, 1)
            	KeyPress ch, 1
            	Delay 100
            Next

            KeyPress 13, 1

            TracePrint "now press 100 for to sell " & stk
            For i = 1 To len("100")
            	ch = mid("100", i, 1)
            	KeyPress ch, 1
            	Delay 100
            Next
            KeyPress 13, 1
            Delay 100
            KeyPress 13, 1
            Delay 100
            KeyPress 27, 1
            Delay 100
            KeyPress 27, 1
            Delay 1000
            Plugin.Window.Min Hwnd
        End if
	End If
End Sub

Sub keepWindowAlive()
	//Hwnd = Lib.API.查找窗口句柄(0, "广发证券至强版v7.511 - [定制版面-广发首页]")
    Hwnd = Plugin.Window.Find("TdxW_MainFrame_Class", 0)

	If (Hwnd <> 0) Then 
		Call Plugin.Window.Active(Hwnd)
		TracePrint "Now press F9 activate trade window"
		KeyPress 120, 1// pressing F9 twice keep trade window alive
		Delay 2000
		KeyPress 120, 1
		Delay 2000
		KeyPress 27, 1
	End If
End Sub