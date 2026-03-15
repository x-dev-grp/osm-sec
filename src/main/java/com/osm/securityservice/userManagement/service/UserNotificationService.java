package com.osm.securityservice.userManagement.service;

import org.springframework.stereotype.Service;

@Service
public class UserNotificationService {
    private final HtmlMailService htmlMailService;
    private final EmailTemplateService emailTemplateService;

    public UserNotificationService(HtmlMailService htmlMailService, EmailTemplateService emailTemplateService) {
        this.htmlMailService = htmlMailService;
        this.emailTemplateService = emailTemplateService;
    }

    public void sendAccountActivationOtp(String email, String fullName, String code, String activationLink) throws Exception {
        String html = emailTemplateService.buildAccountActivationEmail(fullName, code, 15, activationLink
        );

        htmlMailService.sendHtmlEmail(email, "Activation de votre compte OSM", html);
    }


    public void sendResendActivationOtp(String email, String fullName, String code, String activationLink) throws Exception {
        String html = emailTemplateService.buildResendActivationOtpEmail(fullName, code, 15, activationLink);
        htmlMailService.sendHtmlEmail(email, "Nouveau code OTP", html);
    }

    public void sendResetPasswordCode(String email, String fullName, String code, String resetLink) throws Exception {
        String html = emailTemplateService.buildResetPasswordCodeEmail(fullName, code, resetLink);
        htmlMailService.sendHtmlEmail(email, "Réinitialisation du mot de passe", html);
    }
}