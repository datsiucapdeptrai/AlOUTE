package vn.edu.hcmute.fakebook.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.hcmute.fakebook.entity.Notification;
import vn.edu.hcmute.fakebook.entity.UserAccount;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = "actor")
    Page<Notification> findByRecipientOrderByCreatedAtDesc(
        UserAccount recipient,
        Pageable pageable
    );

    List<Notification> findByRecipientAndReadFalse(
        UserAccount recipient
    );

    long countByRecipientAndReadFalse(
        UserAccount recipient
    );
}