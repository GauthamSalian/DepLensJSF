package com.deplens;

import com.deplens.model.RepositoryEntity;
import com.deplens.model.RepositoryStateEntity;
import com.deplens.repository.RepositoryEntityRepository;
import com.deplens.repository.RepositoryStateRepository;
import com.deplens.service.RepositoryScanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseSchemaIntegrationTest {

    @Autowired
    private RepositoryEntityRepository repositoryRepo;

    @Autowired
    private RepositoryStateRepository stateRepo;

    @Autowired
    private RepositoryScanService scanService;

    @Test
    @DisplayName("Verify Hibernate creates MySQL tables and persists relational graph data")
    void testDatabasePersistence() {
        RepositoryStateEntity state = scanService.recordOrGetScanState(
                "GauthamSalian",
                "DepLensJSF",
                "main",
                false,
                "https://github.com/GauthamSalian/DepLensJSF",
                "4082c07",
                "feat: implement supply chain intelligence scanner",
                "2026-10-18T08:00:00Z",
                "Gautham Salian",
                List.of("pom.xml"),
                List.of("Maven (Java)")
        );

        assertNotNull(state.getId(), "State should have generated database ID");
        assertNotNull(state.getRepository().getId(), "Repository should have generated database ID");
        assertNotNull(state.getDependencyGraph().getId(), "Graph should have generated database ID");
        assertFalse(state.getDependencyGraph().getPackageMetrics().isEmpty(), "Package metrics should be persisted");

        System.out.println("✅ Persisted Repository ID: " + state.getRepository().getId());
        System.out.println("✅ Persisted State ID: " + state.getId());
        System.out.println("✅ Persisted Graph ID: " + state.getDependencyGraph().getId());
        System.out.println("✅ Persisted Package Metrics Count: " + state.getDependencyGraph().getPackageMetrics().size());
    }
}
