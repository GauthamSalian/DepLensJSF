package com.deplens.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dependency_graphs")
public class DependencyGraphEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id", nullable = false, unique = true)
    @JsonIgnore
    private RepositoryStateEntity state;

    /**
     * Stored JSON array of all dependency nodes:
     * [
     *   { "id": "express@4.18.2", "name": "express", "version": "4.18.2", "isDirect": true, "ecosystem": "npm", "depth": 1 },
     *   { "id": "debug@4.3.4", "name": "debug", "version": "4.3.4", "isDirect": false, "ecosystem": "npm", "depth": 2 }
     * ]
     */
    @Column(name = "nodes_json", columnDefinition = "LONGTEXT")
    private String nodesJson;

    /**
     * Stored JSON array of directed dependency edges (dependencies -> sub-dependencies):
     * [
     *   { "source": "root", "target": "express@4.18.2", "relation": "direct_dependency", "depth": 1 },
     *   { "source": "express@4.18.2", "target": "debug@4.3.4", "relation": "transitive_dependency", "depth": 2 }
     * ]
     */
    @Column(name = "edges_json", columnDefinition = "LONGTEXT")
    private String edgesJson;

    @Column(name = "total_nodes")
    private Integer totalNodes = 0;

    @Column(name = "total_edges")
    private Integer totalEdges = 0;

    @Column(name = "direct_dependencies_count")
    private Integer directDependenciesCount = 0;

    @Column(name = "transitive_dependencies_count")
    private Integer transitiveDependenciesCount = 0;

    /**
     * Maximum graph depth (d_max) used in Section 3.3 formula:
     * Propagation Depth Ratio (P) = (Reverse Traversal Depth d / Max Graph Depth d_max) * 100
     */
    @Column(name = "max_graph_depth")
    private Integer maxGraphDepth = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "graph", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PackageMetricEntity> packageMetrics = new ArrayList<>();

    public DependencyGraphEntity() {
    }

    public DependencyGraphEntity(RepositoryStateEntity state, String nodesJson, String edgesJson,
                                 Integer totalNodes, Integer totalEdges, Integer directDependenciesCount,
                                 Integer transitiveDependenciesCount, Integer maxGraphDepth) {
        this.state = state;
        this.nodesJson = nodesJson;
        this.edgesJson = edgesJson;
        this.totalNodes = totalNodes;
        this.totalEdges = totalEdges;
        this.directDependenciesCount = directDependenciesCount;
        this.transitiveDependenciesCount = transitiveDependenciesCount;
        this.maxGraphDepth = (maxGraphDepth != null && maxGraphDepth > 0) ? maxGraphDepth : 1;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RepositoryStateEntity getState() { return state; }
    public void setState(RepositoryStateEntity state) { this.state = state; }

    public String getNodesJson() { return nodesJson; }
    public void setNodesJson(String nodesJson) { this.nodesJson = nodesJson; }

    public String getEdgesJson() { return edgesJson; }
    public void setEdgesJson(String edgesJson) { this.edgesJson = edgesJson; }

    public Integer getTotalNodes() { return totalNodes; }
    public void setTotalNodes(Integer totalNodes) { this.totalNodes = totalNodes; }

    public Integer getTotalEdges() { return totalEdges; }
    public void setTotalEdges(Integer totalEdges) { this.totalEdges = totalEdges; }

    public Integer getDirectDependenciesCount() { return directDependenciesCount; }
    public void setDirectDependenciesCount(Integer directDependenciesCount) { this.directDependenciesCount = directDependenciesCount; }

    public Integer getTransitiveDependenciesCount() { return transitiveDependenciesCount; }
    public void setTransitiveDependenciesCount(Integer transitiveDependenciesCount) { this.transitiveDependenciesCount = transitiveDependenciesCount; }

    public Integer getMaxGraphDepth() { return maxGraphDepth; }
    public void setMaxGraphDepth(Integer maxGraphDepth) { this.maxGraphDepth = maxGraphDepth; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<PackageMetricEntity> getPackageMetrics() { return packageMetrics; }
    public void setPackageMetrics(List<PackageMetricEntity> packageMetrics) { this.packageMetrics = packageMetrics; }

    public void addMetric(PackageMetricEntity metric) {
        packageMetrics.add(metric);
        metric.setGraph(this);
    }
}
