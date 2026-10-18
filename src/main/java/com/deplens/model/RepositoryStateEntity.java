package com.deplens.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "repository_states", indexes = {
    @Index(name = "idx_state_repo_id", columnList = "repository_id"),
    @Index(name = "idx_state_commit_sha", columnList = "latest_commit_sha")
})
public class RepositoryStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    @JsonIgnore
    private RepositoryEntity repository;

    @Column(name = "first_scanned_at")
    private LocalDateTime firstScannedAt;

    @Column(name = "last_scanned_at")
    private LocalDateTime lastScannedAt;

    @Column(name = "latest_commit_sha")
    private String latestCommitSha;

    @Column(name = "latest_commit_name")
    private String latestCommitName;

    @Column(name = "latest_commit_date")
    private String latestCommitDate;

    @Column(name = "latest_commit_author")
    private String latestCommitAuthor;

    @Column(name = "detected_manifests", columnDefinition = "TEXT")
    private String detectedManifests; // JSON array: ["package.json", "pom.xml"]

    @Column(name = "ecosystems", columnDefinition = "TEXT")
    private String ecosystems; // JSON array: ["npm (Node.js)", "Maven (Java)"]

    @Column(name = "total_dependencies")
    private Integer totalDependencies = 0;

    @Column(name = "vulnerable_dependencies")
    private Integer vulnerableDependencies = 0;

    @Column(name = "composite_risk_score")
    private Double compositeRiskScore = 0.0;

    @Column(name = "risk_category")
    private String riskCategory = "LOW"; // LOW, MEDIUM, HIGH, CRITICAL

    @OneToOne(mappedBy = "state", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private DependencyGraphEntity dependencyGraph;

    public RepositoryStateEntity() {
    }

    public RepositoryStateEntity(RepositoryEntity repository, String latestCommitSha, String latestCommitName,
                                 String latestCommitDate, String latestCommitAuthor, String detectedManifests, String ecosystems) {
        this.repository = repository;
        this.latestCommitSha = latestCommitSha;
        this.latestCommitName = latestCommitName;
        this.latestCommitDate = latestCommitDate;
        this.latestCommitAuthor = latestCommitAuthor;
        this.detectedManifests = detectedManifests;
        this.ecosystems = ecosystems;
        this.firstScannedAt = LocalDateTime.now();
        this.lastScannedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.firstScannedAt == null) {
            this.firstScannedAt = LocalDateTime.now();
        }
        this.lastScannedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastScannedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RepositoryEntity getRepository() { return repository; }
    public void setRepository(RepositoryEntity repository) { this.repository = repository; }

    public LocalDateTime getFirstScannedAt() { return firstScannedAt; }
    public void setFirstScannedAt(LocalDateTime firstScannedAt) { this.firstScannedAt = firstScannedAt; }

    public LocalDateTime getLastScannedAt() { return lastScannedAt; }
    public void setLastScannedAt(LocalDateTime lastScannedAt) { this.lastScannedAt = lastScannedAt; }

    public String getLatestCommitSha() { return latestCommitSha; }
    public void setLatestCommitSha(String latestCommitSha) { this.latestCommitSha = latestCommitSha; }

    public String getLatestCommitName() { return latestCommitName; }
    public void setLatestCommitName(String latestCommitName) { this.latestCommitName = latestCommitName; }

    public String getLatestCommitDate() { return latestCommitDate; }
    public void setLatestCommitDate(String latestCommitDate) { this.latestCommitDate = latestCommitDate; }

    public String getLatestCommitAuthor() { return latestCommitAuthor; }
    public void setLatestCommitAuthor(String latestCommitAuthor) { this.latestCommitAuthor = latestCommitAuthor; }

    public String getDetectedManifests() { return detectedManifests; }
    public void setDetectedManifests(String detectedManifests) { this.detectedManifests = detectedManifests; }

    public String getEcosystems() { return ecosystems; }
    public void setEcosystems(String ecosystems) { this.ecosystems = ecosystems; }

    public Integer getTotalDependencies() { return totalDependencies; }
    public void setTotalDependencies(Integer totalDependencies) { this.totalDependencies = totalDependencies; }

    public Integer getVulnerableDependencies() { return vulnerableDependencies; }
    public void setVulnerableDependencies(Integer vulnerableDependencies) { this.vulnerableDependencies = vulnerableDependencies; }

    public Double getCompositeRiskScore() { return compositeRiskScore; }
    public void setCompositeRiskScore(Double compositeRiskScore) { this.compositeRiskScore = compositeRiskScore; }

    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }

    public DependencyGraphEntity getDependencyGraph() { return dependencyGraph; }
    public void setDependencyGraph(DependencyGraphEntity dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
        if (dependencyGraph != null) {
            dependencyGraph.setState(this);
        }
    }
}
