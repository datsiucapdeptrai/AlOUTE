package vn.edu.hcmute.fakebook.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.CreatePostRequest;
import vn.edu.hcmute.fakebook.entity.Post;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.entity.VideoCategory;
import vn.edu.hcmute.fakebook.enums.FriendshipStatus;
import vn.edu.hcmute.fakebook.enums.PostStatus;
import vn.edu.hcmute.fakebook.enums.PostVisibility;
import vn.edu.hcmute.fakebook.repository.PostRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;
import vn.edu.hcmute.fakebook.repository.FriendshipRepository;
import vn.edu.hcmute.fakebook.repository.VideoCategoryRepository;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int PAGE_SIZE = 10;

    private final PostRepository postRepository;
    private final UserAccountRepository userAccountRepository;
    private final FileStorageService fileStorageService;
    private final VideoCategoryRepository videoCategoryRepository;
    private final FriendshipRepository friendshipRepository;

    @Transactional
    public void createPost(
            String username,
            CreatePostRequest request) {

        UserAccount author = findUser(username);

        boolean hasContent = request.getContent() != null
            && !request.getContent().trim().isEmpty();
        boolean hasImage = request.getImage() != null
            && !request.getImage().isEmpty();
        boolean hasVideo = request.getVideo() != null
            && !request.getVideo().isEmpty();

        if (!hasContent && !hasImage && !hasVideo) {
            throw new IllegalArgumentException(
                "Hãy nhập nội dung hoặc chọn ảnh/video để đăng"
            );
        }

        VideoCategory category = null;
        if (hasVideo) {
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập tiêu đề video");
            }
            if (request.getVideoCategoryId() == null) {
                throw new IllegalArgumentException("Vui lòng chọn thể loại video");
            }
            category = videoCategoryRepository
                .findByIdAndActiveTrue(request.getVideoCategoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Thể loại video không tồn tại"
                ));
        }

        PostVisibility visibility =
            request.getVisibility() == null
                ? PostVisibility.PUBLIC
                : request.getVisibility();

        String imageUrl =
            fileStorageService.storePostImage(
                request.getImage()
            );

        String videoUrl = fileStorageService.storePostVideo(request.getVideo());
        String videoPosterUrl = hasVideo
            ? fileStorageService.storeVideoPoster(request.getVideoPoster())
            : null;

        Post post = new Post();
        post.setAuthor(author);
        post.setContent(hasContent ? request.getContent().trim() : "");
        post.setVisibility(visibility);
        post.setStatus(PostStatus.ACTIVE);
        post.setImageUrl(imageUrl);
        post.setTitle(hasVideo ? request.getTitle().trim() : null);
        post.setVideoUrl(videoUrl);
        post.setVideoPosterUrl(videoPosterUrl);
        post.setVideoCategory(category);

        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public Page<Post> getVideoPosts(
            String username,
            Long categoryId,
            int page) {
        UserAccount currentUser = findUser(username);
        return postRepository.findVisibleVideoPosts(
            currentUser.getId(),
            categoryId,
            PostStatus.ACTIVE,
            PostVisibility.PUBLIC,
            PostVisibility.FRIENDS,
            FriendshipStatus.ACCEPTED,
            PageRequest.of(Math.max(page, 0), 3)
        );
    }

    @Transactional(readOnly = true)
    public Post getVisiblePost(Long postId, String username) {
        UserAccount currentUser = findUser(username);
        Post post = postRepository.findPostDetailsById(postId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Không tìm thấy bài viết"
            ));
        checkCanView(currentUser, post);
        return post;
    }

    public void checkCanView(UserAccount currentUser, Post post) {
        if (post.getStatus() != PostStatus.ACTIVE) {
            throw new IllegalArgumentException("Bài viết không còn tồn tại");
        }
        if (post.getAuthor().getId().equals(currentUser.getId())
                || post.getVisibility() == PostVisibility.PUBLIC) {
            return;
        }
        if (post.getVisibility() == PostVisibility.PRIVATE) {
            throw new AccessDeniedException("Bạn không có quyền xem bài viết này");
        }

        Long firstId = Math.min(currentUser.getId(), post.getAuthor().getId());
        Long secondId = Math.max(currentUser.getId(), post.getAuthor().getId());
        boolean isFriend = friendshipRepository
            .findByUserOneIdAndUserTwoId(firstId, secondId)
            .filter(friendship -> friendship.getStatus() == FriendshipStatus.ACCEPTED)
            .isPresent();
        if (!isFriend) {
            throw new AccessDeniedException("Chỉ bạn bè mới được xem bài viết này");
        }
    }

    @Transactional(readOnly = true)
    public Page<Post> getFeedPosts(
            String username,
            int page) {

        UserAccount currentUser = findUser(username);
        int validPage = Math.max(page, 0);

        return postRepository.findVisiblePosts(
            currentUser.getId(),
            PostStatus.ACTIVE,
            PostVisibility.PUBLIC,
            PostVisibility.FRIENDS,
            FriendshipStatus.ACCEPTED,
            PageRequest.of(validPage, PAGE_SIZE)
        );
    }

    @Transactional
    public void deletePost(
            Long postId,
            String username) {

        Post post = postRepository
            .findById(postId)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Không tìm thấy bài viết"
                )
            );

        boolean isAuthor = post.getAuthor()
            .getUsername()
            .equalsIgnoreCase(username);

        if (!isAuthor) {
            throw new AccessDeniedException(
                "Bạn không có quyền xóa bài viết này"
            );
        }

        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
    }

    private UserAccount findUser(String username) {
        return userAccountRepository
            .findByUsernameIgnoreCase(username)
            .orElseThrow(() ->
                new IllegalStateException(
                    "Không tìm thấy người dùng"
                )
            );
    }
    @Transactional(readOnly = true)
    public Page<Post> searchFeedPosts(
            String username,
            String keyword,
            String type,
            String sort,
            int page) {

        UserAccount currentUser = findUser(username);
        int validPage = Math.max(page, 0);

        String cleanKeyword = keyword == null ? "" : keyword.trim();
        String cleanType = (type == null || type.isBlank()) ? "ALL" : type.toUpperCase();
        String cleanSort = (sort == null || sort.isBlank()) ? "newest" : sort.toLowerCase();

        PageRequest pageable = PageRequest.of(validPage, PAGE_SIZE);

        if ("popular".equals(cleanSort)) {
            return postRepository.searchFeedPostsByPopular(
                currentUser.getId(),
                cleanKeyword,
                cleanType,
                PostStatus.ACTIVE,
                PostVisibility.PUBLIC,
                PostVisibility.FRIENDS,
                FriendshipStatus.ACCEPTED,
                pageable
            );
        }

        return postRepository.searchFeedPostsByNewest(
            currentUser.getId(),
            cleanKeyword,
            cleanType,
            PostStatus.ACTIVE,
            PostVisibility.PUBLIC,
            PostVisibility.FRIENDS,
            FriendshipStatus.ACCEPTED,
            pageable
        );
    }
}
