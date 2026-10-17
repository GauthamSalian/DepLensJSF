package com.deplens.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_id", unique = true, nullable = false)
    private String githubId;

    @Column(name = "username", nullable = false)
    private String username;

    private String name;

    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "github_access_token", columnDefinition = "TEXT")
    private String githubAccessToken;

    private String role = "ROLE_USER";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public User() {
    }

    public User(Long id, String githubId, String username, String name, String email, String avatarUrl, String githubAccessToken, String role, LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        this.id = id;
        this.githubId = githubId;
        this.username = username;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.githubAccessToken = githubAccessToken;
        this.role = role != null ? role : "ROLE_USER";
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastLoginAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // Builder Pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String githubId;
        private String username;
        private String name;
        private String email;
        private String avatarUrl;
        private String githubAccessToken;
        private String role = "ROLE_USER";
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder githubId(String githubId) { this.githubId = githubId; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public Builder githubAccessToken(String githubAccessToken) { this.githubAccessToken = githubAccessToken; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder lastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; return this; }

        public User build() {
            return new User(id, githubId, username, name, email, avatarUrl, githubAccessToken, role, createdAt, lastLoginAt);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGithubId() { return githubId; }
    public void setGithubId(String githubId) { this.githubId = githubId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getGithubAccessToken() { return githubAccessToken; }
    public void setGithubAccessToken(String githubAccessToken) { this.githubAccessToken = githubAccessToken; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
