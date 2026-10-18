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
    private final EmailVerificationService verificationService;

    @Transactional
    public UserAccount register(RegisterRequest request) {
        String username = request.getUsername().trim()
            .toLowerCase(Locale.ROOT);
        String email = request.getEmail().trim()
            .toLowerCase(Locale.ROOT);

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException(
                "Mật khẩu nhập lại không khớp"
            );
        }

        Optional<UserAccount> byUsername = userRepository
            .findByUsernameIgnoreCase(username);
        Optional<UserAccount> byEmail = userRepository
            .findByEmailIgnoreCase(email);

        if (byUsername.isPresent() || byEmail.isPresent()) {
            return recoverPendingRegistration(
                request,
                username,
                email,
                byUsername,
                byEmail
            );
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
            .orElseThrow(() ->
                new IllegalStateException("Chưa có ROLE_USER")
            );

        UserAccount user = new UserAccount();
        user.setFullName(request.getFullName().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(
            passwordEncoder.encode(request.getPassword())
        );
        user.setStatus(AccountStatus.DISABLED);
        user.addRole(userRole);
        user.setProfile(new UserProfile());

        UserAccount saved = userRepository.saveAndFlush(user);
        verificationService.sendInitialOtp(saved);
        return saved;
    }

    private UserAccount recoverPendingRegistration(
            RegisterRequest request,
            String username,
            String email,
            Optional<UserAccount> byUsername,
            Optional<UserAccount> byEmail) {

        if (byUsername.isEmpty() || byEmail.isEmpty()
                || !byUsername.get().getId().equals(byEmail.get().getId())) {
            if (byUsername.isPresent()) {
                throw new IllegalArgumentException(
                    "Tên tài khoản đã tồn tại"
                );
            }
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        UserAccount existing = byUsername.get();
        if (existing.getStatus() == AccountStatus.ACTIVE) {
            throw new IllegalArgumentException(
                "Tên tài khoản hoặc email đã được sử dụng"
            );
        }
        if (existing.getStatus() == AccountStatus.LOCKED) {
            throw new IllegalArgumentException("Tài khoản đang bị khóa");
        }

        existing.setUsername(username);
        existing.setEmail(email);
        existing.setFullName(request.getFullName().trim());
        existing.setPasswordHash(
            passwordEncoder.encode(request.getPassword())
        );
        UserAccount saved = userRepository.saveAndFlush(existing);
        verificationService.ensureOtpForPendingUser(saved);
        return saved;
    }
}
