package com.deplens.controller;

import com.deplens.model.User;
import com.deplens.repository.UserRepository;
import com.deplens.security.JwtTokenProvider;
import com.deplens.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserRepository userRepository, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Returns the currently authenticated user's profile and session details
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getId() == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        Long userId = userPrincipal.getId();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("id", user.getId());
        response.put("githubId", user.getGithubId());
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("avatarUrl", user.getAvatarUrl());
        response.put("role", user.getRole());
        response.put("hasGithubToken", user.getGithubAccessToken() != null && !user.getGithubAccessToken().isBlank());
        response.put("sessionDays", 30);
        response.put("createdAt", user.getCreatedAt());
        response.put("lastLoginAt", user.getLastLoginAt());

        return ResponseEntity.ok(response);
    }

    /**
     * Clears authentication cookie and session
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        SecurityContextHolder.clearContext();

        // Expire cookie
        Cookie cookie = new Cookie("deplens_token", "");
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "Logged out successfully", "authenticated", false));
    }

    /**
     * Dev/Test login: generates a real 30-day JWT for local testing before GitHub credentials are configured
     */
    @PostMapping("/dev-login")
    public ResponseEntity<Map<String, Object>> devLogin(
            @RequestParam(defaultValue = "octocat") String username,
            HttpServletResponse response
    ) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .githubId("dev_" + username.hashCode())
                            .username(username)
                            .name("Developer " + username)
                            .email(username + "@deplens.local")
                            .avatarUrl("https://github.com/" + username + ".png")
                            .role("ROLE_USER")
                            .build();
                    User saved = userRepository.save(newUser);
                    return Objects.requireNonNull(saved, "Saved user must not be null");
                });

        String token = tokenProvider.generateToken(user);

        // Set 30-day cookie
        int maxAgeSeconds = (int) (tokenProvider.getExpirationMs() / 1000);
        Cookie cookie = new Cookie("deplens_token", token);
        cookie.setPath("/");
        cookie.setHttpOnly(false);
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "tokenType", "Bearer",
                "expiresInDays", 30,
                "user", Map.of(
                        "id", user.getId() != null ? user.getId() : 0L,
                        "username", user.getUsername(),
                        "name", user.getName() != null ? user.getName() : user.getUsername(),
                        "email", user.getEmail() != null ? user.getEmail() : "",
                        "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
                )
        ));
    }
}
