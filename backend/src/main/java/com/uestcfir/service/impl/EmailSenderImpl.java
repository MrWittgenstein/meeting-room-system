package com.uestcfir.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import com.uestcfir.service.EmailSender;
import java.util.Optional;


@Slf4j
@Service
public class EmailSenderImpl implements EmailSender {
    @Autowired
    private JavaMailSender javaMailSender;

    @Retryable(value = {MailAuthenticationException.class, MailSendException.class, MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 500))
    //重试3次，每次间隔0.5秒
    public void sendEmail(String to, String subject, String message)  {
        try{
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);
            mailMessage.setFrom("17638875766@163.com");
            javaMailSender.send(mailMessage);
            log.info("Email successfully sent to " + to);
        }
        catch (Exception e){
            throw new RuntimeException("邮件发送失败");
        }
    }

    public String normalizeEmail(String email) {
        return Optional.ofNullable(email)
                .map(e -> e.trim()                   // 去除首尾空格
                        .replaceAll("\\p{Cntrl}", "") // 移除控制字符
                        .replaceAll("\\s+", "")      // 移除所有空白字符
                        .toLowerCase())              // 统一转为小写
                .filter(e -> !e.isEmpty())           // 过滤空字符串
                .orElseThrow(() ->
                        new IllegalArgumentException("邮箱地址不能为空或空白"));
    }


}
