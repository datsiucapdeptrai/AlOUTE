package vn.edu.hcmute.fakebook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.service.PasswordResetService;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String forgotPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestOtp(
            @RequestParam("email") String email,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.requestOtp(email);
            redirectAttributes.addAttribute("email", email.trim());
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "OTP đặt lại mật khẩu đã được gửi đến Gmail."
            );
            return "redirect:/reset-password";
        } catch (IllegalArgumentException
                | IllegalStateException exception) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", exception.getMessage());
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String resetPage(
            @RequestParam(name = "email", required = false) String email,
            Model model) {
        model.addAttribute("email", email == null ? "" : email);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam("email") String email,
            @RequestParam("otp") String otp,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        try {
            passwordResetService.resetPassword(
                email,
                otp,
                password,
                confirmPassword
            );
            return "redirect:/login?resetSuccess";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", exception.getMessage());
            return "auth/reset-password";
        }
    }

    @PostMapping("/reset-password/resend")
    public String resendResetOtp(
            @RequestParam("email") String email,
            Model model) {
        try {
            passwordResetService.requestOtp(email);
            model.addAttribute(
                "successMessage",
                "OTP đặt lại mật khẩu mới đã được gửi."
            );
        } catch (IllegalArgumentException
                | IllegalStateException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
        }
        model.addAttribute("email", email);
        return "auth/reset-password";
    }
}
