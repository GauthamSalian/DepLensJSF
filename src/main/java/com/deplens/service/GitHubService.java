package com.deplens.service;

import com.deplens.dto.RepositoryDto;
import com.deplens.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private final RestTemplate restTemplate;

    public GitHubService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Fetches public and private repositories belonging to or collaborated on by the authenticated user
     */
    public List<RepositoryDto> getUserRepositories(User user) {
        String token = user.getGithubAccessToken();

        // If the user authenticated via real GitHub OAuth and has an access token
        if (token != null && !token.isBlank()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                headers.set("Accept", "application/vnd.github+json");
                headers.set("X-GitHub-Api-Version", "2022-11-28");
                headers.set("User-Agent", "DepLens-Engine");

                HttpEntity<Void> entity = new HttpEntity<>(headers);

                // Fetch up to 100 repositories, sorted by last updated
                String url = "https://api.github.com/user/repos?per_page=100&sort=updated&affiliation=owner,collaborator,organization_member";

                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                        url,
                        HttpMethod.valueOf("GET"),
                        entity,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );

                List<Map<String, Object>> body = response.getBody();
                if (body != null) {
                    List<RepositoryDto> repos = new ArrayList<>();
                    for (Map<String, Object> map : body) {
                        repos.add(mapToDto(map));
                    }
                    return repos;
                }
            } catch (Exception ex) {
                log.error("Failed to fetch repositories from GitHub API: {}", ex.getMessage());
            }
        }

        // Fallback for dev session or if GitHub token is unavailable
        return getMockRepositories(user.getUsername());
    }

    /**
     * Verifies that the authenticated user actually owns or has authorized push/admin access to the repository.
     * Returns the RepositoryDto if authorized, or Optional.empty() if access is denied.
     */
    public Optional<RepositoryDto> getRepositoryDetails(User user, String owner, String repoName) {
        String token = user.getGithubAccessToken();

        // 1. If real GitHub OAuth token is present, query GitHub API to verify ownership / collaborator permissions
        if (token != null && !token.isBlank()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                headers.set("Accept", "application/vnd.github+json");
                headers.set("X-GitHub-Api-Version", "2022-11-28");
                headers.set("User-Agent", "DepLens-Engine");

                HttpEntity<Void> entity = new HttpEntity<>(headers);
                String url = "https://api.github.com/repos/" + owner + "/" + repoName;

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url,
                        HttpMethod.valueOf("GET"),
                        entity,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                Map<String, Object> body = response.getBody();
                if (body != null) {
                    RepositoryDto dto = mapToDto(body);

                    // Check permissions: user must be the repository owner or have collaborator write/admin access
                    boolean isOwner = owner.equalsIgnoreCase(user.getUsername());

                    Object permissionsObj = body.get("permissions");
                    boolean hasPushOrAdmin = false;
                    if (permissionsObj instanceof Map<?, ?> permissions) {
                        hasPushOrAdmin = Boolean.TRUE.equals(permissions.get("push"))
                                || Boolean.TRUE.equals(permissions.get("admin"));
                    }

                    if (isOwner || hasPushOrAdmin) {
                        return Optional.of(dto);
                    } else {
                        log.warn("User {} denied access to repo {}/{}: not owner and lacks push/admin permissions",
                                user.getUsername(), owner, repoName);
                        return Optional.empty();
                    }
                }
            } catch (Exception ex) {
                log.warn("Access check failed for user {} on repo {}/{}: {}",
                        user.getUsername(), owner, repoName, ex.getMessage());
                return Optional.empty();
            }
        }

        // 2. Fallback for mock/dev session: strictly verify username matches repository owner
        if (user.getUsername() != null && user.getUsername().equalsIgnoreCase(owner)) {
            return Optional.of(new RepositoryDto(
                    101L, repoName, owner + "/" + repoName, owner, false,
                    "Repository managed by @" + owner, "main", "Java", "2026-10-17T20:00:00Z",
                    "https://github.com/" + owner + "/" + repoName
            ));
        }

        return Optional.empty();
    }

    /**
     * Maps GitHub raw API JSON map to RepositoryDto
     */
    private RepositoryDto mapToDto(Map<String, Object> map) {
        Long id = map.get("id") instanceof Number num ? num.longValue() : 0L;
        String name = (String) map.get("name");
        String fullName = (String) map.get("full_name");
        boolean isPrivate = Boolean.TRUE.equals(map.get("private"));
        String description = (String) map.get("description");
        String defaultBranch = (String) map.get("default_branch");
        String language = (String) map.get("language");
        String updatedAt = (String) map.get("updated_at");
        String htmlUrl = (String) map.get("html_url");

        String owner = "";
        Object ownerObj = map.get("owner");
        if (ownerObj instanceof Map<?, ?> ownerMap) {
            owner = (String) ownerMap.get("login");
        }

        return new RepositoryDto(
                id,
                name != null ? name : "unknown",
                fullName != null ? fullName : name,
                owner,
                isPrivate,
                description,
                defaultBranch != null ? defaultBranch : "main",
                language != null ? language : "Java",
                updatedAt,
                htmlUrl
        );
    }

    /**
     * Fallback mock repositories for local dev mode testing
     */
    private List<RepositoryDto> getMockRepositories(String username) {
        String owner = (username != null && !username.isBlank()) ? username : "deplens";
        return List.of(
                new RepositoryDto(101L, "deplens-core", owner + "/deplens-core", owner, false,
                        "Core AST analysis and directed graph engine", "main", "Java", "2026-10-17T18:00:00Z", "https://github.com/" + owner + "/deplens-core"),
                new RepositoryDto(102L, "spring-ecommerce-api", owner + "/spring-ecommerce-api", owner, true,
                        "Microservice ecommerce backend with vulnerable transitive dependencies", "master", "Java", "2026-10-16T12:30:00Z", "https://github.com/" + owner + "/spring-ecommerce-api"),
                new RepositoryDto(103L, "cloud-auth-service", owner + "/cloud-auth-service", owner, false,
                        "OAuth2 and JWT identity service", "main", "Java", "2026-10-15T09:15:00Z", "https://github.com/" + owner + "/cloud-auth-service"),
                new RepositoryDto(104L, "frontend-blast-visualizer", owner + "/frontend-blast-visualizer", owner, true,
                        "SVG and D3 interactive dependency visualizer", "main", "JavaScript", "2026-10-14T20:45:00Z", "https://github.com/" + owner + "/frontend-blast-visualizer"),
                new RepositoryDto(105L, "supply-chain-scanner-cli", owner + "/supply-chain-scanner-cli", owner, false,
                        "CLI tool for maven manifest dependency vulnerability checks", "main", "Python", "2026-10-10T14:20:00Z", "https://github.com/" + owner + "/supply-chain-scanner-cli")
        );
    }
}
