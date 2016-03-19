package com.sn.mail.reporter;

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

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
 
/**
 * 服务器邮箱登录验证
 * 
 * @author MZULE
 * 
 */
public class MailAuthenticator extends Authenticator {
 
    /**
     * 用户名（登录邮箱）
     */
    private String username;
    /**
     * 密码
     */
    private String password;
 
    /**
     * 初始化邮箱和密码
     * 
     * @param username 邮箱
     * @param password 密码
     */
    public MailAuthenticator(String username, String password) {
    this.username = username;
    this.password = password;
    }
 
    String getPassword() {
    return password;
    }
 
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
    return new PasswordAuthentication(username, password);
    }
 
    String getUsername() {
    return username;
    }
 
    public void setPassword(String password) {
    this.password = password;
    }
 
    public void setUsername(String username) {
    this.username = username;
    }
 
}
