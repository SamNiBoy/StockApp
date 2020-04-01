package com.sn.wechat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.log4j.Logger;

import com.sn.db.DBManager;
import com.sn.wechat.msg.itf.IWCMsg;

public class WCMsgSender implements IWCMsg {
    static Logger log = Logger.getLogger(WCMsgSender.class);
    /*
     * <xml> <ToUserName> <![CDATA[gh_a9586d5aa590]]> </ToUserName>
     * 
     * <FromUserName> <![CDATA[osCWfs-ZVQZfrjRK0ml-eEpzeop0]]> </FromUserName>
     * 
     * <CreateTime> 1431827441 </CreateTime>
     * 
     * <MsgType> <![CDATA[text]]> </MsgType>
     * 
     * <Content> <![CDATA[Standard ]]> </Content>
     * 
     * <MsgId> 6149652032815793242 </MsgId>
     * 
     * </xml>
     */
    static private String token = null;
    static long tokenTime;
    String frmUsr = "osCWfs-ZVQZfrjRK0ml-eEpzeop0";
    String toUsr;
    String content;
    String msgId;
    long crtTime;
    static Connection con = DBManager.getConnection();

    String resContent;

    public WCMsgSender() {

        log.info("MsgSender initilized...");
    }

    public static void main(String[] args) {
        WCMsgSender ms = new WCMsgSender();
        //ms.createJasonMsg();
        //ms.sendMsg();
        ms.crtMenu();
    }

    static private void getToken() {
        if (token == null || System.currentTimeMillis() - tokenTime >= 7200000) {
            String APPID = "wx23bd4a8c2f6afbef";
            String APPSECRET = "d67ad4ccd309506bdad0313098dfbe42";
            String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                    + APPID + "&secret=" + APPSECRET;
            URL ul;
            try {
                ul = new URL(url);

                InputStream is = ul.openStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String str = "", txt = "";
                while ((str = br.readLine()) != null) {
                    txt += str;
                    System.out.println(str);
                }
                System.out.println(txt);
                int s = txt.indexOf("token\":\"");
                int e = txt.indexOf("\"", s + 8);
                System.out.println("s:" + s + " e:" + e + "\n"
                        + txt.substring(s + 8, e));
                token = txt.substring(s + 8, e);
                tokenTime = System.currentTimeMillis();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void createJasonMsg() {
        String rspMsg = "";
        if (frmUsr != null && !frmUsr.equals("")) {

            rspMsg = "{";
            rspMsg += "    \"touser\":\"" + frmUsr + "\",";
            rspMsg += "    \"msgtype\":\"text\",";
            rspMsg += "   \"text\":{";
            rspMsg += "    \"content\":\"" + new Date().getTime() + "\"";
            rspMsg += "   }";
            rspMsg += "}";
        }
        System.out.println("jason data:"+rspMsg);
        resContent = rspMsg;
    }

    public void sendMsg() {
        getToken();
        String action = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=" + token;
        URL url;
        try {
        url = new URL(action);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type",
        "application/x-www-form-urlencoded");
        http.setDoOutput(true);
        http.setDoInput(true);
        System.setProperty("sun.net.client.defaultConnectTimeout", "30000");// 连接超时30秒
        System.setProperty("sun.net.client.defaultReadTimeout", "30000"); // 读取超时30秒
        http.connect();
        OutputStream os = http.getOutputStream();
        os.write(resContent.getBytes("UTF-8"));// 传入参数
        InputStream is = http.getInputStream();
        int size = is.available();
        byte[] jsonBytes = new byte[size];
        is.read(jsonBytes);
        String result = new String(jsonBytes, "UTF-8");
        System.out.println("请求返回结果:"+result);
        os.flush();
        os.close();
        } catch (Exception e) {
        e.printStackTrace();
        }
    }
    
    public void crtMenu() {
        getToken();
        String action = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=" + token;
        URL url;
        try {
        url = new URL(action);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type",
        "application/x-www-form-urlencoded");
        http.setDoOutput(true);
        http.setDoInput(true);
        System.setProperty("sun.net.client.defaultConnectTimeout", "30000");// 连接超时30秒
        System.setProperty("sun.net.client.defaultReadTimeout", "30000"); // 读取超时30秒
        http.connect();
        OutputStream os = http.getOutputStream();
        buildMenuJson();
        os.write(resContent.getBytes("UTF-8"));// 传入参数
        InputStream is = http.getInputStream();
        int size = is.available();
        byte[] jsonBytes = new byte[size];
        is.read(jsonBytes);
        String result = new String(jsonBytes, "UTF-8");
        System.out.println("请求返回结果:"+result);
        os.flush();
        os.close();
        } catch (Exception e) {
        e.printStackTrace();
        }
    }
    
    private void buildMenuJson() {
    	resContent =
    			"{"
    			+ " \"button\":["
    			+ "{"
    			+ "    \"type\":\"click\","
    			+ "    \"name\":\"Stock List\","
    			+ "    \"key\":\"V_KEY_STKLST\""
    			+ "}]"
    			+"}";
    }

    private void crtRcvMsg() {
        Statement stm = null;
        try {
            stm = con.createStatement();
            String sql;
            /*
             * msgId number not null, frmUsrID varchar2(100 byte) not null,
             * toUsrID varchar2(100 byte) not null, crtTime number not null,
             * mstType varchar2(20 byte) not null, content varchar2(1000 byte)
             * not null,
             */
            sql = "insert into msg values ('" + msgId + "', '" + frmUsr
                    + "', '" + toUsr + "', " + crtTime + ", 'text', '"
                    + content + "')";
            log.info("Creating msg:" + sql);
            int crted = stm.executeUpdate(sql);
            if (crted == 1) {
                log.info("Msg from user:" + frmUsr + " already being added!");
            } else {
                log.info("Msg from user:" + frmUsr + " NOT being added!");
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error("crtRcvMsg errored:" + e.getMessage());
        }
    }

    @Override
    public String getContent() {
        // TODO Auto-generated method stub
        return content;
    }

    @Override
    public String getCreateTime() {
        // TODO Auto-generated method stub
        return "";
    }

    @Override
    public String getFromUserName() {
        // TODO Auto-generated method stub
        return frmUsr;
    }

    @Override
    public String getMsgId() {
        // TODO Auto-generated method stub
        return "";
    }

    @Override
    public String getMsgType() {
        // TODO Auto-generated method stub
        return "text";
    }

    @Override
    public String getToUserName() {
        // TODO Auto-generated method stub
        return toUsr;
    }
}
