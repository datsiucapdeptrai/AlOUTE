package vn.edu.hcmute.fakebook.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.entity.Post;
import vn.edu.hcmute.fakebook.entity.PostView;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.repository.PostRepository;
import vn.edu.hcmute.fakebook.repository.PostViewRepository;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Service
@RequiredArgsConstructor
public class VideoInteractionService {

    private final PostRepository postRepository;
    private final PostViewRepository postViewRepository;
    private final UserAccountRepository userAccountRepository;
    private final PostService postService;

    @Transactional
    public long recordView(Long postId, String username) {
        UserAccount viewer = findUser(username);
        Post post = postService.getVisiblePost(postId, username);
        requireVideo(post);

        if (!postViewRepository.existsByPostIdAndViewerId(postId, viewer.getId())) {
            PostView view = new PostView();
            view.setPost(post);
            view.setViewer(viewer);
            postViewRepository.saveAndFlush(view);
            postRepository.incrementViewCount(postId);
        }
        return postRepository.findById(postId)
            .map(Post::getViewCount)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy video"));
    }

    @Transactional
    public long recordShare(Long postId, String username) {
        Post post = postService.getVisiblePost(postId, username);
        requireVideo(post);
        postRepository.incrementShareCount(postId);
        return postRepository.findById(postId)
            .map(Post::getShareCount)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy video"));
    }

    private void requireVideo(Post post) {
        if (post.getVideoUrl() == null || post.getVideoUrl().isBlank()) {
            throw new IllegalArgumentException("Bài viết này không có video");
        }
    }

    private UserAccount findUser(String username) {
        return userAccountRepository.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng"));
    }
}
