package com.deplens.repository;

import com.deplens.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGithubId(String githubId);
    Optional<User> findFirstByUsernameIgnoreCase(String username);
    Optional<User> findFirstByEmailIgnoreCase(String email);
}
