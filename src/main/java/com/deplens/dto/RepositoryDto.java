package com.deplens.dto;

import java.util.ArrayList;
import java.util.List;

public class RepositoryDto {

    private Long id;
    private String name;
    private String fullName;
    private String owner;
    private boolean isPrivate;
    private String description;
    private String defaultBranch;
    private String language;
    private String updatedAt;
    private String htmlUrl;

    // Latest commit information
    private String latestCommitMessage;
    private String latestCommitSha;
    private String latestCommitAuthor;
    private String latestCommitDate;
    private String latestCommitUrl;

    // Detected manifest files and ecosystems
    private List<String> detectedManifests = new ArrayList<>();
    private List<String> ecosystems = new ArrayList<>();

    public RepositoryDto() {
    }

    public RepositoryDto(Long id, String name, String fullName, String owner, boolean isPrivate,
                         String description, String defaultBranch, String language, String updatedAt, String htmlUrl) {
        this.id = id;
        this.name = name;
        this.fullName = fullName;
        this.owner = owner;
        this.isPrivate = isPrivate;
        this.description = description;
        this.defaultBranch = defaultBranch;
        this.language = language;
        this.updatedAt = updatedAt;
        this.htmlUrl = htmlUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

    public String getLatestCommitMessage() { return latestCommitMessage; }
    public void setLatestCommitMessage(String latestCommitMessage) { this.latestCommitMessage = latestCommitMessage; }

    public String getLatestCommitSha() { return latestCommitSha; }
    public void setLatestCommitSha(String latestCommitSha) { this.latestCommitSha = latestCommitSha; }

    public String getLatestCommitAuthor() { return latestCommitAuthor; }
    public void setLatestCommitAuthor(String latestCommitAuthor) { this.latestCommitAuthor = latestCommitAuthor; }

    public String getLatestCommitDate() { return latestCommitDate; }
    public void setLatestCommitDate(String latestCommitDate) { this.latestCommitDate = latestCommitDate; }

    public String getLatestCommitUrl() { return latestCommitUrl; }
    public void setLatestCommitUrl(String latestCommitUrl) { this.latestCommitUrl = latestCommitUrl; }

    public List<String> getDetectedManifests() { return detectedManifests; }
    public void setDetectedManifests(List<String> detectedManifests) { this.detectedManifests = detectedManifests; }

    public List<String> getEcosystems() { return ecosystems; }
    public void setEcosystems(List<String> ecosystems) { this.ecosystems = ecosystems; }
}
