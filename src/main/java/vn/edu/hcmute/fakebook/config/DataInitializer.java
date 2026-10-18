package vn.edu.hcmute.fakebook.config;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.entity.Role;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.entity.VideoCategory;
import vn.edu.hcmute.fakebook.enums.AccountStatus;
import vn.edu.hcmute.fakebook.enums.RoleName;
import vn.edu.hcmute.fakebook.repository.RoleRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;
import vn.edu.hcmute.fakebook.repository.VideoCategoryRepository;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final PasswordEncoder passwordEncoder;
    private final UserAccountRepository userAccountRepository;

    // =====================================================
    // 1. TẠO ROLE MẶC ĐỊNH
    // =====================================================
    @Bean
    CommandLineRunner initializeRoles(RoleRepository roleRepository) {
        return args -> {
            Arrays.stream(RoleName.values()).forEach(roleName -> {
                if (roleRepository.findByName(roleName).isEmpty()) {
                    Role role = new Role();
                    role.setName(roleName);
                    roleRepository.save(role);
                }
            });
        };
    }

    // =====================================================
    // 2. TẠO VIDEO CATEGORY MẶC ĐỊNH
    // =====================================================
    @Bean
    CommandLineRunner initializeVideoCategories(
            VideoCategoryRepository categoryRepository) {
        return args -> Arrays.asList(
                "Giải trí", "Âm nhạc", "Thể thao", "Giáo dục", "Khác"
            ).forEach(name -> {
                if (!categoryRepository.existsByNameIgnoreCase(name)) {
                    VideoCategory category = new VideoCategory();
                    category.setName(name);
                    categoryRepository.save(category);
                }
            });
    }

    // =====================================================
    // 3. TẠO TÀI KHOẢN ADMIN + MANAGER MẪU
    // =====================================================
    @Bean
    CommandLineRunner initializeAccounts(RoleRepository roleRepository) {
        return args -> {

            // --- Tạo ADMIN mẫu ---
            if (userAccountRepository.findByUsernameIgnoreCase("admin").isEmpty()) {
                UserAccount admin = new UserAccount();
                admin.setUsername("admin");
                admin.setEmail("admin@fakebook.com");
                admin.setFullName("Quản trị viên");
                admin.setPasswordHash(passwordEncoder.encode("123456"));
                admin.setStatus(AccountStatus.ACTIVE);

                roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .ifPresent(admin::addRole);

                userAccountRepository.save(admin);
                System.out.println(">>> Đã tạo tài khoản ADMIN: admin / 123456");
            }

            // --- Tạo MANAGER mẫu ---
            if (userAccountRepository.findByUsernameIgnoreCase("manager1").isEmpty()) {
                UserAccount manager = new UserAccount();
                manager.setUsername("manager1");
                manager.setEmail("manager1@fakebook.com");
                manager.setFullName("Quản lý nội dung");
                manager.setPasswordHash(passwordEncoder.encode("123456"));
                manager.setStatus(AccountStatus.ACTIVE);

                roleRepository.findByName(RoleName.ROLE_MANAGER)
                    .ifPresent(manager::addRole);

                userAccountRepository.save(manager);
                System.out.println(">>> Đã tạo tài khoản MANAGER: manager1 / 123456");
            }
        };
    }
}