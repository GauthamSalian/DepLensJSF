package com.deplens.security;

import com.deplens.model.User;
import com.deplens.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String githubId = String.valueOf(attributes.get("id"));
        String username = (String) attributes.get("login");
        String name = (String) attributes.get("name");
        String email = (String) attributes.get("email");
        String avatarUrl = (String) attributes.get("avatar_url");

        if (name == null || name.isBlank()) {
            name = username;
        }
        if (email == null || email.isBlank()) {
            email = username + "@users.noreply.github.com";
        }

        final String finalEmail = email;
        final String finalName = name;

        // Upsert User in database
        User user = userRepository.findByGithubId(githubId)
                .map(existingUser -> {
                    existingUser.setUsername(username);
                    existingUser.setName(finalName);
                    existingUser.setEmail(finalEmail);
                    existingUser.setAvatarUrl(avatarUrl);
                    existingUser.setLastLoginAt(LocalDateTime.now());
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .githubId(githubId)
                            .username(username)
                            .name(finalName)
                            .email(finalEmail)
                            .avatarUrl(avatarUrl)
                            .role("ROLE_USER")
                            .build();
                    return userRepository.save(newUser);
                });

        Objects.requireNonNull(user, "User cannot be null after upsert");
        return UserPrincipal.create(user, attributes);
    }
}
