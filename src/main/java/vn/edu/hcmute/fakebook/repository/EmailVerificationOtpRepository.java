package vn.edu.hcmute.fakebook.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.hcmute.fakebook.entity.EmailVerificationOtp;
import vn.edu.hcmute.fakebook.entity.UserAccount;

public interface EmailVerificationOtpRepository
        extends JpaRepository<EmailVerificationOtp, Long> {

    Optional<EmailVerificationOtp> findByUser(UserAccount user);

    void deleteAllByUser(UserAccount user);
}
