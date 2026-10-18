package vn.edu.hcmute.fakebook.repository;

import java.util.List;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.hcmute.fakebook.entity.Friendship;
import vn.edu.hcmute.fakebook.enums.FriendshipStatus;

import org.springframework.data.domain.Pageable;


import vn.edu.hcmute.fakebook.entity.UserAccount;

public interface FriendshipRepository
        extends JpaRepository<Friendship, Long> {

    @EntityGraph(
        attributePaths = {
            "userOne",
            "userTwo",
            "requestedBy"
        }
    )
    Optional<Friendship> findByUserOneIdAndUserTwoId(
        Long userOneId,
        Long userTwoId
    );

    /*
     * Danh sách tất cả quan hệ của một người
     * theo trạng thái.
     */
    @EntityGraph(
        attributePaths = {
            "userOne",
            "userTwo",
            "requestedBy"
        }
    )
    @Query("""
        SELECT f
        FROM Friendship f
        WHERE f.status = :status
          AND (
                f.userOne.id = :userId
                OR f.userTwo.id = :userId
              )
        ORDER BY f.updatedAt DESC
        """)
    List<Friendship> findByUserAndStatus(
        @Param("userId") Long userId,
        @Param("status") FriendshipStatus status
    );

    /*
     * Những lời mời mà người dùng hiện tại nhận được.
     */
    @EntityGraph(
        attributePaths = {
            "userOne",
            "userTwo",
            "requestedBy"
        }
    )
    @Query("""
        SELECT f
        FROM Friendship f
        WHERE f.status = :status
          AND f.requestedBy.id <> :userId
          AND (
                f.userOne.id = :userId
                OR f.userTwo.id = :userId
              )
        ORDER BY f.createdAt DESC
        """)
    List<Friendship> findReceivedRequests(
        @Param("userId") Long userId,
        @Param("status") FriendshipStatus status
    );

    /*
     * Những lời mời do người dùng hiện tại đã gửi.
     */
    @EntityGraph(
        attributePaths = {
            "userOne",
            "userTwo",
            "requestedBy"
        }
    )
    @Query("""
        SELECT f
        FROM Friendship f
        WHERE f.status = :status
          AND f.requestedBy.id = :userId
        ORDER BY f.createdAt DESC
        """)
    List<Friendship> findSentRequests(
        @Param("userId") Long userId,
        @Param("status") FriendshipStatus status
    );
    @Query("""
    	    SELECT COUNT(f.id)
    	    FROM Friendship f
    	    WHERE (f.userOne.id = :userId OR f.userTwo.id = :userId)
    	      AND f.status = 'ACCEPTED'
    	    """)
    	long countFriends(@Param("userId") Long userId);
    @Query("""
    	    SELECT u FROM UserAccount u
    	    WHERE u.id <> :currentUserId
    	      AND u.status = vn.edu.hcmute.fakebook.enums.AccountStatus.ACTIVE
    	      AND NOT EXISTS (
    	          SELECT 1 FROM Friendship f
    	          WHERE (f.userOne.id = :currentUserId AND f.userTwo.id = u.id)
    	             OR (f.userTwo.id = :currentUserId AND f.userOne.id = u.id)
    	      )
    	    ORDER BY (
    	        SELECT COUNT(f2) FROM Friendship f2
    	        WHERE f2.status = vn.edu.hcmute.fakebook.enums.FriendshipStatus.ACCEPTED
    	          AND (
    	              (f2.userOne.id = u.id AND f2.userTwo.id IN :friendIds)
    	              OR (f2.userTwo.id = u.id AND f2.userOne.id IN :friendIds)
    	          )
    	    ) DESC, u.fullName ASC
    	    """)
    	List<UserAccount> findFriendSuggestions(
    	    @Param("currentUserId") Long currentUserId,
    	    @Param("friendIds") List<Long> friendIds,
    	    Pageable pageable
    	);
    @Query("""
    	    SELECT CASE 
    	        WHEN f.userOne.id = :userId THEN f.userTwo.id
    	        ELSE f.userOne.id
    	    END
    	    FROM Friendship f
    	    WHERE f.status = vn.edu.hcmute.fakebook.enums.FriendshipStatus.ACCEPTED
    	      AND (f.userOne.id = :userId OR f.userTwo.id = :userId)
    	    """)
    	List<Long> findAcceptedFriendIds(@Param("userId") Long userId);
}