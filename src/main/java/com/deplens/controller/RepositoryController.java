package com.deplens.controller;

import com.deplens.dto.RepositoryDto;
import com.deplens.model.User;
import com.deplens.repository.UserRepository;
import com.deplens.security.UserPrincipal;
import com.deplens.service.GitHubService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Controller
public class RepositoryController {

    private final GitHubService gitHubService;
    private final UserRepository userRepository;

    public RepositoryController(GitHubService gitHubService, UserRepository userRepository) {
        this.gitHubService = gitHubService;
        this.userRepository = userRepository;
    }

    /**
     * API endpoint returning repositories for the logged-in user
     */
    @GetMapping("/api/github/repos")
    @ResponseBody
    public ResponseEntity<List<RepositoryDto>> getUserRepositories(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        Long userId = Objects.requireNonNull(userPrincipal.getId(), "User id must not be null");
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        List<RepositoryDto> repos = gitHubService.getUserRepositories(user);
        return ResponseEntity.ok(repos);
    }

    /**
     * API endpoint to get details of a specific repository with STRICT ownership & permission verification.
     * Only the repository owner or an authorized collaborator can access this endpoint.
     */
    @GetMapping("/api/github/repos/{owner}/{name}")
    @ResponseBody
    public ResponseEntity<?> getRepositoryDetails(
            @PathVariable String owner,
            @PathVariable String name,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null || userPrincipal.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "You must be signed in to access this repository."));
        }

        Long userId = Objects.requireNonNull(userPrincipal.getId(), "User id must not be null");
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "User session not found."));
        }

        // Strictly verify that the current user owns or has collaborator permissions on this repository
        Optional<RepositoryDto> repoOpt = gitHubService.getRepositoryDetails(user, owner, name);
        if (repoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Forbidden",
                    "message", "Access denied. Only the repository owner (@" + owner + ") or authorized collaborators have permission to view supply chain intelligence for this repository.",
                    "currentUser", user.getUsername(),
                    "requiredOwner", owner,
                    "targetRepo", name
            ));
        }

        return ResponseEntity.ok(repoOpt.get());
    }

    /**
     * Clean slug redirect: navigates /repo/{owner}/{name} to /repo.html?owner={owner}&repo={name}
     */
    @GetMapping("/repo/{owner}/{name}")
    public String viewRepositorySlug(
            @PathVariable String owner,
            @PathVariable String name
    ) {
        return "forward:/repo.html";
    }
}
