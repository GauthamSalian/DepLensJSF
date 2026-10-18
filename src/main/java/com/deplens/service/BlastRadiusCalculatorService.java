package com.deplens.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BlastRadiusCalculatorService {

    /**
     * Section 3.2: AST Code Usage Scoring
     * 0 Project Files (Transitive only / Uncalled): 10 / 100 (Dormant / Edge Library)
     * 1-2 Project Files:                           30 / 100 (Localized / Isolated Usage)
     * 3-5 Project Files:                           60 / 100 (Moderate Multi-Module Usage)
     * 6-10 Project Files:                          80 / 100 (Wide Architectural Dependency)
     * > 10 Project Files:                          100 / 100 (Core System Backbone / Framework)
     */
    public Map.Entry<Double, String> calculateAstCodeUsage(int projectFilesCount) {
        if (projectFilesCount <= 0) {
            return Map.entry(10.0, "Dormant / Edge Library");
        } else if (projectFilesCount <= 2) {
            return Map.entry(30.0, "Localized / Isolated Usage");
        } else if (projectFilesCount <= 5) {
            return Map.entry(60.0, "Moderate Multi-Module Usage");
        } else if (projectFilesCount <= 10) {
            return Map.entry(80.0, "Wide Architectural Dependency");
        } else {
            return Map.entry(100.0, "Core System Backbone / Framework");
        }
    }

    /**
     * Section 3.3: Cascading Blast Radius (BR)
     * BR = 50% * D + 30% * M + 20% * P
     * D = (Affected Dependents / Total Dependencies) * 100
     * M = (Affected Source Code Files / Total Code Modules) * 100
     * P = (Reverse Traversal Depth d / Max Graph Depth d_max) * 100
     */
    public double calculateBlastRadius(double affectedDependents, double totalDependencies,
                                       double affectedCodeFiles, double totalCodeModules,
                                       int reverseTraversalDepth, int maxGraphDepth) {
        double d = totalDependencies > 0 ? (affectedDependents / totalDependencies) * 100.0 : 0.0;
        double m = totalCodeModules > 0 ? (affectedCodeFiles / totalCodeModules) * 100.0 : 0.0;
        double p = maxGraphDepth > 0 ? ((double) reverseTraversalDepth / maxGraphDepth) * 100.0 : 0.0;

        d = Math.min(100.0, Math.max(0.0, d));
        m = Math.min(100.0, Math.max(0.0, m));
        p = Math.min(100.0, Math.max(0.0, p));

        double br = (0.50 * d) + (0.30 * m) + (0.20 * p);
        return Math.round(br * 100.0) / 100.0;
    }

    /**
     * Section 3.4: DepLens Composite Risk Score
     * Risk = 35% * Severity + 30% * BlastRadius + 20% * Centrality + 15% * CodeUsage
     * Severity = CVSS * 10 (0 if no active CVE)
     */
    public double calculateCompositeRisk(double cvss, double blastRadius, double centrality, double codeUsage) {
        double severity = Math.min(100.0, Math.max(0.0, cvss * 10.0));
        blastRadius = Math.min(100.0, Math.max(0.0, blastRadius));
        centrality = Math.min(100.0, Math.max(0.0, centrality));
        codeUsage = Math.min(100.0, Math.max(0.0, codeUsage));

        double risk = (0.35 * severity) + (0.30 * blastRadius) + (0.20 * centrality) + (0.15 * codeUsage);
        return Math.round(risk * 100.0) / 100.0;
    }

    public String categorizeRisk(double compositeRisk) {
        if (compositeRisk < 30.0) return "LOW";
        if (compositeRisk < 60.0) return "MEDIUM";
        if (compositeRisk < 85.0) return "HIGH";
        return "CRITICAL";
    }

    /**
     * Section 3.5: Remediation Cost Optimization Model
     * argmin(Cost) subject to ResidualRisk = OriginalRisk * (1 - F) <= 30
     *
     * Strategies:
     * - MITIGATE: F = 55%, Effort = 3 hrs @ Rs. 1,000/hr = Rs. 3,000
     * - UPDATE:   F = 85%, Effort = 8 hrs @ Rs. 1,000/hr = Rs. 8,000
     * - REPLACE:  F = 94%, Effort = 36 hrs @ Rs. 1,000/hr = Rs. 36,000
     */
    public RemediationPlan optimizeRemediation(double originalRisk) {
        // If already safe (<= 30)
        if (originalRisk <= 30.0) {
            return new RemediationPlan("UPDATE", 0.85, Math.round(originalRisk * 0.15 * 100.0) / 100.0, 2.0, 2000.0);
        }

        // Test MITIGATE (F = 55%): Cost Rs. 3,000 (3 hrs)
        double residualMitigate = originalRisk * (1.0 - 0.55);
        if (residualMitigate <= 30.0) {
            return new RemediationPlan("MITIGATE", 0.55, Math.round(residualMitigate * 100.0) / 100.0, 3.0, 3000.0);
        }

        // Test UPDATE (F = 85%): Cost Rs. 8,000 (8 hrs)
        double residualUpdate = originalRisk * (1.0 - 0.85);
        if (residualUpdate <= 30.0) {
            return new RemediationPlan("UPDATE", 0.85, Math.round(residualUpdate * 100.0) / 100.0, 8.0, 8000.0);
        }

        // Fallback to REPLACE (F = 94%): Cost Rs. 36,000 (36 hrs)
        double residualReplace = originalRisk * (1.0 - 0.94);
        return new RemediationPlan("REPLACE", 0.94, Math.round(residualReplace * 100.0) / 100.0, 36.0, 36000.0);
    }

    public record RemediationPlan(String strategy, double riskReductionFactor, double residualRisk,
                                 double estimatedHours, double estimatedCostInr) {}
}
