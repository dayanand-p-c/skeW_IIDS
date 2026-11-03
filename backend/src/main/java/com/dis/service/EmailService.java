package com.dis.service;

import com.dis.model.Alert;
import com.dis.model.AppUser;
import com.dis.model.enums.UserRole;
import com.dis.repository.AppUserRepository;
import com.dis.repository.SystemConfigRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppUserRepository userRepository;
    private final SystemConfigRepository configRepository;

    @Value("${app.mail.dashboard-url:http://localhost:5173}")
    private String dashboardUrl;

    @Value("${spring.mail.username:noreply@drug-inventory.org}")
    private String fromEmail;

    /**
     * Sends HTML alert notifications to active users matching configured role routing.
     * Guaranteed never to fail or roll back parent transactions.
     */
    @Async
    public void sendAlertNotification(Alert alert, boolean isEscalation) {
        try {
            String routeKey = "route." + alert.getType().name();
            String routeRolesConfig = configRepository.getValueOrDefault(routeKey, "ADMIN,PHARMACIST");

            List<UserRole> targetRoles = Arrays.stream(routeRolesConfig.split(","))
                    .map(String::trim)
                    .map(r -> {
                        try {
                            return UserRole.valueOf(r);
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            List<AppUser> recipients = userRepository.findActiveUsersByRoles(targetRoles);
            if (recipients.isEmpty()) {
                log.info("No active users found for alert routing: {}", routeRolesConfig);
                return;
            }

            for (AppUser user : recipients) {
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    continue;
                }
                sendHtmlEmail(user.getEmail(), alert, isEscalation);
            }
        } catch (Exception e) {
            log.error("Failed to send alert notification email: {}", e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, Alert alert, boolean isEscalation) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);

            String subject = String.format("[%s %s] %s Alert: %s (Score: %.1f)",
                    alert.getSeverity(),
                    isEscalation ? "ESCALATED" : "NEW",
                    alert.getType(),
                    alert.getDrug().getName(),
                    alert.getRiskScore());

            helper.setSubject(subject);

            String htmlBody = buildHtmlBody(alert, isEscalation);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Sent alert email for alert {} to {}", alert.getAlertId(), to);
        } catch (Exception e) {
            log.warn("Could not dispatch email to {} (SMTP may be unconfigured in dev): {}", to, e.getMessage());
        }
    }

    private String buildHtmlBody(Alert alert, boolean isEscalation) {
        String severityColor = alert.getSeverity().name().equals("CRITICAL") ? "#F43F5E" : "#F59E0B";
        String batchInfo = alert.getBatch() != null ? alert.getBatch().getBatchId() : "Drug-Level Aggregated";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"></head>
            <body style="font-family: Arial, sans-serif; background-color: #0B1220; color: #F1F5F9; padding: 20px;">
                <div style="max-width: 600px; margin: 0 auto; background: #111A2E; border: 1px solid rgba(255,255,255,0.1); border-radius: 12px; padding: 24px;">
                    <div style="border-left: 4px solid %s; padding-left: 12px; margin-bottom: 20px;">
                        <h2 style="margin: 0; color: %s;">%s: %s Alert</h2>
                        <p style="margin: 4px 0; color: #94A3B8; font-size: 14px;">%s</p>
                    </div>
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 20px; font-size: 14px;">
                        <tr><td style="padding: 8px 0; color: #94A3B8;">Drug Name:</td><td style="font-weight: bold; color: #FFF;">%s (%s)</td></tr>
                        <tr><td style="padding: 8px 0; color: #94A3B8;">Batch ID:</td><td style="color: #FFF;">%s</td></tr>
                        <tr><td style="padding: 8px 0; color: #94A3B8;">Risk Score:</td><td style="font-weight: bold; color: %s;">%.1f / 100</td></tr>
                        <tr><td style="padding: 8px 0; color: #94A3B8;">Alert Status:</td><td style="color: #FFF;">%s</td></tr>
                    </table>
                    <div style="text-align: center; margin-top: 24px;">
                        <a href="%s" style="background-color: #2DD4BF; color: #0B1220; padding: 10px 20px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">View in DIS Dashboard</a>
                    </div>
                </div>
            </body>
            </html>
            """,
                severityColor,
                severityColor,
                isEscalation ? "ESCALATED ALERT" : "NEW ALERT",
                alert.getType(),
                isEscalation ? "Alert severity has escalated to CRITICAL" : "A new inventory risk alert has been triggered",
                alert.getDrug().getName(),
                alert.getDrug().getDrugId(),
                batchInfo,
                severityColor,
                alert.getRiskScore(),
                alert.getStatus(),
                dashboardUrl + "/alerts"
        );
    }
}
