package vn.edu.hcmute.fakebook.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.entity.EmailVerificationOtp;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.enums.AccountStatus;
import vn.edu.hcmute.fakebook.repository.EmailVerificationOtpRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Logger log = LoggerFactory
        .getLogger(EmailVerificationService.class);
    private static final int EXPIRE_MINUTES = 5;
    private static final int RESEND_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;

    private final EmailVerificationOtpRepository otpRepository;
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Transactional
    public void sendInitialOtp(UserAccount user) {
        issueOtp(user, false);
    }

    @Transactional
    public void ensureOtpForPendingUser(UserAccount user) {
        EmailVerificationOtp current = otpRepository
            .findByUser(user)
            .orElse(null);

        if (current == null || current.isExpired(LocalDateTime.now())) {
            issueOtp(user, false);
        }
    }

    @Transactional
    public void resendOtp(String email) {
        UserAccount user = findUser(email);
        requirePending(user);
        issueOtp(user, true);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verify(String email, String rawOtp) {
        UserAccount user = findUser(email);
        requirePending(user);
        String otp = normalizeOtp(rawOtp);

        EmailVerificationOtp record = otpRepository.findByUser(user)
            .orElseThrow(() -> new IllegalArgumentException(
                "Chưa có OTP. Vui lòng bấm gửi lại OTP."
            ));

        if (record.isExpired(LocalDateTime.now())) {
            otpRepository.delete(record);
            throw new IllegalArgumentException(
                "OTP đã hết hạn. Vui lòng gửi lại OTP mới."
            );
        }

        if (!passwordEncoder.matches(otp, record.getOtpHash())) {
            int attempts = record.getFailedAttempts() + 1;
            if (attempts >= MAX_ATTEMPTS) {
                otpRepository.delete(record);
                throw new IllegalArgumentException(
                    "Bạn đã nhập sai quá nhiều lần. Hãy gửi OTP mới."
                );
            }
            record.setFailedAttempts(attempts);
            otpRepository.save(record);
            throw new IllegalArgumentException(
                "OTP không chính xác. Bạn còn "
                + (MAX_ATTEMPTS - attempts) + " lần thử."
            );
        }

        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        otpRepository.deleteAllByUser(user);
    }

    private void issueOtp(UserAccount user, boolean enforceCooldown) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationOtp record = otpRepository
            .findByUser(user)
            .orElse(null);

        if (record != null && enforceCooldown) {
            long elapsed = Duration.between(record.getSentAt(), now)
                .getSeconds();
            if (elapsed < RESEND_SECONDS) {
                throw new IllegalArgumentException(
                    "Vui lòng đợi " + (RESEND_SECONDS - elapsed)
                    + " giây trước khi gửi lại OTP."
                );
            }
        }

        if (record == null) {
            record = new EmailVerificationOtp();
            record.setUser(user);
        }

        String otp = generateOtp();
        record.setOtpHash(passwordEncoder.encode(otp));
        record.setExpiresAt(now.plusMinutes(EXPIRE_MINUTES));
        record.setSentAt(now);
        record.setFailedAttempts(0);
        otpRepository.saveAndFlush(record);
        sendMail(user, otp);
    }

    private void sendMail(UserAccount user, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(user.getEmail());
        message.setSubject("FAKEBOOK - OTP xác thực tài khoản");
        message.setText(
            "Xin chào " + user.getFullName() + ",\n\n"
            + "Mã OTP xác thực tài khoản FAKEBOOK là: " + otp + "\n\n"
            + "Mã có hiệu lực trong 5 phút."
        );
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Không thể gửi OTP xác thực", exception);
            throw new IllegalStateException(
                "Không thể gửi OTP. Hãy kiểm tra cấu hình Gmail."
            );
        }
    }

    private UserAccount findUser(String email) {
        String value = email == null
            ? ""
            : email.trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmailIgnoreCase(value)
            .orElseThrow(() -> new IllegalArgumentException(
                "Không tìm thấy tài khoản cần xác thực."
            ));
    }

    private void requirePending(UserAccount user) {
        if (user.getStatus() == AccountStatus.ACTIVE) {
            throw new IllegalArgumentException(
                "Tài khoản đã được xác thực."
            );
        }
        if (user.getStatus() == AccountStatus.LOCKED) {
            throw new IllegalArgumentException("Tài khoản đang bị khóa.");
        }
    }

    private String normalizeOtp(String otp) {
        String value = otp == null ? "" : otp.trim();
        if (!value.matches("\\d{6}")) {
            throw new IllegalArgumentException(
                "OTP phải gồm đúng 6 chữ số."
            );
        }
        return value;
    }

    private String generateOtp() {
        return String.format(
            Locale.ROOT,
            "%06d",
            secureRandom.nextInt(1_000_000)
        );
    }
}
