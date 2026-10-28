package vn.edu.hcmute.fakebook.dto;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.edu.hcmute.fakebook.enums.PostVisibility;

@Data
public class CreatePostRequest {

    @Size(
        max = 5000,
        message = "Nội dung bài viết không được quá 5000 ký tự"
    )
    private String content;

    @NotNull(message = "Bạn phải chọn quyền riêng tư")
    private PostVisibility visibility = PostVisibility.PUBLIC;

    private MultipartFile image;

    @Size(max = 200, message = "Tiêu đề video không được quá 200 ký tự")
    private String title;

    private MultipartFile video;

    private MultipartFile videoPoster;

    private Long videoCategoryId;
}
