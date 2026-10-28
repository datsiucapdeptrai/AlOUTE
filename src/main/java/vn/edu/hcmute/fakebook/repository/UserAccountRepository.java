package vn.edu.hcmute.fakebook.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.enums.AccountStatus;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsernameIgnoreCase(String username);
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // =====================================================
    // DÙNG CHO ADMIN (Lỗi ở ảnh 2)
    // =====================================================
    Page<UserAccount> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, 
            String fullName, 
            String email, 
            Pageable pageable
    );

    // =====================================================
    // DÙNG CHO TÌM KIẾM BẠN BÈ (Lỗi ở ảnh 1)
    // =====================================================
    @Query("""
        SELECT u FROM UserAccount u
        WHERE u.id != :currentUserId
          AND u.status = :status
          AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<UserAccount> searchActiveUsers(
            @Param("currentUserId") Long currentUserId,
            @Param("status") AccountStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // =====================================================
    // DÙNG CHO ADMIN DASHBOARD
    // =====================================================
    @Query("SELECT u FROM UserAccount u WHERE " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<UserAccount> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    // =====================================================
    // DÙNG CHO GỢI Ý KẾT BẠN (Đã thêm bộ lọc Admin/Manager)
    // =====================================================
    @Query("""
        SELECT u FROM UserAccount u
        WHERE u.id != :currentUserId
          AND u.status = 'ACTIVE'
          AND NOT EXISTS (
              SELECT 1 FROM u.roles r 
              WHERE r.name IN ('ROLE_ADMIN', 'ROLE_MANAGER')
          )
          AND NOT EXISTS (
              SELECT 1 FROM Friendship f
              WHERE (f.userOne.id = :currentUserId AND f.userTwo.id = u.id)
                 OR (f.userTwo.id = :currentUserId AND f.userOne.id = u.id)
          )
        ORDER BY u.fullName
    """)
    Page<UserAccount> findFriendSuggestions(@Param("currentUserId") Long currentUserId, Pageable pageable);
}