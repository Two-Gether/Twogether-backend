package com.yeoro.twogether.domain.member.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    // 이메일 인증 코드 발송
    public void sendSimpleMessage(String to, String code) throws MessagingException {
        String body = "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f1f1f1; padding: 20px;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 30px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);'>" +
                "<h2 style='color: #4CAF50; font-size: 24px; text-align: center;'>인증을 위한 이메일 인증번호</h2>" +
                "<p style='font-size: 16px; color: #333;'>요청하신 인증 번호는 아래와 같습니다:</p>" +
                "<div style='text-align: center; padding: 20px; background-color: #f9f9f9; border-radius: 8px; margin: 20px 0;'>" +
                "<h1 style='font-size: 36px; color: #4CAF50; font-weight: bold;'>" + code + "</h1>" +
                "</div>" +
                "<p style='font-size: 14px; color: #777;'>감사합니다!</p>" +
                "<footer style='font-size: 12px; color: #aaa; text-align: center;'><p>&copy; 2025 Your Company</p></footer>" +
                "</div></body></html>";
        sendHtml(to, "인증을 위한 이메일 인증번호", body);
    }

    // 비밀번호 재설정 코드 발송
    public void sendPasswordResetCode(String to, String code) throws MessagingException {
        String html = """
        <html><body style='font-family:Arial,sans-serif;'>
          <h2>비밀번호 재설정 코드</h2>
          <p>아래 코드를 입력하면 새 비밀번호를 설정하실 수 있습니다. (유효시간 10분)</p>
          <div style='padding:12px;background:#f5f5f5;border-radius:8px;display:inline-block;'>
            <span style='font-size:26px;font-weight:bold;'>%s</span>
          </div>
        </body></html>
    """.formatted(code);
        sendHtml(to, "비밀번호 재설정 코드", html);
    }

    private void sendHtml(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setTo(to);
        helper.setFrom(senderEmail);
        helper.setSubject(subject);
        helper.setText(body, true);
        mailSender.send(message);
    }
}
