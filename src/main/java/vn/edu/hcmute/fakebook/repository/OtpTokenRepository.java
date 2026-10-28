package vn.edu.hcmute.fakebook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hcmute.fakebook.entity.OtpToken;
import vn.edu.hcmute.fakebook.enums.OtpType;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    // Hàm xóa OTP cũ trước khi tạo OTP mới
    @Modifying
    @Transactional
    void deleteByEmailAndType(String email, OtpType type);

    // Hàm tìm OTP hợp lệ (chưa dùng và chưa hết hạn)
    @Query("SELECT o FROM OtpToken o WHERE o.email = :email AND o.code = :code AND o.type = :type AND o.used = false AND o.expiredAt > :now")
    Optional<OtpToken> findValidOtp(@Param("email") String email,
                                    @Param("code") String code,
                                    @Param("type") OtpType type,
                                    @Param("now") LocalDateTime now);
}