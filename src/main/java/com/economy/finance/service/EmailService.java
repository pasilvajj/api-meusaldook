package com.economy.finance.service;

import com.economy.finance.config.MailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;

    public void sendPasswordResetEmail(String to, String webResetLink, String mobileResetLink) {
        String body =
                """
                Olá,

                Recebemos um pedido para redefinir a tua palavra-passe.

                Web: %s

                App mobile: %s

                Este link expira em breve. Se não pediste esta alteração, ignora este email.

                — Finanças Pessoais
                """
                        .formatted(webResetLink, mobileResetLink);

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailProperties.enabled() || mailSender == null) {
            log.warn(
                    "Email não configurado. Link de recuperação para {} — web: {} | mobile: {}",
                    to,
                    webResetLink,
                    mobileResetLink);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(to);
        message.setSubject("Recuperar palavra-passe — Finanças Pessoais");
        message.setText(body);
        mailSender.send(message);
    }
}
