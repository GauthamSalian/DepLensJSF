package com.deplens.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "repositories", indexes = {
    @Index(name = "idx_repo_full_name", columnList = "full_name"),
    @Index(name = "idx_repo_owner", columnList = "owner_name")
})
public class RepositoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false, unique = true)
    private String fullName;

    @Column(name = "default_branch")
    private String defaultBranch = "main";

    @Column(name = "is_private")
    private boolean isPrivate = false;

    @Column(name = "html_url")
    private String htmlUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "repository", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lastScannedAt DESC")
    private List<RepositoryStateEntity> states = new ArrayList<>();

    public RepositoryEntity() {
    }

    public RepositoryEntity(String ownerName, String name, String fullName, String defaultBranch, boolean isPrivate, String htmlUrl) {
        this.ownerName = ownerName;
        this.name = name;
        this.fullName = fullName;
        this.defaultBranch = defaultBranch != null ? defaultBranch : "main";
        this.isPrivate = isPrivate;
        this.htmlUrl = htmlUrl;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<RepositoryStateEntity> getStates() { return states; }
    public void setStates(List<RepositoryStateEntity> states) { this.states = states; }

    public void addState(RepositoryStateEntity state) {
        states.add(state);
        state.setRepository(this);
    }
}
