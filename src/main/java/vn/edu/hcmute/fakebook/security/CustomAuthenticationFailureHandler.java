package vn.edu.hcmute.fakebook.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import vn.edu.hcmute.fakebook.entity.UserAccount;
import vn.edu.hcmute.fakebook.enums.AccountStatus;
import vn.edu.hcmute.fakebook.repository.UserAccountRepository;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler
        implements AuthenticationFailureHandler {

    private final UserAccountRepository userRepository;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        String login = Optional.ofNullable(
            request.getParameter("username")
        ).orElse("").trim();

        Optional<UserAccount> user = userRepository
            .findByUsernameIgnoreCase(login)
            .or(() -> userRepository.findByEmailIgnoreCase(login));

        if (user.isPresent()
                && user.get().getStatus() == AccountStatus.DISABLED) {
            String url = UriComponentsBuilder
                .fromPath(request.getContextPath() + "/verify")
                .queryParam("email", user.get().getEmail())
                .queryParam("unverified", true)
                .build()
                .encode()
                .toUriString();
            response.sendRedirect(url);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/login?error");
    }
}
