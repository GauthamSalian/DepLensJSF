package com.deplens.security;

import com.deplens.model.User;
import com.deplens.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Objects;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.oauth2.authorized-redirect-uri:http://localhost:8080}")
    private String redirectUri;

    public OAuth2AuthenticationSuccessHandler(
            JwtTokenProvider tokenProvider,
            UserRepository userRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Long userId = Objects.requireNonNull(userPrincipal.getId(), "UserPrincipal id must not be null");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        // If authenticated via OAuth2, capture the GitHub access token for GitHub API repository scans
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            try {
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                        oauthToken.getAuthorizedClientRegistrationId(),
                        oauthToken.getName()
                );
                if (client != null && client.getAccessToken() != null) {
                    user.setGithubAccessToken(client.getAccessToken().getTokenValue());
                    userRepository.save(user);
                }
            } catch (Exception e) {
                log.warn("Could not retrieve GitHub access token from OAuth2 client: {}", e.getMessage());
            }
        }

        // Generate 30-day JWT
        String token = tokenProvider.generateToken(user);

        // Set 30-day cookie (30 * 24 * 60 * 60 = 2,592,000 seconds)
        int maxAgeSeconds = (int) (tokenProvider.getExpirationMs() / 1000);
        Cookie cookie = new Cookie("deplens_token", token);
        cookie.setPath("/");
        cookie.setHttpOnly(false); // Accessible to frontend scripts for convenience
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);

        // Redirect to frontend with token parameter
        String targetBaseUri = (redirectUri != null && !redirectUri.isBlank()) ? redirectUri : "http://localhost:8080";
        String targetUrl = UriComponentsBuilder.fromUriString(targetBaseUri)
                .queryParam("token", token)
                .build().toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
