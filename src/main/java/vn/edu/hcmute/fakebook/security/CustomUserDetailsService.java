package vn.edu.hcmute.fakebook.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login) {
        String value = login == null ? "" : login.trim();
        UserAccount user = userRepository.findByUsernameIgnoreCase(value)
            .or(() -> userRepository.findByEmailIgnoreCase(value))
            .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));

        // Trả về CustomUserDetails thay vì User mặc định
        return new CustomUserDetails(user);
    }
}