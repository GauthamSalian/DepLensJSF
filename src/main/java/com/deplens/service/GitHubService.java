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
                HttpHeaders headers = createAuthHeaders(token);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

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
     * Verifies repository ownership/permission and enriches the repository with:
     * - Latest commit message, author, date, and SHA
     * - Detected manifest files (package.json, package-lock.json, requirements.txt, pom.xml, etc.)
     */
    public Optional<RepositoryDto> getRepositoryDetails(User user, String owner, String repoName) {
        String token = user.getGithubAccessToken();

        // 1. If real GitHub OAuth token is present, query GitHub API
        if (token != null && !token.isBlank()) {
            try {
                HttpHeaders headers = createAuthHeaders(token);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                // Fetch basic repository info
                String repoUrl = "https://api.github.com/repos/" + owner + "/" + repoName;
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        repoUrl,
                        HttpMethod.valueOf("GET"),
                        entity,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                Map<String, Object> body = response.getBody();
                if (body != null) {
                    RepositoryDto dto = mapToDto(body);

                    // Check ownership or collaborator push/admin access
                    boolean isOwner = owner.equalsIgnoreCase(user.getUsername());
                    Object permissionsObj = body.get("permissions");
                    boolean hasPushOrAdmin = false;
                    if (permissionsObj instanceof Map<?, ?> permissions) {
                        hasPushOrAdmin = Boolean.TRUE.equals(permissions.get("push"))
                                || Boolean.TRUE.equals(permissions.get("admin"));
                    }

                    if (!isOwner && !hasPushOrAdmin) {
                        log.warn("User {} denied access to repo {}/{}: not owner and lacks push/admin permissions",
                                user.getUsername(), owner, repoName);
                        return Optional.empty();
                    }

                    // Enrich with latest commit metadata
                    enrichWithLatestCommit(dto, owner, repoName, entity);

                    // Enrich with detected dependency manifests (package.json, requirements.txt, pom.xml, etc.)
                    enrichWithManifests(dto, owner, repoName, entity);

                    return Optional.of(dto);
                }
            } catch (Exception ex) {
                log.warn("Access check failed for user {} on repo {}/{}: {}",
                        user.getUsername(), owner, repoName, ex.getMessage());
                return Optional.empty();
            }
        }

        // 2. Fallback for mock/dev session: strictly verify username matches repository owner
        if (user.getUsername() != null && user.getUsername().equalsIgnoreCase(owner)) {
            RepositoryDto mock = new RepositoryDto(
                    101L, repoName, owner + "/" + repoName, owner, false,
                    "Repository managed by @" + owner, "main", "Java", "2026-10-17T20:00:00Z",
                    "https://github.com/" + owner + "/" + repoName
            );
            mock.setLatestCommitMessage("feat: implement supply chain intelligence scanner");
            mock.setLatestCommitSha("e7f2b1a");
            mock.setLatestCommitAuthor(owner);
            mock.setLatestCommitDate("2026-10-17T19:30:00Z");
            mock.setLatestCommitUrl("https://github.com/" + owner + "/" + repoName + "/commit/e7f2b1a");
            mock.setDetectedManifests(List.of("package.json", "package-lock.json", "pom.xml"));
            mock.setEcosystems(List.of("npm (Node.js)", "Maven (Java)"));
            return Optional.of(mock);
        }

        return Optional.empty();
    }

    /**
     * Fetches the latest commit message, author, date, and SHA from GitHub
     */
    private void enrichWithLatestCommit(RepositoryDto dto, String owner, String repoName, HttpEntity<Void> entity) {
        try {
            String commitsUrl = "https://api.github.com/repos/" + owner + "/" + repoName + "/commits?per_page=1";
            ResponseEntity<List<Map<String, Object>>> commitsRes = restTemplate.exchange(
                    commitsUrl,
                    HttpMethod.valueOf("GET"),
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> commits = commitsRes.getBody();
            if (commits != null && !commits.isEmpty()) {
                Map<String, Object> latestCommitObj = commits.get(0);
                String sha = (String) latestCommitObj.get("sha");
                if (sha != null && sha.length() >= 7) {
                    dto.setLatestCommitSha(sha.substring(0, 7));
                }
                dto.setLatestCommitUrl((String) latestCommitObj.get("html_url"));

                Object commitDetails = latestCommitObj.get("commit");
                if (commitDetails instanceof Map<?, ?> commitMap) {
                    dto.setLatestCommitMessage((String) commitMap.get("message"));

                    Object authorObj = commitMap.get("author");
                    if (authorObj instanceof Map<?, ?> authorMap) {
                        dto.setLatestCommitAuthor((String) authorMap.get("name"));
                        dto.setLatestCommitDate((String) authorMap.get("date"));
                    }

                    // Fallback to committer if author date or name is null
                    if (dto.getLatestCommitDate() == null || dto.getLatestCommitDate().isBlank()) {
                        Object committerObj = commitMap.get("committer");
                        if (committerObj instanceof Map<?, ?> committerMap) {
                            if (dto.getLatestCommitAuthor() == null) {
                                dto.setLatestCommitAuthor((String) committerMap.get("name"));
                            }
                            dto.setLatestCommitDate((String) committerMap.get("date"));
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not fetch latest commit for {}/{}: {}", owner, repoName, ex.getMessage());
        }
    }

    /**
     * Inspects repository root files to detect package.json, package-lock.json, requirements.txt, pom.xml, etc.
     */
    private void enrichWithManifests(RepositoryDto dto, String owner, String repoName, HttpEntity<Void> entity) {
        try {
            String contentsUrl = "https://api.github.com/repos/" + owner + "/" + repoName + "/contents";
            ResponseEntity<List<Map<String, Object>>> contentsRes = restTemplate.exchange(
                    contentsUrl,
                    HttpMethod.valueOf("GET"),
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> files = contentsRes.getBody();
            if (files != null) {
                Set<String> fileNames = new HashSet<>();
                for (Map<String, Object> file : files) {
                    Object name = file.get("name");
                    if (name instanceof String str) {
                        fileNames.add(str.toLowerCase());
                    }
                }

                List<String> manifests = new ArrayList<>();
                Set<String> ecosystems = new LinkedHashSet<>();

                // npm / Node.js
                if (fileNames.contains("package.json")) {
                    manifests.add("package.json");
                    ecosystems.add("npm (Node.js)");
                }
                if (fileNames.contains("package-lock.json")) {
                    manifests.add("package-lock.json");
                    ecosystems.add("npm (Resolved Lockfile)");
                }
                if (fileNames.contains("yarn.lock")) {
                    manifests.add("yarn.lock");
                    ecosystems.add("Yarn (Lockfile)");
                }
                if (fileNames.contains("pnpm-lock.yaml")) {
                    manifests.add("pnpm-lock.yaml");
                    ecosystems.add("pnpm (Lockfile)");
                }

                // Python
                for (String fn : fileNames) {
                    if (fn.startsWith("requirements") && fn.endsWith(".txt")) {
                        manifests.add(fn);
                        ecosystems.add("pip (Python)");
                    }
                }
                if (fileNames.contains("pyproject.toml")) {
                    manifests.add("pyproject.toml");
                    ecosystems.add("Poetry / Flit (Python)");
                }
                if (fileNames.contains("pipfile")) {
                    manifests.add("Pipfile");
                    ecosystems.add("Pipenv (Python)");
                }
                if (fileNames.contains("pipfile.lock")) {
                    manifests.add("Pipfile.lock");
                    ecosystems.add("Pipenv (Lockfile)");
                }
                if (fileNames.contains("setup.py")) {
                    manifests.add("setup.py");
                    ecosystems.add("Python (setuptools)");
                }

                // Java
                if (fileNames.contains("pom.xml")) {
                    manifests.add("pom.xml");
                    ecosystems.add("Maven (Java)");
                }
                if (fileNames.contains("build.gradle") || fileNames.contains("build.gradle.kts")) {
                    manifests.add("build.gradle");
                    ecosystems.add("Gradle (Java / Kotlin)");
                }

                // Go
                if (fileNames.contains("go.mod")) {
                    manifests.add("go.mod");
                    ecosystems.add("Go Modules");
                }

                dto.setDetectedManifests(manifests);
                dto.setEcosystems(new ArrayList<>(ecosystems));
            }
        } catch (Exception ex) {
            log.warn("Could not inspect manifests for {}/{}: {}", owner, repoName, ex.getMessage());
        }
    }

    /**
     * Retrieves the raw decoded text content of a file from GitHub repository
     */
    public Optional<String> getFileContent(User user, String owner, String repoName, String filePath) {
        String token = user != null ? user.getGithubAccessToken() : null;
        if (token != null && !token.isBlank()) {
            try {
                HttpHeaders headers = createAuthHeaders(token);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                String url = "https://api.github.com/repos/" + owner + "/" + repoName + "/contents/" + filePath;
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url,
                        HttpMethod.valueOf("GET"),
                        entity,
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );

                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("content")) {
                    String base64Content = (String) body.get("content");
                    if (base64Content != null) {
                        String cleaned = base64Content.replace("\n", "").replace("\r", "");
                        byte[] decoded = Base64.getDecoder().decode(cleaned);
                        return Optional.of(new String(decoded, java.nio.charset.StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not fetch file content for {}/{}/{}: {}", owner, repoName, filePath, ex.getMessage());
            }
        }
        return Optional.empty();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        headers.set("User-Agent", "DepLens-Engine");
        return headers;
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
