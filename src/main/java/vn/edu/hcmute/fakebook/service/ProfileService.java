package vn.edu.hcmute.fakebook.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.ProfileUpdateRequest;
import vn.edu.hcmute.fakebook.entity.Post;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.entity.UserProfile;
import vn.edu.hcmute.fakebook.repository.FriendshipRepository;
import vn.edu.hcmute.fakebook.repository.PostLikeRepository;
import vn.edu.hcmute.fakebook.repository.PostRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final int POSTS_PER_PAGE = 10;

    private final UserAccountRepository userAccountRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final FriendshipRepository friendshipRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // =====================================================
    // LẤY DỮ LIỆU PROFILE
    // =====================================================

    @Transactional(readOnly = true)
    public UserAccount getUserProfile(String username) {
        return findUser(username);
    }

    @Transactional(readOnly = true)
    public ProfileUpdateRequest getProfileForm(String username) {
        UserAccount user = findUser(username);
        UserProfile profile = user.getProfile();

        ProfileUpdateRequest form = new ProfileUpdateRequest();
        form.setFullName(user.getFullName());

        if (profile != null) {
            form.setBio(profile.getBio());
            form.setPhone(profile.getPhone());
            form.setDateOfBirth(profile.getDateOfBirth());
            form.setLocation(profile.getLocation());
        }

        return form;
    }

    // =====================================================
    // THỐNG KÊ PROFILE
    // =====================================================

    @Transactional(readOnly = true)
    public Map<String, Long> getProfileStats(String username) {
        UserAccount user = findUser(username);
        Long userId = user.getId();

        long postCount = postRepository.countByAuthorId(userId);
        long likeCount = postLikeRepository.countLikesByAuthorId(userId);
        long friendCount = friendshipRepository.countFriends(userId);

        Map<String, Long> stats = new HashMap<>();
        stats.put("postCount", postCount);
        stats.put("likeCount", likeCount);
        stats.put("friendCount", friendCount);
        return stats;
    }

    // =====================================================
    // LẤY BÀI VIẾT CỦA USER
    // =====================================================

    @Transactional(readOnly = true)
    public Page<Post> getUserPosts(String username, int page) {
        UserAccount user = findUser(username);
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(
            user.getId(),
            PageRequest.of(Math.max(page, 0), POSTS_PER_PAGE)
        );
    }

    // =====================================================
    // CẬP NHẬT PROFILE
    // =====================================================

    @Transactional
    public void updateProfile(String username, ProfileUpdateRequest form) {
        UserAccount user = findUser(username);
        user.setFullName(form.getFullName().trim());

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            user.setProfile(profile);
        }

        profile.setBio(clean(form.getBio()));
        profile.setPhone(clean(form.getPhone()));
        profile.setDateOfBirth(form.getDateOfBirth());
        profile.setLocation(clean(form.getLocation()));

        userAccountRepository.save(user);
    }

    // =====================================================
    // UPLOAD AVATAR
    // =====================================================

    @Transactional
    public String updateAvatar(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh đại diện.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh.");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Ảnh không được vượt quá 5MB.");
        }

        try {
            Path uploadPath = Paths.get(uploadDir, "avatars");
            Files.createDirectories(uploadPath);

            String extension = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path targetPath = uploadPath.resolve(filename);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String avatarUrl = "/uploads/avatars/" + filename;

            UserAccount user = findUser(username);
            UserProfile profile = user.getProfile();
            if (profile == null) {
                profile = new UserProfile();
                user.setProfile(profile);
            }
            profile.setAvatarUrl(avatarUrl);
            userAccountRepository.save(user);

            return avatarUrl;

        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu ảnh: " + exception.getMessage());
        }
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private UserAccount findUser(String username) {
        return userAccountRepository
            .findByUsernameIgnoreCase(username)
            .orElseThrow(() ->
                new IllegalStateException("Không tìm thấy người dùng")
            );
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) return ".jpg";
        return filename.substring(dotIndex).toLowerCase();
    }
}