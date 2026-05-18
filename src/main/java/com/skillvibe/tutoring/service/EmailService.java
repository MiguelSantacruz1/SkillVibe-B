package com.skillvibe.tutoring.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:noreply@skillvibes.co}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        String html = buildHtml(
                "Verifica tu correo",
                "Hola " + fullName + ", gracias por registrarte en SkillVibes.",
                "Haz clic en el botón para activar tu cuenta. El enlace expira en 24 horas.",
                link,
                "Verificar mi cuenta",
                "#a855f7"
        );
        sendHtml(toEmail, "SkillVibes — Verifica tu correo", html);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String html = buildHtml(
                "Restablecer contraseña",
                "Hola " + fullName + ", recibimos tu solicitud de cambio de contraseña.",
                "Haz clic en el botón para establecer una nueva contraseña. El enlace expira en 1 hora.",
                link,
                "Restablecer mi contraseña",
                "#ec4899"
        );
        sendHtml(toEmail, "SkillVibes — Restablecer contraseña", html);
    }

    private void sendHtml(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "SkillVibes");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email enviado a {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", to, e.getMessage());
        }
    }

    private String buildHtml(String title, String heading, String body, String link, String btnText, String btnColor) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#0f0a1e;font-family:'Segoe UI',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0f0a1e;padding:40px 0;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0"
                             style="background:rgba(255,255,255,0.05);border-radius:20px;
                                    border:1px solid rgba(168,85,247,0.3);overflow:hidden;max-width:600px;width:100%%;">
                        <!-- Header -->
                        <tr>
                          <td style="background:linear-gradient(135deg,#a855f7,#ec4899);
                                     padding:40px;text-align:center;">
                            <h1 style="color:#fff;margin:0;font-size:28px;font-weight:700;">SkillVibes</h1>
                            <p style="color:rgba(255,255,255,0.8);margin:8px 0 0;font-size:14px;">
                              Plataforma de tutorías universitarias
                            </p>
                          </td>
                        </tr>
                        <!-- Body -->
                        <tr>
                          <td style="padding:40px;">
                            <h2 style="color:#fff;margin:0 0 16px;font-size:22px;">%s</h2>
                            <p style="color:rgba(255,255,255,0.7);line-height:1.6;margin:0 0 12px;">%s</p>
                            <p style="color:rgba(255,255,255,0.6);line-height:1.6;margin:0 0 32px;font-size:14px;">%s</p>
                            <div style="text-align:center;margin-bottom:32px;">
                              <a href="%s"
                                 style="display:inline-block;background:%s;color:#fff;
                                        text-decoration:none;padding:16px 40px;border-radius:50px;
                                        font-weight:700;font-size:16px;letter-spacing:0.5px;">
                                %s
                              </a>
                            </div>
                            <p style="color:rgba(255,255,255,0.4);font-size:12px;text-align:center;margin:0;">
                              Si no realizaste esta solicitud, puedes ignorar este correo.
                            </p>
                          </td>
                        </tr>
                        <!-- Footer -->
                        <tr>
                          <td style="padding:20px 40px;text-align:center;
                                     border-top:1px solid rgba(255,255,255,0.05);">
                            <p style="color:rgba(255,255,255,0.3);font-size:12px;margin:0;">
                              © 2026 SkillVibes · Universidad de Nariño
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(heading, heading, body, link, btnColor, btnText);
    }
}
