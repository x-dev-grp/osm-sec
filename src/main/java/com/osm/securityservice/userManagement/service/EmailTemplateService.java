package com.osm.securityservice.userManagement.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    public EmailTemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }
    public String buildAccountActivationEmail(String fullName, String otpCode, int expiryMinutes, String activationLink) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("otpCode", otpCode);
        context.setVariable("expiryMinutes", expiryMinutes);
        context.setVariable("activationLink", activationLink);

        return templateEngine.process("mail/account-activation", context);
    }
    public String buildResendActivationOtpEmail(String fullName, String otpCode, int expiryMinutes, String activationLink) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("otpCode", otpCode);
        context.setVariable("expiryMinutes", expiryMinutes);
        context.setVariable("activationLink", activationLink);

        return templateEngine.process("mail/resend-activation-otp", context);
    }

    public String buildResetPasswordCodeEmail(String fullName, String otpCode, String resetLink) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("otpCode", otpCode);
        context.setVariable("resetLink", resetLink);

        return templateEngine.process("mail/password-reset-code", context);
    }
}