package com.deplens.service;

import com.deplens.dto.RepositoryDto;
import com.deplens.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GitHubServiceTest {

    private final GitHubService gitHubService = new GitHubService();

    @Test
    @DisplayName("Should retrieve repository details including latest commit message and date")
    void testGetRepositoryDetailsWithCommitInfo() {
        User user = new User();
        user.setUsername("GauthamSalian");
        user.setEmail("test@deplens.io");

        Optional<RepositoryDto> result = gitHubService.getRepositoryDetails(user, "GauthamSalian", "DepLensJSF");

        assertTrue(result.isPresent(), "Repository details should be returned for owner");
        RepositoryDto repo = result.get();

        assertNotNull(repo.getLatestCommitMessage(), "Commit message (name) should not be null");
        assertNotNull(repo.getLatestCommitDate(), "Commit date should not be null");
        assertNotNull(repo.getLatestCommitAuthor(), "Commit author should not be null");
        assertNotNull(repo.getLatestCommitSha(), "Commit SHA should not be null");
        assertNotNull(repo.getLatestCommitUrl(), "Commit URL should not be null");

        // Verify detected manifests and ecosystems
        assertNotNull(repo.getDetectedManifests(), "Detected manifests list should not be null");
        assertFalse(repo.getDetectedManifests().isEmpty(), "Should detect at least one manifest");
        assertTrue(repo.getDetectedManifests().contains("package.json") || repo.getDetectedManifests().contains("pom.xml"));

        System.out.println("Verified Latest Commit Name: " + repo.getLatestCommitMessage());
        System.out.println("Verified Latest Commit Date: " + repo.getLatestCommitDate());
        System.out.println("Verified Latest Commit Author: " + repo.getLatestCommitAuthor());
        System.out.println("Verified Latest Commit SHA: " + repo.getLatestCommitSha());
        System.out.println("Verified Detected Manifests: " + repo.getDetectedManifests());
    }

    @Test
    @DisplayName("Should reject unauthorized user who is not owner or collaborator")
    void testUnauthorizedUserAccessDenied() {
        User user = new User();
        user.setUsername("unauthorized-user");

        Optional<RepositoryDto> result = gitHubService.getRepositoryDetails(user, "GauthamSalian", "DepLensJSF");

        assertTrue(result.isEmpty(), "Unauthorized user must receive empty Optional (403 Forbidden)");
    }
}
