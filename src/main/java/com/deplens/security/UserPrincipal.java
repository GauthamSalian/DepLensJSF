package com.deplens.security;

import com.deplens.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class UserPrincipal implements OAuth2User, UserDetails {

    private final Long id;
    private final String githubId;
    private final String username;
    private final String name;
    private final String email;
    private final String avatarUrl;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;

    public UserPrincipal(Long id, String githubId, String username, String name, String email, String avatarUrl,
                         Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes) {
        this.id = id;
        this.githubId = githubId;
        this.username = username;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.authorities = authorities;
        this.attributes = attributes != null ? attributes : Collections.emptyMap();
    }

    public static UserPrincipal create(User user) {
        return create(user, Collections.emptyMap());
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole() != null ? user.getRole() : "ROLE_USER")
        );

        return new UserPrincipal(
                user.getId(),
                user.getGithubId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getAvatarUrl(),
                authorities,
                attributes
        );
    }

    public Long getId() { return id; }
    public String getGithubId() { return githubId; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getEmail() { return email; }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return name != null ? name : username;
    }
}
