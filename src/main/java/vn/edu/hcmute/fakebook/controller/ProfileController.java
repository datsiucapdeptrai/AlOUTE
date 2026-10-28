package vn.edu.hcmute.fakebook.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.ProfileUpdateRequest;
import vn.edu.hcmute.fakebook.entity.Post;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.service.ProfileService;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // =====================================================
    // 1. XEM PROFILE CỦA CHÍNH MÌNH (Điều hướng)
    // =====================================================
    @GetMapping("/profile")
    public String showOwnProfile(Authentication authentication) {
        // Chuyển hướng sang URL có username để dùng chung logic hiển thị
        return "redirect:/profile/" + authentication.getName();
    }

    // =====================================================
    // 2. XEM PROFILE CỦA NGƯỜI KHÁC (hoặc chính mình)
    // =====================================================
    @GetMapping("/profile/{username}")
    public String showProfile(
            @PathVariable String username,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        UserAccount profileUser = profileService.getUserProfile(username);
        Page<Post> posts = profileService.getUserPosts(username, page);

        // Truyền dữ liệu ra View
        model.addAttribute("profileUser", profileUser);
        model.addAttribute("stats", profileService.getProfileStats(username));
        model.addAttribute("posts", posts);
        
        // Kiểm tra xem người đang xem có phải là chủ tài khoản không (để ẩn/hiện nút Chỉnh sửa)
        boolean isOwnProfile = authentication != null 
                && authentication.getName().equalsIgnoreCase(username);
        model.addAttribute("isOwnProfile", isOwnProfile);

        return "profile/view";
    }

    // =====================================================
    // 3. HIỂN THỊ FORM CHỈNH SỬA
    // =====================================================
    @GetMapping("/profile/edit")
    public String showEditForm(Authentication authentication, Model model) {
        // Lấy form data (chứa các trường text) từ Service
        ProfileUpdateRequest form = profileService.getProfileForm(authentication.getName());
        model.addAttribute("profileForm", form);
        
        // Lấy thông tin user để hiển thị Avatar hiện tại
        model.addAttribute("currentUser", profileService.getUserProfile(authentication.getName()));
        
        return "profile/edit";
    }

    // =====================================================
    // 4. LƯU THAY ĐỔI
    // =====================================================
    @PostMapping("/profile/edit")
    public String updateProfile(
            @Valid @ModelAttribute("profileForm") ProfileUpdateRequest profileForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        // 4.1. Kiểm tra lỗi Validation (ví dụ: Họ tên rỗng)
        if (bindingResult.hasErrors()) {
            model.addAttribute("currentUser", profileService.getUserProfile(authentication.getName()));
            return "profile/edit";
        }

        try {
            // 4.2. Lưu các thông tin văn bản (Họ tên, Bio, SĐT, Ngày sinh, Địa điểm)
            profileService.updateProfile(authentication.getName(), profileForm);

            // 4.3. Xử lý Upload Avatar (nếu người dùng có chọn file)
            MultipartFile avatar = profileForm.getAvatar();
            if (avatar != null && !avatar.isEmpty()) {
                profileService.updateAvatar(authentication.getName(), avatar);
            }

            // 4.4. Thành công, chuyển hướng về trang chỉnh sửa kèm thông báo
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công.");
            return "redirect:/profile/edit";

        } catch (IllegalArgumentException | IllegalStateException exception) {
            // 4.5. Bắt lỗi logic từ Service (ví dụ: lỗi upload ảnh)
            model.addAttribute("updateError", exception.getMessage());
            model.addAttribute("currentUser", profileService.getUserProfile(authentication.getName()));
            return "profile/edit";
        }
    }
}