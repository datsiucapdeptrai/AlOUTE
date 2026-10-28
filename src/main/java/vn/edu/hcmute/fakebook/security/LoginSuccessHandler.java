package vn.edu.hcmute.fakebook.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.service.JwtService;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        // 1. Lấy thông tin UserDetails từ Authentication
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 2. Tạo JWT token
        String jwtToken = jwtService.generateToken(userDetails);

        // 3. Tạo Cookie chứa JWT (HttpOnly để bảo mật, chống XSS)
        Cookie jwtCookie = new Cookie("jwt_token", jwtToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // Đổi thành true nếu chạy trên HTTPS
        jwtCookie.setPath("/");
        // Thời gian sống của cookie tính bằng giây (chuyển từ mili-giây)
        jwtCookie.setMaxAge((int) (jwtService.getExpirationTime() / 1000));
        response.addCookie(jwtCookie);

        // 4. Điều hướng dựa trên Role
        boolean isAdmin = authentication.getAuthorities().contains(
            new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        if (isAdmin) {
            response.sendRedirect(request.getContextPath() + "/admin");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/feed");
    }
}