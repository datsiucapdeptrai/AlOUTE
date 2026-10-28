package vn.edu.hcmute.fakebook.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.hcmute.fakebook.entity.PasswordResetOtp;
import vn.edu.hcmute.fakebook.entity.UserAccount;

public interface PasswordResetOtpRepository
        extends JpaRepository<PasswordResetOtp, Long> {

    Optional<PasswordResetOtp> findByUser(UserAccount user);

    void deleteAllByUser(UserAccount user);
}
