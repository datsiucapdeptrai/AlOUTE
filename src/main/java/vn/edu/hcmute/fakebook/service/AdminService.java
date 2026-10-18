package vn.edu.hcmute.fakebook.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.AdminDashboardDto;
import vn.edu.hcmute.fakebook.entity.Comment;
import vn.edu.hcmute.fakebook.entity.Post;
import vn.edu.hcmute.fakebook.entity.Role;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.enums.AccountStatus;
import vn.edu.hcmute.fakebook.enums.CommentStatus;
import vn.edu.hcmute.fakebook.enums.PostStatus;
import vn.edu.hcmute.fakebook.enums.RoleName;
import vn.edu.hcmute.fakebook.repository.CommentRepository;
import vn.edu.hcmute.fakebook.repository.PostRepository;
import vn.edu.hcmute.fakebook.repository.RoleRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserAccountRepository userAccountRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RoleRepository roleRepository;

    // =====================================================
    // DASHBOARD - THỐNG KÊ
    // =====================================================
    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboardStatistics() {

        long totalUsers = userAccountRepository.count();

        long totalPosts = postRepository.countByStatus(PostStatus.ACTIVE);

        long totalVideos = postRepository
            .countByVideoUrlIsNotNullAndStatus(PostStatus.ACTIVE);

        long totalComments = commentRepository.countByStatus(CommentStatus.ACTIVE);

        long totalViews = postRepository.sumViewCountByStatus(PostStatus.ACTIVE);

        long totalShares = postRepository.sumShareCountByStatus(PostStatus.ACTIVE);

        return new AdminDashboardDto(
            totalUsers,
            totalPosts,
            totalVideos,
            totalComments,
            totalViews,
            totalShares
        );
    }

    // =====================================================
    // QUẢN LÝ USER
    // =====================================================
    @Transactional(readOnly = true)
    public Page<UserAccount> getUsers(String keyword, int page, int size) {

        String searchKeyword = keyword == null ? "" : keyword.trim();
        Pageable pageable = PageRequest.of(page, size);

        return userAccountRepository
            .findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                searchKeyword,
                searchKeyword,
                searchKeyword,
                pageable
            );
    }

    @Transactional
    public void toggleUserStatus(Long userId, String currentUsername) {

        UserAccount user = userAccountRepository
            .findById(userId)
            .orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy tài khoản")
            );

        if (user.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new IllegalArgumentException(
                "Bạn không thể khóa tài khoản đang đăng nhập"
            );
        }

        if (user.getStatus() == AccountStatus.LOCKED) {
            user.setStatus(AccountStatus.ACTIVE);
        } else {
            user.setStatus(AccountStatus.LOCKED);
        }

        userAccountRepository.save(user);
    }

    // =====================================================
    // ĐỔI ROLE USER (CHỈ GIỮ LẠI 1 METHOD NÀY)
    // =====================================================
    @Transactional
    public void changeUserRole(
            Long userId,
            String roleName,
            String currentUsername) {

        UserAccount user = userAccountRepository
            .findById(userId)
            .orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy tài khoản")
            );

        if (user.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new IllegalArgumentException(
                "Bạn không thể đổi role của chính mình"
            );
        }

        // Chuẩn hóa input: tự động thêm ROLE_ nếu chưa có
        String normalized = roleName == null ? "" : roleName.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        RoleName newRole;
        try {
            newRole = RoleName.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role không hợp lệ: " + roleName);
        }

        // Xóa hết role cũ, chỉ giữ 1 role mới
        user.getRoles().clear();

        Role role = roleRepository.findByName(newRole)
            .orElseThrow(() ->
                new IllegalArgumentException("Role không tồn tại trong DB")
            );

        user.addRole(role);
        userAccountRepository.save(user);
    }

    // =====================================================
    // QUẢN LÝ BÀI VIẾT
    // =====================================================
    @Transactional(readOnly = true)
    public Page<Post> getPosts(String keyword, int page, int size) {

        String searchKeyword = keyword == null ? "" : keyword.trim();
        return postRepository.searchForAdmin(
            searchKeyword,
            PageRequest.of(page, size)
        );
    }

    @Transactional
    public void deletePost(Long postId) {

        Post post = postRepository
            .findById(postId)
            .orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy bài viết")
            );

        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
    }

    @Transactional
    public void restorePost(Long postId) {

        Post post = postRepository
            .findById(postId)
            .orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy bài viết")
            );

        post.setStatus(PostStatus.ACTIVE);
        postRepository.save(post);
    }

    // =====================================================
    // QUẢN LÝ BÌNH LUẬN
    // =====================================================
    @Transactional(readOnly = true)
    public Page<Comment> getComments(String keyword, int page, int size) {

        String searchKeyword = keyword == null ? "" : keyword.trim();
        return commentRepository.searchForAdmin(
            searchKeyword,
            PageRequest.of(page, size)
        );
    }

    @Transactional
    public void deleteComment(Long commentId) {

        Comment comment = commentRepository
            .findById(commentId)
            .orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy bình luận")
            );

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
    }

    @Transactional
    public void restoreComment(Long commentId) {

        Comment comment = commentRepository
            .findById(commentId)
            .orElseThrow(() ->
                new IllegalArgumentException("Không tìm thấy bình luận")
            );

        comment.setStatus(CommentStatus.ACTIVE);
        commentRepository.save(comment);
    }

    // =====================================================
    // QUẢN LÝ VIDEO
    // =====================================================
    @Transactional(readOnly = true)
    public Page<Post> getVideos(int page, int size) {

        return postRepository.findByVideoUrlIsNotNullAndStatus(
            PostStatus.ACTIVE,
            PageRequest.of(page, size)
        );
    }
}