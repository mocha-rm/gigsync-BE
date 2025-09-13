package com.jhlab.gigsync.global.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${app.email.sender}")
    private String senderEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    public String sendSimpleMessage(String sendEmail) {
        String verificationCode = createNumber();

        try {
            sendEmailWithResend(sendEmail, verificationCode);
            log.info("이메일 발송 성공: {}", sendEmail);
            return verificationCode;
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}, 오류: {}", sendEmail, e.getMessage(), e);
            throw new IllegalArgumentException("메일 발송 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void sendEmailWithResend(String recipientEmail, String verificationCode) {
        String url = "https://api.resend.com/emails";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey);

        String htmlContent = createEmailHtml(verificationCode);

        Map<String, Object> emailData = Map.of(
                "from", senderEmail,
                "to", new String[]{recipientEmail},
                "subject", "GigSync 이메일 인증",
                "html", htmlContent
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailData, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Resend API 호출 실패: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("Resend API 클라이언트 오류: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage());
        } catch (HttpServerErrorException e) {
            log.error("Resend API 서버 오류: {}, 응답: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("이메일 서비스 일시적 오류");
        }
    }

    private String createEmailHtml(String verificationCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>GigSync 이메일 인증</title>
            </head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5;">
                <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 40px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h1 style="color: #333; margin: 0;">GigSync</h1>
                        <p style="color: #666; margin: 10px 0 0 0;">이메일 인증</p>
                    </div>
                    
                    <h2 style="color: #333; text-align: center;">요청하신 인증 번호입니다</h2>
                    
                    <div style="background-color: #f8f9fa; padding: 30px; text-align: center; margin: 30px 0; border-radius: 6px; border: 2px dashed #007bff;">
                        <h1 style="color: #007bff; margin: 0; font-size: 32px; letter-spacing: 4px; font-weight: bold;">%s</h1>
                    </div>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <p style="color: #666; margin: 0; font-size: 14px;">
                            이 인증번호는 <strong>10분간 유효</strong>합니다.
                        </p>
                        <p style="color: #666; margin: 10px 0 0 0; font-size: 14px;">
                            본인이 요청하지 않은 경우 이 이메일을 무시하세요.
                        </p>
                    </div>
                    
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                    
                    <div style="text-align: center;">
                        <p style="color: #333; margin: 0;">감사합니다.</p>
                        <p style="color: #999; margin: 10px 0 0 0; font-size: 12px;">GigSync Team</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(verificationCode);
    }

    private String createNumber() {
        SecureRandom random = new SecureRandom();
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(3);
            switch (index) {
                case 0 -> key.append((char) (random.nextInt(26) + 97)); // 소문자
                case 1 -> key.append((char) (random.nextInt(26) + 65)); // 대문자
                case 2 -> key.append(random.nextInt(10)); // 숫자
            }
        }
        return key.toString();
    }
}
