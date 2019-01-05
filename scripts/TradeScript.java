Do
    Call Plugin.Sys.SetCLB("B600503300")
    //Rem nextPoint
    stockMsg = Plugin.Sys.GetCLB()
    TracePrint stockMsg
    If stockMsg <> "" Then 
        Call Plugin.Sys.SetCLB("")
        Call trade(stockMsg)
        //Call keepWindowAlive()

    End If
    Delay 5000
    //Call Plugin.Sys.SetCLB("S002431600")
    //Goto nextPoint
Loop
Sub trade(stockMsg)
    Msg1 = left(stockMsg, 7)
    Qty = right(stockMsg, 3)
    bs = left(Msg1, 1)
    stk = right(Msg1, 6)
    If (bs = "B") Then 
        TracePrint "Buy " & stk
        Hwnd = Plugin.Window.Find("Chrome_WidgetWin_1", 0)
        //Hwnd = Plugin.Window.Find("Chrome_WidgetWin_1", 0)
        //Hwnd = Plugin.Window.Foreground()   
//下面这句是得到窗口句柄的类名   
//Class1 = Plugin.Window.GetClass(Hwnd)   
//MsgBox "得到窗口句柄的类名为："& Class1
        If (Hwnd = 1) Then
            Call Lib.API.运行程序("C:\Program Files (x86)\GF-Trader\gf-trader.exe")
            Hwnd =  Hwnd = Plugin.Window.Find("Chrome_WidgetWin_1", 0)
        ElseIf (Hwnd <> 0) Then
            Plugin.Window.Max Hwnd
            Delay 100
            KeyPress 13, 1
            
            For i = 1 To len(stk)
                ch = mid(stk, i, 1)
                KeyPress ch, 1
                Delay 200
            Next
            //enter
            KeyPress 13, 1
            
            TracePrint "now press .+1 to buy with sell 1 price."
            
            KeyPress 110, 1
            Delay 200
            KeyPress 107, 1
            Delay 200
            KeyPress 97, 1
            Delay 200

            KeyPress 13, 1
            TracePrint "now buy " & Qty & " for " & stk
            For i = 1 To len(Qty)
                ch = mid(Qty, i, 1)
                KeyPress ch, 1
                Delay 200
            Next
            //two 'enter' to confirm, and 2 'Esc' to close possible msgbox
            KeyPress 13, 1
            Delay 200
            KeyPress 13, 1
            Delay 200
            KeyPress 27, 1
            Plugin.Window.Min Hwnd
        End if
    End If
    
    If (bs = "S") Then 
        TracePrint "Now Sell" & stk & " with qty:" + Qty
        Hwnd = Plugin.Window.Find("Chrome_WidgetWin_1", 0)
        If (Hwnd = 1) Then 
        ElseIf (Hwnd <> 0) Then
            Plugin.Window.Max Hwnd
            Delay 200
            KeyPress 13, 1
            For i = 1 To len(stk)
                ch = mid(stk, i, 1)
                KeyPress ch, 1
                Delay 200
            Next
            
            KeyPress 13, 1
            
            TracePrint "now press .-1 for S"
            
            KeyPress 110, 1
            Delay 200
            KeyPress 109, 1
            Delay 200
            KeyPress 97, 1
            Delay 200
            
            KeyPress 13, 1
            TracePrint "now press " & Qty & " for to sell " & stk
            For i = 1 To len(Qty)
                ch = mid(Qty, i, 1)
                KeyPress ch, 1
                Delay 200
            Next
            KeyPress 13, 1
            Delay 200
            KeyPress 13, 1
            Delay 200
            KeyPress 27, 1
            Plugin.Window.Min Hwnd
        End if
    End If
End Sub
Sub keepWindowAlive()
    //Hwnd = Lib.API.2é?ò′°?ú??±ú(0, "1?·￠?¤èˉ?á??°?v7.511 - [?¨??°???-1?·￠ê×ò3]")
    //Hwnd = Plugin.Window.Find("TdxW_MainFrame_Class", 0)
    Hwnd = Plugin.Window.Find("Chrome_WidgetWin_1", 0)
    If (Hwnd <> 0) Then 
        Call Plugin.Window.Active(Hwnd)
        //Call Plugin.Window.Top(Hwnd, 0) 
        TracePrint "Now press F9 activate trade window"
        KeyPress 120, 1// pressing F9 twice keep trade window alive
        Delay 200
        KeyPress 120, 1
        Delay 200
    End If
End Sub