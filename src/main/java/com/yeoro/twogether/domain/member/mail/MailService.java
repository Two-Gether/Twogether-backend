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

    public void sendSimpleMessage(String to, String code) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        helper.setTo(to);
        helper.setFrom(senderEmail);
        helper.setSubject("인증을 위한 이메일 인증번호");

        String body = "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f1f1f1; padding: 20px;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 30px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);'>" +
                "<h2 style='color: #4CAF50; font-size: 24px; text-align: center;'>인증을 위한 이메일 인증번호</h2>" +
                "<p style='font-size: 16px; color: #333;'>안녕하세요, <strong>회원님</strong>.</p>" +
                "<p style='font-size: 16px; color: #555;'>요청하신 인증 번호는 아래와 같습니다:</p>" +
                "<div style='text-align: center; padding: 20px; background-color: #f9f9f9; border-radius: 8px; margin: 20px 0;'>" +
                "<h1 style='font-size: 36px; color: #4CAF50; font-weight: bold;'>" + code + "</h1>" +
                "<p style='font-size: 16px; color: #555;'>이 코드를 입력하여 이메일 인증을 완료하세요.</p>" +
                "</div>" +
                "<p style='font-size: 14px; color: #777;'>감사합니다!</p>" +
                "<footer style='font-size: 12px; color: #aaa; text-align: center;'>" +
                "<p>&copy; 2025 Your Company</p>" +
                "</footer>" +
                "</div>" +
                "</body>" +
                "</html>";

        helper.setText(body, true); // HTML true 설정

        mailSender.send(message);
    }
}
