package vn.edu.hcmute.fakebook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.edu.hcmute.fakebook.enums.FriendshipStatus;

@Entity
@Table(
    name = "friendships",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_friendship_user_pair",
            columnNames = {
                "user_one_id",
                "user_two_id"
            }
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Friendship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    /*
     * Người có ID nhỏ hơn luôn được lưu ở userOne.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "user_one_id",
        nullable = false
    )
    private UserAccount userOne;

    /*
     * Người có ID lớn hơn luôn được lưu ở userTwo.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "user_two_id",
        nullable = false
    )
    private UserAccount userTwo;

    /*
     * Người đã gửi lời mời kết bạn.
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "requested_by_id",
        nullable = false
    )
    private UserAccount requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 20
    )
    private FriendshipStatus status =
        FriendshipStatus.PENDING;
}