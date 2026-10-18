package com.deplens.repository;

import com.deplens.model.DependencyGraphEntity;
import com.deplens.model.RepositoryStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DependencyGraphRepository extends JpaRepository<DependencyGraphEntity, Long> {
    Optional<DependencyGraphEntity> findByState(RepositoryStateEntity state);
}
