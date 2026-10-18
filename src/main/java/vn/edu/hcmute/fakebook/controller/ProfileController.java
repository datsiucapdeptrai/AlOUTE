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
    // XEM PROFILE CỦA CHÍNH MÌNH
    // =====================================================
    @GetMapping("/profile")
    public String showOwnProfile(Authentication authentication) {
        return "redirect:/profile/" + authentication.getName();
    }

    // =====================================================
    // XEM PROFILE CỦA NGƯỜI KHÁC (hoặc chính mình)
    // =====================================================
    @GetMapping("/profile/{username}")
    public String showProfile(
            @PathVariable String username,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication,
            Model model) {

        UserAccount profileUser = profileService.getUserProfile(username);
        Page<Post> posts = profileService.getUserPosts(username, page);

        model.addAttribute("profileUser", profileUser);
        model.addAttribute("stats", profileService.getProfileStats(username));
        model.addAttribute("posts", posts);
        model.addAttribute(
            "isOwnProfile",
            authentication != null
                && authentication.getName().equalsIgnoreCase(username)
        );

        return "profile/view";
    }

    // =====================================================
    // HIỂN THỊ FORM CHỈNH SỬA
    // =====================================================
    @GetMapping("/profile/edit")
    public String showEditForm(Authentication authentication, Model model) {
        ProfileUpdateRequest form =
            profileService.getProfileForm(authentication.getName());
        model.addAttribute("profileForm", form);
        model.addAttribute(
            "currentUser",
            profileService.getUserProfile(authentication.getName())
        );
        return "profile/edit";
    }

    // =====================================================
    // LƯU THAY ĐỔI
    // =====================================================
    @PostMapping("/profile/edit")
    public String updateProfile(
            @Valid @ModelAttribute("profileForm") ProfileUpdateRequest profileForm,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                "currentUser",
                profileService.getUserProfile(authentication.getName())
            );
            return "profile/edit";
        }

        try {
            profileService.updateProfile(authentication.getName(), profileForm);

            // Nếu có upload avatar kèm theo
            MultipartFile avatar = profileForm.getAvatar();
            if (avatar != null && !avatar.isEmpty()) {
                profileService.updateAvatar(authentication.getName(), avatar);
            }

            redirectAttributes.addFlashAttribute(
                "successMessage", "Cập nhật hồ sơ thành công."
            );
            return "redirect:/profile/edit";

        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("updateError", exception.getMessage());
            model.addAttribute(
                "currentUser",
                profileService.getUserProfile(authentication.getName())
            );
            return "profile/edit";
        }
    }
}