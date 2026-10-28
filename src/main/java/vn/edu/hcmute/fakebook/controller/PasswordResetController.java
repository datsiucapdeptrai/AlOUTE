package vn.edu.hcmute.fakebook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.hcmute.fakebook.enums.OtpType;
import vn.edu.hcmute.fakebook.service.AuthService;
import vn.edu.hcmute.fakebook.service.OtpService;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final AuthService authService;
    private final OtpService otpService;

    @GetMapping("/forgot-password")
    public String showForgotForm() {
        return "auth/forgot-password";
    }

    // Nhận email -> Gửi OTP -> Chuyển sang trang reset mật khẩu
    @PostMapping("/forgot-password")
    public String processForgot(@RequestParam String email, Model model) {
        if (!authService.isEmailExists(email)) {
            model.addAttribute("errorMessage", "Email không tồn tại trong hệ thống!");
            model.addAttribute("email", email);
            return "auth/forgot-password";
        }

        otpService.generateAndSendOtp(email, OtpType.FORGOT_PASSWORD);
        
        // Chuyển sang trang nhập OTP + pass mới
        model.addAttribute("email", email);
        model.addAttribute("successMessage", "Mã OTP đã được gửi tới email của bạn.");
        return "auth/reset-password"; 
    }

    // Xử lý khi bấm "Đổi mật khẩu" ở trang reset-password
    @PostMapping("/reset-password")
    public String processReset(@RequestParam String email,
                               @RequestParam String otpCode,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               Model model) {
        
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", "Mật khẩu xác nhận không khớp!");
            return "auth/reset-password";
        }

        if (!otpService.verifyOtp(email, otpCode, OtpType.FORGOT_PASSWORD)) {
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", "Mã OTP không đúng hoặc đã hết hạn!");
            return "auth/reset-password";
        }

        authService.updatePassword(email, newPassword);
        return "redirect:/login?resetSuccess";
    }
}