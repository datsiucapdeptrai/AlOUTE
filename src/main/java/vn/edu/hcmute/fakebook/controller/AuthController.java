package vn.edu.hcmute.fakebook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.dto.RegisterRequest;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.service.AuthService;
import vn.edu.hcmute.fakebook.service.EmailVerificationService;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService verificationService;

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "auth/register";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest")
            RegisterRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            UserAccount user = authService.register(request);
            redirectAttributes.addAttribute("email", user.getEmail());
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Hãy nhập OTP đã gửi đến email của bạn."
            );
            return "redirect:/verify";
        } catch (IllegalArgumentException
                | IllegalStateException exception) {
            model.addAttribute("registerError", exception.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/verify")
    public String verifyPage(
            @RequestParam(name = "email", required = false) String email,
            @RequestParam(name = "unverified", required = false)
            Boolean unverified,
            Model model) {

        model.addAttribute("email", email == null ? "" : email);
        if (Boolean.TRUE.equals(unverified)) {
            model.addAttribute(
                "verifyError",
                "Tài khoản chưa xác thực. Hãy nhập OTP hoặc gửi lại OTP."
            );
        }
        return "auth/verify";
    }

    @PostMapping("/verify")
    public String verify(
            @RequestParam("email") String email,
            @RequestParam("otp") String otp,
            Model model) {
        try {
            verificationService.verify(email, otp);
            return "redirect:/login?verified";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("email", email);
            model.addAttribute("verifyError", exception.getMessage());
            return "auth/verify";
        }
    }

    @PostMapping("/verify/resend")
    public String resend(
            @RequestParam("email") String email,
            Model model) {
        try {
            verificationService.resendOtp(email);
            model.addAttribute(
                "successMessage",
                "OTP xác thực mới đã được gửi."
            );
        } catch (IllegalArgumentException
                | IllegalStateException exception) {
            model.addAttribute("verifyError", exception.getMessage());
        }
        model.addAttribute("email", email);
        return "auth/verify";
    }
}
