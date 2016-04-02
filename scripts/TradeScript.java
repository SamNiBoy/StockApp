Do
    //Call Plugin.Sys.SetCLB("S600503")
    //Rem nextPoint
    stockMsg = Plugin.Sys.GetCLB()
    TracePrint stockMsg
    If stockMsg <> "" Then 
        Call Plugin.Sys.SetCLB("")
        Call trade(stockMsg)
    Else 
        //Call keepWindowAlive()
        Delay 100
    End If
    //Goto nextPoint
Loop
Sub trade(stockMsg)
    bs = left(stockMsg, 1)
    stk = right(stockMsg, 6)
    If (bs = "B") Then 
        Hwnd = Plugin.Window.Find("TdxW_MainFrame_Class", 0)
        If (Hwnd = 1) Then 
            Call Lib.API.ÔËÐÐ³ÌÐò("C:\new_gfzq_v6_fhzx\TdxW.exe")
            Hwnd = Lib.API.²éÕÒ´°¿Ú¾ä±ú(0, "¹ã·¢Ö¤È¯ÖÁÇ¿°æv7.511 - [¶¨ÖÆ°æÃæ-¹ã·¢Ê×Ò³]")
        ElseIf (Hwnd <> 0) Then
            Call Lib.API.¼¤»î´°¿Ú²¢ÖÃÇ°(Hwnd)
            Plugin.Window.Max Hwnd
            Delay 100
            KeyPress 13, 1
            TracePrint "Âò" & stk
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
            Plugin.Window.Min Hwnd
        End if
    End If
    TracePrint "Now Sell" & bs
    If (bs = "S") Then 
        Hwnd = Plugin.Window.Find("TdxW_MainFrame_Class", 0)
        If (Hwnd = 1) Then 
            Call Lib.API.ÔËÐÐ³ÌÐò("C:\new_gfzq_v6_fhzx\TdxW.exe")
            Hwnd = Lib.API.²éÕÒ´°¿Ú¾ä±ú(0, "¹ã·¢Ö¤È¯ÖÁÇ¿°æv7.511 - [¶¨ÖÆ°æÃæ-¹ã·¢Ê×Ò³]")
        ElseIf (Hwnd <> 0) Then
            Call Lib.API.¼¤»î´°¿Ú²¢ÖÃÇ°(Hwnd)
            Plugin.Window.Max Hwnd
            Delay 100
            KeyPress 13, 1
            TracePrint "Âô" & stk
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
            Plugin.Window.Min Hwnd
        End if
    End If
End Sub
Sub keepWindowAlive()
    //Hwnd = Lib.API.²éÕÒ´°¿Ú¾ä±ú(0, "¹ã·¢Ö¤È¯ÖÁÇ¿°æv7.511 - [¶¨ÖÆ°æÃæ-¹ã·¢Ê×Ò³]")
    Hwnd = Plugin.Window.Find("TdxW_MainFrame_Class", 0)
    If (Hwnd <> 0) Then 
        Call Plugin.Window.Active(Hwnd)
        TracePrint "Now press F9 activate trade window"
        KeyPress 120, 1// pressing F9 twice keep trade window alive
        Delay 200
        KeyPress 120, 1
        Delay 200
    End If
End Sub