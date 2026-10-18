package vn.edu.hcmute.fakebook.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.hcmute.fakebook.entity.PasswordResetToken;
import vn.edu.hcmute.fakebook.entity.UserAccount;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteAllByUser(UserAccount user);
}
