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
import vn.edu.hcmute.fakebook.enums.NotificationType;
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
    private final VideoCategoryRepository videoCategoryRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;
    
    // ĐÃ ĐỔI SANG CLOUDINARY STORAGE SERVICE
    private final CloudinaryStorageService cloudinaryStorageService; 

    @Transactional
    public void createPost(String username, CreatePostRequest request) {
        UserAccount author = findUser(username);

        boolean hasContent = request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasImage = request.getImage() != null && !request.getImage().isEmpty();
        boolean hasVideo = request.getVideo() != null && !request.getVideo().isEmpty();

        if (!hasContent && !hasImage && !hasVideo) {
            throw new IllegalArgumentException("Hãy nhập nội dung hoặc chọn ảnh/video để đăng");
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
                .orElseThrow(() -> new IllegalArgumentException("Thể loại video không tồn tại"));
        }

        PostVisibility visibility = request.getVisibility() == null 
                ? PostVisibility.PUBLIC 
                : request.getVisibility();

        // ========================================================
        // UPLOAD LÊN CLOUDINARY THAY VÌ LOCAL
        // ========================================================
        String imageUrl = null;
        if (hasImage) {
            imageUrl = cloudinaryStorageService.uploadFile(request.getImage());
        }
        
        String videoUrl = null;
        if (hasVideo) {
            videoUrl = cloudinaryStorageService.uploadFile(request.getVideo());
        }
        
        String videoPosterUrl = null;
        if (hasVideo && request.getVideoPoster() != null && !request.getVideoPoster().isEmpty()) {
            videoPosterUrl = cloudinaryStorageService.uploadFile(request.getVideoPoster());
        }

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
    public Page<Post> getVideoPosts(String username, Long categoryId, int page) {
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

    // Hàm này đã được bạn viết cực chuẩn để chặn xem lậu bài viết
    @Transactional(readOnly = true)
    public Post getVisiblePost(Long postId, String username) {
        UserAccount currentUser = findUser(username);
        Post post = postRepository.findPostDetailsById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));
        
        checkCanView(currentUser, post);
        return post;
    }

    // Logic kiểm tra quyền xem (Chống IDOR)
    public void checkCanView(UserAccount currentUser, Post post) {
        if (post.getStatus() != PostStatus.ACTIVE) {
            throw new IllegalArgumentException("Bài viết không còn tồn tại");
        }
        
        // 1. Nếu là chủ bài viết hoặc bài PUBLIC -> Cho phép xem
        if (post.getAuthor().getId().equals(currentUser.getId())
                || post.getVisibility() == PostVisibility.PUBLIC) {
            return;
        }
        
        // 2. Nếu là bài PRIVATE -> Chặn
        if (post.getVisibility() == PostVisibility.PRIVATE) {
            throw new AccessDeniedException("Bạn không có quyền xem bài viết này");
        }

        // 3. Nếu là bài FRIENDS -> Kiểm tra xem có phải bạn bè không
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
    public Page<Post> getFeedPosts(String username, int page) {
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
    public void deletePost(Long postId, String username) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết"));

        boolean isAuthor = post.getAuthor().getUsername().equalsIgnoreCase(username);

        if (!isAuthor) {
            throw new AccessDeniedException("Bạn không có quyền xóa bài viết này");
        }

        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
    }

    private UserAccount findUser(String username) {
        return userAccountRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng"));
    }

    @Transactional(readOnly = true)
    public Page<Post> searchFeedPosts(String username, String keyword, String type, String sort, int page) {
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
    
    @Transactional
    public Post sharePost(Long originalPostId, String username, String caption) {
        UserAccount currentUser = findUser(username);
        
        Post originalPost = postRepository.findById(originalPostId)
                .orElseThrow(() -> new IllegalArgumentException("Bài viết gốc không tồn tại"));

        // 1. Tạo bài viết mới (bài share)
        Post sharedPost = new Post();
        sharedPost.setAuthor(currentUser);
        sharedPost.setContent(caption == null ? "" : caption.trim());
        sharedPost.setVisibility(PostVisibility.PUBLIC); // Thường share là public
        sharedPost.setStatus(PostStatus.ACTIVE);
        sharedPost.setOriginalPost(originalPost); // Trỏ đến bài gốc
        
        // 2. Tăng lượt share của bài gốc lên 1
        originalPost.setShareCount(originalPost.getShareCount() + 1);
        postRepository.save(originalPost);

        // 3. Lưu bài share
        return postRepository.save(sharedPost);
    }
}