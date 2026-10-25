package com.deplens.service;

import com.deplens.model.DependencyGraphEntity;
import com.deplens.model.PackageMetricEntity;
import com.deplens.model.RepositoryEntity;
import com.deplens.model.RepositoryStateEntity;
import com.deplens.repository.DependencyGraphRepository;
import com.deplens.repository.PackageMetricRepository;
import com.deplens.repository.RepositoryEntityRepository;
import com.deplens.repository.RepositoryStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryScanServiceTest {

    @Mock
    private RepositoryEntityRepository repositoryRepo;

    @Mock
    private RepositoryStateRepository stateRepo;

    @Mock
    private DependencyGraphRepository graphRepo;

    @Mock
    private PackageMetricRepository metricRepo;

    @Mock
    private GitHubService gitHubService;

    @Mock
    private OsvVulnerabilityClient osvClient;

    @Spy
    private DependencyParserService dependencyParserService = new DependencyParserService();

    @Spy
    private BlastRadiusCalculatorService blastRadiusCalculator = new BlastRadiusCalculatorService();

    @InjectMocks
    private RepositoryScanService scanService;

    private RepositoryEntity sampleRepo;

    @BeforeEach
    void setUp() {
        sampleRepo = new RepositoryEntity("GauthamSalian", "DepLensJSF", "GauthamSalian/DepLensJSF", "main", false, "https://github.com/GauthamSalian/DepLensJSF");
        sampleRepo.setId(1L);
    }

    @Test
    @DisplayName("Section 3.2-3.5: Mathematical formula calculations validation")
    void testFormulasCalculations() {
        // Section 3.2: AST Code Usage
        Map.Entry<Double, String> ast0 = blastRadiusCalculator.calculateAstCodeUsage(0);
        assertEquals(10.0, ast0.getKey());
        assertEquals("Dormant / Edge Library", ast0.getValue());

        Map.Entry<Double, String> ast4 = blastRadiusCalculator.calculateAstCodeUsage(4);
        assertEquals(60.0, ast4.getKey());
        assertEquals("Moderate Multi-Module Usage", ast4.getValue());

        // Section 3.3: Cascading Blast Radius (BR = 50% * D + 30% * M + 20% * P)
        // D = (5 / 10) * 100 = 50%
        // M = (3 / 10) * 100 = 30%
        // P = (2 / 4) * 100 = 50%
        // BR = 0.50 * 50 + 0.30 * 30 + 0.20 * 50 = 25 + 9 + 10 = 44.0
        double br = blastRadiusCalculator.calculateBlastRadius(5, 10, 3, 10, 2, 4);
        assertEquals(44.0, br, 0.01);

        // Section 3.4: Composite Risk
        // Risk = 35% * Severity + 30% * BlastRadius + 20% * Centrality + 15% * CodeUsage
        // CVSS = 7.5 -> Severity = 75.0
        // BR = 44.0, Centrality = 65.0, CodeUsage = 60.0
        // Risk = 0.35 * 75 + 0.30 * 44 + 0.20 * 65 + 0.15 * 60 = 26.25 + 13.20 + 13.00 + 9.00 = 61.45
        double risk = blastRadiusCalculator.calculateCompositeRisk(7.5, 44.0, 65.0, 60.0);
        assertEquals(61.45, risk, 0.01);
        assertEquals("HIGH", blastRadiusCalculator.categorizeRisk(risk));

        // Section 3.5: Cost Optimization Model (Residual Risk <= 30)
        // With Risk = 61.45:
        // MITIGATE (F = 55%): Residual = 61.45 * 0.45 = 27.65 <= 30 -> MITIGATE chosen (Cost Rs. 3,000)
        BlastRadiusCalculatorService.RemediationPlan plan = blastRadiusCalculator.optimizeRemediation(risk);
        assertEquals("MITIGATE", plan.strategy());
        assertEquals(0.55, plan.riskReductionFactor());
        assertTrue(plan.residualRisk() <= 30.0);
        assertEquals(3000.0, plan.estimatedCostInr());
    }

    @Test
    @DisplayName("Should create scan state, graph nodes/edges and metrics for new commit")
    void testRecordNewScanState() {
        when(repositoryRepo.findFirstByFullNameIgnoreCase("GauthamSalian/DepLensJSF"))
                .thenReturn(Optional.of(sampleRepo));
        when(stateRepo.findFirstByRepositoryOrderByLastScannedAtDesc(sampleRepo))
                .thenReturn(Optional.empty());
        when(stateRepo.save(any(RepositoryStateEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RepositoryStateEntity state = scanService.recordOrGetScanState(
                "GauthamSalian", "DepLensJSF", "main", false,
                "https://github.com/GauthamSalian/DepLensJSF",
                "4082c07", "feat: implement graph schema", "2026-10-18T08:00:00Z", "Gautham Salian",
                List.of("pom.xml"), List.of("Maven (Java)")
        );

        assertNotNull(state);
        assertEquals("4082c07", state.getLatestCommitSha());
        assertEquals("feat: implement graph schema", state.getLatestCommitName());

        // Verify graph was constructed
        DependencyGraphEntity graph = state.getDependencyGraph();
        assertNotNull(graph);
        assertTrue(graph.getTotalNodes() > 0);
        assertTrue(graph.getTotalEdges() > 0);
        assertTrue(graph.getNodesJson().contains("spring-boot-starter-web"));
        assertTrue(graph.getEdgesJson().contains("transitive_dependency"), "Graph should contain transitive dependencies of dependencies");

        // Verify package metrics were created with formulas
        assertFalse(graph.getPackageMetrics().isEmpty());
        PackageMetricEntity metric = graph.getPackageMetrics().get(0);
        assertNotNull(metric.getBlastRadiusScore());
        assertNotNull(metric.getCompositeRiskScore());
        assertNotNull(metric.getRecommendedStrategy());

        verify(stateRepo, times(1)).save(any(RepositoryStateEntity.class));
    }

    @Test
    @DisplayName("OSV results feed the CVSS and risk score, other packages keep the fallback data")
    void testOsvResultsUsedInRiskScore() {
        when(repositoryRepo.findFirstByFullNameIgnoreCase("GauthamSalian/DepLensJSF"))
                .thenReturn(Optional.of(sampleRepo));
        when(stateRepo.findFirstByRepositoryOrderByLastScannedAtDesc(sampleRepo))
                .thenReturn(Optional.empty());
        when(stateRepo.save(any(RepositoryStateEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, Object> osvCve = Map.of(
                "cveId", "CVE-2020-36518", "severity", "HIGH", "cvss", 7.5,
                "summary", "Deeply nested json causes a stack overflow",
                "advisoryUrl", "https://nvd.nist.gov/vuln/detail/CVE-2020-36518");
        when(osvClient.findVulnerabilities(any(), any()))
                .thenReturn(Map.of("jackson-databind@2.17.1", List.of(osvCve)));

        RepositoryStateEntity state = scanService.recordOrGetScanState(
                "GauthamSalian", "DepLensJSF", "main", false,
                "https://github.com/GauthamSalian/DepLensJSF",
                "9999999", "test", "2026-10-18T08:00:00Z", "Gautham Salian",
                List.of("pom.xml"), List.of("Maven (Java)")
        );

        List<PackageMetricEntity> metrics = state.getDependencyGraph().getPackageMetrics();
        PackageMetricEntity jackson = metrics.stream().filter(m -> m.getPackageName().equals("jackson-databind")).findFirst().orElseThrow();
        assertEquals(7.5, jackson.getCvssScore());
        assertTrue(jackson.getCvesJson().contains("CVE-2020-36518"));
        assertEquals(blastRadiusCalculator.calculateCompositeRisk(7.5, jackson.getBlastRadiusScore(),
                jackson.getBetweennessCentrality(), jackson.getAstCodeUsageScore()), jackson.getCompositeRiskScore());

        // Not in the OSV map, so the old hardcoded data is still used
        PackageMetricEntity tomcat = metrics.stream().filter(m -> m.getPackageName().equals("tomcat-embed-core")).findFirst().orElseThrow();
        assertTrue(tomcat.getCvesJson().contains("CVE-2024-21733"));
    }

    @Test
    @DisplayName("Should return cached state if commit SHA has not changed")
    void testCacheHitWhenCommitUnchanged() {
        RepositoryStateEntity existingState = new RepositoryStateEntity(
                sampleRepo, "4082c07", "feat: implement graph schema",
                "2026-10-18T08:00:00Z", "Gautham Salian", "[\"pom.xml\"]", "[\"Maven\"]"
        );

        when(repositoryRepo.findFirstByFullNameIgnoreCase("GauthamSalian/DepLensJSF"))
                .thenReturn(Optional.of(sampleRepo));
        when(stateRepo.findFirstByRepositoryOrderByLastScannedAtDesc(sampleRepo))
                .thenReturn(Optional.of(existingState));
        when(stateRepo.save(existingState)).thenReturn(existingState);

        RepositoryStateEntity result = scanService.recordOrGetScanState(
                "GauthamSalian", "DepLensJSF", "main", false,
                "https://github.com/GauthamSalian/DepLensJSF",
                "4082c07", "feat: implement graph schema", "2026-10-18T08:00:00Z", "Gautham Salian",
                List.of("pom.xml"), List.of("Maven")
        );

        assertSame(existingState, result, "Should return cached state without re-generating graph");
        verify(stateRepo, times(1)).save(existingState);
    }
}
