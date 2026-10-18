package com.deplens.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "package_metrics", indexes = {
    @Index(name = "idx_pkg_metric_name", columnList = "package_name"),
    @Index(name = "idx_pkg_metric_graph", columnList = "graph_id")
})
public class PackageMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graph_id", nullable = false)
    @JsonIgnore
    private DependencyGraphEntity graph;

    @Column(name = "package_name", nullable = false)
    private String packageName;

    @Column(name = "version")
    private String version;

    @Column(name = "is_direct")
    private boolean isDirect = true;

    @Column(name = "ecosystem")
    private String ecosystem = "npm";

    // =========================================================================
    // Section 3.1: Betweenness Centrality (C_B)
    // C_B(v) = Sum[ sigma_st(v) / sigma_st ] * 100 (for all s != v != t)
    // Normalized on a 0-100 scale using NetworkX / JGraphT.
    // =========================================================================
    @Column(name = "betweenness_centrality")
    private Double betweennessCentrality = 0.0;

    // =========================================================================
    // Section 3.2: AST Code Usage Scoring
    // 0 Project Files (Transitive only / Uncalled) -> 10 / 100 (Dormant / Edge Library)
    // 1-2 Project Files                           -> 30 / 100 (Localized / Isolated Usage)
    // 3-5 Project Files                           -> 60 / 100 (Moderate Multi-Module Usage)
    // 6-10 Project Files                          -> 80 / 100 (Wide Architectural Dependency)
    // > 10 Project Files                          -> 100 / 100 (Core System Backbone / Framework)
    // =========================================================================
    @Column(name = "source_code_references_count")
    private Integer sourceCodeReferencesCount = 0;

    @Column(name = "ast_code_usage_score")
    private Double astCodeUsageScore = 10.0;

    @Column(name = "architectural_classification")
    private String architecturalClassification = "Dormant / Edge Library";

    // =========================================================================
    // Section 3.3: Cascading Blast Radius (BR)
    // BR = 50% * D + 30% * M + 20% * P
    // Dependency Impact (D) = (Affected Dependents / Total Dependencies) * 100
    // Module Impact (M)     = (Affected Source Code Files / Total Code Modules) * 100
    // Propagation Depth (P) = (Reverse Traversal Depth d / Max Graph Depth d_max) * 100
    // =========================================================================
    @Column(name = "dependency_impact_d")
    private Double dependencyImpactD = 0.0;

    @Column(name = "module_impact_m")
    private Double moduleImpactM = 0.0;

    @Column(name = "propagation_depth_p")
    private Double propagationDepthP = 0.0;

    @Column(name = "blast_radius_score")
    private Double blastRadiusScore = 0.0;

    // =========================================================================
    // Section 3.4: DepLens Composite Risk Score
    // Risk = 35% * Severity + 30% * BlastRadius + 20% * Centrality + 15% * CodeUsage
    // Severity = CVSS * 10 (0 if no active CVE)
    // Resulting 0-100 score maps to: Low, Medium, High, Critical
    // =========================================================================
    @Column(name = "cvss_score")
    private Double cvssScore = 0.0;

    @Column(name = "severity_score")
    private Double severityScore = 0.0; // CVSS * 10

    @Column(name = "composite_risk_score")
    private Double compositeRiskScore = 0.0;

    @Column(name = "risk_category")
    private String riskCategory = "LOW"; // LOW, MEDIUM, HIGH, CRITICAL

    // =========================================================================
    // Section 3.5: Remediation Cost Optimization Model
    // argmin(Cost) subject to ResidualRisk = OriginalRisk * (1 - F) <= 30
    // UPDATE:   F = 85%, Effort = Base + Version Drift (4-16 hrs) @ Rs. 1,000/hr
    // MITIGATE: F = 55%, Effort = 2-4 hrs (Fixed Virtual Patch / WAF) @ Rs. 1,000/hr
    // REPLACE:  F = 94%, Effort = 24-60 hrs (Complete migration) @ Rs. 1,000/hr
    // =========================================================================
    @Column(name = "recommended_strategy")
    private String recommendedStrategy = "UPDATE"; // UPDATE, MITIGATE, REPLACE

    @Column(name = "risk_reduction_factor")
    private Double riskReductionFactor = 0.85;

    @Column(name = "residual_risk")
    private Double residualRisk = 0.0;

    @Column(name = "estimated_engineering_hours")
    private Double estimatedEngineeringHours = 4.0;

    @Column(name = "estimated_cost_inr")
    private Double estimatedCostInr = 4000.0;

    /**
     * Stored JSON array of all CVE vulnerabilities affecting this package version:
     * [
     *   {
     *     "cveId": "CVE-2024-21733",
     *     "severity": "HIGH",
     *     "cvss": 7.5,
     *     "summary": "Apache Tomcat HTTP Request Smuggling Vulnerability leading to unauthorized access.",
     *     "advisoryUrl": "https://nvd.nist.gov/vuln/detail/CVE-2024-21733"
     *   }
     * ]
     */
    @Column(name = "cves_json", columnDefinition = "LONGTEXT")
    private String cvesJson = "[]";

    /**
     * Stored JSON array of Abstract Syntax Tree (AST) code references in the codebase:
     * [
     *   {
     *     "filePath": "src/main/java/com/deplens/service/RepositoryScanService.java",
     *     "lineNumber": 18,
     *     "codeSnippet": "import com.fasterxml.jackson.databind.ObjectMapper;",
     *     "usageType": "Class Import / Model Serialization"
     *   }
     * ]
     */
    @Column(name = "ast_references_json", columnDefinition = "LONGTEXT")
    private String astReferencesJson = "[]";

    public PackageMetricEntity() {
    }

    public PackageMetricEntity(String packageName, String version, boolean isDirect, String ecosystem) {
        this.packageName = packageName;
        this.version = version;
        this.isDirect = isDirect;
        this.ecosystem = ecosystem;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DependencyGraphEntity getGraph() { return graph; }
    public void setGraph(DependencyGraphEntity graph) { this.graph = graph; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public boolean isDirect() { return isDirect; }
    public void setDirect(boolean direct) { isDirect = direct; }

    public String getEcosystem() { return ecosystem; }
    public void setEcosystem(String ecosystem) { this.ecosystem = ecosystem; }

    public Double getBetweennessCentrality() { return betweennessCentrality; }
    public void setBetweennessCentrality(Double betweennessCentrality) { this.betweennessCentrality = betweennessCentrality; }

    public Integer getSourceCodeReferencesCount() { return sourceCodeReferencesCount; }
    public void setSourceCodeReferencesCount(Integer sourceCodeReferencesCount) { this.sourceCodeReferencesCount = sourceCodeReferencesCount; }

    public Double getAstCodeUsageScore() { return astCodeUsageScore; }
    public void setAstCodeUsageScore(Double astCodeUsageScore) { this.astCodeUsageScore = astCodeUsageScore; }

    public String getArchitecturalClassification() { return architecturalClassification; }
    public void setArchitecturalClassification(String architecturalClassification) { this.architecturalClassification = architecturalClassification; }

    public Double getDependencyImpactD() { return dependencyImpactD; }
    public void setDependencyImpactD(Double dependencyImpactD) { this.dependencyImpactD = dependencyImpactD; }

    public Double getModuleImpactM() { return moduleImpactM; }
    public void setModuleImpactM(Double moduleImpactM) { this.moduleImpactM = moduleImpactM; }

    public Double getPropagationDepthP() { return propagationDepthP; }
    public void setPropagationDepthP(Double propagationDepthP) { this.propagationDepthP = propagationDepthP; }

    public Double getBlastRadiusScore() { return blastRadiusScore; }
    public void setBlastRadiusScore(Double blastRadiusScore) { this.blastRadiusScore = blastRadiusScore; }

    public Double getCvssScore() { return cvssScore; }
    public void setCvssScore(Double cvssScore) { this.cvssScore = cvssScore; }

    public Double getSeverityScore() { return severityScore; }
    public void setSeverityScore(Double severityScore) { this.severityScore = severityScore; }

    public Double getCompositeRiskScore() { return compositeRiskScore; }
    public void setCompositeRiskScore(Double compositeRiskScore) { this.compositeRiskScore = compositeRiskScore; }

    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }

    public String getRecommendedStrategy() { return recommendedStrategy; }
    public void setRecommendedStrategy(String recommendedStrategy) { this.recommendedStrategy = recommendedStrategy; }

    public Double getRiskReductionFactor() { return riskReductionFactor; }
    public void setRiskReductionFactor(Double riskReductionFactor) { this.riskReductionFactor = riskReductionFactor; }

    public Double getResidualRisk() { return residualRisk; }
    public void setResidualRisk(Double residualRisk) { this.residualRisk = residualRisk; }

    public Double getEstimatedEngineeringHours() { return estimatedEngineeringHours; }
    public void setEstimatedEngineeringHours(Double estimatedEngineeringHours) { this.estimatedEngineeringHours = estimatedEngineeringHours; }

    public Double getEstimatedCostInr() { return estimatedCostInr; }
    public void setEstimatedCostInr(Double estimatedCostInr) { this.estimatedCostInr = estimatedCostInr; }

    public String getCvesJson() { return cvesJson; }
    public void setCvesJson(String cvesJson) { this.cvesJson = cvesJson; }

    public String getAstReferencesJson() { return astReferencesJson; }
    public void setAstReferencesJson(String astReferencesJson) { this.astReferencesJson = astReferencesJson; }
}
