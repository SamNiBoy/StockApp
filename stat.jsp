<%@ page language="java"
	import="java.io.*,java.util.*,java.sql.*,com.sn.db.DBManager,java.text.DecimalFormat,org.apache.log4j.Logger,org.apache.log4j.PropertyConfigurator"
	pageEncoding="UTF-8"%>
<html>
<title>Datas</title>
<body>
<form action="stat.jsp" method="post">ID:<input type="text"
	name="ID"> <input type="submit" value="提交"></form>
<%
    Connection con;
    Statement stmt;
    ResultSet rs;
    String stkName = "";
    String totIncDsc = "";
    try {
        con = DBManager.getConnection();
        stmt = con.createStatement();
        DecimalFormat df = new DecimalFormat("##.##");
        String ID = request.getParameter("ID");
        String sql = "select area, name from stk where id ='" + ID
                + "'";
        rs = stmt.executeQuery(sql);

        if (rs.next()) {
            stkName = rs.getString("area") + ID + ":"
                    + rs.getString("name");
        }
        rs.close();
        sql = "select sum(case when cur_pri >= (td_opn_pri + 0.01) then 1 else 0 end) incNum," +
              "       sum(case when cur_pri <= (td_opn_pri - 0.01) then 1 else 0 end) decNum," +
              "       sum(case when abs(cur_pri - td_opn_pri) < 0.01 then 1 else 0 end) equNum" +
              "  from stkdat2 " +
              " where to_char(dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd') " +
              "   and ft_id in (select max(ft_id) from stkdat2 group by ID)";
        
        System.out.println(sql);
        rs = stmt.executeQuery(sql);
        
        if (rs.next()) {
            totIncDsc = "[" + rs.getString("incNum") + "+/" + rs.getString("decNum") + "-/" + rs.getString("equNum") + "=]";
        }
        
        rs.close();
        
        sql = "select to_char(sd.dl_dt, 'yyyy/mm/dd') Day,"
                + "       to_char(sd.dl_dt, 'HH24:MI:SS') Time,"
                + "       sd.*, " + "       sdf.* "
                + "  from stkdat sd " + "  left join "
                + "  stkddf sdf " + "    on sd.id = sdf.id "
                + "   and sd.ft_id = sdf.ft_id " + " where sd.id = '"
                + ID + "'" + "  order by sd.ft_id desc ";

        System.out.println(sql);
        rs = stmt.executeQuery(sql);
        out.println("<table cellpadding=5 cellspacing=1 align=\"left\"> "
                        + "<tr>"
                        + "<td>昨收</td> "
                        + "<td>今开</td> "
                        + "<td>当前价</td> "
                        + "<td>价差</td> "
                        + "<td>交易量</td> "
                        + "<td>B1</td> "
                        + "<td>B2</td> "
                        + "<td>B3</td> "
                        + "<td>B4</td> "
                        + "<td>B5</td> "
                        + "<td>S1</td> "
                        + "<td>S2</td> "
                        + "<td>S3</td> "
                        + "<td>S4</td> "
                        + "<td>S5</td> "
                        + "<td>时间</td> "
                        + "<td>日期</td> " + "</tr>\n");
        out.println("<caption align=center>" + stkName + totIncDsc + "</caption>");

        String b1_clr, b2_clr, b3_clr, b4_clr, b5_clr, dl_stk_num_df_clr;
        String b1_val, b2_val, b3_val, b4_val, b5_val, dl_stk_num_df_val;
        String s1_clr, s2_clr, s3_clr, s4_clr, s5_clr, cur_pri_df_clr;
        String s1_val, s2_val, s3_val, s4_val, s5_val, cur_pri_df_val;
        while (rs.next()) {
            if (rs.getString("b1_num_df") == null) {
                b1_clr = "AAAAAA\">";
                b1_val = "";
            } else {
                if (rs.getDouble("b1_num_df") > 0
                        && rs.getDouble("b1_pri_df") > 0) {
                    b1_clr = "FF0000\" ><font color=white>";
                } else {
                    b1_clr = "00FF00\">";
                }
                b1_val = rs.getString("b1_num_df") + "|"
                        + df.format(rs.getDouble("b1_pri_df"));
            }
            if (rs.getString("b2_num_df") == null) {
                b2_clr = "AAAAAA\">";
                b2_val = "";
            } else {
                if (rs.getDouble("b2_num_df") > 0
                        && rs.getDouble("b2_pri_df") > 0) {
                    b2_clr = "AA0000\"><font color=white>";
                } else {
                    b2_clr = "00AA00\">";
                }
                b2_val = rs.getString("b2_num_df") + "|"
                        + df.format(rs.getDouble("b2_pri_df"));
            }

            if (rs.getString("b3_num_df") == null) {
                b3_clr = "AAAAAA\">";
                b3_val = "";
            } else {
                if (rs.getDouble("b3_num_df") > 0
                        && rs.getDouble("b3_pri_df") > 0) {
                    b3_clr = "AA0000\"><font color=white>";
                } else {
                    b3_clr = "00AA00\">";
                }
                b3_val = rs.getString("b3_num_df") + "|"
                        + df.format(rs.getDouble("b3_pri_df"));
            }

            if (rs.getString("b4_num_df") == null) {
                b4_clr = "AAAAAA\">";
                b4_val = "";
            } else {
                if (rs.getDouble("b4_num_df") > 0
                        && rs.getDouble("b4_pri_df") > 0) {
                    b4_clr = "AA0000\"><font color=white>";
                } else {
                    b4_clr = "00AA00\">";
                }
                b4_val = rs.getString("b4_num_df") + "|"
                        + df.format(rs.getDouble("b4_pri_df"));
            }

            if (rs.getString("b5_num_df") == null) {
                b5_clr = "AAAAAA\">";
                b5_val = "";
            } else {
                if (rs.getDouble("b5_num_df") > 0
                        && rs.getDouble("b5_pri_df") > 0) {
                    b5_clr = "AA0000\"><font color=white>";
                } else {
                    b5_clr = "00AA00\">";
                }
                b5_val = rs.getString("b5_num_df") + "|"
                        + df.format(rs.getDouble("b5_pri_df"));
            }

            if (rs.getString("s1_num_df") == null) {
                s1_clr = "AAAAAA\">";
                s1_val = "";
            } else {
                if (rs.getDouble("s1_num_df") > 0
                        && rs.getDouble("s1_pri_df") > 0) {
                    s1_clr = "FF0000\"><font color=white>";
                } else {
                    s1_clr = "00FF00\">";
                }
                s1_val = rs.getString("s1_num_df") + "|"
                        + df.format(rs.getDouble("s1_pri_df"));
            }

            if (rs.getString("s2_num_df") == null) {
                s2_clr = "AAAAAA\">";
                s2_val = "";
            } else {
                if (rs.getDouble("s2_num_df") > 0
                        && rs.getDouble("s2_pri_df") > 0) {
                    s2_clr = "AA0000\"><font color=white>";
                } else {
                    s2_clr = "00AA00\">";
                }
                s2_val = rs.getString("s2_num_df") + "|"
                        + df.format(rs.getDouble("s2_pri_df"));
            }

            if (rs.getString("s3_num_df") == null) {
                s3_clr = "AAAAAA\">";
                s3_val = "";
            } else {
                if (rs.getDouble("s3_num_df") > 0
                        && rs.getDouble("s3_pri_df") > 0) {
                    s3_clr = "AA0000\"><font color=white>";
                } else {
                    s3_clr = "00AA00\">";
                }
                s3_val = rs.getString("s3_num_df") + "|"
                        + df.format(rs.getDouble("s3_pri_df"));
            }

            if (rs.getString("s4_num_df") == null) {
                s4_clr = "AAAAAA\">";
                s4_val = "";
            } else {
                if (rs.getDouble("s4_num_df") > 0
                        && rs.getDouble("s4_pri_df") > 0) {
                    s4_clr = "AA0000\"><font color=white>";
                } else {
                    s4_clr = "00AA00\">";
                }
                s4_val = rs.getString("s4_num_df") + "|"
                        + df.format(rs.getDouble("s4_pri_df"));
            }

            if (rs.getString("s5_num_df") == null) {
                s5_clr = "AAAAAA\">";
                s5_val = "";
            } else {
                if (rs.getDouble("s5_num_df") > 0
                        && rs.getDouble("s5_pri_df") > 0) {
                    s5_clr = "AA0000\"><font color=white>";
                } else {
                    s5_clr = "00AA00\">";
                }
                s5_val = rs.getString("s5_num_df") + "|"
                        + df.format(rs.getDouble("s5_pri_df"));
            }
            
            if (rs.getString("dl_stk_num_df") == null) {
                dl_stk_num_df_clr = "AAAAAA\">";
                dl_stk_num_df_val = "";
            } else {
                if (rs.getDouble("dl_stk_num_df") > 0) {
                    dl_stk_num_df_clr = "AA0000\"><font color=white>";
                } else {
                    dl_stk_num_df_clr = "00AA00\">";
                }
                dl_stk_num_df_val = rs.getString("dl_stk_num_df");
            }
            
            if (rs.getString("cur_pri_df") == null) {
                cur_pri_df_clr = "AAAAAA\">";
                cur_pri_df_val = "";
            } else {
                if (rs.getDouble("cur_pri_df") > 0) {
                    cur_pri_df_clr = "AA0000\"><font color=white>";
                } else {
                    cur_pri_df_clr = "00AA00\">";
                }
                cur_pri_df_val = df.format(rs.getDouble("cur_pri_df"));
            }
            out.print("<tr><td>" + rs.getDouble("yt_cls_pri")
                            + "</td>");
            out.print("<td>" + rs.getString("td_opn_pri") + "</td>");
            out.print("<td "
                    + "bgcolor=\"#"
                    + (rs.getDouble("cur_pri") > rs
                            .getDouble("td_opn_pri") ? "AA0000\"><font color=white>"
                            : "00AA00\">")
                    + df.format(rs.getDouble("cur_pri")) + "|" + df.format((rs.getDouble("cur_pri")-rs.getDouble("td_opn_pri"))/rs.getDouble("td_opn_pri")) + "</td>");
            out.print("<td " + "bgcolor=\"#" + cur_pri_df_clr + cur_pri_df_val
                    + "</td>");
            out.print("<td " + "bgcolor=\"#" + dl_stk_num_df_clr + dl_stk_num_df_val
                    + "</td>");
            out.print("<td " + "bgcolor=\"#" + b1_clr + b1_val
                    + "</td>");
            out.print("<td " + "bgcolor=\"#" + b2_clr + b2_val
                    + "</td>");
            out.print("<td " + "bgcolor=\"#" + b3_clr + b3_val
                    + "</td>");
            out.print("<td " + "bgcolor=\"#" + b4_clr + b4_val
                    + "</td>");
            out.print("<td " + "bgcolor=\"#" + b5_clr + b5_val
                    + "</td>");
            out.print("<td " + "bgcolor=\"#" + s1_clr + s1_val
                    + "</td>");
            out.print("<td " + "bgcolor=\"#" + s2_clr + s2_val
                    + "</td>");
            out.print("<td " + "bgcolor=\"#" + s3_clr + s3_val
                    + "</td>");
            out.print("<td " + "bgcolor=\"#" + s4_clr + s4_val
                    + "</td>");
            out.print("<td " + "bgcolor=\"#" + s5_clr + s5_val
                    + "</td>");
            out.print("<td>" + rs.getString("Time") + "</td>");
            out.print("<td>" + rs.getString("Day") + "</td></tr>\n");
        }
        rs.close();
        stmt.close();
        con.close();
    } catch (SQLException sqle) {

    }
%>
</body>
</html>