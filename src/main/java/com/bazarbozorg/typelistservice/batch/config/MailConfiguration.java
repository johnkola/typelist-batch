package com.bazarbozorg.typelistservice.batch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuration for email notifications.
 */
@Configuration
public class MailConfiguration {
    private static final Logger log = LoggerFactory.getLogger(MailConfiguration.class);

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.host:localhost}")
    private String mailHost;

    @Value("${spring.mail.port:25}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean smtpStartTlsEnable;

    /**
     * Creates and configures the JavaMailSender bean.
     *
     * @return Configured JavaMailSender
     */
    @Bean
    public JavaMailSender javaMailSender() {
        if (!emailEnabled) {
            log.info("Email notifications are disabled. Mail sender will be configured but won't send emails.");
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        
        if (mailUsername != null && !mailUsername.isEmpty()) {
            mailSender.setUsername(mailUsername);
            mailSender.setPassword(mailPassword);
        }
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtpStartTlsEnable));
        props.put("mail.debug", "false");
        
        return mailSender;
    }
}