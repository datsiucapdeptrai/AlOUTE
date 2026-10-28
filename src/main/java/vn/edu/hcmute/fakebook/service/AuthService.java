package vn.edu.hcmute.fakebook.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.RegisterRequest;
import vn.edu.hcmute.fakebook.entity.Role;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.entity.UserProfile;
import vn.edu.hcmute.fakebook.enums.AccountStatus;
import vn.edu.hcmute.fakebook.enums.RoleName;
import vn.edu.hcmute.fakebook.repository.RoleRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ==========================================
    // CÁC HÀM MỚI THÊM (Sửa theo đúng Entity của bạn)
    // ==========================================

    // 1. Kiểm tra email tồn tại
    public boolean isEmailExists(String email) {
        return userRepository.findByEmailIgnoreCase(email).isPresent();
    }

    // 2. Kiểm tra username tồn tại
    public boolean isUsernameExists(String username) {
        return userRepository.findByUsernameIgnoreCase(username).isPresent();
    }

    // 3. Kích hoạt tài khoản sau khi nhập đúng OTP (Đổi setEnabled thành setStatus)
    @Transactional
    public void activateUser(String email) {
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
    }

    // 4. Cập nhật mật khẩu mới (Đổi setPassword thành setPasswordHash)
    @Transactional
    public void updatePassword(String email, String newPassword) {
        UserAccount user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ==========================================
    // HÀM ĐĂNG KÝ (Dựa trên code gốc của bạn, bỏ gọi OTP tự động)
    // ==========================================
    @Transactional
    public UserAccount registerPendingUser(RegisterRequest request) {
        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu nhập lại không khớp");
        }

        Optional<UserAccount> byUsername = userRepository.findByUsernameIgnoreCase(username);
        Optional<UserAccount> byEmail = userRepository.findByEmailIgnoreCase(email);

        if (byUsername.isPresent() || byEmail.isPresent()) {
            // Logic xử lý đăng ký lại (Recover Pending) của bạn
            if (byUsername.isEmpty() || byEmail.isEmpty()
                    || !byUsername.get().getId().equals(byEmail.get().getId())) {
                if (byUsername.isPresent()) {
                    throw new IllegalArgumentException("Tên tài khoản đã tồn tại");
                }
                throw new IllegalArgumentException("Email đã được sử dụng");
            }

            UserAccount existing = byUsername.get();
            if (existing.getStatus() == AccountStatus.ACTIVE) {
                throw new IllegalArgumentException("Tên tài khoản hoặc email đã được sử dụng");
            }
            if (existing.getStatus() == AccountStatus.LOCKED) {
                throw new IllegalArgumentException("Tài khoản đang bị khóa");
            }

            // Cập nhật lại thông tin nếu đang chờ kích hoạt
            existing.setUsername(username);
            existing.setEmail(email);
            existing.setFullName(request.getFullName().trim());
            existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            return userRepository.saveAndFlush(existing);
        }

        // Tạo user mới
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("Chưa có ROLE_USER"));

        UserAccount user = new UserAccount();
        user.setFullName(request.getFullName().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(AccountStatus.DISABLED); // Đang chờ kích hoạt
        user.addRole(userRole);
        user.setProfile(new UserProfile());

        return userRepository.saveAndFlush(user);
    }
}