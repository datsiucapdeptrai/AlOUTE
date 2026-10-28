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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.edu.hcmute.fakebook.enums.PostStatus;
import vn.edu.hcmute.fakebook.enums.PostVisibility;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "author_id",
        nullable = false
    )
    private UserAccount author;

    @Column(
        nullable = false,
        columnDefinition = "NVARCHAR(MAX)"
    )
    private String content;

    @Column(
        name = "image_url",
        length = 1000
    )
    private String imageUrl;

    @Column(length = 200)
    private String title;

    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    @Column(name = "video_poster_url", length = 1000)
    private String videoPosterUrl;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @Column(name = "share_count", nullable = false)
    private long shareCount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_category_id")
    private VideoCategory videoCategory;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 20
    )
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 20
    )
    private PostStatus status = PostStatus.ACTIVE;
    @ManyToOne
    @JoinColumn(name = "original_post_id")
    private Post originalPost; 
}
