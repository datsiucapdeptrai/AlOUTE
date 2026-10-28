package vn.edu.hcmute.fakebook.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmute.fakebook.entity.OtpToken;
import vn.edu.hcmute.fakebook.enums.OtpType;
import vn.edu.hcmute.fakebook.repository.OtpTokenRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final JavaMailSender mailSender;

    public void generateAndSendOtp(String email, OtpType type) {
        String cleanEmail = email.trim().toLowerCase();
        
        // 1. Tạo mã OTP
        String code = String.format("%06d", new SecureRandom().nextInt(999999));
        
        // 2. Lưu vào DB thông qua một Transaction riêng biệt (REQUIRES_NEW)
        saveOtpToDatabase(cleanEmail, code, type);

        // 3. Gửi mail (Bọc try-catch để không bao giờ làm sập luồng chính)
        try {
            sendEmail(cleanEmail, code, type);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email OTP tới {}: {}", cleanEmail, e.getMessage());
        }
    }

    // Tách riêng hàm lưu DB để đảm bảo dù gửi mail lỗi thì OTP vẫn được lưu
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOtpToDatabase(String email, String code, OtpType type) {
        // Xóa OTP cũ
        otpTokenRepository.deleteByEmailAndType(email, type);

        OtpToken otp = OtpToken.builder()
                .email(email)
                .code(code)
                .type(type)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
        
        // saveAndFlush để đẩy ngay xuống DB
        otpTokenRepository.saveAndFlush(otp);
    }

    private void sendEmail(String to, String code, OtpType type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(type == OtpType.REGISTER ? "[FAKEBOOK] Mã xác thực đăng ký" : "[FAKEBOOK] Mã khôi phục mật khẩu");
            
            String html = "<div style='font-family:sans-serif; padding:20px; border:1px solid #ddd; border-radius:8px; max-width:500px;'>"
                    + "<h2 style='color:#0d6efd'>FAKEBOOK</h2>"
                    + "<p>Mã xác thực (OTP) của bạn là:</p>"
                    + "<h1 style='color:#0d6efd; letter-spacing: 5px; text-align: center;'>" + code + "</h1>"
                    + "<p style='color:red; font-size:12px;'>Mã này có hiệu lực trong 5 phút.</p>"
                    + "</div>";
            
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Đã gửi OTP thành công tới: {}", to);
        } catch (Exception e) {
            log.error("Chi tiết lỗi gửi mail: ", e);
            throw new RuntimeException("Lỗi gửi mail");
        }
    }

    @Transactional
    public boolean verifyOtp(String email, String code, OtpType type) {
        String cleanCode = code != null ? code.trim() : "";
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";

        return otpTokenRepository.findValidOtp(cleanEmail, cleanCode, type, LocalDateTime.now())
                .map(otp -> {
                    otp.setUsed(true); 
                    otpTokenRepository.save(otp);
                    return true;
                }).orElse(false);
    }
}