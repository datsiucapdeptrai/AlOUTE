package vn.edu.hcmute.fakebook.entity;

import jakarta.persistence.*;
import lombok.*;
import vn.edu.hcmute.fakebook.enums.ReactionType;

@Entity
@Table(name = "post_likes")
@Getter
@Setter
@NoArgsConstructor
public class PostLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    // ✅ THÊM FIELD NÀY
    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", length = 20)
    private ReactionType reactionType = ReactionType.LIKE;
}