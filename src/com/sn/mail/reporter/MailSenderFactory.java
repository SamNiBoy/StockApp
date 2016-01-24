package com.sn.mail.reporter;

import com.sn.mail.reporter.MailSenderType;
import com.sn.mail.reporter.SimpleMailSender;
 
/**
 * 发件箱工厂
 * 
 * @author MZULE
 * 
 */
public class MailSenderFactory {
 
    /**
     * 服务邮箱
     */
    private static SimpleMailSender serviceSms = null;
 
    /**
     * 获取邮箱
     * 
     * @param type 邮箱类型
     * @return 符合类型的邮箱
     */
    public static SimpleMailSender getSender() {
    //if (type == MailSenderType.SERVICE) {
        if (serviceSms == null) {
        serviceSms = new SimpleMailSender("yl_nxj@163.com",
            "13,default");
        }
        return serviceSms;
    //}
    }
 
}