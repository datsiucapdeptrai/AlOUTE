package vn.edu.hcmute.fakebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotBlank(message = "Bình luận không được để trống")
    @Size(
        max = 2000,
        message = "Bình luận không được quá 2000 ký tự"
    )
    private String content;
}