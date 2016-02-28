package com.sn.mail.reporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import org.apache.log4j.Logger;

import com.sn.mail.reporter.MailSenderType;
import com.sn.mail.reporter.MailSenderFactory;
import com.sn.mail.reporter.SimpleMailSender;
import com.sn.sim.SimTrader;

public class StockObserver implements Observer {
    static Logger log = Logger.getLogger(StockObserver.class);
    static public StockObserver globalObs = new StockObserver();

    static public void main(String[] args) {
        StockObserver ppo = new StockObserver();
        StockObserverable gso = new StockObserverable();
        gso.update();
        ppo.update(gso, null);
    }

    @Override
    public void update(Observable obj, Object arg) {
        if (obj instanceof StockObserverable) {
            StockObserverable gso = (StockObserverable) obj;
            // 发送邮件
            SimpleMailSender sms = MailSenderFactory.getSender();
            List<String> recipients = new ArrayList<String>();
            recipients.add("yl_nxj@163.com");
            // recipients.add("samniboy@gmail.com");
            String subject = gso.getSubject();
            String content = gso.getContent();
            log.info("got mail:" + subject + "\n" + content);
            System.out.println("got mail:" + subject + "\n" + content);
            try {
                for (String recipient : recipients) {
                    sms.send(recipient, subject, content);
                }
            } catch (AddressException e) {
                e.printStackTrace();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
        else if (obj instanceof GzStockBuySellPointObserverable) {

            GzStockBuySellPointObserverable gso = (GzStockBuySellPointObserverable) obj;
            // 发送邮件
            SimpleMailSender sms = MailSenderFactory.getSender();
            List<String> recipients = new ArrayList<String>();
            recipients.add("yl_nxj@163.com");
            // recipients.add("samniboy@gmail.com");
            String subject = gso.getSubject();
            String content = gso.getContent();
            log.info("got mail:" + subject + "\n" + content);
            System.out.println("got mail:" + subject + "\n" + content);
            try {
                for (String recipient : recipients) {
                    sms.send(recipient, subject, content);
                }
            } catch (AddressException e) {
                e.printStackTrace();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        
        }
    }

}