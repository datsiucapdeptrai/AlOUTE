package vn.edu.hcmute.fakebook.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.enums.AccountStatus;
import vn.edu.hcmute.fakebook.enums.RoleName;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsernameIgnoreCase(
        String username
    );

    Optional<UserAccount> findByEmailIgnoreCase(
        String email
    );

    boolean existsByUsernameIgnoreCase(
        String username
    );

    boolean existsByEmailIgnoreCase(
        String email
    );

    @Query("""
        SELECT u
        FROM UserAccount u
        WHERE u.id <> :currentUserId
          AND u.status = :status
          AND (
                LOWER(u.username) LIKE
                    LOWER(CONCAT('%', :keyword, '%'))
                OR
                LOWER(u.fullName) LIKE
                    LOWER(CONCAT('%', :keyword, '%'))
              )
        ORDER BY u.fullName ASC
        """)
    List<UserAccount> searchActiveUsers(
        @Param("currentUserId")
        Long currentUserId,

        @Param("status")
        AccountStatus status,

        @Param("keyword")
        String keyword,

        Pageable pageable
    );

    Page<UserAccount>
        findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username,
            String fullName,
            String email,
            Pageable pageable
        );

    @Query("SELECT u FROM UserAccount u JOIN u.roles r WHERE r.name = :roleName")
    Page<UserAccount> findByRoleName(
        @Param("roleName") RoleName roleName,
        Pageable pageable
    );
}