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

public class StockPriceObserver implements Observer {
    static Logger log = Logger.getLogger(StockPriceObserver.class);
    static public StockPriceObserver globalObs = new StockPriceObserver();

    static public void main(String[] args) {
        StockPriceObserver ppo = new StockPriceObserver();
        GzStockObserverable gso = new GzStockObserverable();
        gso.update();
        ppo.update(gso, null);
    }

    @Override
    public void update(Observable obj, Object arg) {
        if (obj instanceof GzStockObserverable) {
            GzStockObserverable gso = (GzStockObserverable) obj;
            // 发送邮件
            SimpleMailSender sms = MailSenderFactory.getSender();
            List<String> recipients = new ArrayList<String>();
            recipients.add("sam.ni@bleum.com");
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