package vn.edu.hcmute.fakebook.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.hcmute.fakebook.entity.DirectMessage;

public interface DirectMessageRepository
        extends JpaRepository<DirectMessage, Long> {

    @EntityGraph(attributePaths = { "sender", "recipient" })
    @Query(
        value = """
            select m from DirectMessage m
            where (m.sender.id = :userAId and m.recipient.id = :userBId)
               or (m.sender.id = :userBId and m.recipient.id = :userAId)
            order by m.createdAt desc, m.id desc
            """,
        countQuery = """
            select count(m) from DirectMessage m
            where (m.sender.id = :userAId and m.recipient.id = :userBId)
               or (m.sender.id = :userBId and m.recipient.id = :userAId)
            """
    )
    Page<DirectMessage> findConversation(
        @Param("userAId") Long userAId,
        @Param("userBId") Long userBId,
        Pageable pageable
    );

    @Modifying
    @Query("""
        update DirectMessage m set m.read = true
        where m.sender.id = :senderId
          and m.recipient.id = :recipientId
          and m.read = false
        """)
    int markReadFromSender(
        @Param("senderId") Long senderId,
        @Param("recipientId") Long recipientId
    );

    @Query("""
        select max(m.id) from DirectMessage m
        where m.sender.id = :senderId
          and m.recipient.id = :recipientId
          and m.read = true
        """)
    Long findLastReadId(
        @Param("senderId") Long senderId,
        @Param("recipientId") Long recipientId
    );

    @Query("""
        select m.sender.id, count(m)
        from DirectMessage m
        where m.recipient.id = :recipientId and m.read = false
        group by m.sender.id
        """)
    List<Object[]> countUnreadBySender(
        @Param("recipientId") Long recipientId
    );
}